package com.sky.utils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.exception.OrderBusinessException;
import com.sky.properties.BaiduMapProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class BaiduMapUtil {
    private BaiduMapProperties baiduMapProperties;

    public BaiduMapUtil(BaiduMapProperties baiduMapProperties){
        this.baiduMapProperties=baiduMapProperties;
    }

    /**
     * 从百度地图获取地址的经纬度
     * @param address
     * @return

     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private String getLocation(String address) throws NoSuchFieldException, IllegalAccessException {


        Map<String,String> paramMap=new HashMap<>();
        paramMap.put("ak",baiduMapProperties.getAk());
        paramMap.put("address",address);
        paramMap.put("output","json");
        String response = HttpClientUtil.doGet(baiduMapProperties.getLUrl(), paramMap);

        JSONObject jsonObject=JSONObject.parseObject(response);


        if (!jsonObject.getString("status").equals("0")){
            log.info("百度地图响应信息：{}",jsonObject);
            throw new OrderBusinessException(address+":"+"地址请求失败");
        }
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lng=location.getString("lng");
        String lat=location.getString("lat");
        return lat+","+lng;

    }

    /**
     * 判断距离是否超过五公里
     * @param destinationAddress
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public void isOverDistance(String destinationAddress) throws  NoSuchFieldException, IllegalAccessException {
        Map<String,String> map=new HashMap<>();
        String origin=getLocation(baiduMapProperties.getAddress());//商铺的经纬度
        String destination=getLocation(destinationAddress);//目的地的经纬度
        //发送请求
        map.put("ak",baiduMapProperties.getAk());
        map.put("origin",origin);
        map.put("destination",destination);
        String response = HttpClientUtil.doGet(baiduMapProperties.getUrl(), map);
        //解析请求结果

        JSONObject jsonObject=JSONObject.parseObject(response);
        if (!jsonObject.getString("status").equals("0")){
            log.info("百度地图响应信息：{}",jsonObject);
            throw new OrderBusinessException("路线规划失败");
        }
        JSONArray jsonArray = jsonObject.getJSONObject("result").getJSONArray("routes");


        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000) {
            //配送距离超过5000米
            log.info("距离：{}",distance);
            throw new OrderBusinessException("超出配送范围");

        }

    }
}
