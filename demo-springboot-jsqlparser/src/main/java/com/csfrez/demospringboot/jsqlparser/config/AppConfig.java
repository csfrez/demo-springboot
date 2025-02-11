package com.csfrez.demospringboot.jsqlparser.config;

import com.csfrez.demospringboot.jsqlparser.enums.UserTypeEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author
 * @date 2025/2/11 15:37
 * @email
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private UserType userType;
    private Database database;
    private Server server;

    @Data
    public static class UserType {
        private UserTypeEnum admin;
        private UserTypeEnum user;
        private UserTypeEnum guest;
        private UserTypeEnum vip;
        private UserTypeEnum moderator;
    }

    @Data
    public static class Database {
        private String url;
        private String username;
        private String password;
    }

    @Data
    public static class Server {
        private int port;
    }
}
