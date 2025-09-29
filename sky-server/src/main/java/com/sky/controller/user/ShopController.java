package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Api(tags = {"店铺相关接口"})
@RequestMapping("/user/shop")
@RestController("userShopController")
public class ShopController {

    @Autowired
    RedisTemplate redisTemplate;

    private static final String SHOP_STATUS="SHOP_STATUS";

    @ApiOperation("设置店铺营业状态")
    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置店铺营业状态为：{}",status==1 ? "营业中" : "打烊了");
        redisTemplate.opsForValue().set(SHOP_STATUS,status);
        return Result.success();
    }

    @ApiOperation("获取店铺营业状态")
    @GetMapping("/status")
    public Result<Integer> getStatus(){
        Integer status =(Integer) redisTemplate.opsForValue().get(SHOP_STATUS);
        log.info("获取到的店铺营业状态：{}",status==1 ? "营业中" : "打烊了");
        return Result.success(status);
    }
}
