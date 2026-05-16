package org.example.service.Impl;



import com.alibaba.fastjson.JSON;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.lang.Strings;
import lombok.extern.slf4j.Slf4j;
import org.example.mapper.ConversationMapper;
import org.example.mapper.MemberMapper;
import org.example.mapper.MessageMapper;
import org.example.mapper.UserMapper;
import org.example.model.pojo.Conversation;
import org.example.model.pojo.Member;
import org.example.model.pojo.Message;
import org.example.model.pojo.User;
import org.example.model.vo.FollowingListVO;
import org.example.model.vo.MessageVO;
import org.example.service.WebSocketService;
import org.example.utils.FileUtil;
import org.example.utils.JwtUtil;
import org.example.utils.SensitiveWordFilter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import jakarta.websocket.Session;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.example.model.normal.RedisKey;
@Slf4j
@Service
public class WebSocketServiceImpl implements WebSocketService {
    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;

    @Autowired
    private UserMapper userMapper;

//    @Autowired
//    @Qualifier("imTaskExecutor")
//    private Executor imTaskExecutor;

    @Autowired
    @Lazy
    private WebSocketService self;

    @Autowired
    @Qualifier("imShardExecutors")
    private List<Executor> imShardExecutors;

    private static final DefaultRedisScript<List> POP_100_OLDEST_SCRIPT = new DefaultRedisScript<>(
            "local key = KEYS[1]\n" +
            "local raw = ARGV[1]\n" +
            "if not raw then return {} end\n" +
            "local cnt = tonumber(raw)\n" +
            "if not cnt then\n" +
            "  raw = string.gsub(raw, '\"', '')\n" +
            "  cnt = tonumber(raw)\n" +
            "end\n" +
            "if not cnt or cnt <= 0 then return {} end\n" +
            "local res = redis.call('ZRANGE', key, 0, cnt-1)\n" +
            "if #res > 0 then redis.call('ZREMRANGEBYRANK', key, 0, cnt-1) end\n" +
            "return res",
            List.class
    );

    private static final DefaultRedisScript<List> POP_LIST_BATCH_SCRIPT = new DefaultRedisScript<>(
            "local key = KEYS[1]\n" +
            "local raw = ARGV[1]\n" +
            "if not raw then return {} end\n" +
            "local cnt = tonumber(raw)\n" +
//            "if not cnt then return {} end\n" +
                    "if not cnt then\n" +

                    " raw = string.gsub(raw, '\"', '')\n" +

                    " cnt = tonumber(raw)\n" +

                    "end\n" +
            "if not cnt or cnt <= 0 then return {} end\n" +
            "local res = redis.call('LRANGE', key, 0, cnt-1)\n" +
            "if #res > 0 then redis.call('LTRIM', key, cnt, -1) end\n" +
            "return res",
            List.class
    );

    private static final DefaultRedisScript<Long> ZADD_TRIM_SCRIPT = new DefaultRedisScript<>(
            "local key = KEYS[1]\n" +
            "local value = ARGV[1]\n" +
            "local rawScore = ARGV[2]\n" +
            "local rawMax = ARGV[3]\n" +
            "if not value or not rawScore or not rawMax then return 0 end\n" +
            "rawScore = string.gsub(rawScore, '\"', '')\n" +
            "rawMax = string.gsub(rawMax, '\"', '')\n" +
            "local score = tonumber(rawScore)\n" +
            "local max = tonumber(rawMax)\n" +
            "if not score or not max or max <= 0 then return 0 end\n" +
            "redis.call('ZADD', key, score, value)\n" +
            "local size = redis.call('ZCARD', key)\n" +
            "if size > max then\n" +
            "  redis.call('ZREMRANGEBYRANK', key, 0, size - max - 1)\n" +
            "end\n" +
            "return size",
            Long.class
    );

    private static final int MAX_CACHE_SIZE = 50;

    private static final int MQ_BATCH_SIZE = 50;
    // private static final long MQ_FLUSH_INTERVAL_MS = TimeUnit.MINUTES.toMillis(30);
    private static final long MQ_FLUSH_LOCK_TTL_SEC = 5;



    @Override
    public String onOpen(Session session, ConcurrentHashMap<String,Session> onlineUsers){
        String userId = authentic(session);
        if(userId==null){
            log.error("invalid token, connection refused");
            throw new IllegalArgumentException("token out date");
        }

        onlineUsers.put(userId, session);
        getUnreadInfo(session,userId);
        return userId;
    }

    @Override
    public void onClose(String id,ConcurrentHashMap<String,Session> onlineUsers) {
        onlineUsers.remove(id);
    }


    @Override
    public void onMessage(String message,String userId,ConcurrentHashMap<String,Session> onlineUsers) {
        Session session = onlineUsers.get(userId);
        if (!Strings.hasText(userId)) {
            return;
        }

        JSONObject jsonObject = JSON.parseObject(message);
        Integer typeObj = jsonObject.getInteger("type");
        if (typeObj == null) {
            return;
        }
        int type = typeObj;
        Member member;
        long id;
        switch (type){
            // 单聊、群聊
            case 0:
            case 1:
                if (!jsonObject.containsKey("id") || !Strings.hasText(jsonObject.getString("id"))) {
                    return;
                }
                self.asyncHandleChatMessage(jsonObject, userId, onlineUsers);
                break;
            // 创建群聊
            case 2:
                List<String> memberIds = jsonObject.getJSONArray("id").toJavaList(String.class);
                haveGroup(userId, memberIds,onlineUsers);
                return;
            case 3:
                // 屏蔽群聊
                // 先查看是否已经屏蔽了
                Long idResult = redisTemplate.opsForSet().add(RedisKey.IM_USER_SHIELD + String.valueOf(userId),jsonObject.getString("id"));
                if (idResult != null && idResult >0) {
                    // 第一次屏蔽
                    member = memberMapper.selectOne(new LambdaQueryWrapper<Member>().eq(Member::getConversationId, Long.parseLong(jsonObject.getString("id"))).eq(Member::getUserId, Long.valueOf(userId)));
                    if (member != null) {
                        member.setShield(1);
                        memberMapper.updateById(member);
                    }
                }
                return;
            case 4:
                // 取消屏蔽
                Long result = redisTemplate.opsForSet().remove(
                        RedisKey.IM_USER_SHIELD + String.valueOf(userId),
                        String.valueOf(jsonObject.getString("id"))
                );
                if (result != null &&  result > 0) {
                    member = memberMapper.selectOne(new LambdaQueryWrapper<Member>().eq(Member::getConversationId, Long.parseLong(jsonObject.getString("id"))).eq(Member::getUserId, Long.valueOf(userId)));
                    if(member!=null ){
                        member.setShield(0);
                        memberMapper.updateById(member);
                    }

                }

                return;
            case 5:
                // 接收信息
                if (!jsonObject.containsKey("id") || !Strings.hasText(jsonObject.getString("id"))) {
                    return;
                }
                id = Long.parseLong(jsonObject.getString("id"));
                recieveMessage(onlineUsers.get(userId),id,Long.valueOf(userId));
                break;
            case 6: {
                // 查看联系人
                if (session != null && session.isOpen()) {
                    getFriends(session, userId);
                }
                return;
            }
            case 7: {
                // 查看最近会话
                if (session != null && session.isOpen()) {
                    sendRecentConversations(session, userId);
                }
                return;
            }
            default:
                sendMsg(session,"invalid message type");
        }
    }

    // 路由方法
    private Executor shareExecutor(Long conversationId){
        int shareCount = imShardExecutors.size();
        int idx = Math.floorMod(conversationId.hashCode(), shareCount);
        return imShardExecutors.get(idx);
    }
    private void executeByConversationId(Long conversationId,Runnable runnable){
        shareExecutor(conversationId).execute(runnable);
    }

    private void sendMsg(Session session,Object msg){
        try {
            session.getBasicRemote().sendText(JSON.toJSONString(msg));
        } catch (IOException e) {
            log.error("send msg error",e);
            throw new RuntimeException(e);
        }
    }
    // 辅助方法
    private String authentic(Session session){
        String queryString = session.getQueryString();
        if(!(Strings.hasText(queryString)&&queryString.contains("token="))){
            return null;
        }
        String token = queryString.substring(queryString.indexOf("token=")+6);
        try{
            Claims claims = JwtUtil.parseJWT(token);
            return claims.getSubject();
        }catch (Exception e){
            log.error("authentic error",e);
            throw new RuntimeException("Invalid token");
        }
    }


    private void getUnreadInfo(Session session,String userId) {
        // 存储用户未读信息
        Map<String,Integer> map = new HashMap<>();
        redisTemplate.opsForHash().entries(RedisKey.IM_UNREAD + String.valueOf(userId)).forEach((key, value) -> {
            map.put(key.toString(),Integer.parseInt(value.toString()));
        });
        Set<String> members = redisTemplate.opsForSet().members(RedisKey.IM_USER_SHIELD + String.valueOf(userId));
        if(members != null && !members.isEmpty()){
            map.keySet().removeAll(members);
        }

        if(!map.isEmpty()){
            sendMsg(session,map);
        }
    }
    // 处理单聊信息
    private void handleChatMessage(JSONObject jsonObject,String userId,ConcurrentHashMap<String,Session> onlineUsers,Long conversationId,long now){
        int type = jsonObject.getInteger("type");
        String image = jsonObject.getString("image");
//        Long id = Long.valueOf(jsonObject.getString("id"));
        String message = jsonObject.getString("message");
        if((image==null|| image.isEmpty())&& (message==null|| message.isEmpty())){
            return;
        }
//        Long now = System.currentTimeMillis();
//        Long conversationId = getConversationId(userId,id,now,type);
//        if(conversationId == null){
//            Session session = onlineUsers.get(userId);
//            sendMsg(session,"not friends or not group member");
//            return;
//        }
        String msg = sensitiveWordFilter.filter(message);

        String seqKey = RedisKey.IM_SEQ + String.valueOf(conversationId);
        Long newSeqVal = redisTemplate.opsForValue().increment(seqKey);
        long newSeq = (newSeqVal == null) ? 1L : newSeqVal;

        Message tempMsg = new Message(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId(),conversationId,(int)newSeq,Long.valueOf(userId),type,image,msg,0,now);
        String cacheKey = RedisKey.IM_MSG_CACHE + String.valueOf(conversationId);

        Long execute =(Long) redisTemplate.execute(
                ZADD_TRIM_SCRIPT,
                Collections.singletonList(cacheKey),
                JSON.toJSONString(tempMsg),
                String.valueOf(newSeq),
                String.valueOf(MAX_CACHE_SIZE)
        );
        log.info("***************************************************"+String.valueOf(execute));

        redisTemplate.opsForList().rightPush(RedisKey.IM_MQ_CACHE, JSON.toJSONString(tempMsg));
        Long mqSize = redisTemplate.opsForList().size(RedisKey.IM_MQ_CACHE);
        if (mqSize != null && mqSize >= MQ_BATCH_SIZE) {
            self.asyncSendToMQ();
        }

        // Long cacheSize = redisTemplate.opsForZSet().size(cacheKey);
        // if (cacheSize != null && cacheSize > MAX_CACHE_SIZE) {
        //     long excess = cacheSize - MAX_CACHE_SIZE;
        //     redisTemplate.execute(
        //             POP_100_OLDEST_SCRIPT,
        //             Collections.singletonList(cacheKey),
        //             String.valueOf(excess)
        //     );
        // }

        Set<String> members = redisTemplate.opsForSet().members(
                RedisKey.IM_CONVERSATION_MEMBER + String.valueOf(conversationId)
        );
        if (members == null || members.isEmpty()) {
            return;
        }
        members.remove(String.valueOf(userId));
        members.forEach(memberId->{
            redisTemplate.opsForHash().increment(
                    RedisKey.IM_UNREAD + String.valueOf(memberId),
                    String.valueOf(conversationId),
                    1
            );
            Boolean isShield = redisTemplate.opsForSet().isMember(
                    RedisKey.IM_USER_SHIELD + String.valueOf(memberId),
                    String.valueOf(conversationId)
            );
            if(!Boolean.TRUE.equals(isShield)){
                sendTips(conversationId, memberId, onlineUsers);
            }

            redisTemplate.opsForZSet().add(
                    RedisKey.IM_USER_CONVERSATION + String.valueOf(memberId),
                    String.valueOf(conversationId),
                    now
            );
        });

    }

    private Long getConversationId(String userId,Long id,Long now,int type){
        Long conversationId;
        if(type == 0){
            // 群聊
            // 判断是否是群成员
            Boolean isMember = redisTemplate.opsForSet().isMember(
                    RedisKey.IM_CONVERSATION_MEMBER + String.valueOf(id),
                    String.valueOf(userId)
            );
            if(!isMember){
                return null;
            }
            conversationId = id;
        }else{
            // 单聊
            conversationId = getSignalConversationId(userId, id, now);
        }
        return conversationId;
    }


    private Long getSignalConversationId(String userId,Long id,Long now){
        long fromId = Long.parseLong(userId);
        Long conversationId;
        long max = Math.max(fromId, id);
        long min = Math.min(fromId, id);
        String key = min +"_"+max;
        // 判断数据库中是否有这个conversation，若有就读取conversation_id，若没有就创建conversation_id
//        Conversation conversation = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>().eq(Conversation::getKey, key));
        String conversation = (String)redisTemplate.opsForHash().get(RedisKey.IM_KEY_TO_ID, key);
        // 使用redis改进

        if(conversation == null){
            if(!sendRelations( userId).contains(id)){
                // 不是好友关系不能聊天
                return null;
            }
            // 创建conversation_id
            // 不论单聊还是群聊，都需要创建conversation的记录
            Conversation con = new Conversation();
            con.setType(1);
            con.setKey(key);
            con.setCreatedAt(now);
            conversationMapper.insert(con);
            conversationId = con.getId();

            memberMapper.insert(new Member(null,conversationId,Long.valueOf(userId),0));
            memberMapper.insert(new Member(null,conversationId,id,0));
            redisTemplate.opsForSet().add(RedisKey.IM_CONVERSATION_MEMBER + String.valueOf(conversationId), String.valueOf(userId));
            redisTemplate.opsForSet().add(RedisKey.IM_CONVERSATION_MEMBER + String.valueOf(conversationId), String.valueOf(id));
            redisTemplate.opsForHash().put(RedisKey.IM_KEY_TO_ID, key, String.valueOf(conversationId));
        }else{
            conversationId = Long.parseLong(conversation);
        }
        return conversationId;
    }

    private void sendTips(Long conversationId ,String toId,ConcurrentHashMap<String,Session> onlineUsers) {
        Session session = onlineUsers.get(toId);
        if(session!=null && session.isOpen()){
            // 将数据反馈给前端
            String meg = "conversation:" + conversationId + " new_message";
            sendMsg(session,meg);

        }

        // 用户不在线那么就在用户连接时进行读取，也就是onopoen方法中实现
    }

    private void recieveMessage(Session session,Long conversationId,Long userId){
        if (session == null || !session.isOpen()) {
            return;
        }
        // 判断是否是群成员
        Boolean isMember = redisTemplate.opsForSet().isMember(
                RedisKey.IM_CONVERSATION_MEMBER + String.valueOf(conversationId),
                String.valueOf(userId)
        );
        if(!Boolean.TRUE.equals(isMember)){
            return;
        }

        Object unReadObj = redisTemplate.opsForHash().get(
                RedisKey.IM_UNREAD + String.valueOf(userId),
                String.valueOf(conversationId)
        );
        int unReadNum = (unReadObj == null) ? 0 : Integer.parseInt(unReadObj.toString());
        if (unReadNum <= 0) {
            return;
        }

        Set<Object> tempSet = redisTemplate.opsForZSet().reverseRange(
                RedisKey.IM_MSG_CACHE + String.valueOf(conversationId),
                0,
                unReadNum - 1
        );
        if (tempSet == null || tempSet.isEmpty()) {
            return;
        }
        List<Message> messages = tempSet.stream()
                .map(obj -> JSON.parseObject(obj.toString(), Message.class))
                .toList();
        // 更新未读信息数量
        redisTemplate.opsForHash().delete(
                RedisKey.IM_UNREAD + String.valueOf(userId),
                String.valueOf(conversationId)
        );
        // 返回给前端
        if(messages.isEmpty()){
            return;
        }

        messages.forEach(message -> {
            MessageVO vo = new MessageVO();
            vo.setFromId(String.valueOf(message.getSenderId()));
            vo.setMessage(message.getContent());
            vo.setImage(message.getImage());
            vo.setSendTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(message.getCreatedAt()), ZoneId.systemDefault()));
            sendMsg(session,vo);
        });
    }




    // 创建群聊   在conversation和member表中添加信息
    private void haveGroup(String userId,List<String> members,ConcurrentHashMap<String,Session> onlineUsers){
        if(members==null||members.isEmpty() || (members.size()==1 && members.contains(userId))){
            log.error("members is empty");
            throw new IllegalArgumentException("members is empty");
        }
        long now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Long id = com.baomidou.mybatisplus.core.toolkit.IdWorker.getId();
        conversationMapper.insert(new Conversation(id,0,String.valueOf(id),now));
        if(!members.contains(userId)){
            members.add(userId);
        }
        List<Member> memberList = members.stream().map(member->{
            Long memberId = com.baomidou.mybatisplus.core.toolkit.IdWorker.getId();
            Member memberEntity = new Member();
            memberEntity.setId(memberId);
            memberEntity.setConversationId(id);
            memberEntity.setUserId(Long.valueOf(member));
            return  memberEntity;
        }).toList();


        memberMapper.insertList(memberList);
        redisTemplate.opsForSet().add(
                RedisKey.IM_CONVERSATION_MEMBER + String.valueOf(id),
                members.toArray()
        );
        redisTemplate.opsForZSet().add(
                RedisKey.IM_USER_CONVERSATION + String.valueOf(userId),
                String.valueOf(id),
                now
        );
        // 将群聊号返回给前端
        Session session = onlineUsers.get(userId);
        String msg = "{成功创建群聊:"+id+"}";
        sendMsg(session,msg);
    }


    private  List<Long> sendRelations(String userId){
        String key = RedisKey.USER_FRIEND + userId;
        redisTemplate.opsForZSet().intersectAndStore(RedisKey.USER_FOLLOWING + userId, RedisKey.USER_FOLLOWER + userId, key);
        redisTemplate.expire(key,30, TimeUnit.SECONDS);
        Set<ZSetOperations.TypedTuple<Object>> set = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0,-1);
        List<String> userIds = set.stream().map(tuple -> tuple.getValue().toString()).toList();
//        String msg = String.join(",", userIds);
//        try {
//            session.getBasicRemote().sendText(msg);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        return userIds.stream().map(Long::valueOf).toList();

    }

    private void getFriends(Session session,String userId){
        List<Long> ids = sendRelations(userId);
        if (ids.isEmpty()) {
            return;
        }

//        List<Long> idLongs = ids.stream()
//                .filter(Strings::hasText)
//                .map(Long::valueOf)
//                .toList();
//
//        if (idLongs.isEmpty()) {
//            return;
//        }

        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().in(User::getId, ids)
        );

        Map<String, String> map = users.stream()
                .collect(Collectors.toMap(
                        u -> String.valueOf(u.getId()),
                        User::getUsername
                ));

         sendMsg(session,map);

    }

    private void sendRecentConversations(Session session,String userId){
        Set<ZSetOperations.TypedTuple<Object>> set = redisTemplate.opsForZSet().reverseRangeWithScores(
                RedisKey.IM_USER_CONVERSATION + String.valueOf(userId),
                0,
                -1
        );
        if(set == null || set.isEmpty()){
            return;
        }
        List<String> conversationIds = set.stream()
                .map(tuple -> tuple.getValue().toString())
                .toList();
        String msg = String.join(",", conversationIds);
        sendMsg(session,msg);
    }

    private void sendToMQ(){
        String key = RedisKey.IM_MQ_CACHE;
        String lockKey = RedisKey.IM_MQ_FLUSH_LOCK_SUFFIX;

        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                lockKey,
                "1",
                MQ_FLUSH_LOCK_TTL_SEC,
                TimeUnit.SECONDS
        );
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }

        try {
            Long sizeObj = redisTemplate.opsForList().size(key);
            long size = (sizeObj == null) ? 0L : sizeObj;
            if (size <= 0) {
                return;
            }

            long count = Math.min(size, MQ_BATCH_SIZE);
            List<Object> raw = (List<Object>) redisTemplate.execute(
                    POP_LIST_BATCH_SCRIPT,
                    Collections.singletonList(key),
                    String.valueOf(count)
            );
            if (raw == null || raw.isEmpty()) {
                return;
            }

            List<Message> batch = raw.stream()
                    .map(obj -> JSON.parseObject(obj.toString(), Message.class))
                    .toList();

            try {
                rabbitTemplate.convertAndSend("exchange.topic", "message", batch);
            } catch (Exception e) {
                // 发送失败，按原顺序写回同一 key（保持队列顺序）
                for (int i = raw.size() - 1; i >= 0; i--) {
                    redisTemplate.opsForList().leftPush(key, raw.get(i));
                }
            }
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    @Async("imTaskExecutor")
    public void asyncHandleChatMessage(JSONObject jsonObject, String userId, ConcurrentHashMap<String, Session> onlineUsers) {
        int type = jsonObject.getInteger("type");
        String image = jsonObject.getString("image");
        Long id = Long.valueOf(jsonObject.getString("id"));
        String message = jsonObject.getString("message");
        if((image==null|| image.isEmpty())&& (message==null|| message.isEmpty())){
            return;
        }
        Long now = System.currentTimeMillis();
        Long conversationId = getConversationId(userId,id,now,type);
        if(conversationId == null){
            Session session = onlineUsers.get(userId);
            sendMsg(session,"not friends or not group member");
            return;
        }

        executeByConversationId(conversationId,()->handleChatMessage(jsonObject, userId, onlineUsers,conversationId,now));
    }

    @Override
    @Async("imTaskExecutor")
    public void asyncSendToMQ() {
        sendToMQ();
    }
}
