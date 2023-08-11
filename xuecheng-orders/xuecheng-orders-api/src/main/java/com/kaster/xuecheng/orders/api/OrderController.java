package com.kaster.xuecheng.orders.api;

import com.alipay.api.AlipayApiException;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.kaster.xuecheng.base.exception.XuechengException;
import com.kaster.xuecheng.orders.config.AlipayConfig;
import com.kaster.xuecheng.orders.model.dto.AddOrderDto;
import com.kaster.xuecheng.orders.model.dto.PayRecordDto;
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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
}
