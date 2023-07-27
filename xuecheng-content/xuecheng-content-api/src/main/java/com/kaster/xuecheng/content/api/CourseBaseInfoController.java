package com.kaster.xuecheng.content.api;

import com.kaster.xuecheng.content.model.dto.AddCourseDto;
import com.kaster.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.kaster.xuecheng.content.model.dto.EditCourseDto;
import com.kaster.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.kaster.xuecheng.content.model.po.CourseBase;
import com.kaster.xuecheng.base.model.PageParams;
import com.kaster.xuecheng.base.model.PageResult;
import com.kaster.xuecheng.content.service.CourseBaseInfoService;
import com.kaster.xuecheng.content.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Api(value = "课程信息管理接口", tags = "课程信息管理")
@RestController
@RequestMapping("/course")
public class CourseBaseInfoController {

    @Autowired
    private CourseBaseInfoService courseBaseInfoService;

    @ApiOperation("课程分页查询接口")
    @PostMapping("/list")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto){

        SecurityUtil.XcUser user = SecurityUtil.getUser();
        if (user != null){
            String companyId = user.getCompanyId();
        }

        return courseBaseInfoService.queryCourseBaseList(pageParams, queryCourseParamsDto);
    }

    @ApiOperation("课程添加接口")
    @PostMapping
    public CourseBaseInfoDto createCourseBase(@RequestBody @Validated AddCourseDto addCourseDto){
        Long companyId = 1232141425L;
        return courseBaseInfoService.createCourseBase(addCourseDto, companyId);
    }

    @ApiOperation("课程查询接口")
    @GetMapping("/{courseId}")
    public CourseBaseInfoDto getCourseBaseInfo(@PathVariable Long courseId){
        return courseBaseInfoService.getCourseBaseInfo(courseId);
    }

    @ApiOperation("修改课程接口")
    @PutMapping
    public CourseBaseInfoDto updateCourseBaseInfo(@RequestBody EditCourseDto editCourseDto){
        Long companyId = 1232141425L;
        return courseBaseInfoService.updateCourseBase( editCourseDto, companyId);
    }
}
