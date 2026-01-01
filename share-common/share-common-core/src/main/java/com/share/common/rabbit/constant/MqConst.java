package com.share.common.rabbit.constant;

public class MqConst {
    /**
     * 订单交换机
     */
    public static final String EXCHANGE_ORDER_DIRECT = "exchange.order.direct";
    public static final String ROUTING_ORDER_CREATE = "routing.order.create";
    public static final String QUEUE_ORDER_CREATE = "queue.order.create";

    public static final String EXCHANGE_ORDER = "exchange.order";
    public static final String QUEUE_END_ORDER = "queue.end.order";
    public static final String ROUTING_END_ORDER = "routing.end.order";

    public static final String QUEUE_SUBMIT_ORDER = "queue.submit.order";
    public static final String ROUTING_SUBMIT_ORDER = "routing.submit.order";

    public static final String EXCHANGE_PAYMENT_PAY = "exchange.payment.pay";
    public static final String QUEUE_PAYMENT_PAY = "queue.payment.pay";
    public static final String ROUTING_PAYMENT_PAY = "routing.payment.pay";
}
