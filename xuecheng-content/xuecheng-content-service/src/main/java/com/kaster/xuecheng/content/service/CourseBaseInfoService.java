package com.kaster.xuecheng.content.service;

import com.kaster.xuecheng.base.model.PageParams;
import com.kaster.xuecheng.base.model.PageResult;
import com.kaster.xuecheng.content.model.dto.AddCourseDto;
import com.kaster.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.kaster.xuecheng.content.model.dto.EditCourseDto;
import com.kaster.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.kaster.xuecheng.content.model.po.CourseBase;

public interface CourseBaseInfoService {

    // 分页查询
    PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto dto);

    CourseBaseInfoDto createCourseBase(AddCourseDto addCourseDto, Long companyId);

    CourseBaseInfoDto getCourseBaseInfo(Long courseId);

    CourseBaseInfoDto updateCourseBase(EditCourseDto editCourseDto, Long companyId);
}
