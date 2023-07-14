package com.kaster.xuecheng.media.api;

import com.kaster.xuecheng.base.model.RestResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Api(value = "大文件上传接口", tags = "大文件上传接口")
@RestController("/upload")

public class BigFilesController {
    @ApiOperation(value = "文件上传前检查文件")
    @PostMapping("/checkfile")
    public RestResponse<Boolean> checkfile(
            @RequestParam("fileMd5") String fileMd5
    ) throws Exception {
        return null;
    }


    @ApiOperation(value = "分块文件上传前的检测")
    @PostMapping("/checkchunk")
    public RestResponse<Boolean> checkchunk(@RequestParam("fileMd5") String fileMd5,
                                            @RequestParam("chunk") int chunk) throws Exception {
        return null;
    }

    @ApiOperation(value = "上传分块文件")
    @PostMapping("/uploadchunk")
    public RestResponse uploadchunk(@RequestParam("file") MultipartFile file,
                                    @RequestParam("fileMd5") String fileMd5,
                                    @RequestParam("chunk") int chunk) throws Exception {

        return null;
    }

    @ApiOperation(value = "合并文件")
    @PostMapping("/mergechunks")
    public RestResponse mergechunks(@RequestParam("fileMd5") String fileMd5,
                                    @RequestParam("fileName") String fileName,
                                    @RequestParam("chunkTotal") int chunkTotal) throws Exception {
        return null;

    }


}
