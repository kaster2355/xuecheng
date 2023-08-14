package com.kaster.xuecheng.learning.service;

import com.kaster.xuecheng.base.model.PageResult;
import com.kaster.xuecheng.learning.model.dto.MyCourseTableParams;
import com.kaster.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.kaster.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.kaster.xuecheng.learning.model.po.XcCourseTables;

public interface MyCourseTablesService {

    XcChooseCourseDto addChooseCourse(String userId, Long courseId);

    XcCourseTablesDto getLearningStatus(String userId, Long courseId);

    boolean saveChooseCourseStauts(String chooseCourseId);

    PageResult<XcCourseTables> mycourestabls(MyCourseTableParams params);
}
