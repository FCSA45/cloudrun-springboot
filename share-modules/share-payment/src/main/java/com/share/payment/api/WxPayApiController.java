package com.share.payment.api;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.security.annotation.RequiresLogin;
import com.share.payment.domain.CreateWxPaymentForm;
import com.share.payment.domain.WxPrepayVo;
import com.share.payment.service.PaymentService;
import com.share.payment.service.IWxPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("/wxPay")
@Slf4j
public class WxPayApiController extends BaseController {

    @Autowired
    private IWxPayService wxPayService;

    @Autowired
    private PaymentService paymentInfoService;

    @RequiresLogin
    @Operation(summary = "微信下单")
    @PostMapping("/createWxPayment")
    public AjaxResult createWxPayment(@RequestBody CreateWxPaymentForm createWxPaymentForm) {
        WxPrepayVo wxPrepayVo = wxPayService.createWxPayment(createWxPaymentForm);
        return AjaxResult.success(wxPrepayVo);
    }
}

