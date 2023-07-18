package com.kaster.xuecheng.media.service;

import com.kaster.xuecheng.base.model.PageParams;
import com.kaster.xuecheng.base.model.PageResult;
import com.kaster.xuecheng.base.model.RestResponse;
import com.kaster.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.kaster.xuecheng.media.model.dto.UploadFileParamsDto;
import com.kaster.xuecheng.media.model.dto.UploadFileResultDto;
import com.kaster.xuecheng.media.model.po.MediaFiles;

import java.io.File;

public interface MediaFileService {

    /**
     * @param pageParams          分页参数
     * @param queryMediaParamsDto 查询条件
     * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
     * @description 媒资文件查询方法
     */
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

    UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String filePath);

    MediaFiles addMediaFilesToDB(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucketFiles, String objectName);

    /**
     * @param fileMd5 文件的md5
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @description 检查文件是否存在
     */
    RestResponse<Boolean> checkFile(String fileMd5) throws Exception;

    /**
     * @param fileMd5    文件的md5
     * @param chunkIndex 分块序号
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @description 检查分块是否存在
     */
    RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex);

    /**
     * 上传分块
     *
     * @param fileMd5
     * @param chunk
     * @param localChunkFilePath
     * @return
     */
    public RestResponse<String> uploadChunk(String fileMd5, int chunk, String localChunkFilePath);

    /**
     * 合并分块
     * @param companyId
     * @param fileMd5
     * @param chunkTotal
     * @param uploadFileParamsDto
     * @return
     */
    public RestResponse<Boolean> mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto);

    File downloadFileFromMinIO(String bucket, String objectName);

    boolean addMediaFilesToMinIO(String localFilePath, String mimeType, String bucket, String objectName);

    MediaFiles getFileById(String mediaId);
}
