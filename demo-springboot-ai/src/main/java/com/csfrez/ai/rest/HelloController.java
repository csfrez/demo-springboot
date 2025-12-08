package com.csfrez.ai.rest;

import io.github.lnyocly.ai4j.listener.SseListener;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatCompletion;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatCompletionResponse;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage;
import io.github.lnyocly.ai4j.service.IChatService;
import io.github.lnyocly.ai4j.service.PlatformType;
import io.github.lnyocly.ai4j.service.factor.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author
 * @date 2025/12/8 15:20
 * @email
 */
@RestController
@Slf4j
public class HelloController {

    // 注入Ai服务
    @Autowired
    private AiService aiService;

    @RequestMapping("/hello")
    public String hello() {
        return "hello world " + System.currentTimeMillis();
    }

    @RequestMapping("/chat")
    public String chat() throws Exception {
        // 获取chat服务实例
        IChatService chatService = aiService.getChatService(PlatformType.DEEPSEEK);

        // 构建请求参数
        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model("deepseek-chat")
                .message(ChatMessage.withUser("鲁迅为什么打周树人"))
                .build();

        // 发送对话请求
        ChatCompletionResponse response = chatService.chatCompletion(chatCompletion);
        log.info("response: {}", response);
        return response.getObject();
    }

    @RequestMapping("/chat/stream")
    public void chatStream() throws Exception {
        // 获取chat服务实例
        IChatService chatService = aiService.getChatService(PlatformType.DEEPSEEK);

        // 构造请求参数
        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model("deepseek-chat")
                .functions("queryWeather")
                .message(ChatMessage.withUser("查询深圳明天的天气"))
                .build();


        // 构造监听器
        SseListener sseListener = new SseListener() {
            @Override
            protected void send() {
                log.info("currStr: {}", this.getCurrStr());
            }
        };
        // 显示函数参数，默认不显示
        sseListener.setShowToolArgs(true);

        // 发送SSE请求
        chatService.chatCompletionStream(chatCompletion, sseListener);
        log.info("output: {}", sseListener.getOutput());
    }
}
