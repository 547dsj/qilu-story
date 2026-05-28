-- 1. 用户表
CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE INDEX idx_username ON `user`(`username`);

-- 2. 故事表
CREATE TABLE `story` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '故事ID',
    `title` VARCHAR(100) NOT NULL COMMENT '故事标题',
    `opening` TEXT NOT NULL COMMENT '故事开头',
    `author_id` BIGINT NOT NULL COMMENT '作者ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT `fk_story_author` FOREIGN KEY (`author_id`) REFERENCES `user`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='故事主表';

CREATE INDEX idx_author_id ON `story`(`author_id`);
CREATE INDEX idx_create_time ON `story`(`create_time`);

-- 3. 故事节点表
CREATE TABLE `story_node` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '节点ID',
    `story_id` BIGINT NOT NULL COMMENT '所属故事ID',
    `parent_node_id` BIGINT DEFAULT NULL COMMENT '父节点ID（根节点为NULL）',
    `content` TEXT NOT NULL COMMENT '节点内容',
    `option_a_label` VARCHAR(50) DEFAULT NULL COMMENT '选项A的文字',
    `option_b_label` VARCHAR(50) DEFAULT NULL COMMENT '选项B的文字',
    `option_a_next_id` BIGINT DEFAULT NULL COMMENT '选项A跳转的下一节点ID',
    `option_b_next_id` BIGINT DEFAULT NULL COMMENT '选项B跳转的下一节点ID',
    `is_ending` TINYINT DEFAULT 0 COMMENT '是否为结局（0-未完结，1-结局）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT `fk_story_node_story` FOREIGN KEY (`story_id`) REFERENCES `story`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_story_node_parent` FOREIGN KEY (`parent_node_id`) REFERENCES `story_node`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_story_node_option_a` FOREIGN KEY (`option_a_next_id`) REFERENCES `story_node`(`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_story_node_option_b` FOREIGN KEY (`option_b_next_id`) REFERENCES `story_node`(`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='故事分支节点表';

CREATE INDEX idx_story_id ON `story_node`(`story_id`);
CREATE INDEX idx_parent_node_id ON `story_node`(`parent_node_id`);
CREATE INDEX idx_option_a_next_id ON `story_node`(`option_a_next_id`);
CREATE INDEX idx_option_b_next_id ON `story_node`(`option_b_next_id`);
CREATE INDEX idx_is_ending ON `story_node`(`is_ending`);
CREATE INDEX idx_create_time ON `story_node`(`create_time`);