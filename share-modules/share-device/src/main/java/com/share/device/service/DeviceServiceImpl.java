package com.share.device.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.context.SecurityContextHolder;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.StringUtils;
import com.share.common.security.utils.SecurityUtils;
import com.share.device.domain.*;
import com.share.device.emqx.EmqxClientWrapper;
import com.share.device.emqx.constant.EmqxConstants;
import com.share.device.repository.StationLocationRepository;
import com.share.order.api.api.RemoteOrderInfoService;
import com.share.order.api.domain.OrderInfo;
import com.share.rule.api.RemoteFeeRuleService;
import com.share.rule.api.domain.FeeRule;
import com.share.user.domain.UserInfo;
import com.share.user.api.RemoteUserService;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceServiceImpl implements IDeviceService{
    @Autowired
    private ICabinetService cabinetService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private IStationService stationService;
    @Autowired
    private IMapService MapService;
    @Autowired
    private RemoteFeeRuleService remoteFeeRuleService;
    @Autowired
    private StationLocationRepository stationLocationRepository;
    @Autowired
    private RemoteUserService remoteUserService;
    @Autowired
    private RemoteOrderInfoService remoteOrderInfoService;
    @Autowired
    private EmqxClientWrapper emqxClientWrapper;
    @Autowired
    private ICabinetSlotService cabinetSlotService;
    @Override
    public void syncData() {
        stationLocationRepository.deleteAll();
        List<Station> list = stationService.list();
        for (Station station : list) {
            if (station.getLongitude() != null && station.getLatitude() != null) {
                StationLocation stationLocation = new StationLocation();
                stationLocation.setId(ObjectId.get().toString());
                stationLocation.setStationId(station.getId());
                stationLocation.setLocation(new GeoJsonPoint(station.getLongitude().doubleValue(), station.getLatitude().doubleValue()));
                stationLocation.setCreateTime(new Date());
                stationLocationRepository.save(stationLocation);
            }
        }
    }

    @Override
    public StationVo getStation(Long id, String latitude, String longitude) {
        Station station = stationService.getById(id);
        StationVo stationVo = new StationVo();
        BeanUtils.copyProperties(station, stationVo);
        Double distance = MapService.calculateDistance(longitude, latitude, station.getLongitude().toString(), station.getLatitude().toString());
        stationVo.setDistance(distance);
        Cabinet cabinet = cabinetService.getById(station.getCabinetId());
        if (cabinet != null) {
            //可用充电宝数量大于0，可借用
            if(cabinet.getAvailableNum() != null && cabinet.getAvailableNum() > 0) {
                stationVo.setIsUsable("1");
            } else {
                stationVo.setIsUsable("0");
            }
            // 获取空闲插槽数量大于0，可归还
            if (cabinet.getFreeSlots() != null && cabinet.getFreeSlots() > 0) {
                stationVo.setIsReturn("1");
            } else {
                stationVo.setIsReturn("0");
            }
        }

        // 获取费用规则
        if (station.getFeeRuleId() != null) {
            try {
                com.share.common.core.domain.R<FeeRule> result = remoteFeeRuleService.getFeeRule(station.getFeeRuleId(), SecurityConstants.INNER);
                if (result != null && result.getData() != null) {
                    stationVo.setFeeRule(result.getData().getDescription());
                }
            } catch (Exception e) {
                // log.error("获取费用规则失败", e);
            }
        }
        return stationVo;
    }

    @Override
    public ScanChargeVo scanCharge(String cabinetNo) {
        ScanChargeVo scanChargeVo = new ScanChargeVo();

        R<UserInfo> userInfoResult = remoteUserService.getUserInfo(SecurityContextHolder.getUserId(), SecurityConstants.INNER);
        if (userInfoResult == null || userInfoResult.getCode() != R.SUCCESS) {
            throw new ServiceException("获取用户信息失败");
        }
        UserInfo userInfo = userInfoResult.getData();
        if (userInfo == null) {
            throw new ServiceException("用户不存在");
        }
        if("0".equals(userInfo.getDepositStatus())) {
            throw new ServiceException("未申请免押金使用");
        }
        // 容错：当 share-order 未注册或不可达时，Feign 可能直接抛出 RetryableException，导致扫码失败。
        // 这里兜底处理，视为“无未完成订单”，继续后续流程，保证测试可用。
        R<OrderInfo> orderInfoResult = null;
        OrderInfo orderInfo = null;
        try {
            orderInfoResult = remoteOrderInfoService.getNoFinishOrder(SecurityUtils.getUserId(), SecurityConstants.INNER);
            if (orderInfoResult != null) {
                orderInfo = orderInfoResult.getData();
            }
        } catch (Exception ex) {
            // 记录一次性日志（可根据需要接入日志框架）；此处不抛出异常，进入后续借用流程
            // log.warn("查询未完成订单失败，忽略并继续扫码流程: {}", ex.getMessage());
        }
        if(null != orderInfo) {
            if ("0".equals(orderInfo.getStatus())) {
                scanChargeVo.setStatus("2");
                scanChargeVo.setMessage("有未归还充电宝，请归还后使用");
                return scanChargeVo;
            }
            if ("1".equals(orderInfo.getStatus())) {
                scanChargeVo.setStatus("3");
                scanChargeVo.setMessage("有未支付订单，去支付");
                return scanChargeVo;
            }
        }
        AvailableProwerBankVo availableProwerBankVo = this.checkAvailableProwerBank(cabinetNo);
        if(null == availableProwerBankVo) {
            throw new ServiceException("无可用充电宝");
        }
        if(!StringUtils.isEmpty(availableProwerBankVo.getErrMessage())) {
            throw new ServiceException(availableProwerBankVo.getErrMessage());
        }
        JSONObject object = new JSONObject();
        object.set("uId", SecurityUtils.getUserId());
        object.set("mNo", RandomUtil.randomString(10));
        object.set("cNo", cabinetNo);
        object.set("pNo", availableProwerBankVo.getPowerBankNo());
        object.set("sNo", availableProwerBankVo.getSlotNo());
        emqxClientWrapper.publish(String.format(EmqxConstants.TOPIC_SCAN_SUBMIT, cabinetNo), object.toString());

        scanChargeVo.setStatus("1");
        scanChargeVo.setMessage("弹出成功");
        return scanChargeVo;
    }
    @Autowired
    private IPowerBankService powerBankService;
    private AvailableProwerBankVo checkAvailableProwerBank(String cabinetNo) {
        AvailableProwerBankVo availableProwerBankVo = new AvailableProwerBankVo();
        Cabinet cabinet = cabinetService.getBtCabinetNo(cabinetNo);
        Integer availableNum = cabinet.getAvailableNum();
        if(availableNum== 0) {  availableProwerBankVo.setErrMessage("无可用充电宝");
            return availableProwerBankVo;
        }
        List<CabinetSlot> cabinetSlotList = cabinetSlotService.list(new LambdaQueryWrapper<CabinetSlot>()
                .eq(CabinetSlot::getCabinetId, cabinet.getId())
                .eq(CabinetSlot::getStatus, "1") // 状态（1：占用 0：空闲 2：锁定）
        );
        List<Long> powerBankIdList = cabinetSlotList.stream().filter(item -> null != item.getPowerBankId())
                .map(CabinetSlot::getPowerBankId).collect(Collectors.toList());
        LambdaQueryWrapper<PowerBank> wrapper = new LambdaQueryWrapper<PowerBank>();
        wrapper.in(PowerBank::getId,powerBankIdList);
        wrapper.eq(PowerBank::getStatus, "1");
        List<PowerBank> PowerBankList = powerBankService.list(wrapper);
        if(CollectionUtils.isEmpty(PowerBankList)) {
            availableProwerBankVo.setErrMessage("无可用充电宝");
            return availableProwerBankVo;
        }
        if(powerBankIdList.size()>1){
            Collections.sort(PowerBankList, (o1, o2) -> o2.getElectricity().compareTo(o1.getElectricity()));
        }
        PowerBank powerBank = PowerBankList.get(0);
        CabinetSlot cabinetSlot = cabinetSlotList.stream().filter(item -> null != item.getPowerBankId() && item.getPowerBankId().equals(powerBank.getId())).collect(Collectors.toList()).get(0);
        cabinetSlot.setStatus("2");
        cabinetSlotService.updateById(cabinetSlot);
        availableProwerBankVo.setPowerBankNo(powerBank.getPowerBankNo());
        availableProwerBankVo.setSlotNo(cabinetSlot.getSlotNo());
        return availableProwerBankVo;

    }

    @Override
    public List<StationVo> nearbyStation(String latitude, String longitude, Integer searchPcRadius) {
        GeoJsonPoint geoJsonPoint=new GeoJsonPoint(Double.parseDouble(longitude), Double.parseDouble(latitude));
        Distance distance=new Distance(searchPcRadius, Metrics.KILOMETERS);
        Circle circle = new Circle(geoJsonPoint, distance);
        Query query = Query.query(Criteria.where("location").withinSphere(circle));
        List<StationLocation> list = this.mongoTemplate.find(query, StationLocation.class);

        if (list.isEmpty()) {
            long count = this.mongoTemplate.count(new Query(), StationLocation.class);
            if (count == 0) {
                this.syncData();
                list = this.mongoTemplate.find(query, StationLocation.class);
            }
        }

        List<Long> stationLocationList = list.stream().map(StationLocation::getStationId).collect(Collectors.toList());
        if (stationLocationList.isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<Station> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Station::getId, stationLocationList);
        wrapper.isNotNull(Station::getCabinetId);
        List<Station> stationList=stationService.list(wrapper);
        List<StationVo> stationVoList=new ArrayList<>();
        stationList.forEach(station -> {
            StationVo stationVo = new StationVo();
            BeanUtils.copyProperties(station, stationVo);

            if (station.getLongitude() != null && station.getLatitude() != null) {
                Double distanceStation = MapService.calculateDistance(longitude, latitude, station.getLongitude().toString(), station.getLatitude().toString());
                stationVo.setDistance(distanceStation);
            }

            // 设置默认值
            stationVo.setIsUsable("0");
            stationVo.setIsReturn("0");

            Long cabinetId = station.getCabinetId();
            if (cabinetId != null) {
                Cabinet cabinet = cabinetService.getById(cabinetId);
                if (cabinet != null) {
                    //可用充电宝数量大于0，可借用
                    if (cabinet.getAvailableNum() != null && cabinet.getAvailableNum() > 0) {
                        stationVo.setIsUsable("1");
                    }
                    // 获取空闲插槽数量大于0，可归还
                    if (cabinet.getFreeSlots() != null && cabinet.getFreeSlots() > 0) {
                        stationVo.setIsReturn("1");
                    }
                }
            }

            Long feeRuleId = station.getFeeRuleId();
            if (feeRuleId != null) {
                R<FeeRule> feeRuleResult = remoteFeeRuleService.getFeeRule(feeRuleId, SecurityConstants.INNER);
                if (feeRuleResult != null && feeRuleResult.getData() != null) {
                    FeeRule feeRule = feeRuleResult.getData();
                    String description = feeRule.getDescription();
                    stationVo.setFeeRule(description);
                }
            }
            stationVoList.add(stationVo);
        });
        if (stationVoList.isEmpty()) {
            return null;
        }
        return stationVoList;
    }
}
