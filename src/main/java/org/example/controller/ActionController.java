package org.example.controller;

import org.example.model.domin.LoginUserCache;
import org.example.model.dto.CommentListDTO;
import org.example.model.dto.CommentPublishDTO;
import org.example.model.dto.LikeActionDTO;
import org.example.model.dto.LikeListDTO;
import org.example.model.pojo.Comment;
import org.example.model.normal.Result;
import org.example.model.pojo.User;
import org.example.model.pojo.Video;
import org.example.model.vo.CommentVO;
import org.example.model.vo.UserVO;
import org.example.model.vo.VideoVO;
import org.example.service.ActionService;
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
import java.util.Objects;

@RestController
public class ActionController {
    @Autowired
    private ActionService actionService;

    @PostMapping("/like/action")
    public Result likeAction(LikeActionDTO likeActionDTO, @AuthenticationPrincipal LoginUserCache loginUserCache){
        if(!StringUtils.hasText(likeActionDTO.getAction_type()) || (!StringUtils.hasText(likeActionDTO.getComment_id()) && !StringUtils.hasText(likeActionDTO.getVideo_id())))
        {
            return Result.error("lack the necessary parameters");
        }
        actionService.likeAction(likeActionDTO,loginUserCache.getUser().getId());
        return Result.success();
    }

    @GetMapping("/like/list")
    public Result likeList(LikeListDTO likeListDTO){
        List<Video> videoList = actionService.likeList(likeListDTO);
        List<VideoVO>  videoVOList = videoList.stream().map(video->{
            LocalDateTime createdAt = Instant.ofEpochMilli(video.getCreatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime deletedAt = Instant.ofEpochMilli(video.getDeletedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime updatedAt = Instant.ofEpochMilli(video.getUpdatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            VideoVO videoVO = new VideoVO();
            BeanUtils.copyProperties(video, videoVO);
            videoVO.setCreatedAt(createdAt);
            videoVO.setDeletedAt(deletedAt);
            videoVO.setUpdatedAt(updatedAt);
            BeanUtils.copyProperties(video, videoVO);
            return videoVO;
        }).toList();
        HashMap<String,Object> map = new HashMap<>();
        map.put("items",videoVOList);
        return Result.success(map);
    }

    @PostMapping("/comment/publish")
    public Result publishComment(CommentPublishDTO comment,@AuthenticationPrincipal LoginUserCache loginUserCache){
        if(!StringUtils.hasText(comment.getContent()) || (!StringUtils.hasText(comment.getComment_id()) && !StringUtils.hasText(comment.getVideo_id())))
        {
            return Result.error("lack the necessary parameters");
        }
        actionService.publishComment(comment,loginUserCache.getUser().getId());
        return Result.success();
    }

    @GetMapping("/comment/list")
    public Result listComments(CommentListDTO commentDTO){
        if(!StringUtils.hasText(commentDTO.getComment_id()) && !StringUtils.hasText(commentDTO.getVideo_id()))
        {
            return Result.error("lack the necessary parameters");
        }
        List<Comment> commentList = actionService.listComments(commentDTO);
        List<CommentVO>  commentVOList = commentList.stream().map(comment->{
            CommentVO commentVO = new CommentVO();
            LocalDateTime createdAt = Instant.ofEpochMilli(comment.getCreatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime deletedAt = Instant.ofEpochMilli(comment.getDeletedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime updatedAt = Instant.ofEpochMilli(comment.getUpdatedAt()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            BeanUtils.copyProperties(comment, commentVO);
            commentVO.setCreatedAt(createdAt);
            commentVO.setDeletedAt(deletedAt);
            commentVO.setUpdatedAt(updatedAt);
            return commentVO;
        }).toList();
        HashMap<String,Object> map = new HashMap<>();
        map.put("items",commentVOList);
        return Result.success(map);
    }

    @DeleteMapping("/comment/delete")
    public Result deleteComment(@RequestParam("video_id") String videoId,
                                @RequestParam("comment_id") String commentId,
                                @AuthenticationPrincipal LoginUserCache loginUserCache){

        if(!StringUtils.hasText(videoId) && !StringUtils.hasText(commentId)) throw new IllegalArgumentException("lack the necessary parameters");
        actionService.deleteComment(videoId,commentId,loginUserCache.getUser().getId());
        return Result.success();
    }
}
