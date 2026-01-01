package com.share.device.controller;

import com.share.common.core.constant.DeviceConstants;
import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.security.annotation.RequiresLogin;
import com.share.device.domain.ScanChargeVo;
import com.share.device.service.IDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.share.device.domain.StationVo;
import java.util.List;

@Tag(name = "设备接口管理")
@RestController
@RequestMapping("/device")
public class DeviceController extends BaseController
{
    @Autowired
    private IDeviceService deviceService;

    @Operation(summary = "获取附近站点信息列表")
    @GetMapping("/nearbyStationList/{latitude}/{longitude}")
    public AjaxResult nearbyStation(@PathVariable String latitude, @PathVariable String longitude)
    {
     List<StationVo> list= deviceService.nearbyStation(latitude, longitude, DeviceConstants.SEARCH_PC_RADIUS);
     return AjaxResult.success(list);
    }

    @Operation(summary = "同步站点数据到MongoDB")
    @GetMapping("/syncData")
    public AjaxResult syncData()
    {
        deviceService.syncData();
        return AjaxResult.success();
    }
    @Operation(summary = "扫码充电")
    @RequiresLogin
    @GetMapping("scanCharge/{cabinetNo}")
    public AjaxResult scanCharge(@PathVariable String cabinetNo) {
        ScanChargeVo scanChargeVo=    deviceService.scanCharge(cabinetNo);
        return success(scanChargeVo);
    }
}

