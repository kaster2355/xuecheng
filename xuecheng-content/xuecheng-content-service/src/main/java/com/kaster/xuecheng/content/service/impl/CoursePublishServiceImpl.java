package com.kaster.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.kaster.xuecheng.base.exception.CommonError;
import com.kaster.xuecheng.base.exception.XuechengException;
import com.kaster.xuecheng.content.config.MultipartSupportConfig;
import com.kaster.xuecheng.content.feignclient.MediaServiceClient;
import com.kaster.xuecheng.content.mapper.CourseBaseMapper;
import com.kaster.xuecheng.content.mapper.CourseMarketMapper;
import com.kaster.xuecheng.content.mapper.CoursePublishMapper;
import com.kaster.xuecheng.content.mapper.CoursePublishPreMapper;
import com.kaster.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.kaster.xuecheng.content.model.dto.CoursePreviewDto;
import com.kaster.xuecheng.content.model.dto.TeachplanDto;
import com.kaster.xuecheng.content.model.po.CourseBase;
import com.kaster.xuecheng.content.model.po.CourseMarket;
import com.kaster.xuecheng.content.model.po.CoursePublish;
import com.kaster.xuecheng.content.model.po.CoursePublishPre;
import com.kaster.xuecheng.content.service.CourseBaseInfoService;
import com.kaster.xuecheng.content.service.CoursePublishService;
import com.kaster.xuecheng.content.service.TeachplanService;
import com.kaster.xuecheng.messagesdk.model.po.MqMessage;
import com.kaster.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
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

    @Autowired
    private CoursePublishMapper coursePublishMapper;

    @Autowired
    MqMessageService mqMessageService;

    @Autowired
    MediaServiceClient mediaServiceClient;

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

    /**
     * 课程发布
     *
     * @param companyId
     * @param courseId
     */
    @Override
    @Transactional
    public void coursePublish(Long companyId, Long courseId) {
        // 查询预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);

        if (coursePublishPre == null) {
            XuechengException.cast("课程未提交审核");
        }

        if (!companyId.equals(coursePublishPre.getCompanyId())) {
            XuechengException.cast("只能发布本公司课程");
        }

        if (!"202004".equals(coursePublishPre.getStatus())) {
            XuechengException.cast("课程未通过审核");
        }

        // 将预发布表转移到发布表
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre, coursePublish);

        CoursePublish coursePublish1 = coursePublishMapper.selectById(courseId);
        if (coursePublish1 == null)
            coursePublishMapper.insert(coursePublish);
        else
            coursePublishMapper.updateById(coursePublish);

        // 写入消息表
        saveCoursePublishMessage(courseId);

        // 删除预发布表
        coursePublishPreMapper.deleteById(courseId);
    }

    private void saveCoursePublishMessage(Long courseId) {
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if (mqMessage == null) {
            XuechengException.cast(CommonError.UNKOWN_ERROR);
        }

    }

    @Override
    public File generateCourseHtml(Long courseId) {
        //静态化文件
        File htmlFile = null;

        try {
            //配置freemarker
            Configuration configuration = new Configuration(Configuration.getVersion());

            //加载模板
            //选指定模板路径,classpath下templates下
            //得到classpath路径
            String classpath = this.getClass().getResource("/").getPath();
            configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
            //设置字符编码
            configuration.setDefaultEncoding("utf-8");

            //指定模板文件名称
            Template template = configuration.getTemplate("course_template.ftl");

            //准备数据
            CoursePreviewDto coursePreviewInfo = this.getCoursePreviewInfo(courseId);

            Map<String, Object> map = new HashMap<>();
            map.put("model", coursePreviewInfo);

            //静态化
            //参数1：模板，参数2：数据模型
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
//            System.out.println(content);
            //将静态化内容输出到文件中
            InputStream inputStream = IOUtils.toInputStream(content);
            //创建静态化文件
            htmlFile = File.createTempFile("course", ".html");
            log.debug("课程静态化，生成静态文件:{}", htmlFile.getAbsolutePath());
            //输出流
            FileOutputStream outputStream = new FileOutputStream(htmlFile);
            IOUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            log.error("课程静态化异常:{}", e.toString());
            XuechengException.cast("课程静态化异常");
        }

        return htmlFile;
    }

    @Override
    public void uploadCourseHtml(Long courseId, File file) {

        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        String course = mediaServiceClient.uploadFile(multipartFile, "course/"+courseId+".html");
        if(course==null){
            XuechengException.cast("上传静态文件异常");
        }

    }

    @Override
    public CoursePublish getCoursePublish(Long courseId) {
        return coursePublishMapper.selectById(courseId);
    }
}