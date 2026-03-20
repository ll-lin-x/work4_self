package org.example.controller;

import org.example.model.domin.LoginUserCache;
import org.example.model.dto.VideoListDTO;
import org.example.model.dto.VideoPublishDTO;
import org.example.model.dto.VideoSearchDTO;
import org.example.model.normal.Result;
import org.example.model.pojo.User;
import org.example.model.pojo.Video;
import org.example.model.vo.VideoVO;
import org.example.service.VideoService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;

@RestController
public class VideoController {
    @Autowired
    private VideoService videoService;


    @GetMapping("/video/feed/")
    public Result getVideoFeed(@RequestParam(value="latest_time",required = false) String latestTime,
                               @AuthenticationPrincipal LoginUserCache loginUserCache){
        Double timeStemp;
        if(StringUtils.isEmpty(latestTime)){
            timeStemp = 0.0;
        }else{
            timeStemp = Double.valueOf(latestTime);
        }
        List<Video> videoList= videoService.getVideoFeed(timeStemp,loginUserCache.getUser().getId());
        List<VideoVO> videoVOList = videoList.stream().map(po -> {
            VideoVO vo = new VideoVO();
            LocalDateTime createdAt = Instant.ofEpochMilli(po.getCreatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime deletedAt = Instant.ofEpochMilli(po.getDeletedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime updatedAt = Instant.ofEpochMilli(po.getUpdatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            BeanUtils.copyProperties(po, vo);
            vo.setCreatedAt(createdAt);
            vo.setDeletedAt(deletedAt);
            vo.setUpdatedAt(updatedAt);
            return vo;
        }).toList();
        HashMap<String, Object> map = new HashMap<>();
        map.put("items", videoVOList);
        return Result.success(map);
    }


    @PostMapping("/video/publish")
    public Result publish(VideoPublishDTO videoPublishDTO, @AuthenticationPrincipal LoginUserCache loginUserCache) {
        Long id = loginUserCache.getUser().getId();
        return videoService.publish(videoPublishDTO,id);
    }
    @GetMapping("/video/list")
    public Result getPublishList(VideoListDTO videoListDTO, @AuthenticationPrincipal LoginUserCache loginUserCache) {
        User user = loginUserCache.getUser();
        List<Video> videoList= videoService.getPublishList(videoListDTO,user.getId());
        List<VideoVO> videoVOList = videoList.stream().map(po->{
            VideoVO vo = new VideoVO();
            LocalDateTime createdAt = Instant.ofEpochMilli(po.getCreatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime deletedAt = Instant.ofEpochMilli(po.getDeletedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime updatedAt = Instant.ofEpochMilli(po.getUpdatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            BeanUtils.copyProperties(po, vo);
            vo.setCreatedAt(createdAt);
            vo.setDeletedAt(deletedAt);
            vo.setUpdatedAt(updatedAt);
            return vo;
        }).toList();
        HashMap<String, Object> map = new HashMap<>();
        map.put("items", videoVOList);
        map.put("total", videoVOList.size());
        return Result.success(map);
    }
    @GetMapping("/video/popular")
    public Result getVideoPopular() {
        List<Video> videoList = videoService.getVideoPopular();
        List<VideoVO> videoVOList = videoList.stream().map(video->{
            VideoVO videoVO = new VideoVO();
            LocalDateTime createdAt = Instant.ofEpochMilli(video.getCreatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime deletedAt = Instant.ofEpochMilli(video.getDeletedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime updatedAt = Instant.ofEpochMilli(video.getUpdatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            BeanUtils.copyProperties(video, videoVO);
            videoVO.setCreatedAt(createdAt);
            videoVO.setDeletedAt(deletedAt);
            videoVO.setUpdatedAt(updatedAt);
            return videoVO;
        }).toList();
        HashMap<String, Object> map = new HashMap<>();
        map.put("items", videoVOList);
        return Result.success(map);
    }

    @PostMapping("/video/visit")
    public Result visitVideo(String videoId) {
        videoService.visitVideo(Long.parseLong(videoId));
        return Result.success();
    }
    @PostMapping("/video/search")
    public Result searchVideo(VideoSearchDTO videoSearchDTO,@AuthenticationPrincipal LoginUserCache loginUserCache) {
        if(StringUtils.hasText(videoSearchDTO.getKeywords()) && videoSearchDTO.getPage_num() != null && videoSearchDTO.getPage_size() != null){
            List<Video> videoList = videoService.searchVideo(videoSearchDTO,loginUserCache.getUser().getId());
            List<VideoVO> videoVOList = videoList.stream().map(video->{
                VideoVO videoVO = new VideoVO();
                LocalDateTime createdAt = Instant.ofEpochMilli(video.getCreatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
                LocalDateTime deletedAt = Instant.ofEpochMilli(video.getDeletedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
                LocalDateTime updatedAt = Instant.ofEpochMilli(video.getUpdatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
                BeanUtils.copyProperties(video, videoVO);
                videoVO.setCreatedAt(createdAt);
                videoVO.setDeletedAt(deletedAt);
                videoVO.setUpdatedAt(updatedAt);
                return videoVO;
            }).toList();
            HashMap<String, Object> map = new HashMap<>();
            map.put("items", videoVOList);
            map.put("total", videoVOList.size());
            return Result.success(map);
        }else{
            return Result.error("lack neccessery msg");
        }

    }
}
