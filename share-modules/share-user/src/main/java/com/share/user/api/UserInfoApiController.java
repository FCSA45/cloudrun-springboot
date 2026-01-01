package com.share.user.api;

import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.context.SecurityContextHolder;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.StringUtils;
import com.share.common.core.utils.ip.IpUtils;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.service.TokenService;
import com.share.user.domain.UserBalanceUpdateRequest;
import com.share.user.domain.UserInfo;
import com.share.user.domain.UserVo;
import com.share.user.domain.dto.AccountLoginRequest;
import com.share.user.domain.dto.AccountRegisterRequest;
import com.share.user.domain.dto.EmailCodeRequest;
import com.share.user.domain.dto.EmailLoginRequest;
import com.share.user.domain.dto.MiniLoginRequest;
import com.share.user.domain.dto.MiniLoginResponse;
import com.share.user.domain.dto.MiniLoginResult;
import com.share.user.service.IUserInfoService;
import com.share.user.service.EmailVerificationService.VerificationScene;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/userInfo")
public class UserInfoApiController extends BaseController {

    @Autowired
    private IUserInfoService userInfoService;

    @Autowired
    private TokenService tokenService;

    //微信授权登录-远程调用
    @Operation(summary = "小程序授权登录")
    @GetMapping("/wxLogin/{code}")
    public R<UserInfo> wxLogin(@PathVariable String code) {
        UserInfo userInfo = userInfoService.wxLogin(code);
        return R.ok(userInfo);
    }

    @Operation(summary = "小程序登录注册")
    @PostMapping("/mini/login")
    public AjaxResult miniLogin(@Validated @RequestBody MiniLoginRequest request, HttpServletRequest httpServletRequest) {
        String ip = IpUtils.getIpAddr(httpServletRequest);
        MiniLoginResult result = userInfoService.miniLogin(request.getCode(), request.getNickname(), request.getAvatarUrl(), request.getPhone(), ip);
        MiniLoginResponse response = buildLoginResponse(result.getUserInfo(), result.isNewUser());
        return AjaxResult.success(response);
    }

    @Operation(summary = "账号注册")
    @PostMapping("/account/register")
    public AjaxResult registerByAccount(@Validated @RequestBody AccountRegisterRequest request,
                                        HttpServletRequest httpServletRequest) {
        if (!StringUtils.equals(request.getPassword(), request.getConfirmPassword())) {
            throw new ServiceException("两次输入的密码不一致");
        }
        String ip = IpUtils.getIpAddr(httpServletRequest);
        MiniLoginResult result = userInfoService.registerByAccount(request.getUsername(), request.getPassword(), request.getNickname(), ip);
        return AjaxResult.success(buildLoginResponse(result.getUserInfo(), result.isNewUser()));
    }

    @Operation(summary = "账号登录")
    @PostMapping("/account/login")
    public AjaxResult loginByAccount(@Validated @RequestBody AccountLoginRequest request,
                                     HttpServletRequest httpServletRequest) {
        String ip = IpUtils.getIpAddr(httpServletRequest);
        MiniLoginResult result = userInfoService.loginByAccount(request.getUsername(), request.getPassword(), ip);
        return AjaxResult.success(buildLoginResponse(result.getUserInfo(), result.isNewUser()));
    }

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/email/sendCode")
    public AjaxResult sendEmailCode(@Validated @RequestBody EmailCodeRequest request) {
        VerificationScene scene = parseScene(request.getScene());
        String mockCode = userInfoService.sendEmailCode(request.getEmail(), scene);
        AjaxResult result = AjaxResult.success();
        if (StringUtils.isNotBlank(mockCode)) {
            result.put("mockCode", mockCode);
        }
        return result;
    }

    @Operation(summary = "邮箱验证码登录/注册")
    @PostMapping("/email/login")
    public AjaxResult loginByEmail(@Validated @RequestBody EmailLoginRequest request,
                                   HttpServletRequest httpServletRequest) {
        String ip = IpUtils.getIpAddr(httpServletRequest);
        VerificationScene scene = parseScene(request.getScene());
        MiniLoginResult result = userInfoService.loginByEmail(request.getEmail(), request.getCode(), request.getNickname(), scene, ip);
        return AjaxResult.success(buildLoginResponse(result.getUserInfo(), result.isNewUser()));
    }

    @Operation(summary = "获取当前登录用户信息")
    @RequiresLogin
    @GetMapping("/getLoginUserInfo")
    public AjaxResult getLoginUserInfo(HttpServletRequest request) {
        Long userId = SecurityContextHolder.getUserId();
        UserInfo userInfo = userInfoService.getById(userId);
        UserVo userInfoVo = new UserVo();
        if (userInfo != null) {
            userInfoVo.setNickname(userInfo.getNickname());
            userInfoVo.setAvatar(userInfo.getAvatarUrl());
            userInfoVo.setWxOpenId(userInfo.getWxOpenId());
            userInfoVo.setDepositStatus(userInfo.getDepositStatus());
            userInfoVo.setBalance(userInfo.getBalance());
            userInfoVo.setFrozenAmount(userInfo.getFrozenAmount());
        }
        return success(userInfoVo);
    }

    @Operation(summary = "是否免押金")
    @RequiresLogin
    @GetMapping("/isFreeDeposit")
    public AjaxResult isFreeDeposit() {
        return success(userInfoService.isFreeDeposit());
    }

    @Operation(summary = "获取用户详细信息")
    @GetMapping(value = "/getUserInfo/{id}")
    public R<UserInfo> getInfo(@PathVariable("id") Long id) {
        UserInfo userInfo = userInfoService.getById(id);
        return R.ok(userInfo);
    }

    @Operation(summary = "内部-更新用户余额")
    @PostMapping("/updateBalance")
    public R<Void> updateBalance(@RequestBody UserBalanceUpdateRequest request,
                                 @RequestHeader(SecurityConstants.FROM_SOURCE) String source) {
        if (!SecurityConstants.INNER.equals(source)) {
            return R.fail("非法访问");
        }
        userInfoService.updateBalance(request.getUserId(), request.getBalance());
        return R.ok();
    }

    //统计2024年每个月注册人数
    //远程调用：统计用户注册数据
    @GetMapping("/getUserCount")
    public R<Map<String, Object>> getUserCount() {
        //[150, 230, 224, 218, 135, 147, 260]
        //['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
        Map<String,Object> map = userInfoService.getUserCount();
        return R.ok(map);
    }

    private MiniLoginResponse buildLoginResponse(UserInfo userInfo, boolean newUser) {
        com.share.system.api.model.LoginUser loginUser = new com.share.system.api.model.LoginUser();
        loginUser.setUserid(userInfo.getId());
        String displayName = userInfo.getNickname();
        if (StringUtils.isBlank(displayName)) {
            displayName = userInfo.getUsername();
        }
        if (StringUtils.isBlank(displayName)) {
            displayName = userInfo.getWxOpenId();
        }
        loginUser.setUsername(displayName);
        loginUser.setRoles(Collections.emptySet());
        loginUser.setPermissions(Collections.emptySet());

        Map<String, Object> tokenInfo = tokenService.createToken(loginUser);

        UserVo userVo = new UserVo();
        userVo.setNickname(userInfo.getNickname());
        userVo.setAvatar(userInfo.getAvatarUrl());
        userVo.setUsername(userInfo.getUsername());
        userVo.setEmail(userInfo.getEmail());
        userVo.setRegisterType(userInfo.getRegisterType());
        userVo.setEmailVerified(userInfo.getEmailVerified());
        userVo.setWxOpenId(userInfo.getWxOpenId());
        userVo.setDepositStatus(userInfo.getDepositStatus());
        userVo.setBalance(userInfo.getBalance());
        userVo.setFrozenAmount(userInfo.getFrozenAmount());

        MiniLoginResponse response = new MiniLoginResponse();
        response.setAccessToken((String) tokenInfo.get("access_token"));
        Object expiresInObj = tokenInfo.get("expires_in");
        if (expiresInObj instanceof Number number) {
            response.setExpiresIn(number.longValue());
        }
        response.setNewUser(newUser);
        response.setUser(userVo);
        return response;
    }

    private VerificationScene parseScene(String sceneValue) {
        if (StringUtils.isBlank(sceneValue)) {
            return VerificationScene.LOGIN;
        }
        try {
            return VerificationScene.valueOf(sceneValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ServiceException("不支持的验证码场景");
        }
    }
}
