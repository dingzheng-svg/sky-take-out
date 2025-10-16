package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.BaseException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.BaiduMapUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {


    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BaiduMapUtil baiduMapUtil;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional

    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) throws NoSuchFieldException, IllegalAccessException {


        //判断地址是否为空
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook addressBook = addressBookMapper.getById(addressBookId);

        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //判断用户收货地址是否超过5公里
        //baiduMapUtil.isOverDistance(addressBook.getWholeAddress());

        //判断购物车是否为空
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(shoppingCart);
        if (shoppingCarts == null || shoppingCarts.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }


        //向订单表插入数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setAddress(addressBook.getDetail());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orderMapper.insert(orders);


        //向订单明细表插入数据
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart cart : shoppingCarts) {
            OrderDetail orderDetail = new OrderDetail();//订单明细对象
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());//设置订单id
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);


        //清空购物车数据
        shoppingCartMapper.deleteByUserId(userId);//userId在判断购物车是否为空时，已获取

        //封装vo返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

/*        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }*/
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        //为替代微信支付成功后的数据库订单状态更新，多定义一个方法进行修改
        Integer orderPaidStatus = Orders.PAID;
        Integer orderStatus = Orders.TO_BE_CONFIRMED;
        //发现没有将支付时间 check out属性赋值，所以在这里更新
        LocalDateTime check_out_time = LocalDateTime.now();
        //获取订单号码
        String orderNumber = ordersPaymentDTO.getOrderNumber();
        Orders ordersDB=orderMapper.getByNumber(orderNumber);
        log.info("调用update,用于替换微信支付更新数据状态问题");
        Orders orders = Orders.builder()
                .status(orderStatus)
                .payStatus(orderPaidStatus)
                .checkoutTime(check_out_time)
                .number(orderNumber)
                .build();

        //向管理端发送消息
        Map map=new HashMap<>();
        map.put("type",1);
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号："+ordersDB.getNumber());
        String jsonString = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(jsonString);
        orderMapper.update(orders);

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 订单分页查询
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        Orders orders = Orders.builder()
                .userId(BaseContext.getCurrentId())
                .number(ordersPageQueryDTO.getNumber())
                .phone(ordersPageQueryDTO.getPhone())
                .status(ordersPageQueryDTO.getStatus())
                .build();
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<OrderVO> page = (Page<OrderVO>) orderMapper.list(orders, ordersPageQueryDTO.getBeginTime(), ordersPageQueryDTO.getEndTime());
        return new PageResult(page.size(), page.getResult());
    }

    /**
     * 获取订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO getOrderDetail(Long id) {

        Orders orders = Orders.builder().id(id).userId(BaseContext.getCurrentId()).build();

        List<OrderVO> list = orderMapper.list(orders, null, null);

        OrderVO orderVO = list.get(0);
        StringBuffer orderDishes = new StringBuffer();
        orderVO.getOrderDetailList().forEach(orderDetail -> {
            orderDishes.append(orderDetail.getName()).append(" ");
        });
        orderVO.setOrderDishes(orderDishes.toString());
        return orderVO;
    }

    /**
     * 再来一单
     *
     * @param id
     */
    @Override
    public void repetition(Long id) {
        Orders orders = Orders.builder()
                .id(id)
                .build();
        orders = orderMapper.list(orders, null, null).get(0);
        List<OrderDetail> orderDetailList = orders.getOrderDetailList();
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> shoppingCarts = new ArrayList<>();
        //获取菜品到购物车
        if (orderDetailList != null && !orderDetailList.isEmpty()) {
            for (OrderDetail detail : orderDetailList) {
                ShoppingCart shoppingCart = new ShoppingCart();
                BeanUtils.copyProperties(detail, shoppingCart);
                shoppingCart.setUserId(userId);
                shoppingCarts.add(shoppingCart);
            }
        }
        shoppingCartMapper.insertBatch(shoppingCarts);
    }

    /**
     * 取消订单
     *
     * @param id
     */
    @Override
    public void cancel(Long id) {
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.CANCELLED)
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }


    /**
     * 接单
     *
     * @param ordersDTO
     */
    @Override
    public void confirm(OrdersDTO ordersDTO) {
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersDTO, orders);
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);
    }

    /**
     * 派送
     *
     * @param id
     */
    @Override
    public void delivery(Long id) {
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        orderMapper.update(orders);

    }

    /**
     * 完成订单
     */
    @Override
    public void complete(Long id) {
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = Orders.builder().status(Orders.CANCELLED).build();
        BeanUtils.copyProperties(ordersRejectionDTO, orders);
        orders.setPayStatus(Orders.REFUND);
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 各状态订单数据统计
     *
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        List<Map<String, Object>> statistics = orderMapper.statistics();

        List<Object> orderStatus = statistics.stream().map(statisticsMap ->
                statisticsMap.get("order_status")
        ).collect(Collectors.toList());

        List<Object> statusNumber = statistics.stream().map(statisticsMap ->
                statisticsMap.get("num")
        ).collect(Collectors.toList());
        //初始化数据
        orderStatisticsVO.setDeliveryInProgress(0L);
        orderStatisticsVO.setConfirmed(0L);
        orderStatisticsVO.setToBeConfirmed(0L);
        //遍历集合，从中获取数据
        for (int i = 0; i < orderStatus.size(); i++) {
            String status=(String) orderStatus.get(i);
            if (status.equals("toBeConfirmed")){
                orderStatisticsVO.setToBeConfirmed((Long) statusNumber.get(i));
            }else if (status.equals("confirmed")){
                orderStatisticsVO.setConfirmed((Long) statusNumber.get(i));
            }else if (status.equals("deliveryInProgress")){
                orderStatisticsVO.setDeliveryInProgress((Long) statusNumber.get(i));
            }
        }

        return orderStatisticsVO;

    }
}
