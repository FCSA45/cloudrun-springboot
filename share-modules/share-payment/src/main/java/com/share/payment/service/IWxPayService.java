package com.share.payment.service;

import com.share.payment.domain.CreateWxPaymentForm;
import com.share.payment.domain.WxPrepayVo;

public interface IWxPayService {
    WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm);
}
