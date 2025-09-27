package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {
    //添加口味数据
    void insertBatch(List<DishFlavor> dishFlavors);

    //根据菜品id删除口味数据(批量删除)

    void deleteDishIds(List<Long> dishIds);

    @Select("select * from dish_flavor where dish_id=#{dishId}")
    List<DishFlavor> getByDishId(Long dishId);

    @Delete("delete from dish_flavor where dish_id=#{dishId}")
    void deleteDishId(Long dishId);
}
