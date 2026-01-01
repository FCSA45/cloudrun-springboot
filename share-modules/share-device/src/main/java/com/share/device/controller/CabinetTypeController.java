package com.share.device.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.log.annotation.Log;
import com.share.common.log.enums.BusinessType;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.device.domain.CabinetType;
import com.share.device.service.ICabinetTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.share.common.core.utils.PageUtils.startPage;

@Tag(name = "柜机类型接口管理")
@RestController
@RequestMapping("/cabinetType")
public class CabinetTypeController extends BaseController {
    @Autowired
    private ICabinetTypeService cabinetTypeService;
    @Operation(summary="柜机类型分页查询")
    @RequiresPermissions("device:cabinetType:list")
    @GetMapping("/list")
    public TableDataInfo list(CabinetType cabinetType) {
        startPage();
        List<CabinetType> list=cabinetTypeService.selectCabinetTypeList(cabinetType);
        return getDataTable(list);
    }

    @Operation(summary="获取柜机类型详细信息")
    @RequiresPermissions("device:cabinetType:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(cabinetTypeService.getById(id));
    }

    @Operation(summary = "新增柜机类型")
    @RequiresPermissions("device:cabinetType:add")
    @Log(title = "柜机类型", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CabinetType cabinetType) {
        return toAjax(cabinetTypeService.save(cabinetType));
    }

    @Operation(summary = "修改柜机类型")
    @RequiresPermissions("device:cabinetType:edit")
    @Log(title = "柜机类型", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CabinetType cabinetType) {
        return toAjax(cabinetTypeService.updateById(cabinetType));
    }

    @Operation(summary = "删除柜机类型")
    @RequiresPermissions("device:cabinetType:remove")
    @Log(title = "柜机类型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cabinetTypeService.removeBatchByIds(Arrays.asList(ids)));
    }
    @Operation(summary = "查询全部柜机类型列表")
    @GetMapping("/getCabinetTypeList")
    public AjaxResult getCabinetTypeList()
    {
        return success(cabinetTypeService.list());
    }
}

