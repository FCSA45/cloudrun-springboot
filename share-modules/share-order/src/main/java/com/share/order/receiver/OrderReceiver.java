package com.share.order.receiver;

import com.alibaba.fastjson2.JSONObject;
import com.rabbitmq.client.Channel;
import com.share.common.core.utils.StringUtils;
import com.share.common.rabbit.constant.MqConst;
import com.share.order.api.domain.EndOrderVo;
import com.share.order.api.domain.SubmitOrderVo;
import com.share.order.service.IOrderInfoService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.share.common.rabbit.constant.MqConst.ROUTING_SUBMIT_ORDER;

@Slf4j
@Component
public class OrderReceiver {

    @Autowired
    private IOrderInfoService orderInfoService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    //接收消息创建订单
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_ORDER, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_SUBMIT_ORDER, durable = "true"),
            key = ROUTING_SUBMIT_ORDER))
    public void createOrder(String content, Message message, Channel channel) {
        log.info("接收到创建订单消息: {}", content);
        SubmitOrderVo submitOrderVo = JSONObject.parseObject(content, SubmitOrderVo.class);
        String messageNo = submitOrderVo.getMessageNo();
        String key = "order:submit:" + messageNo;
        boolean isExist = redisTemplate.opsForValue().setIfAbsent(key, messageNo, 1, TimeUnit.HOURS);
        if (!isExist) {
            log.info("重复请求: {}", content);
            return;
        }

        try {
            orderInfoService.saveOrder(submitOrderVo);
            log.info("创建订单成功: {}", messageNo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("创建订单失败: {}", content, e);
            // 如果是业务异常，可能需要根据情况决定是否重回队列，这里暂时不重回，避免死循环
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }

    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_ORDER, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_END_ORDER, durable = "true"),
            key = MqConst.ROUTING_END_ORDER
    ))
    public void endOrder(String content, Message message, Channel channel) {
        log.info("接收到结束订单消息: {}", content);
        EndOrderVo endOrderVo = JSONObject.parseObject(content, EndOrderVo.class);
        String messageNo = endOrderVo.getMessageNo();
        String key = "order:end:" + messageNo;
        boolean isExist = redisTemplate.opsForValue().setIfAbsent(key, messageNo, 1, TimeUnit.HOURS);
        if (!isExist) {
            return;
        }
        try {
            orderInfoService.endOrder(endOrderVo);
            log.info("结束订单成功: {}", messageNo);

            //手动应答
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("结束订单失败", e);
            redisTemplate.delete(key);
            // 出现异常不再重回队列，防止死循环
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }
}