package com.kaster.xuecheng.content.service;

import com.kaster.xuecheng.content.model.dto.CoursePreviewDto;

import java.io.File;

public interface CoursePublishService {

    CoursePreviewDto getCoursePreviewInfo(Long courseId);

    void commitAudit(Long companyId,Long courseId);

    void coursePublish(Long companyId, Long courseId);

    File generateCourseHtml(Long courseId);

    void  uploadCourseHtml(Long courseId,File file);

}
