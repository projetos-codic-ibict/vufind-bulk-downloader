# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY . .

# The application requires src/main/resources/application.properties at runtime.
RUN test -f src/main/resources/application.properties || (echo "Missing src/main/resources/application.properties. Generate it from .env using ./vufind-bulk-downloader.sh generate-config" && exit 1)
RUN MAVEN_CONFIG= ./mvnw -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN addgroup --system app && adduser --system --ingroup app app
RUN mkdir -p /app/config /app/data && chown -R app:app /app

COPY --from=build /workspace/bulk-downloader.jar /app/bulk-downloader.jar

ENV JAVA_OPTS="-Xms256m -Xmx2g"
EXPOSE 8081

USER app

# /app/config is added to classpath so application.properties can be mounted externally.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -cp /app/config:/app/bulk-downloader.jar org.springframework.boot.loader.launch.JarLauncher"]
