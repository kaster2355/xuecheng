package com.kaster.xuecheng.learning.service;

import com.kaster.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.kaster.xuecheng.learning.model.dto.XcCourseTablesDto;

public interface MyCourseTablesService {

    XcChooseCourseDto addChooseCourse(String userId, Long courseId);

    XcCourseTablesDto getLearningStatus(String userId, Long courseId);
}
