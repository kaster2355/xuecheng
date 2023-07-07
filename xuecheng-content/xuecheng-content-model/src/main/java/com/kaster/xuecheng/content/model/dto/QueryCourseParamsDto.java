package com.kaster.xuecheng.content.model.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class QueryCourseParamsDto {
    // 审核状态
    private String auditStatus;

    private String courseName;

    // 发布状态
    private String publishStatus;
}
