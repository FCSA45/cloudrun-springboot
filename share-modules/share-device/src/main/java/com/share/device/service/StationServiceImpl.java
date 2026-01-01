package com.share.device.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.device.domain.Cabinet;
import com.share.device.domain.Station;
import com.share.device.domain.StationLocation;
import com.share.device.mapper.StationMapper;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import com.share.device.service.ICabinetService;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import com.share.device.repository.StationLocationRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StationServiceImpl extends ServiceImpl<StationMapper, Station> implements IStationService
{
    @Autowired
    private    StationLocationRepository stationLocationRepository;
    @Autowired
    private IRegionService regionService;
    @Autowired
    private StationMapper stationMapper;
    @Autowired
    private ICabinetService cabinetService;
    @Override
    public List<Station> selectStationList(Station station) {
        List<Station>   list= stationMapper.selectStationList(station);
        for (Station station1: list) {
            Long station1Id = station1.getCabinetId();

       Cabinet cabinet= cabinetService.getById(station1Id);
     String cabinetNo=  cabinet.getCabinetNo();
     station1.setCabinetNo(cabinetNo);
        }
        return list;
    }

    @Override
    public int saveStation(Station station) {

        String provinceName = regionService.getNameByCode(station.getProvinceCode());
        String cityName = regionService.getNameByCode(station.getCityCode());
        String districtName = regionService.getNameByCode(station.getDistrictCode());
        station.setFullAddress(provinceName + cityName + districtName + station.getAddress());
        this.save(station);
        int row=  baseMapper.insert(station);
        StationLocation stationLocation = new StationLocation();
        stationLocation.setId(ObjectId.get().toString());
        stationLocation.setStationId(station.getId());
        stationLocation.setLocation(new GeoJsonPoint(station.getLongitude().doubleValue(), station.getLatitude().doubleValue()));
        stationLocation.setCreateTime(new Date());
        stationLocationRepository.save(stationLocation);
      return row;
    }

    @Override
    public int updateStation(Station station) {
        String provinceName = regionService.getNameByCode(station.getProvinceCode());
        String cityName = regionService.getNameByCode(station.getCityCode());
        String districtName = regionService.getNameByCode(station.getDistrictCode());
        station.setFullAddress(provinceName + cityName + districtName + station.getAddress());
        this.updateById(station);
        StationLocation stationLocation = stationLocationRepository.getByStationId(station.getId());
        stationLocation.setLocation(new GeoJsonPoint(station.getLongitude().doubleValue(), station.getLatitude().doubleValue()));
        stationLocationRepository.save(stationLocation);
        int row=  baseMapper.updateById(station);
        return row;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int setData(Station station) {
        this.updateById(station);

        //更正柜机使用状态
        Cabinet cabinet = cabinetService.getById(station.getCabinetId());
        cabinet.setStatus("1");
        cabinetService.updateById(cabinet);
        return 1;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeByIds(Collection<?> list) {
        for (Object id : list) {
            stationLocationRepository.deleteByStationId(Long.parseLong(id.toString()));
        }
        return super.removeByIds(list);
    }
}