package com.sky.properties;

import lombok.Data;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "sky.baidu")
public class BaiduMapProperties {
    private String ak;
    private String url;  //路线规划地址
    private String lUrl; //地理编码地址
    private String address; //商铺地址
}
