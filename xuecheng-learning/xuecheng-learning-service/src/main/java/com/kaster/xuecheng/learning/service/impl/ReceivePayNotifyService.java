package com.kaster.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.kaster.xuecheng.base.exception.XuechengException;
import com.kaster.xuecheng.learning.config.PayNotifyConfig;
import com.kaster.xuecheng.learning.service.MyCourseTablesService;
import com.kaster.xuecheng.messagesdk.model.po.MqMessage;
import com.kaster.xuecheng.messagesdk.service.MqMessageService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class ReceivePayNotifyService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MqMessageService mqMessageService;

    @Autowired
    private MyCourseTablesService myCourseTablesService;

    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //获取消息
        MqMessage mqMessage = JSON.parseObject(message.getBody(), MqMessage.class);
        log.debug("学习中心服务接收支付结果:{}", mqMessage);

        //消息类型
        String messageType = mqMessage.getMessageType();
        //订单类型,60201表示购买课程
        String businessKey2 = mqMessage.getBusinessKey2();

        if (PayNotifyConfig.MESSAGE_TYPE.equals(messageType) && "60201".equals(businessKey2)) {
            String choosecourseId = mqMessage.getBusinessKey1();
            boolean b = myCourseTablesService.saveChooseCourseStauts(choosecourseId);
            if (!b) {
                //添加选课失败，抛出异常，消息重回队列
                XuechengException.cast("收到支付结果，添加选课失败");
            }
        }
    }
}
