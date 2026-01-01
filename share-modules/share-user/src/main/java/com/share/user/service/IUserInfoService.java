package com.share.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.user.domain.UserInfo;
import com.share.user.domain.dto.MiniLoginResult;
import com.share.user.service.EmailVerificationService.VerificationScene;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
/**
 * 用户Service接口
 *
 * @author atguigu
 * @date 2025-02-17
 */
public interface IUserInfoService extends IService<UserInfo>
{

    /**
     * 查询用户列表
     *
     * @param userInfo 用户
     * @return 用户集合
     */
    public List<UserInfo> selectUserInfoList(UserInfo userInfo);

    ////微信授权登录-远程调用
    UserInfo wxLogin(String code);

    /**
     * 小程序登录并完善用户信息。
     *
     * @param code      小程序登录临时凭证
     * @param nickname  用户昵称
     * @param avatarUrl 用户头像
     * @param phone     手机号
     * @param ip        最近登录IP
     * @return 用户信息
     */
    MiniLoginResult miniLogin(String code, String nickname, String avatarUrl, String phone, String ip);

    /**
     * 账号密码注册
     */
    MiniLoginResult registerByAccount(String username, String rawPassword, String nickname, String ip);

    /**
     * 账号密码登录
     */
    MiniLoginResult loginByAccount(String username, String rawPassword, String ip);

    /**
     * 发送邮箱验证码
     */
    String sendEmailCode(String email, VerificationScene scene);

    /**
     * 邮箱验证码登录或注册
     */
    MiniLoginResult loginByEmail(String email, String code, String nickname, VerificationScene scene, String ip);

    Boolean isFreeDeposit();

    //统计2024年每个月注册人数
    //远程调用：统计用户注册数据
    Map<String, Object> getUserCount();

    void updateBalance(Long userId, BigDecimal balance);
}
