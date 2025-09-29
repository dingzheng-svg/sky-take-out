package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/admin/dish")
@RestController
@Api(tags = {"菜品管理"})
public class DishController {
    @Autowired
    DishService dishService;
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品信息");
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询信息：{}",dishPageQueryDTO);
        PageResult pageResult=dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    @ApiOperation("批量删除菜品")
    public Result delete(@RequestParam List<Long> ids){
        log.info("要删除的菜品ids");
        dishService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("查询回显")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("要查询回显的菜品的id:{}",id);
        DishVO dishVO=dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    @PutMapping
    @ApiOperation("修改菜品数据")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("要修改的菜品数据");
        dishService.updateWithFlavor(dishDTO);
        return Result.success();
    }

    @ApiOperation("启售或停售")
    @PostMapping("/status/{status}")
    public Result startOrStop(@PathVariable Integer status,Long id){
        log.info("更改菜品的id：{}，更改菜品的状态：{}",id,status);
        DishDTO dishDTO = new DishDTO();
        dishDTO.setId(id);
        dishDTO.setStatus(status);
        dishService.updateWithFlavor(dishDTO);
        return Result.success();
    }

    @ApiOperation("根据分类id查询菜品")
    @GetMapping("/list")
    public Result<List<DishVO>> list(Integer categoryId){
        log.info("要查询的分类id:{}",categoryId);
        List<DishVO> dishVOS=dishService.getByCategoryId(categoryId);
        return Result.success(dishVOS);
    }
}
