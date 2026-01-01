package com.share.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.device.domain.Cabinet;
import com.share.device.domain.CabinetType;

import java.util.List;

public interface ICabinetService extends IService<Cabinet> {
    List<Cabinet> selectCabinetList(Cabinet cabinet);

    List<Cabinet> searchNoUseList(String keyword);

    Object getAllInfo(Long id);
    Cabinet getBtCabinetNo(String cabinetNo);
}
