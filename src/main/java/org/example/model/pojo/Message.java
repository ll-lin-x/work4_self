package org.example.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long conversationId;
    private int seq;
    private Long senderId;
    private int type;
    private String image;
    private String content;
    private int status;
    private long createdAt;
}
