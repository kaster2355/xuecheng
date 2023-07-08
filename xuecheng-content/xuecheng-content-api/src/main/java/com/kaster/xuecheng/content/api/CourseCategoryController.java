package com.kaster.xuecheng.content.api;

import com.kaster.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.kaster.xuecheng.content.service.CourseCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/course-category")
public class CourseCategoryController {

    @Autowired
    private CourseCategoryService courseCategoryService;

    @GetMapping("/tree-nodes")
    public List<CourseCategoryTreeDto> queryTreeNodes(@RequestParam(defaultValue = "1") String id){
        return courseCategoryService.queryTreeNodes(id);
    }
}
