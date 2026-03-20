package org.example.utils;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.mapper.VideoMapper;
import org.example.model.pojo.Video;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.ScriptType;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


import java.io.File;
import java.util.Map;

@Component
public class AsyncUtil {
    @Autowired
    private FileUtil fileUtil;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Async("videoTaskExecutor")
    public void publishVideoAsync(File file, String title,Long id) {
        try{
            String script = "ctx._source.videoUrl=params.videoUrl;" + "ctx._source.coverUrl=params.coverUrl;";
            String videoUrl = fileUtil.uploadVideoOSS(file,title);
            String coverUrl = videoUrl + "?x-oss-process=video/snapshot,t_1000,f_jpg,w_0,h_0";
            videoMapper.update(null,new LambdaUpdateWrapper<Video>()
                    .eq(Video::getId,id)
                    .set(Video::getVideoUrl,videoUrl)
                    .set(Video::getCoverUrl,coverUrl)
            );
            UpdateQuery search = UpdateQuery.builder(id.toString())
                    .withScript(script)
                    .withScriptType(ScriptType.INLINE)
                    .withParams(Map.of(
                            "videoUrl",videoUrl,
                            "coverUrl",coverUrl
                    )).build();
            elasticsearchOperations.update(search, IndexCoordinates.of("video"));
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
