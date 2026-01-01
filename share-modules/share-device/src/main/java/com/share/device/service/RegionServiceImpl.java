package com.share.device.service;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.utils.StringUtils;
import com.share.device.domain.Region;
import com.share.device.mapper.RegionMapper;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegionServiceImpl extends ServiceImpl<RegionMapper, Region> implements IRegionService {
    @Autowired
    private RegionMapper regionMapper;

    @Override
    public List<Region> treeSelect(String code) {
        List<Region> regionList = regionMapper.selectList(new LambdaQueryWrapper<Region>().eq(Region::getParentCode, code));
        if (!CollectionUtils.isEmpty(regionList)) {
            regionList.forEach(region -> {
                long count = regionMapper.selectCount(new LambdaQueryWrapper<Region>().eq(Region::getParentCode, region.getCode()));
                if (count > 0) {
                    region.setHasChildren(true);
                } else {
                    region.setHasChildren(false);
                }
            });
        }
        return regionList;
    }

    @Override
    public String getNameByCode(String code) {
        if (StringUtils.isEmpty(code)) {
            return "";
        }
        Region region=regionMapper.selectOne(new LambdaQueryWrapper<Region>().eq(Region::getCode, code));
        if (region == null) {
            return region.getName();
        }
        return  "";
    }
}

