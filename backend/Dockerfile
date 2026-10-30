FROM gradle:8.14.3-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY gradlew ./
RUN ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew bootJar --no-daemon
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S noirstay && adduser -S noirstay -G noirstay && mkdir -p /app/media && chown -R noirstay:noirstay /app
WORKDIR /app
COPY --from=build /app/build/libs/noirstay-1.0.0.jar app.jar
USER noirstay
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
