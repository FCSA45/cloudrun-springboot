package com.share.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.device.domain.Cabinet;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CabinetMapper extends BaseMapper<Cabinet> {
    List<Cabinet> selectCabinetList(Cabinet cabinet);
}
