package com.share.device.emqx.handler.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.common.core.constant.DeviceConstants;
import com.share.common.core.utils.StringUtils;
import com.share.common.rabbit.constant.MqConst;
import com.share.common.rabbit.service.RabbitService;
import com.share.device.domain.Cabinet;
import com.share.device.domain.CabinetSlot;
import com.share.device.domain.PowerBank;
import com.share.device.domain.Station;
import com.share.device.emqx.annotation.GuiguEmqx;
import com.share.device.emqx.constant.EmqxConstants;
import com.share.device.emqx.handler.MassageHandler;
import com.share.device.service.ICabinetService;
import com.share.device.service.ICabinetSlotService;
import com.share.device.service.IPowerBankService;
import com.share.device.service.IStationService;
import com.share.order.api.domain.EndOrderVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@GuiguEmqx(topic = EmqxConstants.TOPIC_POWERBANK_CONNECTED)
public class PowerBankConnectedHandler implements MassageHandler {
    @Autowired
    private ICabinetService cabinetService;

    @Autowired
    private IPowerBankService powerBankService;

    @Autowired
    private ICabinetSlotService cabinetSlotService;

    @Autowired
    private IStationService stationService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RabbitService rabbitService;
    @Override
    public void handleMessage(JSONObject message) {
        log.info("收到归还消息: {}", message.toString());
        String messageNo = message.getStr("mNo");
        String key = "powerBank:connected:" + messageNo;
        Boolean IfAbsent = redisTemplate.opsForValue().setIfAbsent(key, messageNo, 1, TimeUnit.HOURS);
   if(!IfAbsent){
       log.info("归还消息重复, 忽略: {}", messageNo);
       return;
   }
        //柜机编号
        String cabinetNo = message.getStr("cNo");
        //充电宝编号
        String powerBankNo = message.getStr("pNo");
        //插槽编号
        String slotNo = message.getStr("sNo");
        //当前电量
        BigDecimal electricity = message.getBigDecimal("ety");
        if (StringUtils.isEmpty(cabinetNo)
                || StringUtils.isEmpty(powerBankNo)
                || StringUtils.isEmpty(slotNo)
                || null == electricity) {
            log.info("参数为空: {}", message.toString());
            return;
        }
        //获取柜机
        Cabinet cabinet = cabinetService.getBtCabinetNo(cabinetNo);
        // 获取充电宝
        PowerBank powerBank = powerBankService.getByPowerBankNo(powerBankNo);
        // 获取插槽
        CabinetSlot cabinetSlot = cabinetSlotService.getBtSlotNo(cabinet.getId(), slotNo);
        powerBank.setElectricity(electricity);
        if (electricity.subtract(DeviceConstants.ELECTRICITY_MIN).doubleValue() > 0) {
            //可以借用
            powerBank.setStatus("1");
        } else {
            //充电中
            powerBank.setStatus("3");
        }
        powerBankService.updateById(powerBank);
        cabinetSlot.setPowerBankId(powerBank.getId());
        cabinetSlot.setStatus("1");
        cabinetSlot.setUpdateTime(new Date());
        cabinetSlotService.updateById(cabinetSlot);
        int freeSlots = cabinet.getFreeSlots() - 1;
        cabinet.setFreeSlots(freeSlots);
        int usedSlots = cabinet.getUsedSlots() + 1;
        cabinet.setUsedSlots(usedSlots);
        //可以借用
        if ("1".equals(powerBank.getStatus())) {
            int availableNum = cabinet.getAvailableNum() + 1;
            cabinet.setAvailableNum(availableNum);
        }
        cabinet.setUpdateTime(new Date());
        cabinetService.updateById(cabinet);
        // 获取站点
        LambdaQueryWrapper<Station> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(Station::getCabinetId, cabinet.getId());
        Station station = stationService.getOne(wrapper);
        // 构建结束订单对象
        EndOrderVo endOrderVo = new EndOrderVo();
        endOrderVo.setMessageNo(messageNo);
        endOrderVo.setEndTime(new Date());
        endOrderVo.setEndCabinetNo(cabinetNo);
        endOrderVo.setEndStationId(station.getId());
        endOrderVo.setEndStationName(station.getName());
        endOrderVo.setPowerBankNo(powerBankNo);
        log.info("构建结束订单对象: {}", JSONUtil.toJsonStr(endOrderVo));
        //发送信息
    boolean sendResult = rabbitService.sendMessage(MqConst.EXCHANGE_ORDER, MqConst.ROUTING_END_ORDER, JSONUtil.toJsonStr(endOrderVo));
    log.info("结束订单消息发送结果: {}, messageNo={}", sendResult, messageNo);







    }
}
