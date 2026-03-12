package org.example.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.mapper.VideoMapper;
import org.example.model.dto.BatchUpdateDTO;
import org.example.model.pojo.Video;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.example.model.normal.RedisKey.VIDEO_RANK_INCREMENT;
import static org.example.model.normal.RedisKey.VIDEO_RANK_TOTAL;

@Component
public class VideoRankSyncTask {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private VideoMapper videoMapper;

    @Scheduled(cron = "0 0/10 * * * ?")
    public void syncRedisToDb(){
        String key = VIDEO_RANK_INCREMENT;
        Map<String,Integer> entries = redisTemplate.opsForHash().entries(key);
        if(CollectionUtils.isEmpty(entries)) return;

        List<BatchUpdateDTO> videos = entries.entrySet().stream().map(
                entry -> new BatchUpdateDTO(Long.parseLong(entry.getKey()),entry.getValue())).toList();
        videoMapper.batchIncrementCount("visit_count",videos);
        redisTemplate.delete(key);
    }

    @Scheduled(cron = "0 5 3 * * ?")
    public void calibrateVideoRank(){
        String key = VIDEO_RANK_TOTAL;
        List<Video> videoList = videoMapper.selectList(new LambdaQueryWrapper<Video>()
                .select(Video::getId, Video::getVisitCount)
                .orderByDesc(Video::getVisitCount)
                .last("limit 10")
        );
        if(videoList.isEmpty()) return;
        redisTemplate.delete(key);
        videoList.forEach(video -> {
            redisTemplate.opsForZSet().add(key,video.getId(),video.getVisitCount());
        });
    }
}
