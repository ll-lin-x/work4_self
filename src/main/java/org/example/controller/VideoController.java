package org.example.controller;

import org.example.model.dto.VideoListDTO;
import org.example.model.dto.VideoPublishDTO;
import org.example.model.dto.VideoSearchDTO;
import org.example.model.pojo.Result;
import org.example.model.pojo.User;
import org.example.model.pojo.Video;
import org.example.model.vo.VideoVO;
import org.example.service.VideoService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

@RestController
public class VideoController {
    @Autowired
    private VideoService videoService;


    @GetMapping("/video/feed/")
    public Result getVideoFeed(@RequestParam(value="latest_time",required = false) String latestTime){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime;
        if(latestTime == null || latestTime.isEmpty() || latestTime.equals("null")){
            dateTime = null;
        }else{
            dateTime = LocalDateTime.parse(latestTime, formatter);
        }
        List<Video> videoList= videoService.getVideoFeed(dateTime);
        List<VideoVO> videoVOList = videoList.stream().map(po -> {
            VideoVO vo = new VideoVO();
            BeanUtils.copyProperties(po, vo); // 自动拷贝同名属性
            return vo;
        }).toList();
        HashMap<String, Object> map = new HashMap<>();
        map.put("items", videoVOList);
        return Result.success(map);
    }


    @PostMapping("/video/publish")
    public Result publish(VideoPublishDTO videoPublishDTO, @AuthenticationPrincipal User loginUserDetails) {
        Long id = loginUserDetails.getId();
        return videoService.publish(videoPublishDTO,id);
    }
    @GetMapping("/video/list")
    public Result getPublishList(VideoListDTO videoListDTO, @AuthenticationPrincipal User loginUserDetails) {
        List<Video> videoList= videoService.getPublishList(videoListDTO,loginUserDetails.getId());
        List<VideoVO> videoVOList = videoList.stream().map(po->{
            VideoVO vo = new VideoVO();
            BeanUtils.copyProperties(po,vo);
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
            BeanUtils.copyProperties(video, videoVO);
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
    public Result searchVideo(VideoSearchDTO videoSearchDTO,@AuthenticationPrincipal User loginUserDetails) {
        if(StringUtils.hasText(videoSearchDTO.getKeywords()) && videoSearchDTO.getPage_num() != null && videoSearchDTO.getPage_size() != null){
            List<Video> videoList = videoService.searchVideo(videoSearchDTO,loginUserDetails.getId());
            List<VideoVO> videoVOList = videoList.stream().map(video->{
                VideoVO videoVO = new VideoVO();
                BeanUtils.copyProperties(video, videoVO);
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
