package com.share.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailLoginRequest {

    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    /** 可选昵称，首次注册时可用 */
    private String nickname;

    /** 操作场景，默认LOGIN */
    private String scene;
}
