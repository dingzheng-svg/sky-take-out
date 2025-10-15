package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    /**
     * 定时订单超时处理
     */
    //@Scheduled(cron = "1/5 * * * * *")  //测试用
    @Scheduled(cron = "0 * * * * *")
    public void processTimeOutOrder(){
        log.info("定时处理超时订单：{}", LocalDateTime.now());
        LocalDateTime time=LocalDateTime.now().plusMinutes(-15);
        List<Orders> orders=orderMapper.getTimeOutOrder(Orders.PENDING_PAYMENT,time);
        if (orders!=null&&!orders.isEmpty()) {
            for (Orders order : orders) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单支付超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    /**
     * 定时处理派送中订单
     */
    //@Scheduled(cron = "0/5 * * * * ?") //测试用
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        log.info("定时处理派送中订单");
        LocalDateTime time=LocalDateTime.now().plusHours(-1);

        List<Orders> orders=orderMapper.getTimeOutOrder(Orders.DELIVERY_IN_PROGRESS,time);

        if (orders!=null&&!orders.isEmpty()) {
            for (Orders order : orders) {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }

}
