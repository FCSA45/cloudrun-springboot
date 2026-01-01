package com.share.user.domain.dto;

import com.share.user.domain.UserVo;
import lombok.Data;

/**
 * 登录返回数据。
 */
@Data
public class MiniLoginResponse {

    /** 访问令牌 */
    private String accessToken;

    /** 令牌有效期（分钟） */
    private Long expiresIn;

    /** 是否首次登录 */
    private boolean newUser;

    /** 用户信息 */
    private UserVo user;
}
