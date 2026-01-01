package com.share.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.email")
public class EmailProperties {

    /** QQ邮箱账号，用作发件人 */
    private String from;

    /** 邮件标题前缀 */
    private String subjectPrefix = "验证码";

    /** 验证码有效时间（分钟） */
    private long ttlMinutes = 10;

    /** 是否启用模拟模式，不实际发送邮件，仅返回验证码 */
    private boolean mockEnabled = true;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    public void setSubjectPrefix(String subjectPrefix) {
        this.subjectPrefix = subjectPrefix;
    }

    public long getTtlMinutes() {
        return ttlMinutes;
    }

    public void setTtlMinutes(long ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }
}
