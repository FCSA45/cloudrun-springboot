package com.share.device.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.security.utils.SecurityUtils;
import com.share.device.domain.PowerBank;
import com.share.device.service.IPowerBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Security;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.share.common.core.utils.PageUtils.startPage;

@Tag(name = "充电宝接口管理")
@RestController
@RequestMapping("/powerBank")
public class PowerBankController extends BaseController {
    @Autowired
    private IPowerBankService powerBankService;
    @Operation(summary = "查询充电宝列表")
    @GetMapping("/list")
    public TableDataInfo list(PowerBank powerBank) {
        startPage();
        List<PowerBank> list = powerBankService.selectListPowerBank(powerBank);
        return getDataTable(list);
    }
    @Operation(summary = "获取充电宝详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id){

 return AjaxResult.success( powerBankService.getById(id));
}
    @Operation(summary = "新增充电宝")
    @PostMapping
    public AjaxResult add(@RequestBody PowerBank powerBank){
        powerBank.setCreateBy(SecurityUtils.getUsername());
        powerBank.setCreateTime(new Date());
        powerBank.setUpdateTime(new Date());
        int flag=powerBankService.savePowerBank(powerBank);
        return toAjax(flag);
    }
    @Operation(summary = "修改充电宝")
    @PutMapping
    public AjaxResult update(@RequestBody PowerBank powerBank)
    {
        powerBank.setUpdateBy(SecurityUtils.getUsername());
        powerBank.setUpdateTime(new Date());
    int flag=  powerBankService.setUpdatePowerBank(powerBank);
    return toAjax(flag);
    }
    @Operation(summary = "删除充电宝")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(powerBankService.removeBatchByIds(Arrays.asList(ids)));
    }
}
