package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    SetmealMapper setmealMapper;
    @Autowired
    SetmealDishMapper setmealDishMapper;


    //新增套餐
    @Override
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal=new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.insert(setmeal);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(!setmealDishes.isEmpty()){
            setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmeal.getId()));
            setmealDishMapper.insertBatch(setmealDishes);
        }

    }
    //套餐分页查询
    @ApiOperation("分页查询")
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> setmealVOS=setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(setmealVOS.getTotal(),setmealVOS.getResult());
    }

    //查询回显
    @ApiOperation("查询回显")
    @Override
    public SetmealVO getById(Long id) {
        return setmealMapper.getById(id);
    }

    //修改套餐信息
    @ApiOperation("修改套餐")
    @Override
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal=new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);
        //删除套餐原本所关联的菜品
        setmealDishMapper.deletebySetmealId(setmeal.getId());
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (!setmealDishes.isEmpty()){
            setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmeal.getId()));
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }


    @ApiOperation("启用或禁用套餐")
    @Override
    public void startOrStop(Integer status,Long id) {
        Setmeal setmeal = new Setmeal();
        setmeal.setId(id);
        setmeal.setStatus(status);
        setmealMapper.update(setmeal);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if(ids.isEmpty()){
            throw new DeletionNotAllowedException(MessageConstant.SELECT_NULL);
        }
        //判断当前菜品是否能够删除--是否存在起售中的菜品
        for (Long id : ids) {
            SetmealVO setmealVO=setmealMapper.getById(id);
            if(setmealVO.getStatus()==1){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        setmealMapper.deleteByIds(ids);
        setmealDishMapper.deletebySetmealIds(ids);

    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Integer id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }


}
