package com.share.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.order.api.api.RemoteOrderInfoService;
import com.share.order.api.domain.OrderInfo;
import com.share.payment.domain.PaymentInfo;
import com.share.payment.mapper.PaymentInfoMapper;
import com.share.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RemoteOrderInfoService remoteOrderInfoService;

    @Override
    public PaymentInfo savePaymentInfo(String orderNo) {
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>()
                .eq(PaymentInfo::getOrderNo, orderNo));
        if (paymentInfo != null) {
            if (paymentInfo.getPaymentStatus() != null && paymentInfo.getPaymentStatus() == -1) {
                throw new ServiceException("订单已关闭");
            }
            return paymentInfo;
        }

        R<OrderInfo> orderInfoResult = remoteOrderInfoService.getByOrderNo(orderNo, SecurityConstants.INNER);
        if (orderInfoResult == null || orderInfoResult.getCode() != R.SUCCESS || orderInfoResult.getData() == null) {
            String msg = orderInfoResult != null ? orderInfoResult.getMsg() : "获取订单信息失败";
            throw new ServiceException(msg);
        }

        OrderInfo orderInfo = orderInfoResult.getData();
        paymentInfo = new PaymentInfo();
        paymentInfo.setUserId(orderInfo.getUserId());
        paymentInfo.setContent("共享充电宝租借");
        paymentInfo.setAmount(orderInfo.getTotalAmount() == null ? BigDecimal.ZERO : orderInfo.getTotalAmount());
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPaymentStatus(0);
        paymentInfo.setPayWay(1);
        paymentInfoMapper.insert(paymentInfo);
        return paymentInfo;
    }

    @Override
    public void markPaySuccess(PaymentInfo paymentInfo, String transactionId, String callbackContent) {
        paymentInfo.setPaymentStatus(1);
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setCallbackContent(callbackContent);
        paymentInfo.setCallbackTime(new Date());
        updateById(paymentInfo);
    }
}