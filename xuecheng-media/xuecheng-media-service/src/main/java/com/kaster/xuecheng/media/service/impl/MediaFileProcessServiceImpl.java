package com.kaster.xuecheng.media.service.impl;

import com.kaster.xuecheng.media.mapper.MediaFilesMapper;
import com.kaster.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.kaster.xuecheng.media.mapper.MediaProcessMapper;
import com.kaster.xuecheng.media.model.po.MediaFiles;
import com.kaster.xuecheng.media.model.po.MediaProcess;
import com.kaster.xuecheng.media.model.po.MediaProcessHistory;
import com.kaster.xuecheng.media.service.MediaFileProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class MediaFileProcessServiceImpl implements MediaFileProcessService {

    @Autowired
    private MediaProcessMapper mediaProcessMapper;

    @Autowired
    private MediaProcessHistoryMapper mediaProcessHistoryMapper;

    @Autowired
    private MediaFilesMapper mediaFilesMapper;

    /**
     * 待转换格式媒资任务查询
     *
     * @param shardIndex
     * @param shardTotal
     * @param count
     * @return
     */
    @Override
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
        return mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, count);
    }

    @Override
    public boolean startTask(long id) {
        return mediaProcessMapper.startTask(id) > 0;
    }

    @Transactional
    @Override
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {

        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if (mediaProcess == null)
            return;

        // 执行失败
        if (status.equals("3")) {
            mediaProcess.setStatus("3");
            mediaProcess.setFailCount(mediaProcess.getFailCount() + 1);
            mediaProcess.setErrormsg(errorMsg);
            mediaProcessMapper.updateById(mediaProcess);
            return;
        }

        // 执行成功
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        mediaFiles.setUrl(url);
        mediaFilesMapper.updateById(mediaFiles);

        mediaProcess.setStatus("2");
        mediaProcess.setFinishDate(LocalDateTime.now());
        mediaProcess.setUrl(url);
        mediaProcessMapper.updateById(mediaProcess);

        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess, mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);

        mediaProcessMapper.deleteById(taskId);
    }
}
