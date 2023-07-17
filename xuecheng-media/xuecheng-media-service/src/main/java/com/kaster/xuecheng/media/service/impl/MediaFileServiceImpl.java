package com.kaster.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.kaster.xuecheng.base.exception.XuechengException;
import com.kaster.xuecheng.base.model.RestResponse;
import com.kaster.xuecheng.media.mapper.MediaProcessMapper;
import com.kaster.xuecheng.media.model.dto.UploadFileParamsDto;
import com.kaster.xuecheng.media.model.dto.UploadFileResultDto;
import com.kaster.xuecheng.media.model.po.MediaProcess;
import com.kaster.xuecheng.media.service.MediaFileService;
import com.kaster.xuecheng.base.model.PageParams;
import com.kaster.xuecheng.base.model.PageResult;
import com.kaster.xuecheng.media.mapper.MediaFilesMapper;
import com.kaster.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.kaster.xuecheng.media.model.po.MediaFiles;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mr.M
 * @version 1.0
 * @description
 * @date 2022/9/10 8:58
 */
@Service
@Slf4j
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MediaProcessMapper mediaProcessMapper;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MediaFileService currentProxy;

    @Value("${minio.bucket.files}")
    private String bucket_Files;

    @Value("${minio.bucket.videofiles}")
    private String bucket_video;


    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        String filename = queryMediaParamsDto.getFilename();
        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(filename != null, MediaFiles::getFilename, filename);

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        return new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());

    }

    private String getDefaultFolderPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        return sdf.format(new Date()) + "/";
    }

    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return DigestUtils.md5Hex(fileInputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean addMediaFilesToMinIO(String localFilePath, String mimeType, String bucket, String objectName) {
        try {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .filename(localFilePath)
                    .contentType(mimeType)
                    .build();
            minioClient.uploadObject(uploadObjectArgs);
            log.debug("上传文件到minio成功,bucket:{},objectName:{}", bucket, objectName);
            System.out.println("上传成功");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件到minio出错,bucket:{},objectName:{},错误原因:{}", bucket, objectName, e.getMessage(), e);
            XuechengException.cast("上传文件到文件系统失败");
        }
        return false;
    }


    private String getMimeType(String extension) {
        if (extension == null) extension = "";
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        //通用mimeType，字节流
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }

    @Transactional
    public MediaFiles addMediaFilesToDB(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName) {

        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);

        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            mediaFiles.setId(fileMd5);
            mediaFiles.setCompanyId(companyId);
            mediaFiles.setBucket(bucket);
            mediaFiles.setFilePath(objectName);
            mediaFiles.setFileId(fileMd5);
            mediaFiles.setUrl("/" + bucket + "/" + objectName);
            mediaFiles.setCreateDate(LocalDateTime.now());
            mediaFiles.setStatus("1");
            mediaFiles.setAuditStatus("002003");

            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert <= 0) {
                log.debug("保存文件信息到数据库成功,{}", mediaFiles.toString());
                return null;
            }
            // 记录待处理任务
            addWaitingTask(mediaFiles);

            return mediaFiles;
        }
        return mediaFiles;
    }

    /**
     * 添加待处理任务
     * @param mediaFiles 媒资文件信息
     */
    private void addWaitingTask(MediaFiles mediaFiles){
        String filename = mediaFiles.getFilename();
        String mimeType = getMimeType(filename.substring(filename.lastIndexOf(".")));
        if ("video/x-msvideo".equals(mimeType)){
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles, mediaProcess);
            mediaProcess.setStatus("1");
            mediaProcess.setCreateDate(LocalDateTime.now());
            mediaProcess.setFailCount(0);
            mediaProcess.setUrl(null);

            mediaProcessMapper.insert(mediaProcess);
        }
    }


    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String filePath) {

        File file = new File(filePath);

        if (!file.exists()) {
            XuechengException.cast("文件不存在");
        }

        String filename = uploadFileParamsDto.getFilename();
        String extension = filename.substring(filename.lastIndexOf("."));

        String mimeType = getMimeType(extension);

        String fileMd5 = getFileMd5(file);

        String defaultFolderPath = getDefaultFolderPath();

        String objectName = defaultFolderPath + fileMd5 + extension;

        boolean b = addMediaFilesToMinIO(filePath, mimeType, bucket_Files, objectName);

        if (!b) {
            XuechengException.cast("文件上传失败");
        }

        MediaFiles mediaFiles = currentProxy.addMediaFilesToDB(companyId, fileMd5, uploadFileParamsDto, bucket_Files, objectName);

        if (mediaFiles == null) {
            XuechengException.cast("文件信息保存失败");
        }

        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);

        return uploadFileResultDto;
    }

    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) throws Exception {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null) {
            String bucket = mediaFiles.getBucket();
            String filePath = mediaFiles.getFilePath();

            InputStream stream = null;

            stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(filePath)
                            .build());
            if (stream != null) {
                return RestResponse.success(true);
            }

        }
        return RestResponse.success(false);
    }

    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {

        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);

        InputStream stream = null;

        try {
            stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket_video)
                            .object(chunkFileFolderPath + chunkIndex)
                            .build()
            );
            if (stream != null) {
                return RestResponse.success(true);
            }
        } catch (Exception ignored){

        }
        return RestResponse.success(false);
    }

    // 分块文件存储位置
    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + "chunk" + "/";
    }

    private String getFilePathByMd5(String fileMd5, String fileExt) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }


    @Override
    public RestResponse<String> uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {

        String mimeType = getMimeType(null);
        String objectName = getChunkFileFolderPath(fileMd5) + chunk;
        boolean b = addMediaFilesToMinIO(localChunkFilePath, mimeType, bucket_video, objectName);

        if (!b) {
            return RestResponse.validfail("分块上传失败");
        }
        return RestResponse.success("分块上传成功");
    }

    @Override
    public RestResponse<Boolean> mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        // 找到分块
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        List<ComposeSource> composeSources = Stream.iterate(0, i -> ++i).limit(chunkTotal)
                .map(i -> ComposeSource.builder()
                        .bucket(bucket_video)
                        .object(chunkFileFolderPath + i)
                        .build()).collect(Collectors.toList());

        String filename = uploadFileParamsDto.getFilename();
        String extName = filename.substring(filename.lastIndexOf("."));
        String objectName = getFilePathByMd5(fileMd5, extName);

        try {
            minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(bucket_video)
                            .object(objectName)
                            .sources(composeSources)
                            .build()
            );
        } catch (Exception e) {
            log.error("合并分块失败，bucket:{}，objectName:{}，错误信息：{}", bucket_video, objectName, e.getMessage());
            return RestResponse.validfail(false, "合并文件异常");
        }

        // 校验合并后与源文件一致
        File mergeFile = downloadFileFromMinIO(bucket_video, objectName);
        if (mergeFile == null) {
            log.debug("下载合并后文件失败,mergeFilePath:{}", objectName);
            return RestResponse.validfail(false, "下载合并后文件失败。");
        }


        try (FileInputStream inputStream = new FileInputStream(mergeFile)) {
            String mergeFileMd5 = DigestUtils.md5Hex(inputStream);
            if (!fileMd5.equals(mergeFileMd5)) {
                return RestResponse.validfail(false, "文件校验失败");
            }
            uploadFileParamsDto.setFileSize(mergeFile.length());
        } catch (Exception ex) {
            log.debug("校验文件失败,fileMd5:{},异常:{}", fileMd5, ex.getMessage(), ex);
            return RestResponse.validfail(false, "文件合并校验失败，最终上传失败。");
        } finally {
            mergeFile.delete();
        }

        // 文件信息入库
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDB(companyId, fileMd5, uploadFileParamsDto, bucket_video, objectName);
        if (mediaFiles == null) {
            return RestResponse.validfail("文件入库失败");
        }

        // 清除分块
        clearChunkFiles(chunkFileFolderPath, chunkTotal);

        return RestResponse.success(true);
    }

    @Override
    public File downloadFileFromMinIO(String bucket, String objectName) {
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile = File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream, outputStream);
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 删除分块文件
     *
     * @param chunkFileFolderPath
     * @param chunkTotal
     */
    private void clearChunkFiles(String chunkFileFolderPath, int chunkTotal) {

        try {
            List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                    .limit(chunkTotal)
                    .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
                    .collect(Collectors.toList());

            RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket("video").objects(deleteObjects).build();
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
            results.forEach(r -> {
                DeleteError deleteError = null;
                try {
                    deleteError = r.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("清除分块文件失败,objectname:{}", deleteError.objectName(), e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("清除分块文件失败,chunkFileFolderPath:{}", chunkFileFolderPath, e);
        }

    }
}
