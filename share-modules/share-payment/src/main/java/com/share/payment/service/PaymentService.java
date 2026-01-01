package com.share.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.payment.domain.PaymentInfo;

public interface PaymentService extends IService<PaymentInfo> {

    /**
     * 获取或创建支付记录
     *
     * @param orderNo 业务订单号
     * @return 支付记录信息
     */
    PaymentInfo savePaymentInfo(String orderNo);

    /**
     * 支付成功后更新支付记录
     *
     * @param paymentInfo 支付记录
     * @param transactionId 交易号
     * @param callbackContent 回调原文
     */
    void markPaySuccess(PaymentInfo paymentInfo, String transactionId, String callbackContent);
}
