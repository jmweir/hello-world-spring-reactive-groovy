# syntax=docker/dockerfile:experimental
FROM openjdk:13-jdk-alpine as build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY src src

RUN --mount=type=cache,target=/root/.gradle ./gradlew clean build -x test
RUN mkdir -p build/dependency && (cd build/dependency && jar -xf ../libs/*.jar)

FROM openjdk:13-alpine
VOLUME /tmp

ARG DEPENDENCY=/app/build/dependency

COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-cp","app:app/lib/*","com.example.app.Application"]
