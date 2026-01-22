package com.game.data.mongo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * MongoDB 服务封装
 *
 * @author GameServer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MongoService {

    private final MongoTemplate mongoTemplate;

    // ==================== 插入操作 ====================

    /**
     * 插入文档
     */
    public <T> T insert(T entity) {
        return mongoTemplate.insert(entity);
    }

    /**
     * 批量插入文档
     */
    public <T> Collection<T> insertAll(Collection<T> entities) {
        return mongoTemplate.insertAll(entities);
    }

    /**
     * 插入到指定集合
     */
    public <T> T insert(T entity, String collectionName) {
        return mongoTemplate.insert(entity, collectionName);
    }

    // ==================== 保存操作 (存在则更新，不存在则插入) ====================

    /**
     * 保存文档
     */
    public <T> T save(T entity) {
        return mongoTemplate.save(entity);
    }

    /**
     * 保存到指定集合
     */
    public <T> T save(T entity, String collectionName) {
        return mongoTemplate.save(entity, collectionName);
    }

    // ==================== 查询操作 ====================

    /**
     * 根据 ID 查询
     */
    public <T> T findById(Object id, Class<T> entityClass) {
        return mongoTemplate.findById(id, entityClass);
    }

    /**
     * 根据 ID 查询 (指定集合)
     */
    public <T> T findById(Object id, Class<T> entityClass, String collectionName) {
        return mongoTemplate.findById(id, entityClass, collectionName);
    }

    /**
     * 查询单个文档
     */
    public <T> T findOne(Query query, Class<T> entityClass) {
        return mongoTemplate.findOne(query, entityClass);
    }

    /**
     * 查询所有文档
     */
    public <T> List<T> findAll(Class<T> entityClass) {
        return mongoTemplate.findAll(entityClass);
    }

    /**
     * 条件查询
     */
    public <T> List<T> find(Query query, Class<T> entityClass) {
        return mongoTemplate.find(query, entityClass);
    }

    /**
     * 条件查询 (指定集合)
     */
    public <T> List<T> find(Query query, Class<T> entityClass, String collectionName) {
        return mongoTemplate.find(query, entityClass, collectionName);
    }

    /**
     * 根据字段查询
     */
    public <T> T findByField(String field, Object value, Class<T> entityClass) {
        Query query = Query.query(Criteria.where(field).is(value));
        return mongoTemplate.findOne(query, entityClass);
    }

    /**
     * 根据字段查询列表
     */
    public <T> List<T> findListByField(String field, Object value, Class<T> entityClass) {
        Query query = Query.query(Criteria.where(field).is(value));
        return mongoTemplate.find(query, entityClass);
    }

    /**
     * 分页查询
     */
    public <T> List<T> findPage(Query query, Class<T> entityClass, int page, int size) {
        query.skip((long) (page - 1) * size).limit(size);
        return mongoTemplate.find(query, entityClass);
    }

    /**
     * 分页查询 (带排序)
     */
    public <T> List<T> findPage(Query query, Class<T> entityClass, int page, int size, 
                                 String sortField, boolean ascending) {
        query.skip((long) (page - 1) * size)
             .limit(size)
             .with(Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, sortField));
        return mongoTemplate.find(query, entityClass);
    }

    /**
     * 统计数量
     */
    public long count(Query query, Class<?> entityClass) {
        return mongoTemplate.count(query, entityClass);
    }

    /**
     * 判断是否存在
     */
    public boolean exists(Query query, Class<?> entityClass) {
        return mongoTemplate.exists(query, entityClass);
    }

    // ==================== 更新操作 ====================

    /**
     * 更新第一个匹配的文档
     */
    public long updateFirst(Query query, Update update, Class<?> entityClass) {
        return mongoTemplate.updateFirst(query, update, entityClass).getModifiedCount();
    }

    /**
     * 更新所有匹配的文档
     */
    public long updateMulti(Query query, Update update, Class<?> entityClass) {
        return mongoTemplate.updateMulti(query, update, entityClass).getModifiedCount();
    }

    /**
     * 根据 ID 更新字段
     */
    public long updateById(Object id, Update update, Class<?> entityClass) {
        Query query = Query.query(Criteria.where("_id").is(id));
        return mongoTemplate.updateFirst(query, update, entityClass).getModifiedCount();
    }

    /**
     * 更新或插入
     */
    public long upsert(Query query, Update update, Class<?> entityClass) {
        return mongoTemplate.upsert(query, update, entityClass).getModifiedCount();
    }

    // ==================== 删除操作 ====================

    /**
     * 删除文档
     */
    public <T> long remove(T entity) {
        return mongoTemplate.remove(entity).getDeletedCount();
    }

    /**
     * 条件删除
     */
    public long remove(Query query, Class<?> entityClass) {
        return mongoTemplate.remove(query, entityClass).getDeletedCount();
    }

    /**
     * 根据 ID 删除
     */
    public long removeById(Object id, Class<?> entityClass) {
        Query query = Query.query(Criteria.where("_id").is(id));
        return mongoTemplate.remove(query, entityClass).getDeletedCount();
    }

    // ==================== 聚合操作 ====================

    /**
     * 获取 MongoTemplate (用于复杂查询)
     */
    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    // ==================== 便捷方法 ====================

    /**
     * 创建查询对象
     */
    public Query createQuery() {
        return new Query();
    }

    /**
     * 创建更新对象
     */
    public Update createUpdate() {
        return new Update();
    }

    /**
     * 创建条件对象
     */
    public Criteria createCriteria(String key) {
        return Criteria.where(key);
    }
}
