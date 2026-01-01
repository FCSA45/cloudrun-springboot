package com.share.payment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.payment.config.WxPayV3Properties;
import com.share.payment.domain.UserWallet;
import com.share.payment.mapper.UserWalletMapper;
import com.share.payment.service.UserWalletService;
import com.share.user.api.RemoteUserService;
import com.share.user.domain.UserBalanceUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class UserWalletServiceImpl extends ServiceImpl<UserWalletMapper, UserWallet> implements UserWalletService {

    @Autowired
    private WxPayV3Properties wxPayV3Properties;

    @Autowired
    private RemoteUserService remoteUserService;

    @Override
    public UserWallet ensureWallet(Long userId) {
        UserWallet wallet = lambdaQuery().eq(UserWallet::getUserId, userId).one();
        if (wallet != null) {
            return wallet;
        }
        wallet = new UserWallet();
        wallet.setUserId(userId);
        wallet.setBalance(resolveInitialBalance());
        wallet.setFrozenAmount(BigDecimal.ZERO);
        save(wallet);
        syncUserBalance(userId, wallet.getBalance());
        return wallet;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductBalance(Long userId, BigDecimal amount) {
        if (amount == null) {
            throw new ServiceException("扣款金额不能为空");
        }
        BigDecimal normalizedAmount = amount.max(BigDecimal.ZERO);
        UserWallet wallet = ensureWallet(userId);
        BigDecimal currentBalance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        if (currentBalance.compareTo(normalizedAmount) < 0) {
            throw new ServiceException("模拟余额不足，请先为账户充值");
        }
        BigDecimal targetBalance = currentBalance.subtract(normalizedAmount);
        boolean updated = lambdaUpdate()
                .set(UserWallet::getBalance, targetBalance)
                .eq(UserWallet::getId, wallet.getId())
                .eq(UserWallet::getBalance, currentBalance)
                .update();
        if (!updated) {
            throw new ServiceException("模拟扣款失败，请重试");
        }
        syncUserBalance(userId, targetBalance);
    }

    private BigDecimal resolveInitialBalance() {
        BigDecimal balance = wxPayV3Properties.getMockInitialBalance();
        if (Objects.isNull(balance) || balance.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return balance;
    }

    private void syncUserBalance(Long userId, BigDecimal balance) {
        UserBalanceUpdateRequest request = new UserBalanceUpdateRequest();
        request.setUserId(userId);
        request.setBalance(balance);
        R<Void> result = remoteUserService.updateBalance(request, SecurityConstants.INNER);
        if (result == null || result.getCode() != R.SUCCESS) {
            throw new ServiceException(result != null ? result.getMsg() : "同步用户余额失败");
        }
    }
}
