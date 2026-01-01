package com.share.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.device.domain.PowerBank;

import java.util.List;

/**
 * 充电宝Mapper接口
 *
 * @author atguigu
 * @date 2024-10-22
 */
public interface PowerBankMapper extends BaseMapper<PowerBank>
{
    List<PowerBank> selectListPowerBank(PowerBank powerBank);
}