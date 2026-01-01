package com.share.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.device.domain.Cabinet;
import com.share.device.domain.CabinetSlot;
import com.share.device.mapper.CabinetMapper;
import com.share.device.mapper.CabinetSlotMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CabinetServiceImpl extends ServiceImpl<CabinetMapper, Cabinet> implements ICabinetService {
    @Autowired
    private CabinetMapper cabinetMapper;
    @Autowired
    private CabinetSlotMapper cabinetSlotMapper;

    @Override
    public List<Cabinet> selectCabinetList(Cabinet cabinet)
    {
        return cabinetMapper.selectCabinetList(cabinet);
    }

    @Override
    public List<Cabinet> searchNoUseList(String keyword) {
        LambdaQueryWrapper<Cabinet> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Cabinet::getCabinetNo, keyword);
        queryWrapper.eq(Cabinet::getStatus, 0);
    List<Cabinet> list=   cabinetMapper.selectList(queryWrapper);
    return list;
    }

    @Override
    public Object getAllInfo(Long id) {
        Cabinet cabinet = cabinetMapper.selectById(id);
        LambdaQueryWrapper<CabinetSlot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CabinetSlot::getCabinetId, id);
        List<CabinetSlot> cabinetSlotList = cabinetSlotMapper.selectList(queryWrapper);
        Map<String, Object> map = new HashMap<>();
        map.put("cabinet", cabinet);
        map.put("cabinetSlotList", cabinetSlotList);
        return map;
    }

    @Override
    public Cabinet getBtCabinetNo(String cabinetNo) {
        return cabinetMapper.selectOne(new LambdaQueryWrapper<Cabinet>().eq(Cabinet::getCabinetNo, cabinetNo));
    }
}
