package com.kaster.xuecheng.content.service.impl;

import com.kaster.xuecheng.content.mapper.TeachplanMapper;
import com.kaster.xuecheng.content.model.dto.TeachplanDto;
import com.kaster.xuecheng.content.service.TeachplanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Autowired
    private TeachplanMapper teachplanMapper;

    @Override
    public List<TeachplanDto> findTeachplanTree(long courseId) {
        return teachplanMapper.getTreeNodes(courseId);
    }
}
