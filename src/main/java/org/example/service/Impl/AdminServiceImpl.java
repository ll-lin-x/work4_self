package org.example.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.mapper.SensitiveWordMapper;
import org.example.mapper.VideoMapper;
import org.example.model.dto.VideoReviewDTO;
import org.example.model.normal.RedisKey;
import org.example.model.pojo.SensitiveWord;
import org.example.model.pojo.Video;
import org.example.model.vo.VideoSimpleVo;
import org.example.model.vo.VideoVO;
import org.example.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;


@Service
public class AdminServiceImpl implements AdminService {


    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private RedisTemplate  redisTemplate;

    @Autowired
    private SensitiveWordLoader sensitiveWordLoader;
    @Autowired
    private VideoMapper videoMapper;


    @Override
    public void addSensitiveWord(String word) {
        // 新增敏感词要先往数据库中新增，然后同时写入到redis中，最后刷新本地的dfa树
        // 可以先判断redis中是否有
        Boolean isMember = redisTemplate.opsForSet().isMember(RedisKey.IM_SENSITIVEWORD, word);
        if (isMember) {
            return;
        }
        sensitiveWordMapper.insert(new SensitiveWord(word));
        redisTemplate.delete(RedisKey.IM_SENSITIVEWORD);
        sensitiveWordLoader.loadTreeFromRedis();
    }

    @Override
    public void deleteSensitiveWord(String word) {
        // 删除敏感词要先删除数据库中的数据，然后同时删除redis中的数据，最后刷新本地的dfa树
        Boolean isMember = redisTemplate.opsForSet().isMember(RedisKey.IM_SENSITIVEWORD, word);
        if (!isMember) {
            return;
        }
        sensitiveWordMapper.deleteById(word);
        redisTemplate.delete(RedisKey.IM_SENSITIVEWORD);
        sensitiveWordLoader.loadTreeFromRedis();
    }


    @Override
    public List<String> getSensitiveWord() {
        Boolean haveKey = redisTemplate.hasKey(RedisKey.IM_SENSITIVEWORD);
        if (!haveKey) {
            List<SensitiveWord> sensitiveWords = sensitiveWordMapper.selectList(null);
            return sensitiveWords.stream().map(SensitiveWord::getWord).toList();
        }
        Set<String> cached = redisTemplate.opsForSet().members(RedisKey.IM_SENSITIVEWORD);

        if (cached == null || cached.isEmpty()) {
            return null;
        }

        List<String> words = cached.stream()
                .filter(w -> w != null && !w.isBlank())
                .toList();

        return words;
    }

    @Override
    public List<VideoSimpleVo> getVideoList() {
        return videoMapper.selectList(new LambdaQueryWrapper<Video>().eq(Video::getState, 0))
                .stream()
                .map(video -> new VideoSimpleVo(video.getId(), video.getVideoUrl()))
                .toList();
    }


    @Override
    public void reviewVideo(VideoReviewDTO videoReviewDTO) {
        Long videoId = Long.valueOf(videoReviewDTO.getId());
        int state = videoReviewDTO.getState();
        String reason = videoReviewDTO.getReason();
        if (reason.isEmpty()) {
            reason =null;
        }
        videoMapper.update(new LambdaUpdateWrapper<Video>().eq(Video::getId, videoId)
                .set(Video::getState, state)
                .set(Video::getReason, reason));
    }
}
