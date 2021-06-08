SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

CREATE TABLE IF NOT EXISTS `log`
(
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT,
    `user_id`       char(32)              DEFAULT NULL,
    `relation_id`   char(32)              DEFAULT NULL,
    `relation_type` varchar(20)           DEFAULT NULL,
    `resource`      varchar(200) NOT NULL,
    `create_date`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `remark`        text,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `org`
(
    `id`     char(32)    NOT NULL COMMENT 'ID',
    `pid`    char(32) DEFAULT NULL,
    `name`   varchar(50) NOT NULL COMMENT '名称',
    `remark` text COMMENT '备注',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `org_user`
(
    `org_id`  char(32) NOT NULL,
    `user_id` char(32) NOT NULL,
    PRIMARY KEY (`org_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `role`
(
    `id`     char(32)    NOT NULL COMMENT 'ID',
    `name`   varchar(50) NOT NULL COMMENT '名称',
    `remark` text COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `role_user`
(
    `role_id` char(32) NOT NULL,
    `user_id` char(32) NOT NULL,
    PRIMARY KEY (`role_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `user`
(
    `id`          char(32)    NOT NULL COMMENT 'ID',
    `mobile`      varchar(20) NOT NULL COMMENT '手机号',
    `nickname`    varchar(50)          DEFAULT NULL COMMENT '昵称',
    `password`    varchar(50)          DEFAULT NULL COMMENT '登录密码',
    `create_date` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `remark`      text COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `mobile` (`mobile`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户';

CREATE TABLE IF NOT EXISTS `user_openid`
(
    `openid`  varchar(100) NOT NULL,
    `user_id` char(32)     NOT NULL,
    PRIMARY KEY (`openid`),
    KEY `user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;
