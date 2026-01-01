package com.share.device.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.device.domain.Cabinet;
import com.share.device.service.ICabinetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Tag(name = "充电宝柜机接口管理")
@RestController
@RequestMapping("/cabinet")
public class CabinetController extends BaseController {
    @Autowired
    private ICabinetService cabinetService;
    @Operation(summary = "查询充电宝柜机列表")
    @GetMapping("/list")
    public TableDataInfo list(Cabinet cabinet){
        startPage();
     List<Cabinet> list  = cabinetService.selectCabinetList(cabinet);
     TableDataInfo dataInfo = getDataTable(list);
     return dataInfo;
    }
    @Operation(summary = "获取充电宝柜机详细信息")
    @GetMapping("{id}")
    public AjaxResult getInfo(@PathVariable Long id){
    Cabinet cabinet=    cabinetService.getById(id);
    return AjaxResult.success(cabinet);
    }
    @Operation(summary = "新增充电宝柜机")
    @PostMapping
    public AjaxResult add(@RequestBody Cabinet cabinet){
    boolean flag=     cabinetService.save(cabinet);
       AjaxResult ajaxResult = toAjax(flag);
       return ajaxResult;
    }
    @Operation(summary = "修改充电宝柜机")
    @PutMapping
    public AjaxResult edit(@RequestBody Cabinet cabinet)
    {
        return toAjax(cabinetService.updateById(cabinet));
    }
    @Operation(summary = "删除充电宝柜机")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
    boolean flag=    cabinetService.removeBatchByIds(Arrays.asList(ids));
return toAjax(flag);
    }
    @Operation(summary = "搜索未使用柜机")
    @GetMapping(value = "/searchNoUseList/{keyword}")
    public AjaxResult searchNoUseList(@PathVariable String keyword){
        return success(cabinetService.searchNoUseList(keyword));
    }

    @Operation(summary = "获取充电宝柜机全部详细信息")
    @GetMapping(value = "/getAllInfo/{id}")
    public AjaxResult getAllInfo(@PathVariable Long id){
        return success(cabinetService.getAllInfo(id));
    }
}
