package org.example.model.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Relation {
    @TableId
    private Long id;
    private Long userId;
    private Long FocusUserId;
    private LocalDateTime createdAt;
}
