package com.share.device.domain;

import lombok.Data;

@Data
public class FeeRuleVo {
    private Long id;
    private String name;
    private String feeRuleName; // Frontend expects feeRuleName in some places?
    // In 05_...md: <el-table-column label="费用规则" prop="feeRuleName" />
    // In form: label="item.name" or "item.feeRuleName"?
    // In 05_...md: <el-option ... :label="item.feeRuleName" ... /> ?
    // Let's check the vue file again.
}
