package com.share.payment.service;

import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.bean.BeanUtils;
import com.share.payment.config.WxPayV3Properties;
import com.share.payment.domain.CreateWxPaymentForm;
import com.share.payment.domain.PaymentInfo;
import com.share.payment.domain.WxPrepayVo;
import com.share.payment.service.PaymentService;
import com.share.payment.service.UserWalletService;
import com.share.user.api.RemoteUserService;
import com.share.user.domain.UserInfo;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.partnerpayments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.Amount;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.Payer;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.PrepayWithRequestPaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class WxPayServiceImpl implements IWxPayService {

  @Autowired
  private PaymentService paymentInfoService;

  @Autowired
  private RemoteUserService remoteUserService;

  @Autowired
  private UserWalletService userWalletService;

  @Autowired
  private WxPayV3Properties wxPayV3Properties;

  @Autowired(required = false)
  private RSAAutoCertificateConfig rsaAutoCertificateConfig;

  @Override
  public WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm) {
    PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(createWxPaymentForm.getOrderNo());
    R<UserInfo> userInfoResult = remoteUserService.getInfo(paymentInfo.getUserId());
    if (userInfoResult == null || userInfoResult.getCode() != R.SUCCESS || userInfoResult.getData() == null) {
      throw new ServiceException(userInfoResult != null ? userInfoResult.getMsg() : "获取用户信息失败");
    }
    UserInfo userInfo = userInfoResult.getData();

    if (wxPayV3Properties.isMockEnabled()) {
      return handleMockPayment(paymentInfo);
    }

    if (rsaAutoCertificateConfig == null) {
      throw new ServiceException("未配置微信支付证书信息");
    }

    String openid = userInfo.getWxOpenId();
    if (openid == null) {
      throw new ServiceException("用户未绑定微信 OpenId");
    }

    JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();
    PrepayRequest request = new PrepayRequest();
    Amount amount = new Amount();
    BigDecimal paymentAmount = paymentInfo.getAmount() == null ? BigDecimal.ZERO : paymentInfo.getAmount();
    if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ServiceException("订单金额异常");
    }
    amount.setTotal(paymentAmount.multiply(BigDecimal.valueOf(100)).intValue());
    request.setAmount(amount);
    request.setSpAppid(wxPayV3Properties.getAppid());
    request.setSpMchid(wxPayV3Properties.getMerchantId());
    request.setDescription(paymentInfo.getContent());
    request.setNotifyUrl(wxPayV3Properties.getNotifyUrl());
    request.setOutTradeNo(paymentInfo.getOrderNo());
    Payer payer = new Payer();
    payer.setSpOpenid(openid);
    request.setPayer(payer);
    PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request, wxPayV3Properties.getAppid());
    WxPrepayVo wxPrepayVo = new WxPrepayVo();
    BeanUtils.copyProperties(response, wxPrepayVo);
    wxPrepayVo.setTimeStamp(response.getTimeStamp());
    return wxPrepayVo;
  }

  private WxPrepayVo handleMockPayment(PaymentInfo paymentInfo) {
    BigDecimal amount = paymentInfo.getAmount() == null ? BigDecimal.ZERO : paymentInfo.getAmount();
    userWalletService.deductBalance(paymentInfo.getUserId(), amount);
    String transactionId = "MOCK-" + paymentInfo.getOrderNo();
    paymentInfoService.markPaySuccess(paymentInfo, transactionId, "模拟支付自动完成");
    WxPrepayVo wxPrepayVo = new WxPrepayVo();
    wxPrepayVo.setAppId(wxPayV3Properties.getAppid());
    wxPrepayVo.setTimeStamp(String.valueOf(Instant.now().getEpochSecond()));
    wxPrepayVo.setNonceStr(UUID.randomUUID().toString().replace("-", ""));
    wxPrepayVo.setPackageVal("prepay_id=" + transactionId);
    wxPrepayVo.setSignType("MOCK");
    wxPrepayVo.setPaySign("MOCK_SIGN");
    wxPrepayVo.setMock(true);
    return wxPrepayVo;
  }
}