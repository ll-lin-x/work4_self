package org.example.controller;

import net.bytebuddy.asm.Advice;
import org.example.model.dto.VideoReviewDTO;
import org.example.model.normal.Result;
import org.example.model.vo.VideoSimpleVo;
import org.example.model.vo.VideoVO;
import org.example.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class AdminController {

    @Autowired
    private AdminService adminService;

    // 敏感词管理
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/admin/sensitive-word/add")
    public Result addSensitiveWord(String word) {
        if(word==null|| word.isEmpty()){
            return Result.error("敏感词不能为空");
        }
        adminService.addSensitiveWord(word);
        return Result.success();
    }


    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/admin/sensitive-word/delete")
    public Result deleteSensitiveWord(String word) {
        if(word==null|| word.isEmpty()){
            return Result.error("敏感词不能为空");
        }
        adminService.deleteSensitiveWord(word);
        return Result.success();
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/admin/sensitive-word/get")
    public Result getSensitiveWord() {

        List<String> sensitiveWord = adminService.getSensitiveWord();
        return Result.success(sensitiveWord);
    }

    @GetMapping("/admin/video")
    public Result getVideo() {
        List<VideoSimpleVo> urls = adminService.getVideoList();
        return Result.success(urls);
    }

    @PatchMapping("/admin/video/review")
    public Result reviewVideo(VideoReviewDTO videoReviewDTO) {
        adminService.reviewVideo(videoReviewDTO);
        return Result.success();
    }

}
