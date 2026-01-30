# 1. ビルドステージ
FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# 2. 実行ステージ
FROM eclipse-temurin:21-jdk-alpine
COPY --from=build /target/*.jar app.jar

# .env を読み込んでから Java を起動する設定
ENTRYPOINT ["sh", "-c", "if [ -f .env ]; then export $(cat .env | grep -v '^#' | xargs); fi && java -jar /app.jar"]