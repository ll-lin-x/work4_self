package org.example.model.ES;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "video")
public class VideoES {
    @Id
    private Long id;
    @Field(type = FieldType.Keyword)
    private Long userId;
    @Field(type = FieldType.Keyword, index = false)
    private String videoUrl;
    @Field(type = FieldType.Keyword, index = false)
    private String coverUrl;
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String description;
    @Field(type = FieldType.Integer)
    private int visitCount;
    @Field(type = FieldType.Integer)
    private int likeCount;
    @Field(type = FieldType.Integer)
    private int commentCount;
    @Field(type = FieldType.Date)
    private Long createdAt;
    @Field(type = FieldType.Date)
    private Long updatedAt;
    @Field(type = FieldType.Date)
    private Long deletedAt;
}
