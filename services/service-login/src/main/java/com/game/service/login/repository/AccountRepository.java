package com.game.service.login.repository;

import com.game.service.login.entity.Account;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * 账号仓库
 *
 * @author GameServer
 */
public interface AccountRepository extends MongoRepository<Account, String> {

    /**
     * 根据平台用户 ID 查找
     */
    Optional<Account> findByPlatformUserId(String platformUserId);

    /**
     * 根据用户名查找
     */
    Optional<Account> findByUsername(String username);

    /**
     * 根据手机号查找
     */
    Optional<Account> findByPhone(String phone);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查角色名是否存在
     */
    boolean existsByRoleIdsContaining(long roleId);

    /**
     * 根据设备 ID 和平台类型查找 (游客登录)
     */
    Optional<Account> findByDeviceIdAndPlatformType(String deviceId, int platformType);

    /**
     * 根据平台类型和平台用户 ID 查找 (第三方登录)
     */
    Optional<Account> findByPlatformTypeAndPlatformUserId(int platformType, String platformUserId);
}
