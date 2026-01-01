package com.share.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.context.SecurityContextHolder;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.StringUtils;
import com.share.common.security.utils.SecurityUtils;
import com.share.user.domain.UserCountVo;
import com.share.user.domain.UserInfo;
import com.share.user.domain.dto.MiniLoginResult;
import com.share.user.mapper.UserInfoMapper;
import com.share.user.service.IUserInfoService;
import com.share.user.service.EmailVerificationService;
import com.share.user.service.EmailVerificationService.VerificationScene;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Date;
import java.util.Objects;

/**
 * 用户Service业务层处理
 *
 * @author atguigu
 * @date 2025-02-17
 */
@Service
public  class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService
{
    private static final String REGISTER_TYPE_WECHAT = "WECHAT";
    private static final String REGISTER_TYPE_ACCOUNT = "ACCOUNT";
    private static final String REGISTER_TYPE_EMAIL = "EMAIL";
    private static final String EMAIL_VERIFIED = "1";
    private static final String EMAIL_UNVERIFIED = "0";

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public UserInfo wxLogin(String code) {
        String openId = null;
        try {
            //获取openId
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            openId = sessionInfo.getOpenid();
            LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserInfo::getWxOpenId,openId);
            UserInfo userInfo=userInfoMapper.selectOne(wrapper);
            if(userInfo==null){
                userInfo = new UserInfo();
                userInfo.setNickname(String.valueOf(System.currentTimeMillis()));
                userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
                     userInfo.setWxOpenId(openId);
                     userInfo.setBalance(BigDecimal.ZERO);
                     userInfo.setFrozenAmount(BigDecimal.ZERO);
                     userInfo.setDepositStatus("0");
                     userInfo.setStatus("1");
                     userInfo.setRegisterType(REGISTER_TYPE_WECHAT);
                     userInfo.setEmailVerified(EMAIL_UNVERIFIED);
                    userInfoMapper.insert(userInfo);
            } else if (StringUtils.isBlank(userInfo.getRegisterType())) {
                userInfo.setRegisterType(REGISTER_TYPE_WECHAT);
                userInfoMapper.updateById(userInfo);
            }
            return userInfo;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("微信登录失败");
        }
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public MiniLoginResult miniLogin(String code, String nickname, String avatarUrl, String phone, String ip) {
        UserInfo userInfo = wxLogin(code);
        boolean newUser = userInfo.getLastLoginTime() == null;
        boolean needUpdate = false;
        if (StringUtils.isNotBlank(nickname) && !nickname.equals(userInfo.getNickname())) {
            userInfo.setNickname(nickname);
            needUpdate = true;
        }
        if (StringUtils.isNotBlank(avatarUrl) && !avatarUrl.equals(userInfo.getAvatarUrl())) {
            userInfo.setAvatarUrl(avatarUrl);
            needUpdate = true;
        }
        if (StringUtils.isNotBlank(phone) && !phone.equals(userInfo.getPhone())) {
            userInfo.setPhone(phone);
            needUpdate = true;
        }
        if (StringUtils.isNotBlank(ip)) {
            userInfo.setLastLoginIp(ip);
            needUpdate = true;
        }
        userInfo.setLastLoginTime(new Date());
        if (StringUtils.isBlank(userInfo.getRegisterType())) {
            userInfo.setRegisterType(REGISTER_TYPE_WECHAT);
        }
        needUpdate = true;
        if (needUpdate) {
            userInfoMapper.updateById(userInfo);
        }
        return new MiniLoginResult(userInfo, newUser);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public MiniLoginResult registerByAccount(String username, String rawPassword, String nickname, String ip) {
        String normalizedUsername = StringUtils.trim(username);
        if (StringUtils.isBlank(normalizedUsername)) {
            throw new ServiceException("用户名不能为空");
        }
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getUsername, normalizedUsername);
        if (userInfoMapper.selectOne(wrapper) != null) {
            throw new ServiceException("用户名已存在");
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(normalizedUsername);
        userInfo.setPassword(SecurityUtils.encryptPassword(rawPassword));
        userInfo.setNickname(StringUtils.isNotBlank(nickname) ? nickname.trim() : normalizedUsername);
        userInfo.setRegisterType(REGISTER_TYPE_ACCOUNT);
        userInfo.setStatus("1");
        userInfo.setDepositStatus("0");
        userInfo.setBalance(BigDecimal.ZERO);
        userInfo.setFrozenAmount(BigDecimal.ZERO);
        userInfo.setEmailVerified(EMAIL_UNVERIFIED);
        if (StringUtils.isNotBlank(ip)) {
            userInfo.setLastLoginIp(ip);
        }
        userInfo.setLastLoginTime(new Date());
        userInfoMapper.insert(userInfo);
        return new MiniLoginResult(userInfo, true);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public MiniLoginResult loginByAccount(String username, String rawPassword, String ip) {
        String normalizedUsername = StringUtils.trim(username);
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getUsername, normalizedUsername);
        UserInfo userInfo = userInfoMapper.selectOne(wrapper);
        if (userInfo == null || StringUtils.isBlank(userInfo.getPassword())) {
            throw new ServiceException("用户名或密码错误");
        }
        if (!SecurityUtils.matchesPassword(rawPassword, userInfo.getPassword())) {
            throw new ServiceException("用户名或密码错误");
        }
        if (!Objects.equals("1", userInfo.getStatus())) {
            throw new ServiceException("账号已被禁用");
        }
        if (StringUtils.isBlank(userInfo.getRegisterType())) {
            userInfo.setRegisterType(REGISTER_TYPE_ACCOUNT);
        }
        if (StringUtils.isNotBlank(ip)) {
            userInfo.setLastLoginIp(ip);
        }
        userInfo.setLastLoginTime(new Date());
        userInfoMapper.updateById(userInfo);
        return new MiniLoginResult(userInfo, false);
    }

    @Override
    public String sendEmailCode(String email, VerificationScene scene) {
        VerificationScene targetScene = scene == null ? VerificationScene.LOGIN : scene;
        return emailVerificationService.sendCode(email, targetScene);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public MiniLoginResult loginByEmail(String email, String code, String nickname, VerificationScene scene, String ip) {
        VerificationScene targetScene = scene == null ? VerificationScene.LOGIN : scene;
        emailVerificationService.validateCode(email, code, targetScene);

        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getEmail, email);
        UserInfo userInfo = userInfoMapper.selectOne(wrapper);
        boolean newUser = false;
        if (userInfo == null) {
            userInfo = new UserInfo();
            userInfo.setEmail(email);
            userInfo.setEmailVerified(EMAIL_VERIFIED);
            userInfo.setRegisterType(REGISTER_TYPE_EMAIL);
            userInfo.setUsername(email);
            String defaultNickname = StringUtils.substringBefore(email, "@");
            userInfo.setNickname(StringUtils.isNotBlank(nickname) ? nickname.trim() : defaultNickname);
            userInfo.setStatus("1");
            userInfo.setDepositStatus("0");
            userInfo.setBalance(BigDecimal.ZERO);
            userInfo.setFrozenAmount(BigDecimal.ZERO);
            if (StringUtils.isNotBlank(ip)) {
                userInfo.setLastLoginIp(ip);
            }
            userInfo.setLastLoginTime(new Date());
            userInfoMapper.insert(userInfo);
            newUser = true;
        } else {
            if (StringUtils.isBlank(userInfo.getRegisterType())) {
                userInfo.setRegisterType(REGISTER_TYPE_EMAIL);
            }
            userInfo.setEmail(email);
            userInfo.setEmailVerified(EMAIL_VERIFIED);
            if (StringUtils.isBlank(userInfo.getUsername())) {
                userInfo.setUsername(email);
            }
            if (StringUtils.isNotBlank(nickname) && !nickname.equals(userInfo.getNickname())) {
                userInfo.setNickname(nickname.trim());
            }
            if (StringUtils.isNotBlank(ip)) {
                userInfo.setLastLoginIp(ip);
            }
            userInfo.setLastLoginTime(new Date());
            userInfoMapper.updateById(userInfo);
        }
        return new MiniLoginResult(userInfo, newUser);
    }

    /**
     * 查询用户列表
     *
     * @param userInfo 用户
     * @return 用户
     */
    @Override
    public List<UserInfo> selectUserInfoList(UserInfo userInfo)
    {
        return userInfoMapper.selectUserInfoList(userInfo);
    }

    @Override
    public Boolean isFreeDeposit() {
        UserInfo userInfo = userInfoMapper.selectById(SecurityContextHolder.getUserId());
userInfo.setDepositStatus("1");
userInfoMapper.updateById(userInfo);

        return true;
    }

    @Override
    public Map<String, Object> getUserCount() {
        List<UserCountVo> list = userInfoMapper.selectUserCount();
        if (list != null && !list.isEmpty()) {
             List<String> dateList = list.stream().map(UserCountVo::getRegisterDate).collect(Collectors.toList());
             List<Integer> countList = list.stream().map(UserCountVo::getCount).collect(Collectors.toList());
             Map<String, Object> map = new HashMap<>();
             map.put("dateList", dateList);
             map.put("countList", countList);
             return map;
        }
        return new HashMap<>();
    }

    @Override
    public void updateBalance(Long userId, BigDecimal balance) {
        if (userId == null) {
            throw new ServiceException("用户ID不能为空");
        }
        if (balance == null) {
            throw new ServiceException("余额不能为空");
        }
        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (userInfo == null) {
            throw new ServiceException("用户不存在");
        }
        userInfo.setBalance(balance);
        userInfoMapper.updateById(userInfo);
    }
}

