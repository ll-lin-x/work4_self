package org.example.model.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Video {
    @TableId
    private Long id;
    private Long userId;
    private String videoUrl;
    private String coverUrl;
    private String title;
    private String description;
    private int visitCount;
    private int likeCount;
    private int commentCount;
    // state: 0-待审核, 1-审核通过, 2-审核不通过
    private int state;
    private String reason;
    private Long createdAt;
    private Long updatedAt;
    private Long deletedAt;
}
