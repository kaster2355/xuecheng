package com.kaster.xuecheng.content.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

/**
 * 课程预览，发布
 */

@Controller
public class CoursePublishController {

    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView preview(@PathVariable("courseId") Long courseId){

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("model",null);
        modelAndView.setViewName("course_template");
        return modelAndView;
    }

}
