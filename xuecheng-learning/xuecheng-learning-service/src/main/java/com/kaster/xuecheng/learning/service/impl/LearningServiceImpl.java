package com.kaster.xuecheng.learning.service.impl;

import com.kaster.xuecheng.base.exception.XuechengException;
import com.kaster.xuecheng.base.model.RestResponse;
import com.kaster.xuecheng.content.model.po.CoursePublish;
import com.kaster.xuecheng.learning.feignclient.ContentServiceClient;
import com.kaster.xuecheng.learning.feignclient.MediaServiceClient;
import com.kaster.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.kaster.xuecheng.learning.service.LearningService;
import com.kaster.xuecheng.learning.service.MyCourseTablesService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LearningServiceImpl implements LearningService {

    @Autowired
    private ContentServiceClient contentServiceClient;

    @Autowired
    private MediaServiceClient mediaServiceClient;

    @Autowired
    private MyCourseTablesService myCourseTablesService;

    @Override
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId) {

        // 查询课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null) {
            XuechengException.cast("课程信息不存在");
        }

        // 校验学习资格
        if (StringUtils.isNotEmpty(userId)) {

            //判断是否选课，根据选课情况判断学习资格
            XcCourseTablesDto xcCourseTablesDto = myCourseTablesService.getLearningStatus(userId, courseId);
            //学习资格状态 [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
            String learnStatus = xcCourseTablesDto.getLearnStatus();
            if (learnStatus.equals("702001")) {
                return mediaServiceClient.getPlayUrlByMediaId(mediaId);
            } else if (learnStatus.equals("702003")) {
                RestResponse.validfail("您的选课已过期需要申请续期或重新支付");
            }
        }

        //未登录或未选课判断是否收费
        String charge = coursepublish.getCharge();
        if(charge.equals("201000")){//免费可以正常学习
            return mediaServiceClient.getPlayUrlByMediaId(mediaId);
        }

        return RestResponse.validfail("请购买课程后继续学习");
    }
}
