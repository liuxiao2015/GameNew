package com.game.entity.repository;

import com.game.entity.document.Account;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 账号仓库
 *
 * @author GameServer
 */
@Repository
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
     * 根据邮箱查找
     */
    Optional<Account> findByEmail(String email);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

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
