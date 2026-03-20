package org.example.service.Impl;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.mapper.LikeMapper;
import org.example.mapper.RelationMapper;
import org.example.mapper.UserMapper;
import org.example.mapper.VideoMapper;
import org.example.model.ES.VideoES;
import org.example.model.dto.VideoListDTO;
import org.example.model.dto.VideoPublishDTO;
import org.example.model.dto.VideoSearchDTO;
import org.example.model.normal.RedisKey;
import org.example.model.normal.Result;
import org.example.model.pojo.Relation;
import org.example.model.pojo.User;
import org.example.model.pojo.Video;
import org.example.model.vo.UserVO;
import org.example.repository.VideoRepository;
import org.example.service.VideoService;
import org.example.utils.AsyncUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.ScriptType;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;


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
                    0,0,0, System.currentTimeMillis(),System.currentTimeMillis(),LocalDateTime.now().plusYears(10).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            videoMapper.insert(video);


            Long videoId = video.getId();
            List<Relation> relations = relationMapper.selectList(new LambdaQueryWrapper<Relation>()
                    .eq(Relation::getFocusUserId, id)
            );
            VideoES videoES = new VideoES();
            BeanUtils.copyProperties(video,videoES);
            videoRepository.save(videoES);
            // feed流，通知关注了该用户的粉丝
            List<Long> ids = relations.stream().map(Relation::getUserId).toList();
            for (int i = 0; i < ids.size(); i++) {
                redisTemplate.opsForZSet().add(RedisKey.USER_FEED+ids.get(i),videoId,System.currentTimeMillis());
            }

            asyncUtil.publishVideoAsync(tempFile,videoPublishDTO.getTitle(),videoId);

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

        String script = "ctx._source.visitCount += params.visitCount;" + "ctx._source.updatedAt = params.updatedAt;";
        UpdateQuery updateQuery = UpdateQuery.builder(videoId.toString())
                .withScript(script)
                .withScriptType(ScriptType.INLINE)
                .withParams(Map.of(
                        "visitCount",1,
                        "updatedAt", System.currentTimeMillis()
                )).build();
        elasticsearchOperations.update(updateQuery, IndexCoordinates.of("video"));
    }

    @Override
    public List<Video> searchVideo(VideoSearchDTO videoSearchDTO,Long id){
        int current = videoSearchDTO.getPage_num() > 0 ? videoSearchDTO.getPage_num()-1 : 0;
        int size = videoSearchDTO.getPage_size() >0 ? videoSearchDTO.getPage_size() : 10;
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<Video>();
        String keywords = videoSearchDTO.getKeywords();
        // 将搜索记录存到redis中，后续若有别的操作可以将其取出来
        redisTemplate.opsForZSet().add(RedisKey.VIDEO_SEARCH_HISTORY+id,keywords,LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        NativeQuery nativeQuery = NativeQuery.builder()
                        .withQuery(q->q.bool(
                                b->b.must(m->m
                                        .multiMatch(mm->mm
                                                .fields("title^2","description")
                                                .query(keywords)
                                        )
                                )
                        ))
                                .withPageable(PageRequest.of(current,size))
                                                .build();
//        NativeQuery nativeQuery = NativeQuery.builder()
//
//                .withQuery(q -> q.matchAll(m -> m)) // 查所有
//
//                .withPageable(PageRequest.of(0, 10)) // 第0页
//
//                .build();
        SearchHits<VideoES> search = elasticsearchOperations.search(nativeQuery, VideoES.class);
        List<VideoES> list = search.stream().map(SearchHit::getContent).toList();
        return list.stream().map(videoES -> {
            Video video = new Video();
            BeanUtils.copyProperties(videoES, video);
            return video;
        }).toList();

    }


}
