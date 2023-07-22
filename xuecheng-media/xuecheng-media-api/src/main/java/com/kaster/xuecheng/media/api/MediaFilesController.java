package com.kaster.xuecheng.media.api;

import com.kaster.xuecheng.base.exception.XuechengException;
import com.kaster.xuecheng.base.model.PageParams;
import com.kaster.xuecheng.base.model.PageResult;
import com.kaster.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.kaster.xuecheng.media.model.dto.UploadFileParamsDto;
import com.kaster.xuecheng.media.model.dto.UploadFileResultDto;
import com.kaster.xuecheng.media.model.po.MediaFiles;
import com.kaster.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理接口
 * @date 2022/9/6 11:29
 */
@Api(value = "媒资文件管理接口", tags = "媒资文件管理接口")
@RestController
public class MediaFilesController {

    @Autowired
    private MediaFileService mediaFileService;


    @ApiOperation("媒资列表查询接口")
    @PostMapping("/files")
    public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto) {
        Long companyId = 1232141425L;
        return mediaFileService.queryMediaFiels(companyId, pageParams, queryMediaParamsDto);

    }

    @ApiOperation("上传图片")
    @PostMapping(value = "/upload/coursefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadFileResultDto upload(@RequestPart("filedata") MultipartFile file,
                                      @RequestParam(value = "objectName", required = false) String objectName) {

        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        Long companyId = 1232141425L;

        uploadFileParamsDto.setFilename(file.getOriginalFilename());
        uploadFileParamsDto.setFileSize(file.getSize());
        uploadFileParamsDto.setFileType("001001");

        // 创建临时文件
        File tempFile = null;
        try {
            tempFile = File.createTempFile("minio", ".temp");
            file.transferTo(tempFile);
        } catch (IOException e) {
            XuechengException.cast("文件上传失败");
        }


        return mediaFileService.uploadFile(companyId, uploadFileParamsDto, tempFile.getAbsolutePath(), objectName);
    }
}
