package org.example.utils;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.mapper.VideoMapper;
import org.example.model.pojo.Video;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


import java.io.File;

@Component
public class AsyncUtil {
    @Autowired
    private FileUtil fileUtil;
    @Autowired
    private VideoMapper videoMapper;

    @Async("videoTaskExecutor")
    public void publishVideoAsync(File file, String title,Long id) {
        try{
            String videoUrl = fileUtil.uploadVideoOSS(file,title);
            String coverUrl = videoUrl + "?x-oss-process=video/snapshot,t_1000,f_jpg,w_0,h_0";
            videoMapper.update(null,new LambdaUpdateWrapper<Video>()
                    .eq(Video::getUserId,id)
                    .set(Video::getVideoUrl,videoUrl)
                    .set(Video::getCoverUrl,coverUrl)
            );
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
