package com.share.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.exception.ServiceException;
import com.share.device.domain.PowerBank;
import com.share.device.mapper.PowerBankMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PowerBankServiceImpl extends ServiceImpl<PowerBankMapper, PowerBank> implements IPowerBankService
{
    @Autowired
    private PowerBankMapper powerBankMapper;
    @Override
    public List<PowerBank> selectListPowerBank(PowerBank powerBank) {
     return    powerBankMapper.selectListPowerBank(powerBank);
    }

    @Override
    public int savePowerBank(PowerBank powerBank) {
     String PowerBankNo=  powerBank.getPowerBankNo();
     LambdaQueryWrapper<PowerBank> queryWrapper = new LambdaQueryWrapper<>();
     queryWrapper.eq(PowerBank::getPowerBankNo,PowerBankNo);

   Long count=  powerBankMapper.selectCount(queryWrapper);
   if(count>0){
       throw new SecurityException("该充电宝编号已存在");
   }
 int row=  powerBankMapper.insert(powerBank);
   return row;
    }

    @Override
    public int setUpdatePowerBank(PowerBank powerBank) {
        Long id= powerBank.getId();
     PowerBank oldPowerBank =  powerBankMapper.selectById(id);
        if ( oldPowerBank!= null && !"0".equals(oldPowerBank.getStatus())) {
        int row=    powerBankMapper.updateById(powerBank);
            return row;
        }
        return 0;
    }

    @Override
    public PowerBank getByPowerBankNo(String powerBankNo) {
        return powerBankMapper.selectOne(new LambdaQueryWrapper<PowerBank>().eq(PowerBank::getPowerBankNo, powerBankNo));
    }
}