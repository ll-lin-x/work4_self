package org.example.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentListDTO {
    private String video_id;
    private String comment_id;
    private Integer page_num;
    private Integer page_size;
}
