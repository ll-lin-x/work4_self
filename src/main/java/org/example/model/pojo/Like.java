package org.example.model.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("`like`")
public class Like {
    @TableId
    private Long id;
    private Long userId;
    private Long videoId;
    private Long commentId;
    private Long createdAt;
}
