FROM gradle:8.14.3-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY gradlew ./
RUN ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew bootJar --no-daemon
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S sundrift && adduser -S sundrift -G sundrift && mkdir -p /app/media && chown -R sundrift:sundrift /app
WORKDIR /app
COPY --from=build /app/build/libs/sundrift-1.0.0.jar app.jar
USER sundrift
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
