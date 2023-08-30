# syntax=docker/dockerfile:1

FROM gradle:jdk17-jammy

COPY --chown=gradle:gradle . /app

WORKDIR /app

RUN gradle build --no-daemon 


FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY build/libs/*.jar app.jar

CMD ["java", "-jar", "app.jar", "4441"]

EXPOSE 8080

