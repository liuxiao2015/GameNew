package com.game.common.tools.proto;

import com.game.common.protocol.ProtoField;
import com.game.common.protocol.ProtoMessage;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Protobuf 文件生成器
 * <p>
 * 自动扫描带有 @ProtoMessage 注解的 DTO 类，生成对应的 .proto 文件
 * </p>
 * <p>
 * 使用方式:
 * <pre>
 * java -cp common-api.jar com.game.common.tools.proto.ProtoGenerator \
 *     --scan-packages=com.game.api \
 *     --output-dir=proto/generated \
 *     --classpath=target/classes
 * </pre>
 * </p>
 *
 * @author GameServer
 */
public class ProtoGenerator {

    /**
     * Java 类型到 Protobuf 类型映射
     */
    private static final Map<Class<?>, String> TYPE_MAPPING = new HashMap<>();

    static {
        TYPE_MAPPING.put(boolean.class, "bool");
        TYPE_MAPPING.put(Boolean.class, "bool");
        TYPE_MAPPING.put(int.class, "int32");
        TYPE_MAPPING.put(Integer.class, "int32");
        TYPE_MAPPING.put(long.class, "int64");
        TYPE_MAPPING.put(Long.class, "int64");
        TYPE_MAPPING.put(float.class, "float");
        TYPE_MAPPING.put(Float.class, "float");
        TYPE_MAPPING.put(double.class, "double");
        TYPE_MAPPING.put(Double.class, "double");
        TYPE_MAPPING.put(String.class, "string");
        TYPE_MAPPING.put(byte[].class, "bytes");
    }

    private final String[] scanPackages;
    private final String outputDir;
    private final String classpath;

    /**
     * 按模块分组的消息
     */
    private final Map<String, List<MessageInfo>> moduleMessages = new TreeMap<>();

    /**
     * 已处理的消息类型
     */
    private final Set<Class<?>> processedTypes = new HashSet<>();

    public ProtoGenerator(String[] scanPackages, String outputDir, String classpath) {
        this.scanPackages = scanPackages;
        this.outputDir = outputDir;
        this.classpath = classpath;
    }

    /**
     * 执行生成
     */
    public void generate() throws Exception {
        System.out.println("========================================");
        System.out.println("       Proto 文件生成器");
        System.out.println("========================================");
        System.out.println("扫描包: " + Arrays.toString(scanPackages));
        System.out.println("输出目录: " + outputDir);
        System.out.println();

        // 扫描类
        List<Class<?>> classes = scanClasses();
        System.out.println("扫描到 " + classes.size() + " 个带有 @ProtoMessage 注解的类");

        // 分析消息
        for (Class<?> clazz : classes) {
            analyzeClass(clazz);
        }

        // 生成 .proto 文件
        for (Map.Entry<String, List<MessageInfo>> entry : moduleMessages.entrySet()) {
            generateProtoFile(entry.getKey(), entry.getValue());
        }

        System.out.println();
        System.out.println("生成完成！共生成 " + moduleMessages.size() + " 个 .proto 文件");
    }

    /**
     * 扫描所有带有 @ProtoMessage 注解的类
     */
    private List<Class<?>> scanClasses() throws Exception {
        List<Class<?>> result = new ArrayList<>();
        ClassLoader classLoader = getClassLoader();

        for (String packageName : scanPackages) {
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();

                if ("file".equals(protocol)) {
                    scanDirectory(new File(resource.toURI()), packageName, result, classLoader);
                } else if ("jar".equals(protocol)) {
                    scanJar(resource, packageName, result, classLoader);
                }
            }
        }

        return result;
    }

    /**
     * 获取类加载器
     */
    private ClassLoader getClassLoader() throws Exception {
        if (classpath != null && !classpath.isEmpty()) {
            List<URL> urls = new ArrayList<>();
            for (String cp : classpath.split(File.pathSeparator)) {
                urls.add(new File(cp).toURI().toURL());
            }
            return new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
        }
        return getClass().getClassLoader();
    }

    /**
     * 扫描目录
     */
    private void scanDirectory(File dir, String packageName, List<Class<?>> result,
                               ClassLoader classLoader) {
        if (!dir.exists()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), result, classLoader);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." +
                        file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = classLoader.loadClass(className);
                    if (clazz.isAnnotationPresent(ProtoMessage.class)) {
                        result.add(clazz);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // 忽略无法加载的类
                }
            }
        }
    }

    /**
     * 扫描 JAR 文件
     */
    private void scanJar(URL jarUrl, String packageName, List<Class<?>> result,
                         ClassLoader classLoader) throws Exception {
        String jarPath = jarUrl.getPath();
        int index = jarPath.indexOf("!");
        if (index > 0) {
            jarPath = jarPath.substring(5, index);
        }

        try (JarFile jarFile = new JarFile(jarPath)) {
            String path = packageName.replace('.', '/');
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(path) && name.endsWith(".class")) {
                    String className = name.substring(0, name.length() - 6).replace('/', '.');
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        if (clazz.isAnnotationPresent(ProtoMessage.class)) {
                            result.add(clazz);
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        // 忽略
                    }
                }
            }
        }
    }

    /**
     * 分析类
     */
    private void analyzeClass(Class<?> clazz) {
        if (processedTypes.contains(clazz)) {
            return;
        }
        processedTypes.add(clazz);

        ProtoMessage annotation = clazz.getAnnotation(ProtoMessage.class);
        String module = annotation.module();
        String messageName = annotation.name().isEmpty() ? clazz.getSimpleName() : annotation.name();

        MessageInfo messageInfo = new MessageInfo();
        messageInfo.name = messageName;
        messageInfo.desc = annotation.desc();
        messageInfo.protocolId = annotation.protocolId();
        messageInfo.javaClass = clazz;

        // 分析字段
        int fieldNumber = 1;
        for (Field field : getAllFields(clazz)) {
            ProtoField protoField = field.getAnnotation(ProtoField.class);
            int number = protoField != null ? protoField.value() : fieldNumber++;
            String desc = protoField != null ? protoField.desc() : "";

            FieldInfo fieldInfo = analyzeField(field, number, desc);
            messageInfo.fields.add(fieldInfo);

            // 检查是否需要处理嵌套类型
            Class<?> fieldType = getActualType(field);
            if (fieldType.isAnnotationPresent(ProtoMessage.class) && !processedTypes.contains(fieldType)) {
                analyzeClass(fieldType);
            }
        }

        moduleMessages.computeIfAbsent(module, k -> new ArrayList<>()).add(messageInfo);
    }

    /**
     * 获取所有字段 (包括父类)
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                        !java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * 分析字段
     */
    private FieldInfo analyzeField(Field field, int number, String desc) {
        FieldInfo fieldInfo = new FieldInfo();
        fieldInfo.name = field.getName();
        fieldInfo.number = number;
        fieldInfo.desc = desc;

        Class<?> type = field.getType();
        Type genericType = field.getGenericType();

        // 处理 List 类型
        if (List.class.isAssignableFrom(type)) {
            fieldInfo.repeated = true;
            if (genericType instanceof ParameterizedType pt) {
                Type actualType = pt.getActualTypeArguments()[0];
                if (actualType instanceof Class<?> actualClass) {
                    fieldInfo.protoType = getProtoType(actualClass);
                }
            }
        }
        // 处理 Map 类型
        else if (Map.class.isAssignableFrom(type)) {
            fieldInfo.map = true;
            if (genericType instanceof ParameterizedType pt) {
                Type keyType = pt.getActualTypeArguments()[0];
                Type valueType = pt.getActualTypeArguments()[1];
                if (keyType instanceof Class<?> keyClass && valueType instanceof Class<?> valueClass) {
                    fieldInfo.mapKeyType = getProtoType(keyClass);
                    fieldInfo.mapValueType = getProtoType(valueClass);
                }
            }
        }
        // 普通类型
        else {
            fieldInfo.protoType = getProtoType(type);
        }

        return fieldInfo;
    }

    /**
     * 获取字段的实际类型
     */
    private Class<?> getActualType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType pt) {
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?>) {
                return (Class<?>) typeArgs[0];
            }
        }
        return field.getType();
    }

    /**
     * 获取 Protobuf 类型
     */
    private String getProtoType(Class<?> javaType) {
        String protoType = TYPE_MAPPING.get(javaType);
        if (protoType != null) {
            return protoType;
        }
        // 其他类型认为是消息类型
        return javaType.getSimpleName();
    }

    /**
     * 生成 .proto 文件
     */
    private void generateProtoFile(String module, List<MessageInfo> messages) throws IOException {
        StringBuilder sb = new StringBuilder();

        // 文件头
        sb.append("// ========================================\n");
        sb.append("// 自动生成的 Protobuf 文件，请勿手动修改\n");
        sb.append("// 模块: ").append(module).append("\n");
        sb.append("// 生成时间: ").append(new Date()).append("\n");
        sb.append("// ========================================\n\n");

        sb.append("syntax = \"proto3\";\n\n");
        sb.append("package game.").append(module).append(";\n\n");
        sb.append("option java_package = \"com.game.proto.").append(module).append("\";\n");
        sb.append("option java_outer_classname = \"").append(capitalize(module)).append("Proto\";\n");
        sb.append("option java_multiple_files = true;\n\n");

        // 导入其他模块
        Set<String> imports = new TreeSet<>();
        for (MessageInfo msg : messages) {
            for (FieldInfo field : msg.fields) {
                String protoType = field.protoType;
                if (protoType != null && !TYPE_MAPPING.containsValue(protoType)) {
                    // 查找该类型所属模块
                    for (Map.Entry<String, List<MessageInfo>> entry : moduleMessages.entrySet()) {
                        if (!entry.getKey().equals(module)) {
                            for (MessageInfo m : entry.getValue()) {
                                if (m.name.equals(protoType)) {
                                    imports.add(entry.getKey() + ".proto");
                                }
                            }
                        }
                    }
                }
            }
        }
        for (String importFile : imports) {
            sb.append("import \"").append(importFile).append("\";\n");
        }
        if (!imports.isEmpty()) {
            sb.append("\n");
        }

        // 生成消息
        for (MessageInfo msg : messages) {
            if (!msg.desc.isEmpty()) {
                sb.append("// ").append(msg.desc).append("\n");
            }
            sb.append("message ").append(msg.name).append(" {\n");

            for (FieldInfo field : msg.fields) {
                sb.append("    ");
                if (!field.desc.isEmpty()) {
                    sb.append("// ").append(field.desc).append("\n    ");
                }

                if (field.repeated) {
                    sb.append("repeated ");
                } else if (field.map) {
                    sb.append("map<").append(field.mapKeyType)
                            .append(", ").append(field.mapValueType).append("> ");
                    sb.append(toSnakeCase(field.name)).append(" = ").append(field.number).append(";\n");
                    continue;
                }

                sb.append(field.protoType).append(" ");
                sb.append(toSnakeCase(field.name)).append(" = ").append(field.number).append(";\n");
            }

            sb.append("}\n\n");
        }

        // 写入文件
        Path outputPath = Paths.get(outputDir, module + ".proto");
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, sb.toString());
        System.out.println("生成: " + outputPath);
    }

    /**
     * 首字母大写
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 驼峰转下划线
     */
    private String toSnakeCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 消息信息
     */
    private static class MessageInfo {
        String name;
        String desc = "";
        int protocolId;
        Class<?> javaClass;
        final List<FieldInfo> fields = new ArrayList<>();
    }

    /**
     * 字段信息
     */
    private static class FieldInfo {
        String name;
        int number;
        String desc = "";
        String protoType;
        boolean repeated;
        boolean map;
        String mapKeyType;
        String mapValueType;
    }

    /**
     * 主入口
     */
    public static void main(String[] args) throws Exception {
        String scanPackages = "com.game.api";
        String outputDir = "proto/generated";
        String classpath = null;

        for (String arg : args) {
            if (arg.startsWith("--scan-packages=")) {
                scanPackages = arg.substring("--scan-packages=".length());
            } else if (arg.startsWith("--output-dir=")) {
                outputDir = arg.substring("--output-dir=".length());
            } else if (arg.startsWith("--classpath=")) {
                classpath = arg.substring("--classpath=".length());
            }
        }

        ProtoGenerator generator = new ProtoGenerator(
                scanPackages.split(","),
                outputDir,
                classpath
        );
        generator.generate();
    }
}
