package com.share.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 小程序登录请求参数。
 */
@Data
public class MiniLoginRequest {

    /** 小程序登录临时凭证 code */
    @NotBlank(message = "登录凭证code不能为空")
    private String code;

    /** 用户昵称，可选 */
    private String nickname;

    /** 用户头像，可选 */
    private String avatarUrl;

    /** 手机号，可选 */
    private String phone;
}
