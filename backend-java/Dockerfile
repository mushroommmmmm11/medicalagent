# --- 绗竴闃舵锛氭瀯寤洪樁娈?---
ARG MAVEN_BASE_IMAGE=m.daocloud.io/docker.io/library/maven:3.9-eclipse-temurin-17
ARG JAVA_RUNTIME_IMAGE=m.daocloud.io/docker.io/library/eclipse-temurin:17-jre-alpine
FROM ${MAVEN_BASE_IMAGE} AS builder

# 璁剧疆瀛楃闆嗭紝闃叉鏋勫缓杩囩▼涓殑涓枃涔辩爜
ENV LANG C.UTF-8

WORKDIR /app

# 鍒╃敤 Docker 灞傜紦瀛橈細鍏堝彧澶嶅埗 pom.xml
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 鍐嶅鍒舵簮鐮佸苟鎵撳寘
COPY src ./src
RUN mvn clean package -DskipTests -q

# --- 绗簩闃舵锛氳繍琛岄樁娈?---
# 使用 jre-alpine 版本，镜像体积更小（约 100MB+）
FROM ${JAVA_RUNTIME_IMAGE}

# 璁剧疆杩愯鏃跺瓧绗﹂泦鐜锛堥潪甯搁噸瑕侊紒锛?
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8

WORKDIR /app

# 浠庢瀯寤洪樁娈垫嫹璐?JAR 鍖?
# 寤鸿鎸囧畾鍏蜂綋鍚嶇О锛岄槻姝?target 涓嬫湁澶氫釜 jar 瀵艰嚧鎷疯礉澶辫触
COPY --from=builder /app/target/*.jar app.jar

# 鍒涘缓蹇呰鐩綍
RUN mkdir -p /app/uploads /app/logs

# 鍋ュ悍妫€鏌ワ細澧炲姞璧峰绛夊緟鏃堕棿 (start-period)
# 闃叉搴旂敤杩樻病鍚姩瀹屽氨琚?Docker 璇垽涓轰笉鍋ュ悍
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/api/v1/file/health || exit 1

# 鍚姩鍙傛暟浼樺寲
# 娣诲姞 -Dfile.encoding=UTF-8 纭繚 Java 鍐呴儴澶勭悊瀛楃涓蹭篃鏄?UTF-8
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Dlog4j.configurationFile=/app/logs/log4j2.xml", "-jar", "app.jar"]

EXPOSE 8080
