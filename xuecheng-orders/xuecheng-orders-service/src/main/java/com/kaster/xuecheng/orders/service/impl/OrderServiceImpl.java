package com.kaster.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaster.xuecheng.base.exception.XuechengException;
import com.kaster.xuecheng.base.utils.IdWorkerUtils;
import com.kaster.xuecheng.base.utils.QRCodeUtil;
import com.kaster.xuecheng.orders.config.AlipayConfig;
import com.kaster.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.kaster.xuecheng.orders.mapper.XcOrdersMapper;
import com.kaster.xuecheng.orders.mapper.XcPayRecordMapper;
import com.kaster.xuecheng.orders.model.dto.AddOrderDto;
import com.kaster.xuecheng.orders.model.dto.PayRecordDto;
import com.kaster.xuecheng.orders.model.dto.PayStatusDto;
import com.kaster.xuecheng.orders.model.po.XcOrders;
import com.kaster.xuecheng.orders.model.po.XcOrdersGoods;
import com.kaster.xuecheng.orders.model.po.XcPayRecord;
import com.kaster.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
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

    @Value("${pay.alipay.APP_ID}")
    private String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    private String APP_PRIVATE_KEY;

    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    private String ALIPAY_PUBLIC_KEY;

    @Override
    @Transactional
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {

        // 插入订单表
        XcOrders orders = currentProxy.saveXcOrders(userId, addOrderDto);
        if (orders == null) {
            XuechengException.cast("订单创建失败");
        }
        if (orders.getStatus().equals("600002")) {
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
        BeanUtils.copyProperties(payRecord, payRecordDto);
        payRecordDto.setQrcode(qrCode);
        return payRecordDto;
    }

    @Transactional
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto) {
        // 幂等性
        XcOrders order = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if (order != null) {
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

    public XcOrders getOrderByBusinessId(String businessId) {
        return ordersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId));
    }

    public XcPayRecord createPayRecord(XcOrders order) {

        if (order == null) {
            XuechengException.cast("订单不存在");
        }
        if (order.getStatus().equals("600002")) {
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

    @Override
    public PayRecordDto queryPayResult(String payNo) {

        XcPayRecord payRecord = getPayRecordByPayno(payNo);
        if (payRecord == null) {
            XuechengException.cast("请重新点击支付获取二维码");
        }

        String status = payRecord.getStatus();
        if ("601002".equals(status)) {
            PayRecordDto payRecordDto = new PayRecordDto();
            BeanUtils.copyProperties(payRecord, payRecordDto);
            return payRecordDto;
        }

        // 查询支付结果
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);

        // 更新支付记录表和订单表
        currentProxy.saveAliPayStatus(payStatusDto);

        payRecord = getPayRecordByPayno(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        return payRecordDto;
    }

    public PayStatusDto queryPayResultFromAlipay(String payNo) {
        // 请求支付宝查询支付结果
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, "json", AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        request.setBizContent(bizContent.toJSONString());
        AlipayTradeQueryResponse response = null;

        try {
            response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                XuechengException.cast("请求支付查询查询失败");
            }
        } catch (AlipayApiException e) {
            log.error("请求支付宝查询支付结果异常:{}", e.toString(), e);
            XuechengException.cast("请求支付查询查询失败");
        }

        // 获取支付结果
        String resultJson = response.getBody();
        Map resultMap = JSON.parseObject(resultJson, Map.class);
        Map alipay_trade_query_response = (Map) resultMap.get("alipay_trade_query_response");
        String trade_status = (String) alipay_trade_query_response.get("trade_status");
        String total_amount = (String) alipay_trade_query_response.get("total_amount");
        String trade_no = (String) alipay_trade_query_response.get("trade_no");

        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_status(trade_status);
        payStatusDto.setApp_id(APP_ID);
        payStatusDto.setTrade_no(trade_no);
        payStatusDto.setTotal_amount(total_amount);
        return payStatusDto;
    }

    @Override
    @Transactional
    public void saveAliPayStatus(PayStatusDto payStatusDto) {
        //支付流水号
        String payNo = payStatusDto.getOut_trade_no();
        XcPayRecord payRecord = getPayRecordByPayno(payNo);
        if (payRecord == null) {
            XuechengException.cast("支付记录找不到");
        }

        String trade_status = payStatusDto.getTrade_status();
        log.debug("收到支付结果:{},支付记录:{}}", payStatusDto.toString(), payRecord.toString());
        if (trade_status.equals("TRADE_SUCCESS")) {
            //支付金额变为分
            float totalPrice = payRecord.getTotalPrice() * 100;
            float total_amount = Float.parseFloat(payStatusDto.getTotal_amount()) * 100;
            //校验是否一致
            if (!payStatusDto.getApp_id().equals(APP_ID) || (int) totalPrice != (int) total_amount) {
                //校验失败
                log.info("校验支付结果失败,支付记录:{},APP_ID:{},totalPrice:{}", payRecord.toString(), payStatusDto.getApp_id(), (int) total_amount);
                XuechengException.cast("校验支付结果失败");
            }
            log.debug("更新支付结果,支付交易流水号:{},支付结果:{}", payNo, trade_status);

            XcPayRecord updatePayRecord = new XcPayRecord();
            updatePayRecord.setStatus("601002");//支付成功
            updatePayRecord.setOutPayChannel("Alipay");
            updatePayRecord.setOutPayNo(payStatusDto.getTrade_no());//支付宝交易号
            updatePayRecord.setPaySuccessTime(LocalDateTime.now());//通知时间

            int update = payRecordMapper.update(updatePayRecord, new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
            if (update > 0) {
                log.info("更新支付记录状态成功:{}", updatePayRecord.toString());
            } else {
                log.info("更新支付记录状态失败:{}", updatePayRecord.toString());
                XuechengException.cast("更新支付记录状态失败");
            }

            //关联的订单号
            Long orderId = payRecord.getOrderId();
            XcOrders orders = ordersMapper.selectById(orderId);
            if (orders == null) {
                log.info("根据支付记录[{}}]找不到订单", updatePayRecord.toString());
                XuechengException.cast("根据支付记录找不到订单");
            }

            XcOrders updateOrder = new XcOrders();
            updateOrder.setStatus("600002");//支付成功
            update = ordersMapper.update(updateOrder, new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getId, orderId));
            if (update > 0) {
                log.info("更新订单表状态成功,订单号:{}", orderId);
            } else {
                log.info("更新订单表状态失败,订单号:{}", orderId);
                XuechengException.cast("更新订单表状态失败");
            }

        }
    }
}
