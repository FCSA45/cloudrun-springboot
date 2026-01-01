package com.share.rule.api.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class FeeRuleResponseVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 免费时长描述
     */
    private String freeDescription;

    /**
     * 超出金额
     */
    private BigDecimal exceedPrice;

    /**
     * 超出描述
     */
    private String exceedDescription;

    /**
     * 免费价格
     */
    private BigDecimal freePrice;
}
