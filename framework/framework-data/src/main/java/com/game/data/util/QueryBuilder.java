package com.game.data.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * MongoDB 查询构建器
 * <p>
 * 链式 API 构建查询条件
 * </p>
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 简单查询
 * Query query = QueryBuilder.create()
 *     .eq("serverId", 1001)
 *     .gte("level", 10)
 *     .build();
 *
 * // 复杂查询
 * Query query = QueryBuilder.create()
 *     .eq("serverId", 1001)
 *     .in("status", List.of(1, 2, 3))
 *     .range("createTime", startTime, endTime)
 *     .like("nickname", "test")
 *     .orderByDesc("combatPower")
 *     .page(0, 20)
 *     .build();
 *
 * List<Player> players = mongoTemplate.find(query, Player.class);
 * }
 * </pre>
 *
 * @author GameServer
 */
public class QueryBuilder {

    private final List<Criteria> criteriaList = new ArrayList<>();
    private Sort sort;
    private Integer skip;
    private Integer limit;

    private QueryBuilder() {
    }

    /**
     * 创建查询构建器
     */
    public static QueryBuilder create() {
        return new QueryBuilder();
    }

    // ==================== 条件方法 ====================

    /**
     * 等于
     */
    public QueryBuilder eq(String field, Object value) {
        if (value != null) {
            criteriaList.add(Criteria.where(field).is(value));
        }
        return this;
    }

    /**
     * 不等于
     */
    public QueryBuilder ne(String field, Object value) {
        if (value != null) {
            criteriaList.add(Criteria.where(field).ne(value));
        }
        return this;
    }

    /**
     * 大于
     */
    public QueryBuilder gt(String field, Object value) {
        if (value != null) {
            criteriaList.add(Criteria.where(field).gt(value));
        }
        return this;
    }

    /**
     * 大于等于
     */
    public QueryBuilder gte(String field, Object value) {
        if (value != null) {
            criteriaList.add(Criteria.where(field).gte(value));
        }
        return this;
    }

    /**
     * 小于
     */
    public QueryBuilder lt(String field, Object value) {
        if (value != null) {
            criteriaList.add(Criteria.where(field).lt(value));
        }
        return this;
    }

    /**
     * 小于等于
     */
    public QueryBuilder lte(String field, Object value) {
        if (value != null) {
            criteriaList.add(Criteria.where(field).lte(value));
        }
        return this;
    }

    /**
     * 范围查询 (包含边界)
     */
    public QueryBuilder range(String field, Object min, Object max) {
        if (min != null || max != null) {
            Criteria criteria = Criteria.where(field);
            if (min != null) {
                criteria = criteria.gte(min);
            }
            if (max != null) {
                criteria = criteria.lte(max);
            }
            criteriaList.add(criteria);
        }
        return this;
    }

    /**
     * IN 查询
     */
    public QueryBuilder in(String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            criteriaList.add(Criteria.where(field).in(values));
        }
        return this;
    }

    /**
     * NOT IN 查询
     */
    public QueryBuilder notIn(String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            criteriaList.add(Criteria.where(field).nin(values));
        }
        return this;
    }

    /**
     * 模糊查询 (包含)
     */
    public QueryBuilder like(String field, String value) {
        if (value != null && !value.isEmpty()) {
            criteriaList.add(Criteria.where(field).regex(".*" + escapeRegex(value) + ".*", "i"));
        }
        return this;
    }

    /**
     * 前缀匹配
     */
    public QueryBuilder startsWith(String field, String value) {
        if (value != null && !value.isEmpty()) {
            criteriaList.add(Criteria.where(field).regex("^" + escapeRegex(value), "i"));
        }
        return this;
    }

    /**
     * 正则匹配
     */
    public QueryBuilder regex(String field, String pattern) {
        if (pattern != null && !pattern.isEmpty()) {
            criteriaList.add(Criteria.where(field).regex(pattern));
        }
        return this;
    }

    /**
     * 是否为 null
     */
    public QueryBuilder isNull(String field) {
        criteriaList.add(Criteria.where(field).is(null));
        return this;
    }

    /**
     * 是否不为 null
     */
    public QueryBuilder isNotNull(String field) {
        criteriaList.add(Criteria.where(field).ne(null));
        return this;
    }

    /**
     * 是否存在该字段
     */
    public QueryBuilder exists(String field, boolean exists) {
        criteriaList.add(Criteria.where(field).exists(exists));
        return this;
    }

    // ==================== 排序方法 ====================

    /**
     * 升序排序
     */
    public QueryBuilder orderByAsc(String field) {
        this.sort = Sort.by(Sort.Direction.ASC, field);
        return this;
    }

    /**
     * 降序排序
     */
    public QueryBuilder orderByDesc(String field) {
        this.sort = Sort.by(Sort.Direction.DESC, field);
        return this;
    }

    /**
     * 自定义排序
     */
    public QueryBuilder orderBy(Sort sort) {
        this.sort = sort;
        return this;
    }

    /**
     * 多字段排序
     */
    public QueryBuilder orderBy(String... fields) {
        this.sort = Sort.by(fields);
        return this;
    }

    // ==================== 分页方法 ====================

    /**
     * 跳过记录数
     */
    public QueryBuilder skip(int skip) {
        this.skip = skip;
        return this;
    }

    /**
     * 限制返回数量
     */
    public QueryBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * 分页
     */
    public QueryBuilder page(int page, int size) {
        this.skip = page * size;
        this.limit = size;
        return this;
    }

    // ==================== 构建方法 ====================

    /**
     * 构建 Query 对象
     */
    public Query build() {
        Query query = new Query();

        // 添加条件
        if (!criteriaList.isEmpty()) {
            Criteria[] criteriaArray = criteriaList.toArray(new Criteria[0]);
            query.addCriteria(new Criteria().andOperator(criteriaArray));
        }

        // 添加排序
        if (sort != null) {
            query.with(sort);
        }

        // 添加分页
        if (skip != null) {
            query.skip(skip);
        }
        if (limit != null) {
            query.limit(limit);
        }

        return query;
    }

    /**
     * 转义正则表达式特殊字符
     */
    private String escapeRegex(String str) {
        return str.replaceAll("([\\\\^$.|?*+()\\[\\]{}])", "\\\\$1");
    }
}
