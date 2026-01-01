package com.share.rules.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.rule.api.domain.FeeRule;
import com.share.rule.api.domain.FeeRuleRequestForm;
import com.share.rule.api.domain.FeeRuleResponseVo;
import com.share.rules.domain.vo.FeeRuleRequest;
import com.share.rules.domain.vo.FeeRuleResponse;

public interface IFeeRuleService extends IService<FeeRule>{
    FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm feeRuleRequest);
}
