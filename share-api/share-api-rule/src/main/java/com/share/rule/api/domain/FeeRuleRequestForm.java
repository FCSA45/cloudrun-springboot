package com.share.rule.api.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class FeeRuleRequestForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 计费规则ID
     */
    private Long feeRuleId;

    /**
     * 租借时长（分钟）
     */
    private Integer duration;
}
