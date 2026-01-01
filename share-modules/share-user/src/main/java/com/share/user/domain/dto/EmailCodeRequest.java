package com.share.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    private String email;

    /** 操作场景，可选值：REGISTER、LOGIN，默认LOGIN */
    private String scene;
}
