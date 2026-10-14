# tea-saas 后端运行时镜像（产物由 build.ps1 复制到本目录，构建上下文最小化）
FROM eclipse-temurin:21-jre-alpine

# tzdata 供 TZ=Asia/Shanghai 生效；busybox wget 供 healthcheck 用（alpine 基础自带）
RUN apk add --no-cache tzdata

WORKDIR /app
COPY app.jar app.jar

ENV TZ=Asia/Shanghai
EXPOSE 8080

# JAVA_OPTS 经 compose 注入（默认 -Xms512m -Xmx1024m）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
