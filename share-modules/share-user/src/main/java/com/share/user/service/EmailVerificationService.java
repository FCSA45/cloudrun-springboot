package com.share.user.service;

import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.StringUtils;
import com.share.common.redis.service.RedisService;
import com.share.user.config.EmailProperties;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class EmailVerificationService {

    private static final String KEY_TEMPLATE = "user:email:code:%s:%s";
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;
    private final RedisService redisService;

    public EmailVerificationService(JavaMailSender mailSender,
                                    EmailProperties emailProperties,
                                    RedisService redisService) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
        this.redisService = redisService;
    }

    public String sendCode(String email, VerificationScene scene) {
        if (!EmailValidator.getInstance().isValid(email)) {
            throw new ServiceException("邮箱格式不正确");
        }
        String cacheKey = buildKey(scene, email);
        String code = RandomStringUtils.randomNumeric(6);
        redisService.setCacheObject(cacheKey, code, emailProperties.getTtlMinutes(), TimeUnit.MINUTES);

        if (emailProperties.isMockEnabled()) {
            log.info("[MockEmail] 向 {} 发送验证码 {}，场景 {}", email, code, scene.name());
            return code;
        }

        if (StringUtils.isBlank(emailProperties.getFrom())) {
            throw new ServiceException("邮箱发送方未配置，请设置 auth.email.from");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailProperties.getFrom());
        message.setTo(email);
        message.setSubject(String.format("%s-%s", emailProperties.getSubjectPrefix(), scene.getDisplayName()));
        message.setText(String.format("您的验证码是 %s ，有效期 %d 分钟。若非本人操作请忽略本邮件。",
                code, emailProperties.getTtlMinutes()));
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("发送邮箱验证码失败", ex);
            throw new ServiceException("发送邮箱验证码失败，请稍后重试");
        }
        return null;
    }

    public void validateCode(String email, String code, VerificationScene scene) {
        String cacheKey = buildKey(scene, email);
        String cached = redisService.getCacheObject(cacheKey);
        if (StringUtils.isBlank(cached)) {
            throw new ServiceException("验证码已失效，请重新获取");
        }
        if (!StringUtils.equalsIgnoreCase(code, cached)) {
            throw new ServiceException("验证码错误");
        }
        redisService.deleteObject(cacheKey);
    }

    private String buildKey(VerificationScene scene, String email) {
        return String.format(Locale.ROOT, KEY_TEMPLATE, scene.name(), email.toLowerCase(Locale.ROOT));
    }

    public enum VerificationScene {
        REGISTER("注册"),
        LOGIN("登录");

        private final String displayName;

        VerificationScene(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
