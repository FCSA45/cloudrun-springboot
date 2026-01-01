package com.share.user.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "用户余额更新请求")
public class UserBalanceUpdateRequest {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "账户余额")
    private BigDecimal balance;
}
