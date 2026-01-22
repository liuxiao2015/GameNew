package com.game.data.mongo.index;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * MongoDB 索引管理器
 * <p>
 * 自动扫描带有 @Document 注解的类，并根据索引注解创建索引
 * </p>
 *
 * @author GameServer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoIndexManager {

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 要扫描的包路径
     */
    private static final String[] SCAN_PACKAGES = {
            "com.game.actor",
            "com.game.service"
    };

    @PostConstruct
    public void init() {
        log.info("开始扫描 MongoDB 索引...");
        int indexCount = 0;

        for (String basePackage : SCAN_PACKAGES) {
            indexCount += scanAndCreateIndexes(basePackage);
        }

        log.info("MongoDB 索引扫描完成，共创建 {} 个索引", indexCount);
    }

    /**
     * 扫描包并创建索引
     */
    private int scanAndCreateIndexes(String basePackage) {
        int count = 0;
        ClassPathScanningCandidateComponentProvider scanner = 
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Document.class));

        Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);

        for (BeanDefinition bd : candidates) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                count += createIndexesForClass(clazz);
            } catch (ClassNotFoundException e) {
                log.error("找不到类: {}", bd.getBeanClassName(), e);
            }
        }

        return count;
    }

    /**
     * 为单个类创建索引
     */
    private int createIndexesForClass(Class<?> clazz) {
        int count = 0;
        Document docAnnotation = clazz.getAnnotation(Document.class);
        if (docAnnotation == null) {
            return 0;
        }

        String collectionName = docAnnotation.collection();
        if (collectionName.isEmpty()) {
            collectionName = clazz.getSimpleName();
        }

        IndexOperations indexOps = mongoTemplate.indexOps(collectionName);

        // 处理字段级索引
        for (Field field : clazz.getDeclaredFields()) {
            MongoIndex indexAnnotation = field.getAnnotation(MongoIndex.class);
            if (indexAnnotation != null) {
                createFieldIndex(indexOps, field, indexAnnotation, collectionName);
                count++;
            }
        }

        // 处理父类字段
        Class<?> superClass = clazz.getSuperclass();
        while (superClass != null && superClass != Object.class) {
            for (Field field : superClass.getDeclaredFields()) {
                MongoIndex indexAnnotation = field.getAnnotation(MongoIndex.class);
                if (indexAnnotation != null) {
                    createFieldIndex(indexOps, field, indexAnnotation, collectionName);
                    count++;
                }
            }
            superClass = superClass.getSuperclass();
        }

        // 处理复合索引
        CompoundIndex[] compoundIndexes = clazz.getAnnotationsByType(CompoundIndex.class);
        for (CompoundIndex compoundIndex : compoundIndexes) {
            createCompoundIndex(indexOps, compoundIndex, collectionName);
            count++;
        }

        return count;
    }

    /**
     * 创建字段索引
     */
    private void createFieldIndex(IndexOperations indexOps, Field field, 
                                  MongoIndex annotation, String collectionName) {
        try {
            String fieldName = field.getName();
            String indexName = annotation.name().isEmpty() ? 
                    "idx_" + fieldName : annotation.name();

            Index index = new Index()
                    .named(indexName)
                    .on(fieldName, annotation.order() == 1 ? Sort.Direction.ASC : Sort.Direction.DESC);

            if (annotation.unique()) {
                index.unique();
            }
            if (annotation.sparse()) {
                index.sparse();
            }
            if (annotation.background()) {
                index.background();
            }
            if (annotation.expireAfterSeconds() > 0) {
                index.expire(Duration.ofSeconds(annotation.expireAfterSeconds()));
            }

            indexOps.ensureIndex(index);
            log.debug("创建索引: collection={}, index={}", collectionName, indexName);

        } catch (Exception e) {
            log.error("创建索引失败: collection={}, field={}", collectionName, field.getName(), e);
        }
    }

    /**
     * 创建复合索引
     */
    private void createCompoundIndex(IndexOperations indexOps, CompoundIndex annotation, 
                                     String collectionName) {
        try {
            Map<String, Integer> indexDef = objectMapper.readValue(
                    annotation.def(), new TypeReference<Map<String, Integer>>() {});

            String indexName = annotation.name().isEmpty() ? 
                    "idx_compound_" + String.join("_", indexDef.keySet()) : annotation.name();

            Index index = new Index().named(indexName);

            for (Map.Entry<String, Integer> entry : indexDef.entrySet()) {
                index.on(entry.getKey(), entry.getValue() == 1 ? 
                        Sort.Direction.ASC : Sort.Direction.DESC);
            }

            if (annotation.unique()) {
                index.unique();
            }
            if (annotation.sparse()) {
                index.sparse();
            }
            if (annotation.background()) {
                index.background();
            }

            indexOps.ensureIndex(index);
            log.debug("创建复合索引: collection={}, index={}", collectionName, indexName);

        } catch (Exception e) {
            log.error("创建复合索引失败: collection={}, def={}", collectionName, annotation.def(), e);
        }
    }
}
