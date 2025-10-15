package com.sky.config;

import com.sky.properties.BaiduMapProperties;
import com.sky.utils.BaiduMapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class BaiduConfiguration {

    @Bean

    public BaiduMapUtil baiduMapUtil(BaiduMapProperties baiduMapProperties){
        log.info("开始创建百度地图工具对象:{}",baiduMapProperties);
        return new BaiduMapUtil(baiduMapProperties);
    }
}
