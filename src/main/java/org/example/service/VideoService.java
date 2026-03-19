package org.example.service;

import org.example.model.dto.VideoListDTO;
import org.example.model.dto.VideoPublishDTO;
import org.example.model.dto.VideoSearchDTO;
import org.example.model.normal.Result;
import org.example.model.pojo.Video;

import java.time.LocalDateTime;
import java.util.List;

public interface VideoService {
    Result publish(VideoPublishDTO videoPublishDTO, Long id);

    List<Video> getVideoFeed(Double dateTime,Long id);

    List<Video> getPublishList(VideoListDTO videoListDTO, Long id);

    List<Video> getVideoPopular();

    void visitVideo(Long videoId);

    List<Video> searchVideo(VideoSearchDTO videoSearchDTO,Long id);
}
