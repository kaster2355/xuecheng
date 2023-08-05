package com.kaster.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaster.xuecheng.base.exception.XuechengException;
import com.kaster.xuecheng.content.model.po.CoursePublish;
import com.kaster.xuecheng.learning.feignclient.ContentServiceClient;
import com.kaster.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.kaster.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.kaster.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.kaster.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.kaster.xuecheng.learning.model.po.XcChooseCourse;
import com.kaster.xuecheng.learning.model.po.XcCourseTables;
import com.kaster.xuecheng.learning.service.MyCourseTablesService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MyCourseTableServiceImpl implements MyCourseTablesService {

    @Autowired
    private XcChooseCourseMapper xcChooseCourseMapper;

    @Autowired
    private XcCourseTablesMapper xcCourseTablesMapper;

    @Autowired
    private ContentServiceClient contentServiceClient;

    @Override
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        // 查询课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null) {
            XuechengException.cast("课程信息不存在");
        }

        String charge = coursepublish.getCharge();
        XcChooseCourse chooseCourse;
        if ("201000".equals(charge)) {
            // 免费课程，写选课记录表、我的课程表
            chooseCourse = addFreeCourse(userId, coursepublish);
            XcCourseTables xcCourseTables = addCourseTables(chooseCourse);
        } else {
            // 收费课程，写选课记录表
            chooseCourse = addChargeCourse(userId, coursepublish);
        }

        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(chooseCourse,xcChooseCourseDto);
        //获取学习资格
        XcCourseTablesDto xcCourseTablesDto = getLearningStatus(userId, courseId);
        xcChooseCourseDto.setLearnStatus(xcCourseTablesDto.getLearnStatus());
        return xcChooseCourseDto;
    }

    //添加免费课程,免费课程加入选课记录表
    public XcChooseCourse addFreeCourse(String userId, CoursePublish coursepublish) {
        // 存在直接返回
        LambdaQueryWrapper<XcChooseCourse> wrapper = new LambdaQueryWrapper<XcChooseCourse>()
                .eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getOrderType, "700001")
                .eq(XcChooseCourse::getStatus, "701001");
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(wrapper);
        if (xcChooseCourses.size() > 0) {
            return xcChooseCourses.get(0);
        }

        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(0f);//免费课程价格为0
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");//免费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701001");//选课成功

        xcChooseCourse.setValidDays(365);//免费课程默认365
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));

        xcChooseCourseMapper.insert(xcChooseCourse);

        return xcChooseCourse;
    }

    //添加收费课程
    public XcChooseCourse addChargeCourse(String userId, CoursePublish coursepublish) {

        //如果存在待支付交易记录直接返回
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getOrderType, "700002")//收费订单
                .eq(XcChooseCourse::getStatus, "701002");//待支付
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size() > 0) {
            return xcChooseCourses.get(0);
        }

        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");//收费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701002");//待支付

        xcChooseCourse.setValidDays(coursepublish.getValidDays());
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(coursepublish.getValidDays()));
        xcChooseCourseMapper.insert(xcChooseCourse);
        return xcChooseCourse;

    }

    //添加到我的课程表
    public XcCourseTables addCourseTables(XcChooseCourse xcChooseCourse) {

        String status = xcChooseCourse.getStatus();
        if (!"701001".equals(status)) {
            XuechengException.cast("选课未成功，无法添加到课程表");
        }

        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if (xcCourseTables != null) {
            return xcCourseTables;
        }

        XcCourseTables xcCourseTablesNew = new XcCourseTables();
        xcCourseTablesNew.setChooseCourseId(xcChooseCourse.getId());
        xcCourseTablesNew.setUserId(xcChooseCourse.getUserId());
        xcCourseTablesNew.setCourseId(xcChooseCourse.getCourseId());
        xcCourseTablesNew.setCompanyId(xcChooseCourse.getCompanyId());
        xcCourseTablesNew.setCourseName(xcChooseCourse.getCourseName());
        xcCourseTablesNew.setCreateDate(LocalDateTime.now());
        xcCourseTablesNew.setValidtimeStart(xcChooseCourse.getValidtimeStart());
        xcCourseTablesNew.setValidtimeEnd(xcChooseCourse.getValidtimeEnd());
        xcCourseTablesNew.setCourseType(xcChooseCourse.getOrderType());

        xcCourseTablesMapper.insert(xcCourseTablesNew);
        return xcCourseTablesNew;
    }

    public XcCourseTables getXcCourseTables(String userId, Long courseId) {
        return xcCourseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, userId).eq(XcCourseTables::getCourseId, courseId));
    }


    //[{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        //查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        if (xcCourseTables == null) {
            XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
            //没有选课或选课后没有支付
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        BeanUtils.copyProperties(xcCourseTables, xcCourseTablesDto);
        //是否过期,true过期，false未过期
        boolean isExpires = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if (!isExpires) {
            //正常学习
            xcCourseTablesDto.setLearnStatus("702001");
        } else {
            //已过期
            xcCourseTablesDto.setLearnStatus("702003");
        }
        return xcCourseTablesDto;
    }
}
