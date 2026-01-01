package com.share.device.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.device.domain.FeeRuleVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "计费规则接口管理")
@RestController
@RequestMapping("/feeRule")
public class FeeRuleController extends BaseController {

    @Operation(summary = "获取全部计费规则")
    @GetMapping("/getALLFeeRuleList")
    public AjaxResult getALLFeeRuleList() {
        List<FeeRuleVo> list = new ArrayList<>();
        // Mock data
        FeeRuleVo rule1 = new FeeRuleVo();
        rule1.setId(1L);
        rule1.setName("默认计费规则");
        list.add(rule1);
        
        FeeRuleVo rule2 = new FeeRuleVo();
        rule2.setId(2L);
        rule2.setName("VIP计费规则");
        list.add(rule2);

        return success(list);
    }
}
