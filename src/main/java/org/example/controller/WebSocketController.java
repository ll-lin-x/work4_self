package org.example.controller;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import org.example.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws")
@Component
public class WebSocketController {

    private static WebSocketService webSocketService;

    @Autowired
    public void setWebSocketService(WebSocketService webSocketService) {
        WebSocketController.webSocketService = webSocketService;
    }



    public static ConcurrentHashMap<String,Session> onlineUsers = new ConcurrentHashMap<>();
    private String userId;

    @OnOpen
    public void onOpen(Session session) {
       this.userId =  webSocketService.onOpen(session,onlineUsers);
    }

    @OnClose
    public void onClose(Session session) {
        webSocketService.onClose(this.userId,onlineUsers);
    }


    @OnMessage
    public void onMessage(String message) {
        webSocketService.onMessage(message,this.userId,onlineUsers);
    }

}