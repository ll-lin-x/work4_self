package org.example.service;


import com.alibaba.fastjson.JSONObject;
import jakarta.websocket.Session;

import java.util.concurrent.ConcurrentHashMap;

public interface WebSocketService {
    String onOpen(Session session, ConcurrentHashMap<String,Session> onlineUsers);

    void onMessage(String message,String userId,ConcurrentHashMap<String,Session> onlineUsers);
    void onClose(String id,ConcurrentHashMap<String,Session> onlineUsers);

    void asyncHandleChatMessage(JSONObject jsonObject, String userId, ConcurrentHashMap<String, Session> onlineUsers);

    void asyncSendToMQ();
}
