package com.game.service.guild.impl;

import com.game.actor.core.ActorMessage;
import com.game.actor.core.ActorSystem;
import com.game.api.guild.GuildDTO;
import com.game.entity.guild.GuildData;
import com.game.service.guild.actor.GuildActor;
import com.game.service.guild.actor.GuildActor.MessageTypes;
import com.game.api.guild.GuildMemberDTO;
import com.game.api.guild.GuildService;
import com.game.common.enums.ErrorCode;
import com.game.common.result.Result;
import com.game.common.util.IdGenerator;
import com.game.common.util.StringUtil;
import com.game.data.mongo.MongoService;
import com.game.data.redis.RedisService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 公会服务实现
 *
 * @author GameServer
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class GuildServiceImpl implements GuildService {

    private final RedisService redisService;
    private final MongoService mongoService;

    @Value("${game.actor.guild.max-size:1000}")
    private int maxSize;

    @Value("${game.actor.guild.idle-timeout-minutes:60}")
    private int idleTimeoutMinutes;

    @Value("${game.actor.guild.save-interval-seconds:600}")
    private int saveIntervalSeconds;

    @Value("${game.guild.create-cost:500}")
    private int createCost;

    @Value("${game.guild.name-min-length:2}")
    private int nameMinLength;

    @Value("${game.guild.name-max-length:12}")
    private int nameMaxLength;

    @Value("${game.guild.init-max-member:30}")
    private int initMaxMember;

    /**
     * 公会 Actor 系统
     */
    private ActorSystem<GuildActor> guildActorSystem;

    @PostConstruct
    public void init() {
        ActorSystem.ActorSystemConfig config = ActorSystem.ActorSystemConfig.create()
                .maxSize(maxSize)
                .idleTimeoutMinutes(idleTimeoutMinutes)
                .saveIntervalSeconds(saveIntervalSeconds);

        guildActorSystem = new ActorSystem<>(
                "GuildActorSystem",
                config,
                guildId -> new GuildActor(guildId, redisService, mongoService)
        );
        guildActorSystem.init();

        log.info("公会服务初始化完成: maxSize={}, idleTimeout={}min", maxSize, idleTimeoutMinutes);
    }

    @Override
    public Result<GuildDTO> createGuild(long roleId, String guildName, String declaration, int iconId) {
        log.info("创建公会: roleId={}, guildName={}", roleId, guildName);

        // 校验公会名
        if (!StringUtil.isValidName(guildName, nameMinLength, nameMaxLength)) {
            return Result.fail(ErrorCode.PARAM_ERROR, 
                    String.format("公会名长度应在%d-%d之间", nameMinLength, nameMaxLength));
        }

        // 检查公会名是否已存在
        if (isGuildNameExists(guildName)) {
            return Result.fail(ErrorCode.GUILD_NAME_EXISTS);
        }

        // TODO: 检查玩家是否已在公会
        // TODO: 扣除创建费用

        // 创建公会数据
        long guildId = IdGenerator.generateGuildId();
        GuildData guildData = GuildData.create(guildId, guildName, roleId, 
                "Leader_" + roleId, declaration, iconId);
        guildData.setMaxMember(initMaxMember);

        // 保存到数据库
        mongoService.save(guildData);

        log.info("创建公会成功: guildId={}, guildName={}", guildId, guildName);

        return Result.success(toGuildDTO(guildData));
    }

    @Override
    public Result<GuildDTO> getGuildInfo(long guildId) {
        GuildActor actor = guildActorSystem.getActor(guildId);
        if (actor == null || actor.getData() == null) {
            return Result.fail(ErrorCode.GUILD_NOT_FOUND);
        }

        return Result.success(toGuildDTO(actor.getData()));
    }

    @Override
    public Result<GuildDTO> getPlayerGuild(long roleId) {
        // 从数据库查询玩家所在公会
        // TODO: 实现查询逻辑
        return Result.fail(ErrorCode.GUILD_NOT_JOINED);
    }

    @Override
    public Result<List<GuildDTO>> searchGuild(String keyword, int page, int size) {
        Query query = new Query();
        if (StringUtil.isNotBlank(keyword)) {
            query.addCriteria(Criteria.where("guildName").regex(keyword, "i"));
        }

        List<GuildData> guilds = mongoService.findPage(query, GuildData.class, page, size, 
                "memberCount", false);

        List<GuildDTO> result = guilds.stream()
                .map(this::toGuildDTO)
                .collect(Collectors.toList());

        return Result.success(result);
    }

    @Override
    public Result<Void> applyJoinGuild(long roleId, long guildId, String message) {
        log.info("申请加入公会: roleId={}, guildId={}", roleId, guildId);

        GuildActor actor = guildActorSystem.getActor(guildId);
        if (actor == null || actor.getData() == null) {
            return Result.fail(ErrorCode.GUILD_NOT_FOUND);
        }

        // TODO: 检查玩家是否已在公会
        // TODO: 检查是否已申请
        // TODO: 添加申请记录

        return Result.success();
    }

    @Override
    public Result<Void> joinGuild(long roleId, long guildId) {
        log.info("加入公会: roleId={}, guildId={}", roleId, guildId);

        GuildActor actor = guildActorSystem.getActor(guildId);
        if (actor == null || actor.getData() == null) {
            return Result.fail(ErrorCode.GUILD_NOT_FOUND);
        }

        GuildData guildData = actor.getData();

        // 检查公会是否已满
        if (guildData.getMembers().size() >= guildData.getMaxMember()) {
            return Result.fail(ErrorCode.GUILD_FULL);
        }

        // 发送消息到 Actor
        ActorMessage actorMessage = ActorMessage.of(
                MessageTypes.ADD_MEMBER,
                new GuildActor.AddMemberRequest(roleId, "Player_" + roleId, 1, 1, 0)
        );

        boolean sent = actor.tell(actorMessage);
        if (!sent) {
            return Result.fail(ErrorCode.SYSTEM_ERROR, "消息发送失败");
        }

        return Result.success();
    }

    @Override
    public Result<Void> handleApply(long operatorId, long applyId, boolean accept) {
        // TODO: 实现申请处理逻辑
        return Result.success();
    }

    @Override
    public Result<Void> leaveGuild(long roleId) {
        log.info("退出公会: roleId={}", roleId);

        // TODO: 获取玩家所在公会
        // TODO: 发送 REMOVE_MEMBER 消息

        return Result.success();
    }

    @Override
    public Result<Void> kickMember(long operatorId, long targetId) {
        log.info("踢出成员: operatorId={}, targetId={}", operatorId, targetId);

        // TODO: 验证权限
        // TODO: 发送 REMOVE_MEMBER 消息

        return Result.success();
    }

    @Override
    public Result<Long> donate(long roleId, int donateType, long amount) {
        log.info("公会捐献: roleId={}, donateType={}, amount={}", roleId, donateType, amount);

        // TODO: 获取玩家所在公会
        // TODO: 扣除玩家资源
        // TODO: 发送 DONATE 消息

        return Result.success(amount);
    }

    @Override
    public Result<Void> changeMemberPosition(long operatorId, long targetId, int newPosition) {
        log.info("修改成员职位: operatorId={}, targetId={}, newPosition={}", 
                operatorId, targetId, newPosition);

        // TODO: 验证权限
        // TODO: 发送 CHANGE_POSITION 消息

        return Result.success();
    }

    @Override
    public Result<Void> transferLeader(long currentLeaderId, long newLeaderId) {
        log.info("转让会长: currentLeaderId={}, newLeaderId={}", currentLeaderId, newLeaderId);

        // TODO: 验证权限
        // TODO: 发送 TRANSFER_LEADER 消息

        return Result.success();
    }

    @Override
    public Result<Void> dissolveGuild(long leaderId) {
        log.info("解散公会: leaderId={}", leaderId);

        // TODO: 验证权限
        // TODO: 删除公会数据

        return Result.success();
    }

    @Override
    public Result<List<GuildMemberDTO>> getMembers(long guildId) {
        GuildActor actor = guildActorSystem.getActor(guildId);
        if (actor == null || actor.getData() == null) {
            return Result.fail(ErrorCode.GUILD_NOT_FOUND);
        }

        List<GuildMemberDTO> members = actor.getData().getMembers().stream()
                .map(this::toMemberDTO)
                .collect(Collectors.toList());

        return Result.success(members);
    }

    /**
     * 检查公会名是否存在
     */
    private boolean isGuildNameExists(String guildName) {
        Query query = Query.query(Criteria.where("guildName").is(guildName));
        return mongoService.exists(query, GuildData.class);
    }

    /**
     * 转换为 GuildDTO
     */
    private GuildDTO toGuildDTO(GuildData data) {
        GuildDTO dto = new GuildDTO();
        dto.setGuildId(data.getGuildId());
        dto.setGuildName(data.getGuildName());
        dto.setLevel(data.getLevel());
        dto.setExp(data.getExp());
        dto.setIconId(data.getIconId());
        dto.setDeclaration(data.getDeclaration());
        dto.setLeaderId(data.getLeaderId());
        dto.setLeaderName(data.getLeaderName());
        dto.setMemberCount(data.getMemberCount());
        dto.setMaxMember(data.getMaxMember());
        dto.setCreateTime(data.getGuildCreateTime());
        dto.setJoinType(data.getJoinType());
        dto.setJoinLevel(data.getJoinLevel());
        dto.setFund(data.getFund());
        return dto;
    }

    /**
     * 转换为 GuildMemberDTO
     */
    private GuildMemberDTO toMemberDTO(GuildData.GuildMember member) {
        GuildMemberDTO dto = new GuildMemberDTO();
        dto.setRoleId(member.getRoleId());
        dto.setRoleName(member.getRoleName());
        dto.setLevel(member.getLevel());
        dto.setAvatarId(member.getAvatarId());
        dto.setPosition(member.getPosition());
        dto.setContribution(member.getContribution());
        dto.setTodayContribution(member.getTodayContribution());
        dto.setJoinTime(member.getJoinTime());
        dto.setLastLoginTime(member.getLastLoginTime());
        dto.setCombatPower(member.getCombatPower());
        return dto;
    }

    @Override
    public Result<Void> dailyReset() {
        log.info("执行公会每日重置");
        // 实际实现：遍历所有公会执行每日重置
        // 这里简化处理，实际应该通过批量任务处理
        return Result.success();
    }
}
