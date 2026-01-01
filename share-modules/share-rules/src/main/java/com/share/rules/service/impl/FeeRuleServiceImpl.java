package com.share.rules.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.rule.api.domain.FeeRule;
import com.share.rule.api.domain.FeeRuleRequestForm;
import com.share.rule.api.domain.FeeRuleResponseVo;
import com.share.rules.domain.vo.FeeRuleRequest;
import com.share.rules.domain.vo.FeeRuleResponse;
import com.share.rules.mapper.FeeRuleMapper;
import com.share.rules.service.IFeeRuleService;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 费用规则Service业务层处理
 *
 * @author atguigu
 * @date 2024-10-25
 */
@Slf4j
@Service
public class FeeRuleServiceImpl extends ServiceImpl<FeeRuleMapper, FeeRule> implements IFeeRuleService
{
    @Autowired
    private FeeRuleMapper feeRuleMapper;

    @Autowired
    private KieContainer kieContainer;

    @Override
    public FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm feeRuleRequestForm) {
        KieSession kieSession = kieContainer.newKieSession();
        FeeRuleRequest feeRuleRequest=new FeeRuleRequest();
feeRuleRequest.setDurations(feeRuleRequestForm.getDuration());
FeeRuleResponse feeRuleResponse=new FeeRuleResponse();
kieSession.setGlobal("feeRuleResponse", feeRuleResponse);
kieSession.insert(feeRuleRequest);
kieSession.fireAllRules();
kieSession.dispose();
        FeeRuleResponseVo feeRuleResponseVo=new FeeRuleResponseVo();
        feeRuleResponseVo.setTotalAmount(new BigDecimal(feeRuleResponse.getTotalAmount()));
        feeRuleResponseVo.setFreePrice(new BigDecimal(feeRuleResponse.getFreePrice()));
        feeRuleResponseVo.setExceedPrice(new BigDecimal(feeRuleResponse.getExceedPrice()));
        feeRuleResponseVo.setFreeDescription(feeRuleResponse.getFreeDescription());
        return feeRuleResponseVo;
    }


    //计算订单费
}
