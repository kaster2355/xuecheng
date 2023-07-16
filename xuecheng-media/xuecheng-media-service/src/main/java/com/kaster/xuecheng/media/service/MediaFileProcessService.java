package com.kaster.xuecheng.media.service;

import com.kaster.xuecheng.media.model.po.MediaProcess;

import java.util.List;

public interface MediaFileProcessService {
    List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count);

    // 开始任务
    boolean startTask(long id);
}
