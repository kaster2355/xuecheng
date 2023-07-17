package com.kaster.xuecheng.content.service;

import com.kaster.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.kaster.xuecheng.content.model.dto.SaveTeachplanDto;
import com.kaster.xuecheng.content.model.dto.TeachplanDto;
import com.kaster.xuecheng.content.model.po.TeachplanMedia;

import java.util.List;

public interface TeachplanService {

    List<TeachplanDto> findTeachplanTree(long courseId);

    public void saveTeachplan(SaveTeachplanDto teachplanDto);

    TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);
}
