package com.game.data.base;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository 基类
 * <p>
 * 封装常用的数据访问操作，简化业务开发
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * @Repository
 * public class PlayerRepository extends BaseRepository<PlayerData, Long> {
 *
 *     public PlayerRepository(MongoTemplate mongoTemplate) {
 *         super(mongoTemplate, PlayerData.class);
 *     }
 *
 *     public List<PlayerData> findByLevel(int level) {
 *         return findByField("level", level);
 *     }
 *
 *     public List<PlayerData> findTopPlayers(int limit) {
 *         return findAll(Sort.by(Sort.Direction.DESC, "combatPower"), limit);
 *     }
 * }
 * }
 * </pre>
 *
 * @param <T>  实体类型
 * @param <ID> ID 类型
 * @author GameServer
 */
public abstract class BaseRepository<T, ID> {

    protected final MongoTemplate mongoTemplate;
    protected final Class<T> entityClass;

    protected BaseRepository(MongoTemplate mongoTemplate, Class<T> entityClass) {
        this.mongoTemplate = mongoTemplate;
        this.entityClass = entityClass;
    }

    // ==================== 基础 CRUD ====================

    /**
     * 根据 ID 查询
     */
    public Optional<T> findById(ID id) {
        T entity = mongoTemplate.findById(id, entityClass);
        return Optional.ofNullable(entity);
    }

    /**
     * 根据 ID 查询 (不存在返回 null)
     */
    public T getById(ID id) {
        return mongoTemplate.findById(id, entityClass);
    }

    /**
     * 查询所有
     */
    public List<T> findAll() {
        return mongoTemplate.findAll(entityClass);
    }

    /**
     * 根据 ID 列表查询
     */
    public List<T> findAllById(Collection<ID> ids) {
        Query query = new Query(Criteria.where("_id").in(ids));
        return mongoTemplate.find(query, entityClass);
    }

    /**
     * 保存实体
     */
    public T save(T entity) {
        return mongoTemplate.save(entity);
    }

    /**
     * 批量保存
     */
    public List<T> saveAll(Collection<T> entities) {
        return entities.stream()
                .map(mongoTemplate::save)
                .toList();
    }

    /**
     * 根据 ID 删除
     */
    public void deleteById(ID id) {
        Query query = new Query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, entityClass);
    }

    /**
     * 删除实体
     */
    public void delete(T entity) {
        mongoTemplate.remove(entity);
    }

    /**
     * 判断是否存在
     */
    public boolean existsById(ID id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return mongoTemplate.exists(query, entityClass);
    }

    /**
     * 统计总数
     */
    public long count() {
        return mongoTemplate.count(new Query(), entityClass);
    }

    // ==================== 查询方法 ====================

    /**
     * 根据字段查询单个
     */
    public Optional<T> findOneByField(String field, Object value) {
        Query query = new Query(Criteria.where(field).is(value));
        return Optional.ofNullable(mongoTemplate.findOne(query, entityClass));
    }

    /**
     * 根据字段查询列表
     */
    public List<T> findByField(String field, Object value) {
        Query query = new Query(Criteria.where(field).is(value));
        return mongoTemplate.find(query, entityClass);
    }

    /**
     * 根据字段查询列表 (带排序)
     */
    public List<T> findByField(String field, Object value, Sort sort) {
        Query query = new Query(Criteria.where(field).is(value)).with(sort);
        return mongoTemplate.find(query, entityClass);
    }

    /**
     * 根据字段查询列表 (带分页)
     */
    public List<T> findByField(String field, Object value, int page, int size) {
        Query query = new Query(Criteria.where(field).is(value))
                .with(PageRequest.of(page, size));
        return mongoTemplate.find(query, entityClass);
    }

    /**
     * 查询所有 (带排序)
     */
    public List<T> findAll(Sort sort) {
        Query query = new Query().with(sort);
        return mongoTemplate.find(query, entityClass);
    }

    /**
     * 查询所有 (带排序和限制)
     */
    public List<T> findAll(Sort sort, int limit) {
        Query query = new Query().with(sort).limit(limit);
        return mongoTemplate.find(query, entityClass);
    }

    /**
     * 分页查询
     */
    public List<T> findPage(int page, int size) {
        Query query = new Query().with(PageRequest.of(page, size));
        return mongoTemplate.find(query, entityClass);
    }

    /**
     * 分页查询 (带排序)
     */
    public List<T> findPage(int page, int size, Sort sort) {
        Query query = new Query().with(PageRequest.of(page, size, sort));
        return mongoTemplate.find(query, entityClass);
    }

    // ==================== 更新方法 ====================

    /**
     * 更新单个字段
     */
    public void updateField(ID id, String field, Object value) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().set(field, value);
        mongoTemplate.updateFirst(query, update, entityClass);
    }

    /**
     * 增加字段值
     */
    public void incrementField(ID id, String field, long delta) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().inc(field, delta);
        mongoTemplate.updateFirst(query, update, entityClass);
    }

    /**
     * 批量更新字段
     */
    public void updateFieldBatch(Collection<ID> ids, String field, Object value) {
        Query query = new Query(Criteria.where("_id").in(ids));
        Update update = new Update().set(field, value);
        mongoTemplate.updateMulti(query, update, entityClass);
    }

    // ==================== 统计方法 ====================

    /**
     * 按条件统计
     */
    public long countByField(String field, Object value) {
        Query query = new Query(Criteria.where(field).is(value));
        return mongoTemplate.count(query, entityClass);
    }

    /**
     * 按条件判断是否存在
     */
    public boolean existsByField(String field, Object value) {
        Query query = new Query(Criteria.where(field).is(value));
        return mongoTemplate.exists(query, entityClass);
    }

    // ==================== 辅助方法 ====================

    /**
     * 执行自定义查询
     */
    public List<T> find(Query query) {
        return mongoTemplate.find(query, entityClass);
    }

    /**
     * 执行自定义查询 (单个)
     */
    public Optional<T> findOne(Query query) {
        return Optional.ofNullable(mongoTemplate.findOne(query, entityClass));
    }

    /**
     * 执行自定义更新
     */
    public void update(Query query, Update update) {
        mongoTemplate.updateFirst(query, update, entityClass);
    }

    /**
     * 执行自定义批量更新
     */
    public void updateMulti(Query query, Update update) {
        mongoTemplate.updateMulti(query, update, entityClass);
    }

    /**
     * 获取 MongoTemplate
     */
    protected MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }
}
