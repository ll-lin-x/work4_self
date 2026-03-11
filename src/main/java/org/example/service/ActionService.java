package org.example.service;

import org.example.model.dto.CommentListDTO;
import org.example.model.dto.CommentPublishDTO;
import org.example.model.dto.LikeActionDTO;
import org.example.model.dto.LikeListDTO;
import org.example.model.pojo.Comment;
import org.example.model.pojo.Video;

import java.util.List;

public interface ActionService {
    void publishComment(CommentPublishDTO comment, Long id);

    List<Comment> listComments(CommentListDTO comment);

    void deleteComment(String videoId, String commentId, Long id);

    void likeAction(LikeActionDTO likeActionDTO, Long id);

    List<Video> likeList(LikeListDTO likeListDTO);
}
