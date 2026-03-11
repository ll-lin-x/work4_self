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
public class VideoVO {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonProperty("user_id")
    private Long userId;
    @JsonProperty("video_url")
    private String videoUrl;
    @JsonProperty("cover_url")
    private String coverUrl;
    private String title;
    private String description;
    @JsonProperty("visit_count")
    private int visitCount;
    @JsonProperty("like_count")
    private int likeCount;
    @JsonProperty("comment_count")
    private int commentCount;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    @JsonProperty("deleted_at")
    private LocalDateTime deletedAt;
}
