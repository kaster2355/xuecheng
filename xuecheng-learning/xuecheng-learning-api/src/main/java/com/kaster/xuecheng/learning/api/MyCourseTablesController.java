package com.kaster.xuecheng.learning.api;

import com.kaster.xuecheng.base.exception.XuechengException;
import com.kaster.xuecheng.base.model.PageResult;
import com.kaster.xuecheng.learning.service.MyCourseTablesService;
import com.kaster.xuecheng.learning.util.SecurityUtil;
import com.kaster.xuecheng.learning.model.dto.MyCourseTableParams;
import com.kaster.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.kaster.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.kaster.xuecheng.learning.model.po.XcCourseTables;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Mr.M
 * @version 1.0
 * @description 我的课程表接口
 * @date 2022/10/25 9:40
 */

@Api(value = "我的课程表接口", tags = "我的课程表接口")
@Slf4j
@RestController
public class MyCourseTablesController {

    @Autowired
    private MyCourseTablesService myCourseTablesService;

    @ApiOperation("添加选课")
    @PostMapping("/choosecourse/{courseId}")
    public XcChooseCourseDto addChooseCourse(@PathVariable("courseId") Long courseId) {

        SecurityUtil.XcUser user = SecurityUtil.getUser();
        if (user == null){
            XuechengException.cast("请先登录再选课");
        }
        String userId = user.getId();

        return myCourseTablesService.addChooseCourse(userId, courseId);
    }

    @ApiOperation("查询学习资格")
    @PostMapping("/choosecourse/learnstatus/{courseId}")
    public XcCourseTablesDto getLearnstatus(@PathVariable("courseId") Long courseId) {

        SecurityUtil.XcUser user = SecurityUtil.getUser();
        if (user == null){
            XuechengException.cast("请先登录再选课");
        }
        String userId = user.getId();

        return myCourseTablesService.getLearningStatus(userId, courseId);
    }

    @ApiOperation("我的课程表")
    @GetMapping("/mycoursetable")
    public PageResult<XcCourseTables> mycoursetable(MyCourseTableParams params) {

        SecurityUtil.XcUser user = SecurityUtil.getUser();
        if(user == null){
            XuechengException.cast("请登录后继续选课");
        }
        String userId = user.getId();
        params.setUserId(userId);

        return myCourseTablesService.mycourestabls(params);
    }

}
