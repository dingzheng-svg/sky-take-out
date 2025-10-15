package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 输入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 订单列表查询
     * @param orders
     * @param beginTime
     * @param endTime
     * @return
     */
    List<OrderVO> list(Orders orders, LocalDateTime beginTime, LocalDateTime endTime);

    List<Map<String,Object>> statistics();

    @Select("select * from orders where status=#{status} and order_time< #{time}")
    List<Orders> getTimeOutOrder(Integer status, LocalDateTime time);
}
