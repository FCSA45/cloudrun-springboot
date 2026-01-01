package com.share.user.domain.dto;

import com.share.user.domain.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序登录业务处理结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniLoginResult {

    private UserInfo userInfo;

    private boolean newUser;
}
