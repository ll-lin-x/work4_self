package org.example.model.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Conversation {
    @TableId
    private Long id;
    private int type;
    @TableField("`key`")
    private String key;
    private Long createdAt;
}
