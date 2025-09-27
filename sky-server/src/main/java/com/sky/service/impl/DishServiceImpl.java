package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    DishMapper dishMapper;
    @Autowired
    DishFlavorMapper dishFlavorMapper;
    @Autowired
    SetmealDishMapper setmealDishMapper;

    @Transactional
    @Override
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish=new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.insert(dish);
        Long id=dish.getId();

        //给口味的菜品id赋值
        List<DishFlavor> dishFlavors = dish.getFlavors();
        if(dishFlavors !=null&& !dishFlavors.isEmpty()){
            dishFlavors.forEach(dishFlavor -> dishFlavor.setDishId(id));
            dishFlavorMapper.insertBatch(dishFlavors);
        }




    }


    //菜品分页查询
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());

        Page<DishVO> page=dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }


    //批量删除菜品
    @Override
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能够删除--是否存在起售中的菜品
        for (Long id : ids) {
            Dish dish=dishMapper.getById(id);
            if(dish.getStatus()==1){
                throw new  DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        List<Long> setMealIds = setmealDishMapper.getByDishIds(ids);
        //判断当前菜品是否能够删除--是否被套餐关联了
        if(!setMealIds.isEmpty()){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品表中的菜品数据
            dishMapper.deleteByIds(ids);
            //删除菜品关联口味数据
            dishFlavorMapper.deleteDishIds(ids);



    }

    //查询回显
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        //获得菜品数据
        Dish dish = dishMapper.getById(id);
        //获得菜品口味数据
        List<DishFlavor> dishFlavors=dishFlavorMapper.getByDishId(id);

        DishVO dishVO=new DishVO();
        BeanUtils.copyProperties(dish,dishVO);

        if(!dishFlavors.isEmpty()){
            dishVO.setFlavors(dishFlavors);
        }
        return dishVO;

    }

    //修改菜品数据
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish=new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        //删除菜品关联的菜品口味
        dishFlavorMapper.deleteDishId(dish.getId());

        //修改菜品数据
        dishMapper.update(dish);

        List<DishFlavor> flavors = dish.getFlavors();
        //给每个口味添加上关联的菜品id
        if(!flavors.isEmpty()){
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dish.getId()));
            dishFlavorMapper.insertBatch(flavors);
        }


    }


    //根据分类id查询
    @Override
    public List<DishVO> getByCategoryId(Integer categoryId) {
        List<DishVO> dishVOS=dishMapper.getByCategoryId(categoryId);
        if(!dishVOS.isEmpty()){
            return dishVOS;
        }
        return null;
    }
}
