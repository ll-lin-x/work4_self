package org.example.model.normal;


public class RedisKey {
    // 用户登录
    public static final String USER_LOGIN = "user:login:";
    // 视频详情
    public static final String VIDEO_DETAIL = "video:detail:";
    // 热门排行
    public static final String VIDEO_RANK_TOTAL = "video:rank:total:";
    // 播放量增量
    public static final String VIDEO_RANK_INCREMENT = "video:rank:increment:";
    // 视频点赞
    public static final String USER_LIKE = "user:like:";
    // 用户收件箱
    public static final String USER_FEED = "user:feed:";
    // 搜索历史
    public static final String VIDEO_SEARCH_HISTORY = "video:searchHistory:";
    // 关注列表
    public static final String USER_FOLLOWING =  "user:following:";
    // 粉丝列表
    public static final String USER_FOLLOWER = "user:follower:";
    // 朋友列表
    public static final String USER_FRIEND = "user:friend:";
    // 分布式锁
    public static final String LOCK_RANK_INIT = "lock:video:rank:init:";
    public static final String LOCK_VIDEO_RANK = "lock:video:rank:";

    // IM相关
    // 记录每个会话的最大消息数 可以使用String
    public static final String IM_SEQ = "im:seq:";
    // 消息暂存 使用ZSet
    public static final  String IM_MSG_CACHE = "im:msg:cache:";
    // 消息未读数 使用Hash
    public static final String IM_UNREAD = "im:unread:";
    // 在线用户 使用String
//    public static final String IM_USER_ONLINE = "im:user:online:";
    // 会话成员表 可以使用Set
    public static final String IM_CONVERSATION_MEMBER = "im:conversation:member:";
    // 单聊key映射conversationId 使用hash
    public static final String IM_KEY_TO_ID = "im:key:toid:";
    // 用户会话ZSet
    public static final String IM_USER_CONVERSATION = "im:user:conversation:";
    // 用户屏蔽Set
    public static final String IM_USER_SHIELD = "im:user:shield:";

    // 敏感词  使用list
    public static final String IM_SENSITIVEWORD = "im:sensitive:word:";
    // mq最近更新时间
    public static final String IM_MSG_LAST_FLUSH = "im:msg:lastFlush:";
    // 需要转发的消息列表
    public static final String IM_MQ_CACHE = "im:mq:cache:";

    public static final String IM_MQ_FLUSH_LOCK_SUFFIX = "im:mq:lock:";

}
