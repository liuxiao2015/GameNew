package com.game.service.login.repository;

import com.game.service.login.entity.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * 角色仓库
 *
 * @author GameServer
 */
public interface RoleRepository extends MongoRepository<Role, Long> {

    /**
     * 根据账号查找角色列表
     */
    List<Role> findByAccountIdAndStatus(String accountId, int status);

    /**
     * 根据账号和服务器查找角色
     */
    Optional<Role> findByAccountIdAndServerIdAndStatus(String accountId, int serverId, int status);

    /**
     * 检查角色名是否存在
     */
    boolean existsByRoleName(String roleName);

    /**
     * 根据角色名查找
     */
    Optional<Role> findByRoleName(String roleName);
}
