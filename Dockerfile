# ─── Stage 1: Build ───────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Копіюємо pom.xml окремо — щоб кешувати залежності
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копіюємо вихідний код та збираємо jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

COPY --from=build /app/target/read-sprinter-back-0.0.1-SNAPSHOT.jar app.jar

# Документуємо порт (реальне значення береться зі змінної PORT)
EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]

