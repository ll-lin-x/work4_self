package org.example.model.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Member {
    @TableId
    private Long id;
    private Long conversationId;
    private Long userId;
    private int shield;// 是否屏蔽
}
