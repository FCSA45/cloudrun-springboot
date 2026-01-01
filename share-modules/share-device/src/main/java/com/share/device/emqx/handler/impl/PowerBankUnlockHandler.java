package com.share.device.emqx.handler.impl;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.share.order.api.domain.SubmitOrderVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@GuiguEmqx(topic = EmqxConstants.TOPIC_POWERBANK_UNLOCK)
public class PowerBankUnlockHandler implements MassageHandler {

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
private  RabbitService rabbitService;
    @Override
    public void handleMessage(JSONObject message) {
        log.info("收到解锁消息: {}", message.toString());
        String messageNo = message.getStr("mNo");
        String key = "powerBank:unlock:" + messageNo;
        boolean isExist = redisTemplate.opsForValue().setIfAbsent(key, messageNo, 1, TimeUnit.HOURS);
        if (!isExist) {
            log.info("解锁消息重复, 忽略: {}", messageNo);
            return;
        }
        //柜机编号
        String cabinetNo = message.getStr("cNo");
        //充电宝编号
        String powerBankNo = message.getStr("pNo");
        //插槽编号
        String slotNo = message.getStr("sNo");
        Long userId = message.getLong("uId");
        if (StringUtils.isEmpty(cabinetNo)
                || StringUtils.isEmpty(powerBankNo)
                || StringUtils.isEmpty(slotNo)
                || null == userId) {
            log.info("参数为空: {}", message.toString());
            return;
        }
        Cabinet cabinet = cabinetService.getBtCabinetNo(cabinetNo);

        CabinetSlot cabinetSlot = cabinetSlotService.getBtSlotNo(cabinet.getId(), slotNo);
        // 获取站点
        LambdaQueryWrapper<Station> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Station::getCabinetId, cabinet.getId());
        Station station = stationService.getOne(wrapper);
        PowerBank powerBank = powerBankService.getByPowerBankNo(powerBankNo);
        powerBank.setStatus("2");
        powerBankService.updateById(powerBank);
        cabinetSlot.setStatus("0");
        cabinetSlot.setPowerBankId(null);
        cabinetSlot.setUpdateTime(new Date());
        cabinetSlotService.updateById(cabinetSlot);
        int freeSlots = cabinet.getFreeSlots() + 1;
        cabinet.setFreeSlots(freeSlots);
        int usedSlots = cabinet.getUsedSlots() - 1;
        cabinet.setUsedSlots(usedSlots);
        //可以借用
        int availableNum = cabinet.getAvailableNum() - 1;
        cabinet.setAvailableNum(availableNum);
        cabinet.setUpdateTime(new Date());
        cabinetService.updateById(cabinet);
        SubmitOrderVo submitOrderVo=new SubmitOrderVo();
        submitOrderVo.setMessageNo(messageNo);
        submitOrderVo.setUserId(userId);
        submitOrderVo.setPowerBankNo(powerBankNo);
        submitOrderVo.setStartStationId(station.getId());
        submitOrderVo.setStartStationName(station.getName());
        submitOrderVo.setStartCabinetNo(cabinetNo);
        submitOrderVo.setFeeRuleId(station.getFeeRuleId());
        //发送mq消息
    boolean sendResult = rabbitService.sendMessage(MqConst.EXCHANGE_ORDER,
        MqConst.ROUTING_SUBMIT_ORDER,
        com.alibaba.fastjson2.JSONObject.toJSONString(submitOrderVo));
    log.info("提交订单消息发送结果: {}, messageNo={}", sendResult, messageNo);

    }
}