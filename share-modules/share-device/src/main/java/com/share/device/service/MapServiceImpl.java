package com.share.device.service;

import cn.hutool.json.JSONObject;
import com.share.common.core.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@SuppressWarnings({"unchecked","rawtypes"})
public class MapServiceImpl implements IMapService {
@Autowired
private RestTemplate restTemplate;
private  String key="IIYBZ-PUEEG-PTBQL-QRZUC-N76E6-OEBUA";
    @Override
    public Double calculateDistance(String startLongitude, String startLatitude, String endLongitude, String endLatitude) {
        String url = "https://apis.map.qq.com/ws/direction/v1/walking/?from={from}&to={to}&key={key}";

        Map<String, String> map = new HashMap<>();
        map.put("from", startLatitude + "," + startLongitude);
        map.put("to", endLatitude + "," + endLongitude);
        map.put("key", key);

        JSONObject result = restTemplate.getForObject(url, JSONObject.class, map);
        if(result.getInt("status") != 0) {
            // 记录错误日志但不抛出异常，避免影响主流程
            log.error("地图服务调用失败: {}", result);
            // 返回默认距离或估算距离
            Double distance = getDistance(Double.parseDouble(startLatitude), Double.parseDouble(startLongitude), Double.parseDouble(endLatitude), Double.parseDouble(endLongitude));
            return new BigDecimal(distance / 1000).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        //返回第一条最佳线路
        JSONObject route = result.getJSONObject("result").getJSONArray("routes").getJSONObject(0);
        // 单位：米
        Double distance = route.getBigDecimal("distance").doubleValue();
        return new BigDecimal(distance / 1000).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 地球半径,单位 m
     */
    private static final double EARTH_RADIUS = 6378137;

    /**
     * 根据经纬度，计算两点间的距离
     *
     * @param latitude1  第一个点的纬度
     * @param longitude1 第一个点的经度
     * @param latitude2  第二个点的纬度
     * @param longitude2 第二个点的经度
     * @return 返回距离 单位米
     */
    public static Double getDistance(Double latitude1, Double longitude1, Double latitude2, Double longitude2) {
        // 纬度
        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);
        // 经度
        double lng1 = Math.toRadians(longitude1);
        double lng2 = Math.toRadians(longitude2);
        // 纬度之差
        double a = lat1 - lat2;
        // 经度之差
        double b = lng1 - lng2;
        // 计算两点距离的公式
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(b / 2), 2)));
        // 弧长乘地球半径, 返回单位: 米
        s = s * EARTH_RADIUS;
        return s;
    }
}
