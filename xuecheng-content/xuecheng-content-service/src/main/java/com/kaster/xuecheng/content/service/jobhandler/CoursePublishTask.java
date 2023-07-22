package com.kaster.xuecheng.content.service.jobhandler;

import com.kaster.xuecheng.messagesdk.model.po.MqMessage;
import com.kaster.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.kaster.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

@Component
public class CoursePublishTask extends MessageProcessAbstract {

    @XxlJob("coursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {

        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        process(shardIndex, shardTotal, "course_publish", 30, 60);

    }

    /**
     * 执行任务逻辑
     *
     * @param mqMessage 执行任务内容
     * @return
     */
    @Override
    public boolean execute(MqMessage mqMessage) {
        Long courseId = Long.valueOf(mqMessage.getBusinessKey1());

        // 课程静态化上传到minio
        generateCourseHtml(mqMessage, courseId);

        // 向elasticsearch写索引

        // 向redis写缓存

        return false;
    }

    public void generateCourseHtml(MqMessage mqMessage, long courseId) {
        MqMessageService mqMessageService = this.getMqMessageService();
        Long taskId = mqMessage.getId();
        int stageOne = mqMessageService.getStageOne(taskId);

        if (stageOne > 0){
            return;
        }


        mqMessageService.completedStageOne(taskId);
    }
}
