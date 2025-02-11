package com.csfrez.demospringboot.jsqlparser.enums;

public enum UserTypeEnum {

    ADMIN("管理员"),
    USER("普通用户"),
    GUEST("游客"),
    VIP("VIP用户"),
    MODERATOR("版主");

    private final String description;

    UserTypeEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
