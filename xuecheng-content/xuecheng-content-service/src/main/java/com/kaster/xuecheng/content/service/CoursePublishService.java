package com.kaster.xuecheng.content.service;

import com.kaster.xuecheng.content.model.dto.CoursePreviewDto;

public interface CoursePublishService {

    CoursePreviewDto getCoursePreviewInfo(Long courseId);

    void commitAudit(Long companyId,Long courseId);
}
