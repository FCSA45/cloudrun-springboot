package com.share.order.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.StringUtils;
import com.share.common.security.utils.SecurityUtils;
import com.share.order.api.domain.EndOrderVo;
import com.share.order.api.domain.OrderBill;
import com.share.order.api.domain.OrderInfo;
import com.share.order.api.domain.SubmitOrderVo;
import com.share.order.api.domain.UserInfoVo;
import com.share.order.mapper.OrderBillMapper;
import com.share.order.mapper.OrderInfoMapper;
import com.share.order.service.IOrderInfoService;
import com.share.rule.api.RemoteFeeRuleService;
import com.share.rule.api.domain.FeeRule;
import com.share.rule.api.domain.FeeRuleRequestForm;
import com.share.rule.api.domain.FeeRuleResponseVo;
import com.share.user.api.RemoteUserService;
import com.share.user.domain.UserInfo;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements IOrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderBillMapper orderBillMapper;

    @Autowired
    private RemoteFeeRuleService remoteFeeRuleService;

    @Autowired
    private RemoteUserService remoteUserInfoService;

    @Override
    public OrderInfo getNoFinishOrder(Long userId) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<OrderInfo>();
        queryWrapper.eq(OrderInfo::getUserId,userId);
        queryWrapper.in(OrderInfo::getStatus, Arrays.asList("0", "1"));
        queryWrapper.orderByDesc(OrderInfo::getId);
        queryWrapper.last("limit 1");
        OrderInfo orderInfo = baseMapper.selectOne(queryWrapper);
        return orderInfo;
    }

    @Override
    public Long saveOrder(SubmitOrderVo orderForm) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setUserId(orderForm.getUserId());
        orderInfo.setOrderNo(RandomUtil.randomString(8));
        orderInfo.setPowerBankNo(orderForm.getPowerBankNo());
        orderInfo.setStartTime(new Date());
        orderInfo.setStartStationId(orderForm.getStartStationId());
        orderInfo.setStartStationName(orderForm.getStartStationName());
        orderInfo.setStartCabinetNo(orderForm.getStartCabinetNo());
        // 费用规则
        R<FeeRule> feeRuleResult = remoteFeeRuleService.getFeeRule(orderForm.getFeeRuleId(), SecurityConstants.INNER);
        if (feeRuleResult == null || feeRuleResult.getCode() != R.SUCCESS || feeRuleResult.getData() == null) {
            throw new ServiceException("获取费用规则失败: " + (feeRuleResult != null ? feeRuleResult.getMsg() : "远程调用无响应"));
        }
        FeeRule feeRule = feeRuleResult.getData();
        orderInfo.setFeeRuleId(orderForm.getFeeRuleId());
        orderInfo.setFeeRule(feeRule.getDescription());
        orderInfo.setStatus("0");
        orderInfo.setCreateTime(new Date());
        orderInfo.setCreateBy(SecurityUtils.getUsername());
        //用户昵称
        R<UserInfo> userInfoResult = remoteUserInfoService.getUserInfo(orderInfo.getUserId(), SecurityConstants.INNER);
        if (userInfoResult == null || userInfoResult.getCode() != R.SUCCESS || userInfoResult.getData() == null) {
            throw new ServiceException("获取用户信息失败: " + (userInfoResult != null ? userInfoResult.getMsg() : "远程调用无响应"));
        }
        UserInfo userInfo = userInfoResult.getData();
        orderInfo.setNickname(userInfo.getNickname());

        orderInfoMapper.insert(orderInfo);
        return orderInfo.getId();
    }

    @Override
    public void endOrder(EndOrderVo endOrderVo) {
        // 获取充电中的订单，如果存在，则结束订单； 如果不存在，则返回（初始化插入，无订单）
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getPowerBankNo, endOrderVo.getPowerBankNo())
                .eq(OrderInfo::getStatus, "0") //订单状态：0:充电中
                .orderByDesc(OrderInfo::getCreateTime)
                .last("limit 1")
        );
        if (orderInfo == null) {
            return;
        }

        orderInfo.setEndTime(endOrderVo.getEndTime());
        orderInfo.setEndStationId(endOrderVo.getEndStationId());
        orderInfo.setEndStationName(endOrderVo.getEndStationName());
        orderInfo.setEndCabinetNo(endOrderVo.getEndCabinetNo());
        int duration = Minutes.minutesBetween(new DateTime(orderInfo.getStartTime()), new DateTime(orderInfo.getEndTime())).getMinutes();
        orderInfo.setDuration(duration);

        // 费用计算
        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        feeRuleRequestForm.setDuration(duration);
        feeRuleRequestForm.setFeeRuleId(orderInfo.getFeeRuleId());
        R<FeeRuleResponseVo> feeRuleResponseVoResult = remoteFeeRuleService.calculateOrderFee(feeRuleRequestForm, SecurityConstants.INNER);
        if (R.FAIL == feeRuleResponseVoResult.getCode()) {
            throw new ServiceException(feeRuleResponseVoResult.getMsg());
        }
        FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();

        // 设置订单金额
        orderInfo.setTotalAmount(feeRuleResponseVo.getTotalAmount());
        orderInfo.setDeductAmount(new BigDecimal(0));
        orderInfo.setRealAmount(feeRuleResponseVo.getTotalAmount());
        if(orderInfo.getRealAmount().subtract(new BigDecimal(0)).doubleValue() == 0) {
            orderInfo.setStatus("2");
        } else {
            orderInfo.setStatus("1");
        }
        orderInfoMapper.updateById(orderInfo);

        // 插入免费订单账单
        OrderBill freeOrderBill = new OrderBill();
        freeOrderBill.setOrderId(orderInfo.getId());
        freeOrderBill.setBillItem(feeRuleResponseVo.getFreeDescription());
        freeOrderBill.setBillAmount(new BigDecimal(0));
        orderBillMapper.insert(freeOrderBill);

        // 插入超出免费订单账单
        if (feeRuleResponseVo.getExceedPrice().doubleValue() > 0) {
            OrderBill exceedOrderBill = new OrderBill();
            exceedOrderBill.setOrderId(orderInfo.getId());
            exceedOrderBill.setBillItem(feeRuleResponseVo.getExceedDescription());
            exceedOrderBill.setBillAmount(feeRuleResponseVo.getExceedPrice());
            orderBillMapper.insert(exceedOrderBill);
        }




    }

    @Override
    public List<OrderInfo> selectOrderListByUserId(Long userId) {
        List<OrderInfo> orderInfoList = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, userId)
                .orderByDesc(OrderInfo::getId)
        );
        if (!CollectionUtils.isEmpty(orderInfoList)) {
            for (OrderInfo orderInfo : orderInfoList) {
                //充电中实时计算使用时间与金额
                if ("0".equals(orderInfo.getStatus())) {
                    //充电中实时计算使用时间
                    int duration = Minutes.minutesBetween(new DateTime(orderInfo.getStartTime()), new DateTime()).getMinutes();
                    if (duration > 0) {
                        orderInfo.setDuration(duration);

                        // 费用计算
                        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
                        feeRuleRequestForm.setDuration(duration);
                        feeRuleRequestForm.setFeeRuleId(orderInfo.getFeeRuleId());
                        R<FeeRuleResponseVo> feeRuleResponseVoResult = remoteFeeRuleService.calculateOrderFee(feeRuleRequestForm, SecurityConstants.INNER);
                        if (R.FAIL == feeRuleResponseVoResult.getCode()) {
                            throw new ServiceException(feeRuleResponseVoResult.getMsg());
                        }
                        FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();

                        // 设置订单金额
                        orderInfo.setTotalAmount(feeRuleResponseVo.getTotalAmount());
                        orderInfo.setDeductAmount(new BigDecimal(0));
                        orderInfo.setRealAmount(feeRuleResponseVo.getTotalAmount());
                    } else {
                        orderInfo.setDuration(0);
                        orderInfo.setTotalAmount(new BigDecimal(0));
                        orderInfo.setDeductAmount(new BigDecimal(0));
                        orderInfo.setRealAmount(new BigDecimal(0));
                    }

                }
            }
        }
        return orderInfoList;
    }

    @Override
    public OrderInfo selectOrderInfoById(Long id) {
        OrderInfo orderInfo = orderInfoMapper.selectById(id);

        //充电中实时计算使用时间与金额
        if ("0".equals(orderInfo.getStatus())) {
            //充电中实时计算使用时间
            int duration = Minutes.minutesBetween(new DateTime(orderInfo.getStartTime()), new DateTime()).getMinutes();
            if (duration > 0) {
                orderInfo.setDuration(duration);

                // 费用计算
                FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
                feeRuleRequestForm.setDuration(duration);
                feeRuleRequestForm.setFeeRuleId(orderInfo.getFeeRuleId());
                R<FeeRuleResponseVo> feeRuleResponseVoResult = remoteFeeRuleService.calculateOrderFee(feeRuleRequestForm, SecurityConstants.INNER);
                if (R.FAIL == feeRuleResponseVoResult.getCode()) {
                    throw new ServiceException(feeRuleResponseVoResult.getMsg());
                }
                FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();

                // 设置订单金额
                orderInfo.setTotalAmount(feeRuleResponseVo.getTotalAmount());
                orderInfo.setDeductAmount(new BigDecimal(0));
                orderInfo.setRealAmount(feeRuleResponseVo.getTotalAmount());
            } else {
                orderInfo.setDuration(0);
                orderInfo.setTotalAmount(new BigDecimal(0));
                orderInfo.setDeductAmount(new BigDecimal(0));
                orderInfo.setRealAmount(new BigDecimal(0));
            }
        }

        List<OrderBill> orderBillList = orderBillMapper.selectList(new LambdaQueryWrapper<OrderBill>().eq(OrderBill::getOrderId, id));
        orderInfo.setOrderBillList(orderBillList);

        R<UserInfo> userInfoResult = remoteUserInfoService.getUserInfo(orderInfo.getUserId(), SecurityConstants.INNER);
        if (StringUtils.isNull(userInfoResult) || StringUtils.isNull(userInfoResult.getData())) {
            throw new ServiceException("获取用户信息失败");
        }
        if (R.FAIL == userInfoResult.getCode()) {
            throw new ServiceException(userInfoResult.getMsg());
        }
        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(userInfoResult.getData(), userInfoVo);
        orderInfo.setUserInfoVo(userInfoVo);
        return orderInfo;
    }

    @Override
    public OrderInfo getByOrderNo(String orderNo) {
        return orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
    }

    @Override
    public void processPaySucess(String orderNo) {
        //获取订单信息
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo).select(OrderInfo::getId, OrderInfo::getStatus));
        //未支付
        if ("1".equals(orderInfo.getStatus())) {
            orderInfo.setStatus("2");
            orderInfo.setPayTime(new Date());
            orderInfoMapper.updateById(orderInfo);
        }
    }

    @Override
    public Map<String, Object> getOrderCount(String sql) {
        List<Map<String, Object>> list = baseMapper.getOrderCount(sql);

        Map dataMap = new HashMap<>();

        List<Object> monthList = new ArrayList<>();
        List<Object> orderCountList = new ArrayList<>();

        for (Map<String, Object> map : list) {
            monthList.add(map.get("order_date"));
            orderCountList.add(map.get("order_count"));
        }

        dataMap.put("dateList", monthList);
        dataMap.put("countList", orderCountList);

        return dataMap;
    }
}
