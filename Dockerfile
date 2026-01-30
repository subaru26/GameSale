# 1. ビルドステージ: Maven と Java 21 を使用して JAR ファイルを作成
FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# 2. 実行ステージ: 最小限の Java 21 実行環境
FROM eclipse-temurin:21-jdk-alpine
# ビルドステージで生成された JAR ファイルを app.jar という名前でコピー
COPY --from=build /target/*.jar app.jar

# Render から提供されるポート番号（デフォルト 8080）を公開
EXPOSE 8080

# アプリケーションの起動
# Render の Environment Variables (MAIL_HOST など) は Java 起動時に自動で読み込まれます
ENTRYPOINT ["java", "-jar", "/app.jar"]