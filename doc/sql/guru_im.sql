-- 用户表
DROP TABLE IF EXISTS `im_user`;  
CREATE TABLE `im_user` (
                        `uid` bigint NOT NULL COMMENT '用户ID',
                        `user_name` varchar(50) NOT NULL COMMENT '用户名',
                        `password` varchar(100) NOT NULL COMMENT '密码',
                        `phone` varchar(100) DEFAULT NULL COMMENT '手机',
                        `avatar` varchar(150) DEFAULT NULL COMMENT '头像',
                        `enabled_flag` tinyint(1) DEFAULT 1 COMMENT '是否可用',
                        PRIMARY KEY (`uid`),
                        UNIQUE KEY `idx_user_name` (`user_name`) COMMENT '用户名唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 好友关系表
DROP TABLE IF EXISTS `im_friend_relation`;  
CREATE TABLE `im_friend_relation` (
                                   `id` bigint NOT NULL COMMENT '主键ID',
                                   `uid` bigint NOT NULL COMMENT '用户ID',
                                   `friend_id` bigint NOT NULL COMMENT '好友ID',
                                   `user_name` varchar(50) DEFAULT NULL COMMENT '用户名称(冗余)',
                                   `friend_name` varchar(50) DEFAULT NULL COMMENT '好友名称(冗余)',
                                   `relation_status` tinyint NOT NULL COMMENT '状态：1:正常 2:拉黑',
                                   `create_time` bigint NOT NULL COMMENT '创建时间(时间戳)',
                                   `update_time` bigint NOT NULL COMMENT '更新时间(时间戳)',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uniq_uid_friend` (`uid`,`friend_id`) COMMENT '防止重复添加',
                                   KEY `idx_uid` (`uid`) COMMENT '用户ID索引',
                                   KEY `idx_friend_id` (`friend_id`) COMMENT '好友ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';


-- 好友请求表
DROP TABLE IF EXISTS `im_friend_request`;
CREATE TABLE `im_friend_request` (
                                   `id` bigint NOT NULL COMMENT '主键ID',
                                   `global_seq` bigint DEFAULT 0 COMMENT '全局序列号(严格递增)',
                                   `requester_id` bigint NOT NULL COMMENT '请求方ID',
                                   `requester_name` VARCHAR(120) COMMENT '请求方姓名',
                                   `responder_id` bigint NOT NULL COMMENT '响应方ID',
                                   `responder_name` VARCHAR(120) COMMENT '响应方姓名',
                                   `request_status` tinyint NOT NULL COMMENT '请求状态，0:待处理 1:已同意 2:已拒绝',
                                   `request_msg` VARCHAR(500) COMMENT '请求附加消息',
                                   `request_type` tinyint NOT NULL COMMENT '请求类型，1:好友申请，2:拉黑好友，3:删除好友 4:解除拉黑',
                                   `notify_status` tinyint NOT NULL COMMENT '通知状态，0:待通知 1:已通知',
                                   `create_time` bigint NOT NULL COMMENT '创建时间(时间戳)',
                                   `update_time` bigint NOT NULL COMMENT '更新时间(时间戳)',
                                   PRIMARY KEY (`id`),
                                   KEY `uniq_requester_receiver` (`requester_id`,`responder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友请求表';


-- 文件信息表
DROP TABLE IF EXISTS `im_file_info`;
CREATE TABLE `im_file_info` (
                             `id` bigint NOT NULL COMMENT '文件ID，雪花算法生成',
                             `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
                             `storage_name` varchar(255) NOT NULL COMMENT '存储的文件名',
                             `size` bigint NOT NULL COMMENT '文件大小(字节)',
                             `md5` varchar(32) NOT NULL COMMENT '文件MD5值',
                             `content_type` varchar(100) NOT NULL COMMENT '文件类型',
                             `path` varchar(500) NOT NULL COMMENT '文件存储路径',
                             `user_id` bigint NOT NULL COMMENT '上传用户ID',
                             `status` tinyint NOT NULL DEFAULT '0' COMMENT '上传状态(0:上传中,1:上传完成,2:上传失败)',
                             `create_time` bigint NOT NULL COMMENT '创建时间(时间戳)',
                             `update_time` bigint NOT NULL COMMENT '更新时间(时间戳)',
                             PRIMARY KEY (`id`),
                             KEY `idx_md5` (`md5`),
                             KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件信息表';

-- 文件分片信息表
DROP TABLE IF EXISTS `im_file_part_info`;
CREATE TABLE `im_file_part_info` (
                                  `id` bigint NOT NULL COMMENT '分片ID，雪花算法生成',
                                  `file_id` bigint NOT NULL COMMENT '文件ID',
                                  `part_number` int NOT NULL COMMENT '分片序号',
                                  `part_size` bigint NOT NULL COMMENT '分片大小',
                                  `part_md5` varchar(32) DEFAULT NULL COMMENT '分片MD5',
                                  `status` tinyint NOT NULL DEFAULT '0' COMMENT '分片上传状态(0:未上传,1:已上传)',
                                  `create_time` bigint NOT NULL COMMENT '创建时间(时间戳)',
                                  `update_time` bigint NOT NULL COMMENT '更新时间(时间戳)',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_file_id` (`file_id`),
                                  KEY `idx_file_id_part_number` (`file_id`, `part_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件分片信息表';


-- 消息表
DROP TABLE IF EXISTS `im_message`;
CREATE TABLE `im_message` (
                              `id` bigint NOT NULL COMMENT '消息ID(雪花ID)',
                              `client_msg_id` varchar(50) DEFAULT NULL COMMENT '客户端消息ID',
                              `conversation_type` tinyint NOT NULL COMMENT '会话类型(1:私聊 2:群聊 3:系统)',
                              `conversation_id` bigint NOT NULL COMMENT '会话ID',
                              `sender_id` bigint NOT NULL COMMENT '发送者ID',
                              `receiver_id` bigint NOT NULL COMMENT '接收者ID',
                              `message_type` tinyint NOT NULL COMMENT '消息类型',
                              `message_content` text NOT NULL COMMENT '消息内容(JSON格式)',
                              `server_seq` bigint DEFAULT 0 COMMENT '服务端序列号',
                              `client_seq` bigint DEFAULT 0 COMMENT '客户端序列号',
                              `client_send_time` bigint DEFAULT 0 COMMENT '客户端发送时间',
                              `at_users` varchar(500) DEFAULT NULL COMMENT '@的用户列表(JSON数组)',
                              `is_recalled` tinyint DEFAULT 0 COMMENT '是否已撤回(0:否 1:是)',
                              `recall_time` bigint DEFAULT NULL COMMENT '撤回时间',
                              `read_count` int DEFAULT 0 COMMENT '已读人数',
                              `delivery_status` tinyint DEFAULT 0 COMMENT '推送状态: 0:未推送 1:已推送，待送达 2:推送失败 3:已送达',
                              `delivery_time` bigint DEFAULT NULL COMMENT '送达时间',
                              `retry_count` int DEFAULT 0 COMMENT '推送重试次数',
                              `last_retry_time` bigint DEFAULT NULL COMMENT '最后重试时间',
                              `status` tinyint DEFAULT 1 COMMENT '消息状态(0:删除 1:正常)',
                              `create_time` bigint NOT NULL COMMENT '创建时间(时间戳)',
                              `update_time` bigint NOT NULL COMMENT '更新时间(时间戳)',
                              PRIMARY KEY (`id`),
                              KEY `idx_conversation` (`conversation_type`, `conversation_id`),
                              KEY `idx_conversation_id` (`conversation_id`),
                              KEY `idx_conv_type_id_sender` (`conversation_type`, `conversation_id`, `sender_id`, `server_seq`),
                              KEY `idx_sender_id` (`sender_id`),
                              KEY `idx_receiver_id` (`receiver_id`),
                              KEY `idx_server_seq` (`server_seq`),
                              KEY `idx_create_time` (`create_time`),
                              UNIQUE INDEX `uniq_client_msg_sender` (`client_msg_id`, `sender_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 会话表（存储会话的全局信息）
DROP TABLE IF EXISTS `im_conversation`;
CREATE TABLE `im_conversation` (
                                   `id` bigint NOT NULL COMMENT '会话ID(雪花ID)',
                                   `conversation_type` tinyint NOT NULL COMMENT '会话类型(1:私聊 2:群聊 3:系统)',
                                   `conversation_key` varchar(100) NOT NULL COMMENT '会话唯一标识',
                                   `conversation_name` varchar(100) DEFAULT NULL COMMENT '会话名称',
                                   `conversation_avatar` varchar(255) DEFAULT NULL COMMENT '会话头像',
                                   `last_message_id` bigint DEFAULT NULL COMMENT '最后一条消息ID',
                                   `last_message_time` bigint DEFAULT NULL COMMENT '最后消息时间',
                                   `last_message_content` varchar(500) DEFAULT NULL COMMENT '最后消息内容摘要',
                                   `last_message_sender` bigint DEFAULT NULL COMMENT '最后消息发送者',
                                   `last_message_seq` bigint DEFAULT NULL COMMENT '最后消息序列号',
                                   `status` tinyint DEFAULT 1 COMMENT '会话状态(0:删除 1:正常)',
                                   `version` bigint DEFAULT 1 COMMENT '版本号，用于乐观锁',
                                   `create_time` bigint NOT NULL COMMENT '创建时间(时间戳)',
                                   `update_time` bigint NOT NULL COMMENT '更新时间(时间戳)',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uniq_conversation_key` (`conversation_key`),
                                   KEY `idx_last_message_time` (`last_message_time`),
                                   KEY `idx_conversation_type` (`conversation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- 用户会话关系表（存储用户的个性化信息）
DROP TABLE IF EXISTS `im_user_conversation`;
CREATE TABLE `im_user_conversation` (
                                        `id` bigint NOT NULL COMMENT '关系ID(雪花ID)',
                                        `user_id` bigint NOT NULL COMMENT '用户ID',
                                        `conversation_id` bigint NOT NULL COMMENT '会话ID',
                                        `conversation_type` tinyint NOT NULL COMMENT '会话类型',
                                        `unread_count` int DEFAULT 0 COMMENT '未读消息数',
                                        `last_read_seq` bigint DEFAULT 0 COMMENT '最后读取的消息序列号',
                                        `is_top` tinyint DEFAULT 0 COMMENT '是否置顶(0:否 1:是)',
                                        `is_mute` tinyint DEFAULT 0 COMMENT '是否免打扰(0:否 1:是)',
                                        `show_nickname` varchar(50) DEFAULT NULL COMMENT '会话中显示的昵称',
                                        `status` tinyint DEFAULT 1 COMMENT '关系状态(0:删除 1:正常)',
                                        `create_time` bigint NOT NULL COMMENT '创建时间(时间戳)',
                                        `update_time` bigint NOT NULL COMMENT '更新时间(时间戳)',
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `uniq_user_conversation` (`user_id`, `conversation_id`),
                                        KEY `idx_user_id` (`user_id`),
                                        KEY `idx_conversation_id` (`conversation_id`),
                                        KEY `idx_user_unread` (`user_id`, `unread_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话关系表';

-- 会话已读指针表
DROP TABLE IF EXISTS `im_conversation_read`;
CREATE TABLE `im_conversation_read` (
                                        `id` bigint NOT NULL COMMENT '主键ID(雪花ID)',
                                        `user_id` bigint NOT NULL COMMENT '用户ID',
                                        `conversation_type` tinyint NOT NULL COMMENT '会话类型(1:私聊 2:群聊 3:系统)',
                                        `conversation_id` bigint NOT NULL COMMENT '会话ID',
                                        `global_seq` bigint DEFAULT 0 COMMENT '全局序列号',
                                        `last_read_seq` bigint DEFAULT 0 COMMENT '用户在此会话中已读的最后一条消息的服务端序列号',
                                        `delivery_status` tinyint DEFAULT 0 COMMENT '推送状态: 0:未推送 1:已推送，待送达 2:推送失败 3:已送达',
                                        `delivery_time` bigint DEFAULT NULL COMMENT '送达时间',
                                        `version` int DEFAULT 1 COMMENT '版本号，用于乐观锁',
                                        `create_time` bigint NOT NULL COMMENT '创建时间(时间戳)',
                                        `update_time` bigint NOT NULL COMMENT '更新时间(时间戳)',
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `uniq_user_conversation` (`user_id`, `conversation_type`, `conversation_id`),
                                        KEY `idx_user_id` (`user_id`),
                                        KEY `idx_conversation` (`conversation_type`, `conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话已读指针表';


-- 群组表
DROP TABLE IF EXISTS `im_group`;
CREATE TABLE `im_group` (
                            `id` bigint NOT NULL COMMENT '群组ID(雪花ID)',
                            `group_name` varchar(100) NOT NULL COMMENT '群组名称',
                            `group_avatar` varchar(255) DEFAULT NULL COMMENT '群组头像',
                            `group_owner` bigint NOT NULL COMMENT '群主ID',
                            `group_intro` varchar(500) DEFAULT NULL COMMENT '群组简介',
                            `group_notice` varchar(500) DEFAULT NULL COMMENT '群公告',
                            `max_members` int DEFAULT 500 COMMENT '最大成员数',
                            `current_members` int DEFAULT 0 COMMENT '当前成员数',
                            `group_status` tinyint DEFAULT 1 COMMENT '群组状态(0:解散 1:正常)',
                            `create_time` bigint NOT NULL COMMENT '创建时间(时间戳)',
                            `update_time` bigint NOT NULL COMMENT '更新时间(时间戳)',
                            PRIMARY KEY (`id`),
                            KEY `idx_group_owner` (`group_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组表';

-- 群成员表
DROP TABLE IF EXISTS `im_group_member`;
CREATE TABLE `im_group_member` (
                                   `id` bigint NOT NULL COMMENT '成员关系ID(雪花ID)',
                                   `group_id` bigint NOT NULL COMMENT '群组ID',
                                   `user_id` bigint NOT NULL COMMENT '用户ID',
                                   `member_role` tinyint DEFAULT 0 COMMENT '成员角色(0:普通成员 1:管理员 2:群主)',
                                   `join_time` bigint NOT NULL COMMENT '加入时间',
                                   `last_ack_seq` bigint DEFAULT 0 COMMENT '最后确认的消息序列号',
                                   `member_status` tinyint DEFAULT 1 COMMENT '成员状态(0:已退出 1:正常)',
                                   `create_time` bigint NOT NULL COMMENT '创建时间(时间戳)',
                                   `update_time` bigint NOT NULL COMMENT '更新时间(时间戳)',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uniq_group_user` (`group_id`, `user_id`),
                                   KEY `idx_group_id` (`group_id`),
                                   KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群成员表';

-- 群聊邀请通知主表
DROP TABLE IF EXISTS `im_group_invite_notify`;
CREATE TABLE `im_group_invite_notify` (
                                       `id` bigint NOT NULL COMMENT '雪花ID主键',
                                       `global_seq` bigint DEFAULT 0 COMMENT '全局序列号(严格递增)',
                                       `group_id` bigint NOT NULL COMMENT '群组ID',
                                       `inviter_id` bigint NOT NULL COMMENT '邀请人用户ID',
                                       `invite_reason` VARCHAR(500) DEFAULT '' COMMENT '邀请理由',
                                       `initial_members_json` JSON NOT NULL COMMENT '初始成员列表JSON格式',
                                       `delivery_status` tinyint DEFAULT 0 COMMENT '推送状态: 0:未推送 1:已推送，待送达 2:推送失败 3:已送达',
                                       `expire_time` bigint NOT NULL COMMENT '过期时间',
                                       `create_time` bigint NOT NULL COMMENT '创建时间(时间戳)',
                                       `update_time` bigint NOT NULL COMMENT '更新时间(时间戳)',
                                       PRIMARY KEY (`id`)
) COMMENT='群聊邀请通知主表';



-- 离线消息内容表
DROP TABLE IF EXISTS `im_offline_message_content`;
CREATE TABLE `im_offline_message_content` (
                                              `message_id` bigint NOT NULL COMMENT '消息ID(主键, 全局唯一)',
                                              `conversation_type` tinyint NOT NULL COMMENT '会话类型',
                                              `conversation_id` bigint NOT NULL COMMENT '会话ID',
                                              `message_seq` bigint NOT NULL COMMENT '消息序列号(会话级严格递增)',
                                              `sender_id` bigint NOT NULL COMMENT '发送者ID',
                                              `receiver_id` bigint NOT NULL COMMENT '接收者ID',
                                              `message_time` bigint NOT NULL COMMENT '消息时间',
                                              `message_type` tinyint NOT NULL COMMENT '消息类型',
                                              `message_content` mediumblob COMMENT '消息内容',
                                              `priority` tinyint DEFAULT 0 COMMENT '消息优先级',
                                              `expire_time` bigint DEFAULT NULL COMMENT '过期时间',
                                              `is_archived` tinyint DEFAULT 0 COMMENT '是否已归档',
                                              `archive_time` bigint DEFAULT NULL COMMENT '归档时间',
                                              `create_time` bigint NOT NULL COMMENT '创建时间',
                                              `update_time` bigint NOT NULL COMMENT '更新时间',
                                              PRIMARY KEY (`message_id`),
                                              KEY `idx_conversation_seq` (`conversation_type`, `conversation_id`, `message_seq`),
                                              KEY `idx_receiver_conversation_seq` (`receiver_id`, `conversation_type`, `conversation_id`, `message_seq`),
                                              KEY `idx_receiver_time` (`receiver_id`, `message_time`),
                                              KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='离线消息内容表';

-- 离线消息投递表
DROP TABLE IF EXISTS `im_offline_message_delivery`;
CREATE TABLE `im_offline_message_delivery` (
                                               `id` bigint NOT NULL COMMENT '记录ID',
                                               `message_id` bigint NOT NULL COMMENT '消息ID',
                                               `message_type` tinyint NOT NULL COMMENT '消息类型(1:聊天，2:好友请求)',
                                               `user_id` bigint NOT NULL COMMENT '用户ID',
                                               `device_id` varchar(100) NOT NULL COMMENT '设备ID',
                                               `delivery_status` tinyint DEFAULT 0 COMMENT '投递状态(0:未投递 1:已投递 2:投递失败)',
                                               `delivery_count` int DEFAULT 0 COMMENT '投递次数',
                                               `last_delivery_time` bigint DEFAULT NULL COMMENT '最后投递时间',
                                               `create_time` bigint NOT NULL COMMENT '创建时间',
                                               `update_time` bigint NOT NULL COMMENT '更新时间',
                                               PRIMARY KEY (`id`),
                                               UNIQUE KEY `uniq_message_user_device` (`message_id`, `user_id`, `device_id`), -- 唯一约束，防止重复插入
                                               KEY `idx_user_device_status` (`user_id`, `device_id`, `delivery_status`), -- 核心查询索引
                                               KEY `idx_message_id` (`message_id`),
                                               KEY `idx_status_createtime` (`delivery_status`, `create_time`),
                                               KEY `idx_message_status` (`message_id`, `delivery_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='离线消息投递状态表';

-- 同步会话表
DROP TABLE IF EXISTS `im_sync_session`;
CREATE TABLE `im_sync_session` (
                                   `id` bigint NOT NULL COMMENT '会话ID(雪花ID)',
                                   `sync_id` bigint NOT NULL COMMENT '同步会话ID',
                                   `user_id` bigint NOT NULL COMMENT '用户ID',
                                   `device_id` varchar(100) NOT NULL COMMENT '设备ID',
                                   `sync_type` tinyint NOT NULL COMMENT '同步类型(0:全量 1:增量 2:按会话 3:按优先级)',
                                   `sync_strategy` varchar(50) DEFAULT 'default' COMMENT '同步策略',
                                   `total_count` int DEFAULT 0 COMMENT '总消息数量',
                                   `synced_count` int DEFAULT 0 COMMENT '已同步数量',
                                   `current_batch` int DEFAULT 0 COMMENT '当前批次',
                                   `batch_size` int DEFAULT 100 COMMENT '批次大小',
                                   `sync_status` tinyint DEFAULT 0 COMMENT '同步状态(1:进行中 2:已推送MQ 3:已送达 4:推送MQ失败 5:送达失败)',
                                   `start_time` bigint NOT NULL COMMENT '开始时间',
                                   `end_time` bigint DEFAULT NULL COMMENT '结束时间',
                                   `last_activity_time` bigint DEFAULT NULL COMMENT '最后活动时间',
                                   `error_message` varchar(500) DEFAULT NULL COMMENT '错误信息',
                                   `sync_start_seq` bigint DEFAULT 0 COMMENT '同步起始消息序列号',
                                   `sync_end_seq` bigint DEFAULT 0 COMMENT '同步结束消息序列号(计划)',
                                   `last_synced_seq` bigint DEFAULT 0 COMMENT '最后已同步的消息序列号(用于断点续传)',
                                   `create_time` bigint NOT NULL COMMENT '创建时间(时间戳)',
                                   `update_time` bigint NOT NULL COMMENT '更新时间(时间戳)',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uniq_sync_id` (`sync_id`),
                                   KEY `idx_user_device` (`user_id`, `device_id`),
                                   KEY `idx_status_time` (`sync_status`, `start_time`),
                                   KEY `idx_last_activity` (`last_activity_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同步会话表';

-- 同步位点表
DROP TABLE IF EXISTS `im_sync_cursor`;
CREATE TABLE `im_sync_cursor` (
                                  `id` bigint NOT NULL COMMENT '记录ID(雪花ID)',
                                  `user_id` bigint NOT NULL COMMENT '用户ID',
                                  `device_id` varchar(100) NOT NULL COMMENT '设备ID',
                                  `conversation_type` tinyint NOT NULL COMMENT '会话类型',
                                  `conversation_id` bigint NOT NULL COMMENT '会话ID(会话的唯一id)',
                                  `last_sync_seq` bigint DEFAULT 0 COMMENT '最后同步序列号',
                                  `last_sync_time` bigint NOT NULL COMMENT '最后同步时间',
                                  `sync_version` int DEFAULT 1 COMMENT '同步版本',
                                  `create_time` bigint NOT NULL COMMENT '创建时间(时间戳)',
                                  `update_time` bigint NOT NULL COMMENT '更新时间(时间戳)',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uniq_user_device_conv` (`user_id`, `device_id`, `conversation_type`, `conversation_id`),
                                  KEY `idx_user_device` (`user_id`, `device_id`),
                                  KEY `idx_update_time` (`update_time`),
                                  KEY `idx_user_updatetime` (`user_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同步位点表';


-- 离线事件内容表
DROP TABLE IF EXISTS `im_offline_events_content`;
CREATE TABLE `im_offline_events_content` (
                                   `id` bigint NOT NULL COMMENT '主键ID',
                                   `user_id` bigint NOT NULL COMMENT '用户ID',
                                   `global_seq` bigint NOT NULL COMMENT '事件的全局唯一递增序列号',
                                   `event_type` INT NOT NULL COMMENT '事件类型',
                                   `event_content` BLOB NOT NULL COMMENT '事件内容（Protobuf序列化后的二进制数据）',
                                   `create_time` bigint NOT NULL COMMENT '事件创建时间',
                                   `update_time` bigint NOT NULL COMMENT '事件更新时间',
                                   `expire_time` bigint NULL COMMENT '事件过期时间',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_user_sequence` (`user_id`, `global_seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户离线事件表';

-- 离线事件投递表
DROP TABLE IF EXISTS `im_offline_events_delivery`;
CREATE TABLE `im_offline_events_delivery` (
                                          `id` bigint NOT NULL COMMENT '主键ID',
                                          `user_id` bigint NOT NULL COMMENT '用户ID',
                                          `device_id` VARCHAR(128) NOT NULL COMMENT '设备ID',
                                          `last_sync_seq` bigint DEFAULT 0 COMMENT '该设备上次同步到的最新事件序列号',
                                          `last_sync_time` bigint COMMENT '上次同步时间',
                                          `delivery_status` tinyint DEFAULT NULL COMMENT '投递状态(1:未投递 2:已推送MQ 3:已送达 4:推送MQ失败 5:送达失败)',
                                          `delivery_count` int DEFAULT 0 COMMENT '投递次数',
                                          `last_delivery_time` bigint DEFAULT NULL COMMENT '最后投递时间',
                                          `create_time` bigint NOT NULL COMMENT '创建时间(时间戳)',
                                          `update_time` bigint NOT NULL COMMENT '更新时间(时间戳)',
                                          PRIMARY KEY (`id`),
                                          UNIQUE KEY `uk_user_device` (`user_id`, `device_id`),
                                          KEY `uk_user_device_status` (`user_id`, `device_id`, `delivery_status`),
                                          KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='离线事件投递表';



-- 通话会话表
DROP TABLE IF EXISTS `im_call_session`;
CREATE TABLE `im_call_session` (
                                   `id` bigint NOT NULL COMMENT '会话ID(雪花ID)',
                                   `session_type` tinyint NOT NULL COMMENT '会话类型(1:私聊通话 2:群组通话 3:会议)',
                                   `media_type` tinyint NOT NULL COMMENT '媒体类型(0:仅音频 1:视频 2:屏幕共享 3:群组视频)',
                                   `initiator_id` bigint NOT NULL COMMENT '发起者用户ID',
                                   `initiator_device` varchar(100) NOT NULL COMMENT '发起者设备ID',
                                   `group_id` bigint DEFAULT NULL COMMENT '群组ID(群通话时)',
                                   `conference_id` bigint DEFAULT NULL COMMENT '会议ID(会议时)',
                                   `conference_title` varchar(200) DEFAULT NULL COMMENT '会议标题',
                                   `call_subject` varchar(200) DEFAULT NULL COMMENT '通话主题',
                                   `call_state` tinyint NOT NULL COMMENT '通话状态(0:空闲 1:拨号中 2:振铃中 3:连接中 4:通话中 5:挂断中 6:已结束)',
                                   `max_participants` int DEFAULT 10 COMMENT '最大参与者数',
                                   `timeout_seconds` int DEFAULT 30 COMMENT '呼叫超时时间(秒)',
                                   `start_time` bigint DEFAULT NULL COMMENT '通话开始时间',
                                   `end_time` bigint DEFAULT NULL COMMENT '通话结束时间',
                                   `duration` int DEFAULT 0 COMMENT '通话时长(秒)',
                                   `hangup_reason` varchar(100) DEFAULT NULL COMMENT '挂断原因',
                                   `hangup_type` tinyint DEFAULT NULL COMMENT '挂断类型',
                                   `hangup_initiator` bigint DEFAULT NULL COMMENT '挂断发起者',
                                   `create_time` bigint NOT NULL COMMENT '创建时间',
                                   `update_time` bigint NOT NULL COMMENT '更新时间',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_initiator` (`initiator_id`),
                                   KEY `idx_group_id` (`group_id`),
                                   KEY `idx_state_time` (`call_state`, `create_time`),
                                   KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通话会话表';

-- 通话参与者表
DROP TABLE IF EXISTS `im_call_participant`;
CREATE TABLE `im_call_participant` (
                                       `id` bigint NOT NULL COMMENT '记录ID(雪花ID)',
                                       `session_id` bigint NOT NULL COMMENT '会话ID',
                                       `user_id` bigint NOT NULL COMMENT '用户ID',
                                       `device_id` varchar(100) NOT NULL COMMENT '设备ID',
                                       `participant_state` tinyint NOT NULL COMMENT '参与者状态(0:已邀请 1:振铃中 2:已加入 3:已拒绝 4:已离开 5:超时 6:被踢出 7:重连中)',
                                       `is_inviter` tinyint DEFAULT 0 COMMENT '是否是邀请者',
                                       `join_time` bigint DEFAULT NULL COMMENT '加入时间',
                                       `leave_time` bigint DEFAULT NULL COMMENT '离开时间',
                                       `duration` int DEFAULT 0 COMMENT '参与时长(秒)',
                                       `media_state` varchar(500) DEFAULT NULL COMMENT '媒体状态(JSON格式)',
                                       `selected_device` varchar(100) DEFAULT NULL COMMENT '服务器选择的设备ID',
                                       `create_time` bigint NOT NULL COMMENT '创建时间',
                                       `update_time` bigint NOT NULL COMMENT '更新时间',
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uniq_session_user_device` (`session_id`, `user_id`, `device_id`),
                                       KEY `idx_session_id` (`session_id`),
                                       KEY `idx_user_id` (`user_id`),
                                       KEY `idx_state_time` (`participant_state`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通话参与者表';

-- 媒体房间映射表
DROP TABLE IF EXISTS `im_media_room`;
CREATE TABLE `im_media_room` (
                                 `id` bigint NOT NULL COMMENT '记录ID(雪花ID)',
                                 `session_id` bigint NOT NULL COMMENT '通话会话ID',
                                 `room_id` varchar(100) NOT NULL COMMENT 'mediasoup房间ID',
                                 `worker_pid` int DEFAULT NULL COMMENT 'mediasoup worker进程ID',
                                 `router_id` varchar(100) DEFAULT NULL COMMENT 'mediasoup router ID',
                                 `active_peers` int DEFAULT 0 COMMENT '活跃对等端数量',
                                 `room_status` tinyint DEFAULT 1 COMMENT '房间状态(0:关闭 1:活跃)',
                                 `create_time` bigint NOT NULL COMMENT '创建时间',
                                 `update_time` bigint NOT NULL COMMENT '更新时间',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uniq_session_id` (`session_id`),
                                 UNIQUE KEY `uniq_room_id` (`room_id`),
                                 KEY `idx_status_time` (`room_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='媒体房间映射表';


