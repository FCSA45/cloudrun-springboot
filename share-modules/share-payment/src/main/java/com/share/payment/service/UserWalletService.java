package com.share.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.payment.domain.UserWallet;

import java.math.BigDecimal;

public interface UserWalletService extends IService<UserWallet> {

    UserWallet ensureWallet(Long userId);

    void deductBalance(Long userId, BigDecimal amount);
}
