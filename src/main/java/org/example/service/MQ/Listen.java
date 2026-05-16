package org.example.service.MQ;

import org.example.mapper.MessageMapper;
import org.example.model.pojo.Message;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Listen {

    @Autowired
    private MessageMapper messageMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "direct.queue.message",durable = "true"),
            exchange = @Exchange(name = "exchange.topic",type = ExchangeTypes.TOPIC),
            key = {"message"}
    ))
    public void listen(List<Message> message) {
        // 处理消息
//        System.out.println("Received message: " + message);
//        messageMapper.insert(message);
        messageMapper.insertList(message);
    }
}
