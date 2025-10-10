package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/admin/setmeal")
@Api(tags = {"套餐相关接口"})
@RestController
public class SetmealController {
    @Autowired
    SetmealService setmealService;


    @PostMapping
    @CacheEvict(cacheNames = "setmealCache",key = "#setmealDTO.categoryId")
    @ApiOperation("新增套餐")
    public Result save(@RequestBody SetmealDTO setmealDTO){
         log.info("要添加的套餐信息:{}",setmealDTO);
         setmealService.save(setmealDTO);
         return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("分页查询数据：{}",setmealPageQueryDTO);
        PageResult pageResult=setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @ApiOperation("查询回显")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("要查询的套餐id:{}",id);
        SetmealVO setmealVO=setmealService.getById(id);
        return Result.success(setmealVO);
    }

    @ApiOperation("修改套餐")
    @CacheEvict(cacheNames = "setmealCache",allEntries = true)
    @PutMapping
    public Result update(@RequestBody SetmealDTO setmealDTO){
        log.info("要修改的套餐信息:{}",setmealDTO);
        setmealService.update(setmealDTO);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    @CacheEvict(cacheNames = "setmealCache",allEntries = true)
    @ApiOperation("启用或禁用套餐")
    public Result startOrStop(@PathVariable Integer status,Long id){
        log.info("启用或禁用：{}",status);
        setmealService.startOrStop(status,id);
        return Result.success();
    }

    @ApiOperation("删除套餐")
    @CacheEvict(cacheNames = "setmealCache",allEntries = true)
    @DeleteMapping
    public Result delete(@RequestParam List<Long> ids){
        log.info("要删除的套餐的id：{}",ids);
        setmealService.deleteByIds(ids);
        return Result.success();
    }
}
