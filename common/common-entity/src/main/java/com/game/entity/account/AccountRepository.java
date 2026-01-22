package com.game.entity.account;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 账号数据仓库
 *
 * @author GameServer
 */
@Repository
public interface AccountRepository extends MongoRepository<Account, String> {

    /**
     * 根据账号 ID 查询
     */
    Optional<Account> findByAccountId(long accountId);

    /**
     * 根据用户名查询
     */
    Optional<Account> findByUsername(String username);

    /**
     * 根据邮箱查询
     */
    Optional<Account> findByEmail(String email);

    /**
     * 根据手机号查询
     */
    Optional<Account> findByPhone(String phone);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
}
