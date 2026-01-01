# 1) 使用 Maven 镜像构建整个项目（跳过测试以加快构建并避免 surefire 问题）
############################################################
# Build stage: 使用 Maven 在容器内构建（仅构建 share-auth 模块）
############################################################
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /build

# 拷贝仓库源码到构建容器
COPY . /build

# 仅构建 share-auth 及其依赖，彻底跳过测试的编译与执行
# -pl 指定模块，-am 自动构建依赖模块
RUN mvn -T 1C clean package -pl share-auth -am -DskipTests -Dmaven.test.skip=true

# 将目标模块生成的 jar 统一复制为 /build/app.jar（避免版本号差异导致 COPY 失败）
RUN set -eux; \
	JAR_PATH=$(find /build -type f -path "*/share-auth/target/*.jar" | head -n 1); \
	echo "Found JAR: $JAR_PATH"; \
	cp "$JAR_PATH" /build/app.jar

############################################################
# Runtime stage: 仅携带运行所需的 JRE 与应用 jar
############################################################
FROM eclipse-temurin:17-jre
WORKDIR /app

# 复制构建产物
COPY --from=build /build/app.jar /app/app.jar

# 容器内 Java 运行参数（可按需覆盖）
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# 暴露与 SpringBoot server.port 一致的端口（如未改动，默认 8080）
EXPOSE 8080

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]