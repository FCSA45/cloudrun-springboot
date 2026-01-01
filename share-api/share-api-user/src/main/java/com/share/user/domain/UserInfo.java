package com.share.user.domain;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.share.common.core.annotation.Excel;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户对象 user_info
 *
 * @author atguigu
 * @date 2025-02-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户")
public class UserInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 微信openId */
    @Excel(name = "微信openId")
    @Schema(description = "微信openId")
    private String wxOpenId;

    /** 注册渠道 WECHAT/ACCOUNT/EMAIL */
    @Excel(name = "注册渠道")
    @Schema(description = "注册渠道，WECHAT/ACCOUNT/EMAIL")
    private String registerType;

    /** 用户名 */
    @Excel(name = "用户名")
    @Schema(description = "用户名")
    private String username;

    /** 加密密码 */
    @JsonIgnore
    @Excel(name = "密码", prompt = "加密存储")
    @Schema(description = "加密后的密码，接口不返回")
    private String password;

    /** 会员昵称 */
    @Excel(name = "会员昵称")
    @Schema(description = "会员昵称")
    private String nickname;

    /** 性别 */
    @Excel(name = "性别")
    @Schema(description = "性别")
    private String gender;

    /** 头像 */
    @Excel(name = "头像")
    @Schema(description = "头像")
    private String avatarUrl;

    /** 电话 */
    @Excel(name = "电话")
    @Schema(description = "电话")
    private String phone;

    /** 邮箱 */
    @Excel(name = "邮箱")
    @Schema(description = "邮箱")
    private String email;

    /** 邮箱是否验证 */
    @Excel(name = "邮箱是否验证")
    @Schema(description = "邮箱是否验证，0未验证 1已验证")
    private String emailVerified;

    /** 最后一次登录ip */
    @Excel(name = "最后一次登录ip")
    @Schema(description = "最后一次登录ip")
    private String lastLoginIp;

    /** 最后一次登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后一次登录时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后一次登录时间")
    private Date lastLoginTime;

    @Excel(name = "押金状态")
    @Schema(description = "押金状态（0：未验证 1：免押金 2：已交押金）")
    private String depositStatus;

    @Excel(name = "账户余额")
    @Schema(description = "账户余额")
    private BigDecimal balance;

    @Excel(name = "冻结金额")
    @Schema(description = "冻结金额")
    private BigDecimal frozenAmount;

    /** 1有效，2禁用 */
    @Excel(name = "1有效，2禁用")
    @Schema(description = "1有效，2禁用")
    private String status;

}
