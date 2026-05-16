package org.example.service;


import org.example.model.dto.FollowingListDTO;
import org.example.model.vo.FollowingListVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RelationService {
    void relationAction(String toUserId, Integer actionType,Long userId);

    List<FollowingListVO> followingList(FollowingListDTO followingListDTO);

    List<FollowingListVO> followerList(FollowingListDTO followingListDTO);

    List<FollowingListVO> friendsList(FollowingListDTO followingListDTO,Long userId);

    String imageURL(MultipartFile file);
}
