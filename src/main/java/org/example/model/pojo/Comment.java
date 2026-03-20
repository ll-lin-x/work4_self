package org.example.model.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    @TableId
    private Long id;
    private Long videoId;
    private Long userId;
    private Long rootId;
    private Long parentId;
    private Long replyUserId;
    private String content;
    private int likeCount;
    private int childCount;
    private Long createdAt;
    private Long updatedAt;
    private Long deletedAt;
}
