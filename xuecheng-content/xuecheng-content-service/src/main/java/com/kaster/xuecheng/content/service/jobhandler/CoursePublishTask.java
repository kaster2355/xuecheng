package com.kaster.xuecheng.content.service.jobhandler;

import com.kaster.xuecheng.base.exception.XuechengException;
import com.kaster.xuecheng.content.feignclient.SearchServiceClient;
import com.kaster.xuecheng.content.mapper.CoursePublishMapper;
import com.kaster.xuecheng.content.model.dto.CourseIndex;
import com.kaster.xuecheng.content.model.po.CoursePublish;
import com.kaster.xuecheng.content.service.CoursePublishService;
import com.kaster.xuecheng.messagesdk.model.po.MqMessage;
import com.kaster.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.kaster.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
public class CoursePublishTask extends MessageProcessAbstract {

    @Autowired
    private CoursePublishService coursePublishService;

    @Autowired
    private CoursePublishMapper coursePublishMapper;

    @Autowired
    private SearchServiceClient searchServiceClient;

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
        saveCourseIndex(mqMessage, courseId);

        // 向redis写缓存

        return false;
    }

    public void generateCourseHtml(MqMessage mqMessage, long courseId) {
        MqMessageService mqMessageService = this.getMqMessageService();
        Long taskId = mqMessage.getId();
        int stageOne = mqMessageService.getStageOne(taskId);

        if (stageOne > 0) {
            return;
        }

        File file = coursePublishService.generateCourseHtml(courseId);

        if (file == null) {
            XuechengException.cast("生成静态页面为空");
        }
        coursePublishService.uploadCourseHtml(courseId, file);

        mqMessageService.completedStageOne(taskId);
    }

    public void saveCourseIndex(MqMessage mqMessage, long courseId) {
        log.debug("保存课程索引信息,课程id:{}", courseId);

        //消息id
        Long id = mqMessage.getId();
        //消息处理的service
        MqMessageService mqMessageService = this.getMqMessageService();
        //消息幂等性处理
        int stageTwo = mqMessageService.getStageTwo(id);
        if (stageTwo > 0) {
            log.debug("课程索引已处理直接返回，课程id:{}", courseId);
            return;
        }

        Boolean result = saveCourseIndex(courseId);
        if (result) {
            //保存第二阶段状态
            mqMessageService.completedStageTwo(id);
        }
    }

    private Boolean saveCourseIndex(Long courseId) {

        //取出课程发布信息
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        //拷贝至课程索引对象
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);
        //远程调用搜索服务api添加课程信息到索引
        Boolean add = searchServiceClient.add(courseIndex);
        if(!add){
            XuechengException.cast("添加索引失败");
        }
        return add;

    }


}
