# 1) 使用 Maven 镜像构建整个项目（跳过测试以加快构建并避免 surefire 问题）
FROM maven:3.8.8-eclipse-temurin-17 as build
WORKDIR /build

# 拷贝所有源码到容器并执行打包（可并行）
COPY . /build
# 如果网络慢或你希望更细化，可以指定 -pl 指定模块，如 -pl share-modules/share-auth
RUN mvn -T 1C -DskipTests clean package

# 2) 运行时镜像：仅复制目标模块的 jar（这里示例为 share-auth 模块）
FROM eclipse-temurin:17-jre
WORKDIR /app

# 请确认你要运行的具体模块 target 下 jar 的路径，下面是示例路径
COPY --from=build /build/share-modules/share-auth/target/*-SNAPSHOT.jar ./app.jar
# 如果构建输出是带版本号的 jar，可以使用通配符如 *auth*.jar 或将上面路径改为 /build/**/share-auth/target/*.jar

ENV JAVA_OPTS=""

EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]