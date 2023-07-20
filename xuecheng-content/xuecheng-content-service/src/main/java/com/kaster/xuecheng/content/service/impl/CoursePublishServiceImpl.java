package com.kaster.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.kaster.xuecheng.base.exception.XuechengException;
import com.kaster.xuecheng.content.mapper.CourseBaseMapper;
import com.kaster.xuecheng.content.mapper.CourseMarketMapper;
import com.kaster.xuecheng.content.mapper.CoursePublishPreMapper;
import com.kaster.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.kaster.xuecheng.content.model.dto.CoursePreviewDto;
import com.kaster.xuecheng.content.model.dto.TeachplanDto;
import com.kaster.xuecheng.content.model.po.CourseBase;
import com.kaster.xuecheng.content.model.po.CourseMarket;
import com.kaster.xuecheng.content.model.po.CoursePublishPre;
import com.kaster.xuecheng.content.service.CourseBaseInfoService;
import com.kaster.xuecheng.content.service.CoursePublishService;
import com.kaster.xuecheng.content.service.TeachplanService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CoursePublishServiceImpl implements CoursePublishService {

    @Autowired
    private CourseBaseInfoService courseBaseInfoService;

    @Autowired
    private TeachplanService teachplanService;

    @Autowired
    private CourseMarketMapper courseMarketMapper;

    @Autowired
    private CoursePublishPreMapper coursePublishPreMapper;

    @Autowired
    private CourseBaseMapper courseBaseMapper;

    /**
     * 获取课程预览信息
     *
     * @param courseId
     * @return
     */
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {

        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);

        List<TeachplanDto> teachplanDtoList = teachplanService.findTeachplanTree(courseId);

        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplanDtoList);
        return coursePreviewDto;
    }


    /**
     * 提交审核
     *
     * @param companyId
     * @param courseId
     */
    @Override
    public void commitAudit(Long companyId, Long courseId) {

        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);

        if (courseBaseInfo == null) {
            XuechengException.cast("课程不存在");
        }

        // 审核状态为已提交不得提交
        String auditStatus = courseBaseInfo.getAuditStatus();
        if ("202003".equals(auditStatus)) {
            XuechengException.cast("课程已提交，请等待审核");
        }
        if (!companyId.equals(courseBaseInfo.getCompanyId())) {
            XuechengException.cast("只能提交本机构的课程");
        }

        // 未完善课程图片 计划信息不允许提交
        String pic = courseBaseInfo.getPic();
        if (StringUtils.isEmpty(pic)) {
            XuechengException.cast("请上传课程图片");
        }

        List<TeachplanDto> teachplanDtos = teachplanService.findTeachplanTree(courseId);
        if (teachplanDtos == null || teachplanDtos.size() == 0) {
            XuechengException.cast("请编写课程计划");
        }

        // 查询到基本信息 营销信息 课程计划插入到课程预发布表
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfo, coursePublishPre);
        coursePublishPre.setCompanyId(companyId);

        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);

        String courseMarketJson = JSON.toJSONString(courseMarket);
        String teachplanJson = JSON.toJSONString(teachplanDtos);

        coursePublishPre.setMarket(courseMarketJson);
        coursePublishPre.setTeachplan(teachplanJson);
        coursePublishPre.setStatus("202003");
        coursePublishPre.setCreateDate(LocalDateTime.now());

        // 更新或创建
        CoursePublishPre coursePublishPre1 = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre1 == null) {
            coursePublishPreMapper.insert(coursePublishPre);
        } else {
            coursePublishPreMapper.updateById(coursePublishPre);
        }

        // 更新课程基础信息表的审核字段
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);

    }
}
