package com.kaster.xuecheng.content.service;

import com.kaster.xuecheng.base.model.PageParams;
import com.kaster.xuecheng.base.model.PageResult;
import com.kaster.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.kaster.xuecheng.content.model.po.CourseBase;

public interface CourseBaseInfoService {

    // 分页查询
    PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto dto);
}
