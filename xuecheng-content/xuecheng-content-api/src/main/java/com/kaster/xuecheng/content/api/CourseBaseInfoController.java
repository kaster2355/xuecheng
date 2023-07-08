package com.kaster.xuecheng.content.api;

import com.kaster.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.kaster.xuecheng.content.model.po.CourseBase;
import com.kaster.xuecheng.base.model.PageParams;
import com.kaster.xuecheng.base.model.PageResult;
import com.kaster.xuecheng.content.service.CourseBaseInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "课程信息管理接口", tags = "课程信息管理")
@RestController
@RequestMapping("/course")
public class CourseBaseInfoController {

    @Autowired
    private CourseBaseInfoService courseBaseInfoService;

    @ApiOperation("课程查询接口")
    @PostMapping("/list")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto){
        return courseBaseInfoService.queryCourseBaseList(pageParams, queryCourseParamsDto);
    }
}
