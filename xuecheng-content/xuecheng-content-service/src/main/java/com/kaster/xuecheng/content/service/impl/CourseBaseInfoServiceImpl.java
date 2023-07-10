package com.kaster.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kaster.xuecheng.base.exception.XuechengException;
import com.kaster.xuecheng.base.model.PageParams;
import com.kaster.xuecheng.base.model.PageResult;
import com.kaster.xuecheng.content.mapper.CourseBaseMapper;
import com.kaster.xuecheng.content.mapper.CourseCategoryMapper;
import com.kaster.xuecheng.content.mapper.CourseMarketMapper;
import com.kaster.xuecheng.content.model.dto.AddCourseDto;
import com.kaster.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.kaster.xuecheng.content.model.dto.EditCourseDto;
import com.kaster.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.kaster.xuecheng.content.model.po.CourseBase;
import com.kaster.xuecheng.content.model.po.CourseCategory;
import com.kaster.xuecheng.content.model.po.CourseMarket;
import com.kaster.xuecheng.content.service.CourseBaseInfoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Autowired
    private CourseMarketMapper courseMarketMapper;

    @Autowired
    private CourseCategoryMapper courseCategoryMapper;

    /**
     * 课程分页查询
     * @param pageParams 分页参数
     * @param dto 查询参数
     * @return 分页课程信息结果
     */
    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto dto) {

        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.like(StringUtils.isNotEmpty(dto.getCourseName()),CourseBase::getName,dto.getCourseName());
        queryWrapper.eq(StringUtils.isNotEmpty(dto.getAuditStatus()),CourseBase::getAuditStatus,dto.getAuditStatus());
        queryWrapper.eq(StringUtils.isNotEmpty(dto.getPublishStatus()), CourseBase::getStatus, dto.getPublishStatus());

        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());

        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        List<CourseBase> items = pageResult.getRecords();
        long total = pageResult.getTotal();

        return new PageResult<>(items, total, pageParams.getPageNo(), pageParams.getPageSize());
    }

    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(AddCourseDto dto, Long companyId) {
        //合法性校验
        if (StringUtils.isBlank(dto.getName())) {
            throw new XuechengException("课程名称为空");
        }

        if (StringUtils.isBlank(dto.getMt())) {
            throw new XuechengException("课程分类为空");
        }

        if (StringUtils.isBlank(dto.getSt())) {
            throw new XuechengException("课程分类为空");
        }

        if (StringUtils.isBlank(dto.getGrade())) {
            throw new XuechengException("课程等级为空");
        }

        if (StringUtils.isBlank(dto.getTeachmode())) {
            throw new XuechengException("教育模式为空");
        }

        if (StringUtils.isBlank(dto.getUsers())) {
            throw new XuechengException("适应人群为空");
        }

        if (StringUtils.isBlank(dto.getCharge())) {
            throw new XuechengException("收费规则为空");
        }

        //新增对象
        CourseBase courseBaseNew = new CourseBase();
        //将填写的课程信息赋值给新增对象
        BeanUtils.copyProperties(dto,courseBaseNew);
        //设置审核状态
        courseBaseNew.setAuditStatus("202002");
        //设置发布状态
        courseBaseNew.setStatus("203001");
        //机构id
        courseBaseNew.setCompanyId(companyId);
        //添加时间
        courseBaseNew.setCreateDate(LocalDateTime.now());

        int insert = courseBaseMapper.insert(courseBaseNew);
        if(insert<=0){
            throw new XuechengException("新增课程基本信息失败");
        }

        CourseMarket courseMarketNew = new CourseMarket();
        Long courseId = courseBaseNew.getId();
        BeanUtils.copyProperties(dto,courseMarketNew);
        courseMarketNew.setId(courseId);
        int i = saveCourseMarket(courseMarketNew);
        if(i<=0){
            throw new XuechengException("保存课程营销信息失败");
        }
        //查询课程基本信息及营销信息并返回
        return getCourseBaseInfo(courseId);


    }

    private int saveCourseMarket(CourseMarket courseMarketNew){
        //收费规则
        String charge = courseMarketNew.getCharge();
        if(StringUtils.isBlank(charge)){
            throw new XuechengException("收费规则没有选择");
        }
        //收费规则为收费
        if(charge.equals("201001")){
            if(courseMarketNew.getPrice() == null || courseMarketNew.getPrice() <=0){
                throw new XuechengException("课程为收费价格不能为空且必须大于0");
            }
        }
        //根据id从课程营销表查询
        CourseMarket courseMarketObj = courseMarketMapper.selectById(courseMarketNew.getId());
        if(courseMarketObj == null){
            return courseMarketMapper.insert(courseMarketNew);
        }else{
            BeanUtils.copyProperties(courseMarketNew,courseMarketObj);
            courseMarketObj.setId(courseMarketNew.getId());
            return courseMarketMapper.updateById(courseMarketObj);
        }
    }

    @Override
    //根据课程id查询课程基本信息，包括基本信息和营销信息
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId){

        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if(courseBase == null){
            return null;
        }
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        if(courseMarket != null){
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);
        }

        //查询分类名称
        CourseCategory courseCategoryBySt = courseCategoryMapper.selectById(courseBase.getSt());
        courseBaseInfoDto.setStName(courseCategoryBySt.getName());
        CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt());
        courseBaseInfoDto.setMtName(courseCategoryByMt.getName());

        return courseBaseInfoDto;

    }

    @Override
    @Transactional
    public CourseBaseInfoDto updateCourseBase(EditCourseDto editCourseDto, Long companyId) {

        CourseBase courseBase = this.getCourseBaseInfo(editCourseDto.getId());

        if (courseBase == null){
            throw new XuechengException("课程不存在");
        }
        if (!courseBase.getCompanyId().equals(companyId)){
            throw new XuechengException("不能修改非本机构课程");
        }

        CourseBase newCourse = new CourseBase();
        BeanUtils.copyProperties(editCourseDto, newCourse);

        newCourse.setChangeDate(LocalDateTime.now());

        int i = courseBaseMapper.updateById(newCourse);
        if (i <= 0){
            throw new XuechengException("修改失败");
        }

        CourseMarket newMarket = new CourseMarket();
        BeanUtils.copyProperties(editCourseDto, newMarket);
        int j = courseMarketMapper.updateById(newMarket);
        if (j <= 0) {
            throw new XuechengException("课程营销信息更新失败");
        }

        return this.getCourseBaseInfo(editCourseDto.getId());
    }
}
