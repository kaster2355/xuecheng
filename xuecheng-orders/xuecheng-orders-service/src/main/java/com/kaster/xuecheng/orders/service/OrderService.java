package com.kaster.xuecheng.orders.service;

import com.kaster.xuecheng.messagesdk.model.po.MqMessage;
import com.kaster.xuecheng.orders.model.dto.AddOrderDto;
import com.kaster.xuecheng.orders.model.dto.PayRecordDto;
import com.kaster.xuecheng.orders.model.dto.PayStatusDto;
import com.kaster.xuecheng.orders.model.po.XcOrders;
import com.kaster.xuecheng.orders.model.po.XcPayRecord;

public interface OrderService {
    PayRecordDto createOrder(String userId, AddOrderDto addOrderDto);

    XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto);

    XcPayRecord createPayRecord(XcOrders orders);

    XcPayRecord getPayRecordByPayno(String payNo);

    PayRecordDto queryPayResult(String payNo);

    void saveAliPayStatus(PayStatusDto payStatusDto);

    void notifyPayResult(MqMessage message);
}
