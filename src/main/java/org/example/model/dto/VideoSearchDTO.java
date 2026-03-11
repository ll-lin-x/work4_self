package org.example.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoSearchDTO {
    private String keywords;

    private Integer page_size;

    private Integer page_num;

    private Integer from_data;

    private Integer to_data;
    private String username;

}
