package com.kaster.xuecheng.media.service.jobhandler;

import com.kaster.xuecheng.base.utils.Mp4VideoUtil;
import com.kaster.xuecheng.media.model.po.MediaProcess;
import com.kaster.xuecheng.media.service.MediaFileProcessService;
import com.kaster.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

@Component
@Slf4j
public class VideoTask {

    @Autowired
    private MediaFileProcessService mediaFileProcessService;

    @Autowired
    private MediaFileService mediaFileService;

    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegPath;

    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        // cpu核心数
        int processors = Runtime.getRuntime().availableProcessors();

        // 查询任务
        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
        int size = mediaProcessList.size();

        if (size == 0)
            return;

        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(size);

        CountDownLatch countDownLatch = new CountDownLatch(size);

        mediaProcessList.forEach(mediaProcess -> {
            executorService.execute(() -> {

                try {
                    //开启任务
                    Long taskId = mediaProcess.getId();
                    boolean b = mediaFileProcessService.startTask(taskId);
                    if (!b) {
                        log.debug("抢占失败, 任务id:{}", taskId);
                        return;
                    }
                    // 处理任务
                    String bucket = mediaProcess.getBucket();
                    String objectName = mediaProcess.getFilePath();

                    File file = mediaFileService.downloadFileFromMinIO(bucket, objectName);
                    if (file == null) {
                        log.debug("下载视频出错，任务：{}，bucket：{}，objectName：{}", taskId, bucket, objectName);
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", mediaProcess.getFileId(), null, "下载视频到本地失败");
                        return;
                    }
                    String videoPath = file.getAbsolutePath();
                    String mp4Name = mediaProcess.getFileId();
                    File tempFile = null;
                    try {
                        tempFile = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        log.debug("创建临时文件异常");
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", mediaProcess.getFileId(), null, "创建临时文件失败");
                        return;
                    }
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegPath, videoPath, mp4Name, tempFile.getAbsolutePath());

                    // 转码
                    String result = videoUtil.generateMp4();

                    if (!result.equals("success")) {
                        log.debug("视频转码失败,原因:{},bucket:{},objectName：{}", result, bucket, objectName);
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", mediaProcess.getFileId(), null, result);
                        return;
                    }

                    // 上传
                    String mp4FilePath = getFilePath(mediaProcess.getFileId(), ".mp4");
                    boolean b1 = mediaFileService.addMediaFilesToMinIO(tempFile.getAbsolutePath(), "video/mp4", bucket, mp4FilePath);
                    if (!b1) {
                        log.debug("上传mp4到minio失败,taskId:{}", taskId);
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", mediaProcess.getFileId(), null, "上传mp4到minio失败");
                    }

                    // 保存结果
                    mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "2", mediaProcess.getFileId(), mp4FilePath, null);

                } finally {
                    countDownLatch.countDown();
                }
            });
        });

        countDownLatch.await(30, TimeUnit.MINUTES);

    }

    private String getFilePath(String fileMd5, String fileExt) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

}
