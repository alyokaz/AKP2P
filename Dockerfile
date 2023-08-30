# syntax=docker/dockerfile:1

FROM gradle:jdk17-jammy as build

COPY --chown=gradle:gradle . /app

WORKDIR /app

RUN gradle build --no-daemon 


FROM eclipse-temurin:17-jdk-jammy

COPY --from=build /app/build/libs/*.jar /app/app.jar

CMD ["java", "-jar", "app.jar", "4441"]

EXPOSE 8080

