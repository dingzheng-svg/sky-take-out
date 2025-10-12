package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    public static final String WECHAT_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    WeChatProperties weChatProperties;

    @Autowired
    UserMapper userMapper;

    //微信登录
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        String openid = getOpenid(userLoginDTO.getCode());
        //判断openid是否为空，如果为空表示登录失败，抛出业务异常
        if(openid==null|| openid.isEmpty()) {
            throw new LoginFailedException("微信登录失败");
        }
        //判断当前用户是否为新用户
            User user=userMapper.getByOpenId(openid);
        if (user==null){
            //新用户自动完成注册
            user= User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        return user;
    }
    private String getOpenid(String code){
        //调用微信接口服务，获取当前微信用户的openid
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String data = HttpClientUtil.doGet(WECHAT_LOGIN, map);
        JSONObject jsonObject = JSON.parseObject(data);
        return (String) jsonObject.get("openid");
    }
}
