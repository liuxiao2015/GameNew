// MongoDB 初始化脚本
// 创建游戏数据库和用户

// 切换到 admin 数据库
db = db.getSiblingDB('admin');

// 创建游戏数据库用户
db = db.getSiblingDB('game_server');
db.createUser({
    user: 'game_user',
    pwd: 'game_user_123',
    roles: [
        { role: 'readWrite', db: 'game_server' }
    ]
});

// 创建集合和索引

// 玩家数据集合
db.createCollection('player_data');
db.player_data.createIndex({ "roleId": 1 }, { unique: true });
db.player_data.createIndex({ "accountId": 1 });
db.player_data.createIndex({ "roleName": 1 });
db.player_data.createIndex({ "level": -1 });
db.player_data.createIndex({ "guildId": 1 });
db.player_data.createIndex({ "createTime": 1 });

// 公会数据集合
db.createCollection('guild_data');
db.guild_data.createIndex({ "guildId": 1 }, { unique: true });
db.guild_data.createIndex({ "guildName": 1 }, { unique: true });
db.guild_data.createIndex({ "leaderId": 1 });
db.guild_data.createIndex({ "level": -1 });
db.guild_data.createIndex({ "createTime": 1 });

// 邮件集合
db.createCollection('mail');
db.mail.createIndex({ "mailId": 1 }, { unique: true });
db.mail.createIndex({ "roleId": 1, "createTime": -1 });
db.mail.createIndex({ "expireTime": 1 }, { expireAfterSeconds: 0 });

// 聊天记录集合
db.createCollection('chat_log');
db.chat_log.createIndex({ "msgId": 1 }, { unique: true });
db.chat_log.createIndex({ "channel": 1, "sendTime": -1 });
db.chat_log.createIndex({ "senderId": 1, "sendTime": -1 });
db.chat_log.createIndex({ "sendTime": 1 }, { expireAfterSeconds: 604800 }); // 7 天过期

// 操作日志集合
db.createCollection('operation_log');
db.operation_log.createIndex({ "logId": 1 }, { unique: true });
db.operation_log.createIndex({ "roleId": 1, "logTime": -1 });
db.operation_log.createIndex({ "logType": 1, "logTime": -1 });
db.operation_log.createIndex({ "logTime": 1 }, { expireAfterSeconds: 2592000 }); // 30 天过期

// GM 操作日志集合
db.createCollection('gm_log');
db.gm_log.createIndex({ "logId": 1 }, { unique: true });
db.gm_log.createIndex({ "gmAccount": 1, "logTime": -1 });
db.gm_log.createIndex({ "targetRoleId": 1, "logTime": -1 });
db.gm_log.createIndex({ "logTime": 1 }, { expireAfterSeconds: 7776000 }); // 90 天过期

print('MongoDB 初始化完成!');
