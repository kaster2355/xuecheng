package com.kaster.xuecheng.content.service;

import com.kaster.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

public interface TeachplanService {

    List<TeachplanDto> findTeachplanTree(long courseId);
}
