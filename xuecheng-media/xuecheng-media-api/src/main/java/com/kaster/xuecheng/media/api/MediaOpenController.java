package com.kaster.xuecheng.media.api;

import com.kaster.xuecheng.base.exception.XuechengException;
import com.kaster.xuecheng.base.model.RestResponse;
import com.kaster.xuecheng.media.model.po.MediaFiles;
import com.kaster.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/open")
public class MediaOpenController {

    @Autowired
    MediaFileService mediaFileService;

    @ApiOperation("预览文件")
    @GetMapping("/preview/{mediaId}")
    public RestResponse<String> getPlayUrlByMediaId(@PathVariable String mediaId){

        MediaFiles mediaFiles = mediaFileService.getFileById(mediaId);
        if(mediaFiles == null || StringUtils.isEmpty(mediaFiles.getUrl())){
            XuechengException.cast("视频还没有转码处理");
        }
        return RestResponse.success(mediaFiles.getUrl());

    }

}
