package com.kaster.xuecheng.content.service.impl;

import com.kaster.xuecheng.content.mapper.CourseCategoryMapper;
import com.kaster.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.kaster.xuecheng.content.model.po.CourseCategory;
import com.kaster.xuecheng.content.service.CourseCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Autowired
    private CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.queryTreeNodes(id);

        List<CourseCategoryTreeDto> courseCategoryTreeDtoList = new ArrayList<>();

        Map<String, CourseCategoryTreeDto> courseCategoryTreeDtoMap = courseCategoryTreeDtos.stream()
                .filter(item -> !id.equals(item.getId()))
                .collect(Collectors.toMap(CourseCategoryTreeDto::getId, value -> value, (key1, key2) -> key2));

        courseCategoryTreeDtos.stream()
                .filter(item -> !id.equals(item.getId()))
                .forEach(item ->{
                    if (id.equals(item.getParentid())){
                        courseCategoryTreeDtoList.add(item);
                    }
                    CourseCategoryTreeDto parentDto = courseCategoryTreeDtoMap.get(item.getParentid());
                    if (parentDto != null){
                        if (parentDto.getChildrenTreeNodes() == null){
                            parentDto.setChildrenTreeNodes(new ArrayList<>());
                        }
                        parentDto.getChildrenTreeNodes().add(item);
                    }
                });

        return courseCategoryTreeDtoList;
    }
}
