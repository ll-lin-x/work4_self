package org.example.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentVO {
    @JsonFormat(shape=JsonFormat.Shape.STRING)
    private Long id;
    @JsonFormat(shape=JsonFormat.Shape.STRING)
    @JsonProperty("video_id")
    private Long videoId;
    @JsonFormat(shape=JsonFormat.Shape.STRING)
    @JsonProperty("user_id")
    private Long userId;
    @JsonFormat(shape=JsonFormat.Shape.STRING)
    @JsonProperty("parent_id")
    private Long parentId;

    private String content;
    @JsonProperty("like_count")
    private int likeCount;
    @JsonProperty("child_count")
    private int childCount;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    @JsonProperty("deleted_at")
    private LocalDateTime deletedAt;
}
