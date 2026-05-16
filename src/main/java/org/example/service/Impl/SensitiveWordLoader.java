package org.example.service.Impl;

import org.example.mapper.SensitiveWordMapper;
import org.example.model.normal.RedisKey;
import org.example.model.pojo.SensitiveWord;
import org.example.utils.SensitiveWordFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;



@Service
public class SensitiveWordLoader implements CommandLineRunner {

    @Autowired
    private RedisTemplate  redisTemplate;

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private SensitiveWordFilter  sensitiveWordFilter;

    @Override
    public void run(String... args) throws Exception {
        loadTreeFromRedis();
    }

    public void loadTreeFromRedis() {

        Boolean hasKey = redisTemplate.hasKey(RedisKey.IM_SENSITIVEWORD);

        if (Boolean.FALSE.equals(hasKey)) {

            List<SensitiveWord> sensitiveWords = sensitiveWordMapper.selectList(null);

            if (sensitiveWords != null && !sensitiveWords.isEmpty()) {

                List<String> wordsToCache = sensitiveWords.stream()
                        .map(SensitiveWord::getWord)
                        .filter(w -> w != null && !w.isBlank())
                        .toList();

                if (!wordsToCache.isEmpty()) {
                    redisTemplate.opsForSet().add(RedisKey.IM_SENSITIVEWORD, wordsToCache.toArray());
                }
            }
        }

        Set<String> cached = redisTemplate.opsForSet().members(RedisKey.IM_SENSITIVEWORD);

        if (cached == null || cached.isEmpty()) {
            return;
        }

        List<String> words = cached.stream()
                .filter(w -> w != null && !w.isBlank())
                .toList();

        if (!words.isEmpty()) {
            sensitiveWordFilter.addNode(words);
        }
    }
}
