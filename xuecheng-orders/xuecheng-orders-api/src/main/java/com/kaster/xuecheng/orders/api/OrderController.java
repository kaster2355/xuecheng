package com.kaster.xuecheng.orders.api;

import com.alipay.api.AlipayApiException;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.kaster.xuecheng.base.exception.XuechengException;
import com.kaster.xuecheng.orders.config.AlipayConfig;
import com.kaster.xuecheng.orders.model.dto.AddOrderDto;
import com.kaster.xuecheng.orders.model.dto.PayRecordDto;
import com.kaster.xuecheng.orders.model.dto.PayStatusDto;
import com.kaster.xuecheng.orders.model.po.XcPayRecord;
import com.kaster.xuecheng.orders.service.OrderService;
import com.kaster.xuecheng.orders.util.SecurityUtil;
import groovy.util.logging.Slf4j;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Api(value = "订单支付接口", tags = "订单支付接口")
@Slf4j
@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Value("${pay.alipay.APP_ID}")
    private String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    private String APP_PRIVATE_KEY;

    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    private String ALIPAY_PUBLIC_KEY;


    @ApiOperation("生成支付二维码")
    @PostMapping("/generatepaycode")
    @ResponseBody
    public PayRecordDto generatePayCode(@RequestBody AddOrderDto addOrderDto) {

        SecurityUtil.XcUser user = SecurityUtil.getUser();

        if (user == null) {
            XuechengException.cast("请登录后继续选课");
        }

        return orderService.createOrder(user.getId(), addOrderDto);
    }

    @ApiOperation("扫码下单接口")
    @GetMapping("/requestpay")
    public void requestpay(String payNo, HttpServletResponse httpResponse) throws IOException {
        XcPayRecord payRecord = orderService.getPayRecordByPayno(payNo);
        if (payRecord == null) {
            XuechengException.cast("请重新点击支付获取二维码");
        }
        String status = payRecord.getStatus();
        if ("601002".equals(status)) {
            XuechengException.cast("此订单已支付");
        }

        DefaultAlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, AlipayConfig.FORMAT, AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE);
        AlipayTradeWapPayRequest payRequest = new AlipayTradeWapPayRequest();
//        payRequest.setReturnUrl("http://domain.com/CallBack/return_url.jsp");
//        payRequest.setNotifyUrl("http://tjxt-user-t.itheima.net/xuecheng/orders/paynotify");//在公共参数中设置回跳和通知地址

        payRequest.setBizContent("{" +
                " \"out_trade_no\":\"" + payRecord.getPayNo() + "\"," +
                " \"total_amount\":\"" + payRecord.getTotalPrice() + "\"," +
                " \"subject\":\"" + payRecord.getOrderName() + "\"," +
                " \"product_code\":\"QUICK_WAP_PAY\"" +
                " }");
        String form = "";
        try {
            form = alipayClient.pageExecute(payRequest).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
            XuechengException.cast("系统原因，支付失败，稍后重试");
        }
        httpResponse.setContentType("text/html;charset=" + AlipayConfig.CHARSET);
        httpResponse.getWriter().write(form);
        httpResponse.getWriter().flush();
        httpResponse.getWriter().close();
    }

    @ApiOperation("查询支付结果")
    @GetMapping("/payresult")
    @ResponseBody
    public PayRecordDto payresult(String payNo) throws IOException {

        //查询支付结果
        return orderService.queryPayResult(payNo);
    }

    @ApiOperation("接收支付结果通知")
    @PostMapping("/receivenotify")
    public void receivenotify(HttpServletRequest request, HttpServletResponse out) throws UnsupportedEncodingException, AlipayApiException {
        Map<String, String> params = new HashMap<String, String>();
        Map requestParams = request.getParameterMap();

        for (Iterator iterator = requestParams.keySet().iterator(); iterator.hasNext(); ) {
            String name = (String) iterator.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }

        //验签
        boolean verifyResult = AlipaySignature.rsaCheckV1(params, ALIPAY_PUBLIC_KEY, AlipayConfig.CHARSET, "RSA2");

        if (verifyResult) {
            //商户订单号
            String outTradeNo = new String(request.getParameter("out_trade_no").getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            //支付宝交易号
            String tradeNo = new String(request.getParameter("trade_no").getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            //交易状态
            String tradeStatus = new String(request.getParameter("trade_status").getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            //appid
            String appId = new String(request.getParameter("app_id").getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            //total_amount
            String totalAmount = new String(request.getParameter("total_amount").getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            //交易成功处理
            if (tradeStatus.equals("TRADE_SUCCESS")) {
                PayStatusDto payStatusDto = new PayStatusDto();
                payStatusDto.setOut_trade_no(outTradeNo);
                payStatusDto.setTrade_status(tradeStatus);
                payStatusDto.setApp_id(appId);
                payStatusDto.setTrade_no(tradeNo);
                payStatusDto.setTotal_amount(totalAmount);

                orderService.saveAliPayStatus(payStatusDto);
            }
        }
    }
}
