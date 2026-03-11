package org.example.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.example.mapper.RelationMapper;
import org.example.model.dto.FollowingListDTO;
import org.example.model.pojo.Relation;
import org.example.model.pojo.Video;
import org.example.model.vo.FollowingListVO;
import org.example.service.RelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RelationServiceImpl implements RelationService {
    @Autowired
    private RelationMapper relationMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void relationAction(String toUserId, Integer actionType,Long userId) {
        Long focusId ;
        try{
           focusId = Long.parseLong(toUserId);
        }catch(Exception e){
            throw new IllegalArgumentException("to_user_id illegal value");
        }
        // 自己不能关注/取消关注自己
        if(focusId.equals(userId)) return;

        Relation relation = relationMapper.selectOne(new LambdaQueryWrapper<Relation>()
                .eq(Relation::getFocusUserId,focusId)
                .eq(Relation::getUserId,userId)
        );

        Relation relationFocus = relationMapper.selectOne(new LambdaQueryWrapper<Relation>()
                .eq(Relation::getFocusUserId,userId)
                .eq(Relation::getUserId,focusId)
        );

        if(actionType==1){
            // 关注操作
            // 若已关注则不能再次点击关注
            if(relation != null) return;
            // 查看对方是否关注
            if(relationFocus != null){
                // 对方有关注
                relationMapper.insert(new Relation(null,userId,focusId,1,LocalDateTime.now(),LocalDateTime.now()));
                relationMapper.update(new LambdaUpdateWrapper<Relation>()
                        .eq(Relation::getFocusUserId,userId)
                        .eq(Relation::getUserId,focusId)
                        .set(Relation::getFlag,1)
                        .set(Relation::getUpdatedAt,LocalDateTime.now())
                );
            }else{
                // 对方没有关注
                relationMapper.insert(new Relation(null,userId,focusId,0,LocalDateTime.now(),LocalDateTime.now()));
            }


        }else if(actionType==2){
            // 取消关注
            // 若未关注则不能取消关注
            if(relation == null) return;
            // 查看对方是否关注
            if(relationFocus != null){
                // 对方有关注
                relationMapper.update(new LambdaUpdateWrapper<Relation>()
                        .eq(Relation::getFocusUserId,userId)
                        .eq(Relation::getUserId,focusId)
                        .set(Relation::getFlag,0)
                        .set(Relation::getUpdatedAt,LocalDateTime.now())
                );
            }
            relationMapper.deleteById(relation.getId());
        }else{
            throw new IllegalArgumentException("action_type illegal");
        }

    }

    @Override
    public List<FollowingListVO> followingList(FollowingListDTO followingListDTO){
        Long id = Long.parseLong(followingListDTO.getUser_id());
        int current = followingListDTO.getPage_num() > 0 ? followingListDTO.getPage_num() : 1;
        int size = followingListDTO.getPage_size() >0 ? followingListDTO.getPage_size() : 10;
        Page<FollowingListVO> Page = new Page<>(current,size);
        IPage<FollowingListVO> followingPage = relationMapper.getFollowingPage(Page, id);
        return followingPage.getRecords();
    }

    @Override
    public List<FollowingListVO> followerList(FollowingListDTO followingListDTO){
        Long id = Long.parseLong(followingListDTO.getUser_id());
        int current = followingListDTO.getPage_num() > 0 ? followingListDTO.getPage_num() : 1;
        int size = followingListDTO.getPage_size() >0 ? followingListDTO.getPage_size() : 10;
        Page<FollowingListVO> Page = new Page<>(current,size);
        IPage<FollowingListVO> followerPage = relationMapper.getFollowerPage(Page, id);
        return followerPage.getRecords();
    }
    @Override
    public List<FollowingListVO> friendsList(FollowingListDTO followingListDTO,Long userId) {
        int current = followingListDTO.getPage_num() > 0 ? followingListDTO.getPage_num() : 1;
        int size = followingListDTO.getPage_size() >0 ? followingListDTO.getPage_size() : 10;
        Page<FollowingListVO> Page = new Page<>(current,size);
        LambdaQueryWrapper<Relation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Relation::getUserId,userId)
                .eq(Relation::getFlag,1);
        IPage<FollowingListVO> friendsrPage = relationMapper.getFriendsPage(Page, queryWrapper);
        return friendsrPage.getRecords();
    }


}
