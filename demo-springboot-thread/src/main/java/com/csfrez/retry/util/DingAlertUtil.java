package com.csfrez.retry.util;

import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * 钉钉告警工具类
 *
 */
@Slf4j
public class DingAlertUtil {

    /**
     *
     * @param throwable   the throwable
     * @param alertPrompt 告警提示
     */
    public static void robotAlert(Throwable throwable, String alertPrompt) {
        try {
            // 告警内容拼接
            String content = alertPrompt;
            if (Objects.nonNull(throwable)) {
                content += ":" + throwable;
            }
            String bodyJson = "{\"msgtype\": \"text\",\"text\": {\"content\":\"" + content +"\"}}";
            String url = "https://oapi.dingtalk.com/robot/send?access_token=";;
            HttpUtil.post(url, bodyJson);
        } catch (Exception e) {
            log.error("告警信息发送失败: ", e);
        }
    }

    public static void robotAlert(String alertPrompt) {
        robotAlert(null, alertPrompt);
    }

}
