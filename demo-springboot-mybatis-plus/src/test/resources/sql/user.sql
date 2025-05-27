CREATE TABLE `user`
(
    `id`          INT(11) NOT NULL AUTO_INCREMENT,
    `nickname`    VARCHAR(255) NOT NULL COMMENT '昵称' COLLATE 'utf8mb4_general_ci',
    `sex`         TINYINT(1) NOT NULL COMMENT '性别，1男2女',
    `create_time` DATETIME     NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    `version`     INT(11) NULL DEFAULT NULL COMMENT '版本',
    `is_delete`   TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除 1是，0否',
    PRIMARY KEY (`id`) USING BTREE
) COLLATE='utf8mb4_general_ci'
ENGINE=InnoDB
AUTO_INCREMENT=1
;