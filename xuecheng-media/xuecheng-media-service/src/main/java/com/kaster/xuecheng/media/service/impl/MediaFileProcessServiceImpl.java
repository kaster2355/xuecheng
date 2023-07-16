package com.kaster.xuecheng.media.service.impl;

import com.kaster.xuecheng.media.mapper.MediaProcessMapper;
import com.kaster.xuecheng.media.model.po.MediaProcess;
import com.kaster.xuecheng.media.service.MediaFileProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MediaFileProcessServiceImpl implements MediaFileProcessService {

    @Autowired
    private MediaProcessMapper mediaProcessMapper;

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
}
