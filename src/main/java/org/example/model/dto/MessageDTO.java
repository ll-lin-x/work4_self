package org.example.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageDTO {
    private String reciever;
    private String message;
    private int type;// 群聊、私聊类型，群聊是0，私聊是1
}
