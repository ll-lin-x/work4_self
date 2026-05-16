package org.example.utils;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SensitiveWordFilter {
    private static final String IS_END = "isEnd";
    private  volatile Map<Object,Object> nodes = new HashMap<>();


    public void addNode(List<String> words) {
        Map<Object,Object> newNodes = new HashMap<>();
        for (String word : words) {
            Map<Object, Object> currentNode = newNodes;
            for (char c : word.toCharArray()) {
                currentNode = (Map<Object, Object>) currentNode.computeIfAbsent(c, k -> new HashMap<>());
            }
            currentNode.put(IS_END, true);
        }
        this.nodes = newNodes;
    }


    public String filter(String text){
        if(text == null || text.trim().isEmpty()||nodes.isEmpty()){
            return text;
        }
        StringBuilder result = new StringBuilder(text);
        int len = text.length();
        for (int i = 0; i < len; i++) {
            Map<Object,Object> currentMap = nodes;
            int matchLength = 0;// 当前匹配的长度
            boolean isEnd = false;// 是否发现敏感词结尾
            for (int j = i; j < len; j++) {
                char c = text.charAt(j);
                currentMap = (Map<Object, Object>) currentMap.get(c);
                if(currentMap == null){
                    // 匹配失败
                    break;
                }
                matchLength++;
                if(currentMap.containsKey(IS_END)){
                    isEnd = true;
                }
            }
            if(isEnd){
                for (int k = i; k < i+matchLength; k++) {
                    result.setCharAt(k, '*');
                }
                i = i +matchLength - 1;
            }
        }
        return result.toString();
    }
}
