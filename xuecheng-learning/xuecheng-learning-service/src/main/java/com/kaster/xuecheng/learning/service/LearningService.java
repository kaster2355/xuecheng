package com.kaster.xuecheng.learning.service;

import com.kaster.xuecheng.base.model.RestResponse;

public interface LearningService {
    RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId);
}
