package org.example.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowingListVO {
    @JsonFormat(shape=JsonFormat.Shape.STRING)
    private Long id;
    private String username;
    @JsonProperty("avatar_url")
    private String avatarUrl;
}
