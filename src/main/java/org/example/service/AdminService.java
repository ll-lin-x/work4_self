package org.example.service;

import org.example.model.dto.VideoReviewDTO;
import org.example.model.pojo.SensitiveWord;
import org.example.model.vo.VideoSimpleVo;
import org.example.model.vo.VideoVO;

import java.util.List;
import java.util.Map;

public interface AdminService {
    void addSensitiveWord(String word);

    void deleteSensitiveWord(String word);

    List<String> getSensitiveWord();

   List<VideoSimpleVo> getVideoList();

    void reviewVideo(VideoReviewDTO videoReviewDTO);
}
