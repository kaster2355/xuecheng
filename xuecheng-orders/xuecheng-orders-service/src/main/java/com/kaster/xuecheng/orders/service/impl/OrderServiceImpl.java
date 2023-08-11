package com.kaster.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaster.xuecheng.base.exception.XuechengException;
import com.kaster.xuecheng.base.utils.IdWorkerUtils;
import com.kaster.xuecheng.base.utils.QRCodeUtil;
import com.kaster.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.kaster.xuecheng.orders.mapper.XcOrdersMapper;
import com.kaster.xuecheng.orders.mapper.XcPayRecordMapper;
import com.kaster.xuecheng.orders.model.dto.AddOrderDto;
import com.kaster.xuecheng.orders.model.dto.PayRecordDto;
import com.kaster.xuecheng.orders.model.po.XcOrders;
import com.kaster.xuecheng.orders.model.po.XcOrdersGoods;
import com.kaster.xuecheng.orders.model.po.XcPayRecord;
import com.kaster.xuecheng.orders.service.OrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private XcOrdersMapper ordersMapper;

    @Autowired
    private XcOrdersGoodsMapper ordersGoodsMapper;

    @Autowired
    private XcPayRecordMapper payRecordMapper;

    @Autowired
    private OrderService currentProxy;

    @Value("${pay.qrcodeurl}")
    private String qrcodeUrl;

    @Override
    @Transactional
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {

        // 插入订单表
        XcOrders orders = currentProxy.saveXcOrders(userId, addOrderDto);
        if(orders==null){
            XuechengException.cast("订单创建失败");
        }
        if(orders.getStatus().equals("600002")){
            XuechengException.cast("订单已支付");
        }

        // 插入支付记录
        XcPayRecord payRecord = currentProxy.createPayRecord(orders);

        // 生成二维码
        String qrCode = null;
        try {
            String url = String.format(qrcodeUrl, payRecord.getPayNo());
            qrCode = new QRCodeUtil().createQRCode(url, 200, 200);
        } catch (IOException e) {
            XuechengException.cast("生成二维码出错");
        }

        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord,payRecordDto);
        payRecordDto.setQrcode(qrCode);
        return payRecordDto;
    }

    @Transactional
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto){
        // 幂等性
        XcOrders order = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if (order != null){
            return order;
        }
        order = new XcOrders();
        long orderId = IdWorkerUtils.getInstance().nextId();
        order.setId(orderId);
        order.setTotalPrice(addOrderDto.getTotalPrice());
        order.setCreateDate(LocalDateTime.now());
        order.setStatus("600001");//未支付
        order.setUserId(userId);
        order.setOrderType(addOrderDto.getOrderType());
        order.setOrderName(addOrderDto.getOrderName());
        order.setOrderDetail(addOrderDto.getOrderDetail());
        order.setOrderDescrip(addOrderDto.getOrderDescrip());
        order.setOutBusinessId(addOrderDto.getOutBusinessId());//选课记录id

        ordersMapper.insert(order);
        String orderDetailJson = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> ordersGoods = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        ordersGoods.forEach(goods -> {
            XcOrdersGoods xcOrdersGoods = new XcOrdersGoods();
            BeanUtils.copyProperties(goods, xcOrdersGoods);
            xcOrdersGoods.setOrderId(orderId);
            ordersGoodsMapper.insert(xcOrdersGoods);
        });
        return order;
    }

    public XcOrders getOrderByBusinessId(String businessId){
        return ordersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId));
    }

    public XcPayRecord createPayRecord(XcOrders order){

        if(order==null){
            XuechengException.cast("订单不存在");
        }
        if(order.getStatus().equals("600002")){
            XuechengException.cast("订单已支付");
        }

        XcPayRecord payRecord = new XcPayRecord();

        payRecord.setPayNo(IdWorkerUtils.getInstance().nextId());
        payRecord.setOrderId(order.getId());//商品订单号
        payRecord.setOrderName(order.getOrderName());
        payRecord.setTotalPrice(order.getTotalPrice());
        payRecord.setCurrency("CNY");
        payRecord.setCreateDate(LocalDateTime.now());
        payRecord.setStatus("601001");//未支付
        payRecord.setUserId(order.getUserId());

        payRecordMapper.insert(payRecord);
        return payRecord;
    }

    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        return payRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
    }
}
