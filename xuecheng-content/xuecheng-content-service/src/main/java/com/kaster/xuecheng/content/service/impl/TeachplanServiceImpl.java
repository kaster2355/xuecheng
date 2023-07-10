package com.kaster.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaster.xuecheng.content.mapper.TeachplanMapper;
import com.kaster.xuecheng.content.model.dto.SaveTeachplanDto;
import com.kaster.xuecheng.content.model.dto.TeachplanDto;
import com.kaster.xuecheng.content.model.po.Teachplan;
import com.kaster.xuecheng.content.model.po.TeachplanMedia;
import com.kaster.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
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

    @Override
    public void saveTeachplan(SaveTeachplanDto teachplanDto) {
        Long teachplanId = teachplanDto.getId();
        if (teachplanId == null){
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(teachplanDto, teachplan);

            // 排序字段设为同级个数+1
            long parentId = teachplanDto.getParentid();
            long courseId = teachplanDto.getCourseId();

            LambdaQueryWrapper<Teachplan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Teachplan::getCourseId, courseId)
                            .eq(Teachplan::getParentid, parentId);

            teachplan.setOrderby(teachplanMapper.selectCount(wrapper) + 1);

            teachplanMapper.insert(teachplan);
        } else {
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);
            BeanUtils.copyProperties(teachplanDto, teachplan);
            teachplanMapper.updateById(teachplan);
        }
    }
}
