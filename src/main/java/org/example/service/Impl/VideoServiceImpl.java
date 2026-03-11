package org.example.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.mapper.LikeMapper;
import org.example.mapper.UserMapper;
import org.example.mapper.VideoMapper;
import org.example.model.dto.VideoListDTO;
import org.example.model.dto.VideoPublishDTO;
import org.example.model.dto.VideoSearchDTO;
import org.example.model.pojo.Result;
import org.example.model.pojo.User;
import org.example.model.pojo.Video;
import org.example.service.VideoService;
import org.example.utils.AsyncUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    private LikeMapper likeMapper;

    @Value("${upload.path}")
    private String savaPath;


    @Override
    public List<Video> getVideoFeed(LocalDateTime dateTime) {
        if(Objects.isNull(dateTime)){
            return videoMapper.selectList(null);
        }
        return videoMapper.selectList(new LambdaQueryWrapper<Video>().gt(Video::getUpdatedAt, dateTime));
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

        List<Video> videoList = videoPage.getRecords();
        return videoList;
    }

    @Override
    public List<Video> getVideoPopular(){
        Set<Long> videoIdSet = redisTemplate.opsForZSet().reverseRange("video:rank:total:", 0, 9);
        if(CollectionUtils.isEmpty(videoIdSet)){
            synchronized (this){
                if(videoIdSet.size()==0){
                    List<Video> videoLists = videoMapper.selectList(new LambdaQueryWrapper<Video>()
                            .select(Video::getId, Video::getVisitCount)
                            .orderByDesc(Video::getVisitCount)
                            .last("limit 10"));
                    videoLists.forEach(video->{
                        redisTemplate.opsForZSet().add("video:rank:total:",video.getId(),video.getVisitCount());
                    });
                }
            }
            videoIdSet = redisTemplate.opsForZSet().reverseRange("video:rank:total:", 0, 9);
        }
        List<Long> videoIdList = new ArrayList<Long>(videoIdSet);
        List<String> videoDetailIds = videoIdSet.stream().map(id->"video:detail:" + id).toList();
        List<Object> videoDetails = redisTemplate.opsForValue().multiGet(videoDetailIds);
        List<Video> videoList = new ArrayList<>();
        for (int i = 0; i < videoDetails.size(); i++) {
            Long videoId =videoIdList.get(i);
            Video videoDetail = (Video)videoDetails.get(i);
            if(Objects.isNull(videoDetail)){
                videoDetail = videoMapper.selectById(videoId);
                if(!Objects.isNull(videoDetail)){
                    redisTemplate.opsForValue().set("video:detail:" + videoId, videoDetail, 1, TimeUnit.DAYS);
                }
            }
            if(!Objects.isNull(videoDetail)){
                Double score = redisTemplate.opsForZSet().score("video:rank:total:", videoId);
                if(!Objects.isNull(score)){
                    videoDetail.setVisitCount(score.intValue());
                }
                if(!redisTemplate.hasKey("video:like:"+videoId)){
                    List<Long> userIds = likeMapper.selectUserIdsByVideoId(videoId);
                    userIds.forEach(userId -> {
                        redisTemplate.opsForSet().add("video:like:"+videoId,userId);
                    });
                }
                Long likeCount = redisTemplate.opsForSet().size("video:like:" + videoId);
                if(!Objects.isNull(likeCount)){
                    videoDetail.setLikeCount(likeCount.intValue());
                }
                if(!redisTemplate.opsForHash().hasKey("video:comment:",String.valueOf(videoId))){
                    int commentCount = videoMapper.selectOne(new LambdaQueryWrapper<Video>().eq(Video::getId, videoId)).getCommentCount();
                    redisTemplate.opsForHash().increment("video:comment:",String.valueOf(videoId),commentCount);
                }
                Integer commentCount = (Integer)redisTemplate.opsForHash().get("video:comment:", String.valueOf(videoId));
                if(!Objects.isNull(commentCount)){
                    videoDetail.setCommentCount(commentCount.intValue());
                }
                videoList.add(videoDetail);
            }
        }
        videoList.sort((v1,v2)-> v2.getVisitCount()-v1.getVisitCount());// 以防在前面的系列操作中打乱了顺序

        return videoList;
    }
    
    @Override
    public int visitVideo(Long videoId){
        redisTemplate.opsForHash().increment("video:rank:increment:",String.valueOf(videoId),1);
        Double score = redisTemplate.opsForZSet().score("video:rank:total:", videoId);
        if(Objects.isNull(score)){
            Video video = videoMapper.selectById(videoId);
            if(!Objects.isNull(video)){
                redisTemplate.opsForZSet().add("video:rank:total:",video.getId(),video.getVisitCount());
            }else{
                throw new RuntimeException("the video is not exist");
            }
        }
        Double visitCount = redisTemplate.opsForZSet().incrementScore("video:rank:total:", videoId, 1);
        return visitCount.intValue();
    }

    @Override
    public List<Video> searchVideo(VideoSearchDTO videoSearchDTO,Long id){
        String key = "video:search:history:";

        int current = videoSearchDTO.getPage_num() > 0 ? videoSearchDTO.getPage_num() : 1;
        int size = videoSearchDTO.getPage_size() >0 ? videoSearchDTO.getPage_size() : 10;
        Page<Video> videoPage = new Page<>(current,size);
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<Video>();
        String keywords = videoSearchDTO.getKeywords();
        // 将搜索记录存到redis中，后续若有别的操作可以将其取出来
        redisTemplate.opsForZSet().add(key+id,keywords,LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
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
