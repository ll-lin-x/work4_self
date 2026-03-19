package org.example.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.mapper.LikeMapper;
import org.example.mapper.RelationMapper;
import org.example.mapper.UserMapper;
import org.example.mapper.VideoMapper;
import org.example.model.dto.VideoListDTO;
import org.example.model.dto.VideoPublishDTO;
import org.example.model.dto.VideoSearchDTO;
import org.example.model.normal.RedisKey;
import org.example.model.normal.Result;
import org.example.model.pojo.Relation;
import org.example.model.pojo.User;
import org.example.model.pojo.Video;
import org.example.service.VideoService;
import org.example.utils.AsyncUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class VideoServiceImpl implements VideoService {


    @Autowired
    private AsyncUtil asyncUtil;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RelationMapper relationMapper;

    @Value("${upload.path}")
    private String savaPath;

    @Autowired
    private RedissonClient redissonClient;


    @Override
    public List<Video> getVideoFeed(Double dateTime,Long id) {
        Set<ZSetOperations.TypedTuple<Object>> set = redisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(RedisKey.USER_FEED + id, dateTime ,Double.MAX_VALUE);
        List<Long> videoIds = set.stream().map(tuple-> Long.valueOf(tuple.getValue().toString())).toList();
        if(videoIds.isEmpty()){
            return videoMapper.selectList(null);
        }

        String ids = videoIds.stream().map(Object::toString).collect(Collectors.joining(","));
        return videoMapper.selectList(new LambdaQueryWrapper<Video>()
                .in(Video::getId, videoIds)
                .last("order by field (id," + ids + ")"));
    }

    @Override
    public Result publish(VideoPublishDTO videoPublishDTO, Long id) {
        try{
            // 为保存到本地生成一个唯一名字
            String originalFilename = videoPublishDTO.getFile().getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String tempFileName = UUID.randomUUID().toString()  + extension;

            File tempFile = new File(savaPath, tempFileName);

            if (!tempFile.getParentFile().exists()) {
                tempFile.getParentFile().mkdirs();
            }
            videoPublishDTO.getFile().transferTo(tempFile);
            Video video = new Video(null,id,"","",videoPublishDTO.getTitle(),videoPublishDTO.getDescription(),
                    0,0,0, LocalDateTime.now(),LocalDateTime.now(),LocalDateTime.now().minusYears(100));
            videoMapper.insert(video);
            Long videoId = video.getId();
            List<Relation> relations = relationMapper.selectList(new LambdaQueryWrapper<Relation>()
                    .eq(Relation::getFocusUserId, id)
            );
            List<Long> ids = relations.stream().map(Relation::getUserId).toList();
            for (int i = 0; i < ids.size(); i++) {
                redisTemplate.opsForZSet().add(RedisKey.USER_FEED+ids.get(i),videoId,System.currentTimeMillis());
            }

            asyncUtil.publishVideoAsync(tempFile,videoPublishDTO.getTitle(),id);

            return Result.success();
        }catch(Exception e){
            throw new RuntimeException(e);
        }

    }

    @Override
    public List<Video> getPublishList(VideoListDTO videoListDTO, Long id) {
        Page<Video> videoPage = new Page<>(videoListDTO.getPage_num(),videoListDTO.getPage_size());
        videoMapper.selectPage(videoPage,new LambdaQueryWrapper<Video>().eq(Video::getUserId,Long.parseLong(videoListDTO.getUser_id())));

        return videoPage.getRecords();

    }

    @Override
    public List<Video> getVideoPopular(){
        Set<Long> videoIdSet = redisTemplate.opsForZSet().reverseRange(RedisKey.VIDEO_RANK_TOTAL, 0, 9);
        if(CollectionUtils.isEmpty(videoIdSet)){
            RLock lock = redissonClient.getLock(RedisKey.LOCK_RANK_INIT);
            try{
                boolean success = lock.tryLock(3,TimeUnit.SECONDS);
                if(success){
                    // 再次查询，可能有别的线程已经加载过缓存了
                    videoIdSet = redisTemplate.opsForZSet().reverseRange(RedisKey.VIDEO_RANK_TOTAL, 0, 9);
                    if(CollectionUtils.isEmpty(videoIdSet)){
                        List<Video> videoLists = videoMapper.selectList(new LambdaQueryWrapper<Video>()
                                .select(Video::getId, Video::getVisitCount)
                                .orderByDesc(Video::getVisitCount)
                                .last("limit 10"));
                        videoLists.forEach(video-> redisTemplate.opsForZSet().add(RedisKey.VIDEO_RANK_TOTAL,video.getId(),video.getVisitCount()));
                    }
                }
            }catch (Exception e){
                throw new RuntimeException(e);
            }finally {
                lock.unlock();
            }
            videoIdSet = redisTemplate.opsForZSet().reverseRange(RedisKey.VIDEO_RANK_TOTAL, 0, 9);
        }
        List<Long> videoIdList = new ArrayList<Long>(videoIdSet);
        String ids = videoIdList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return videoMapper.selectList(new LambdaQueryWrapper<Video>().in(Video::getId, videoIdList)
                .last("order by field (id," + ids + ")")
        );
    }

    
    @Override
    public void visitVideo(Long videoId){
        redisTemplate.opsForHash().increment(RedisKey.VIDEO_RANK_INCREMENT,String.valueOf(videoId),1);
        Double score = redisTemplate.opsForZSet().score(RedisKey.VIDEO_RANK_TOTAL, videoId);
        if(score==null){
            RLock lock = redissonClient.getLock(RedisKey.LOCK_VIDEO_RANK + videoId);
            try{
                lock.lock();
                score = redisTemplate.opsForZSet().score(RedisKey.VIDEO_RANK_TOTAL, videoId);
                if(Objects.isNull(score)){
                    Video video = videoMapper.selectById(videoId);
                    if(!Objects.isNull(video)){
                        redisTemplate.opsForZSet().add(RedisKey.VIDEO_RANK_TOTAL,video.getId(),video.getVisitCount());
                    }
                }
            }finally {
                lock.unlock();
            }

        }
        redisTemplate.opsForZSet().incrementScore(RedisKey.VIDEO_RANK_TOTAL, videoId, 1);
    }

    @Override
    public List<Video> searchVideo(VideoSearchDTO videoSearchDTO,Long id){

        int current = videoSearchDTO.getPage_num() > 0 ? videoSearchDTO.getPage_num() : 1;
        int size = videoSearchDTO.getPage_size() >0 ? videoSearchDTO.getPage_size() : 10;
        Page<Video> videoPage = new Page<>(current,size);
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<Video>();
        String keywords = videoSearchDTO.getKeywords();
        // 将搜索记录存到redis中，后续若有别的操作可以将其取出来
        redisTemplate.opsForZSet().add(RedisKey.VIDEO_SEARCH_HISTORY+id,keywords,LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        wrapper.and(StringUtils.hasText(keywords),wrapperItem->wrapperItem
                .like(Video::getTitle,keywords)
                .or()
                .like(Video::getDescription,keywords)
        );
        if (videoSearchDTO.getFrom_data() != null) {
            LocalDateTime from = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(videoSearchDTO.getFrom_data().longValue()),
                    ZoneId.systemDefault()
            );
            wrapper.ge(Video::getUpdatedAt, from);
        }

        if (videoSearchDTO.getTo_data() != null) {
            LocalDateTime to = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(videoSearchDTO.getTo_data().longValue()),
                    ZoneId.systemDefault()
            );
            wrapper.le(Video::getUpdatedAt, to);
        }
        if(StringUtils.hasText(videoSearchDTO.getUsername())){
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, videoSearchDTO.getUsername()));
            if(!Objects.isNull(user)){
                wrapper.eq(Video::getUserId,user.getId());
            }
            else{
                throw new RuntimeException("the user is not exist");
            }

        }
        wrapper.orderByDesc(Video::getUpdatedAt);
        IPage<Video> videoIPage =  videoMapper.selectSearchVideoList(videoPage,wrapper);
        return videoIPage.getRecords();
    }


}
