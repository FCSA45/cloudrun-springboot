package com.share.user.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "用户类")
public class UserVo {

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "微信open id")
    private String wxOpenId;

    @Schema(description = "注册渠道")
    private String registerType;

    @Schema(description = "邮箱是否验证")
    private String emailVerified;

    @Schema(description = "押金状态（0：未验证 1：免押金 2：已交押金）")
    private String depositStatus;

    @Schema(description = "账户余额")
    private BigDecimal balance;

    @Schema(description = "冻结金额")
    private BigDecimal frozenAmount;
}
