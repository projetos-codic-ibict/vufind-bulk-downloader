# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace

# Optional Maven Central mirror. Leave this build argument empty to use Maven's
# default repository configuration.
ARG MAVEN_MIRROR_URL=""

COPY . .

# The application requires src/main/resources/application.properties at runtime.
RUN test -f src/main/resources/application.properties || (echo "Missing src/main/resources/application.properties. Copy application.properties.model and configure it manually." && exit 1)
RUN if [ -n "${MAVEN_MIRROR_URL}" ]; then \
      case "${MAVEN_MIRROR_URL}" in http://*|https://*) ;; *) echo "MAVEN_MIRROR_URL must be an HTTP(S) URL." >&2; exit 1;; esac; \
      mkdir -p /tmp/maven && \
      escaped_mirror_url="$(printf '%s' "${MAVEN_MIRROR_URL}" | sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g')" && \
      printf '%s\n' \
        '<settings>' \
        '  <mirrors>' \
        '    <mirror>' \
        '      <id>configured-maven-central-mirror</id>' \
        '      <name>Configured Maven Central mirror</name>' \
        "      <url>${escaped_mirror_url}</url>" \
        '      <mirrorOf>central</mirrorOf>' \
        '    </mirror>' \
        '  </mirrors>' \
        '</settings>' > /tmp/maven/settings.xml; \
      ./mvnw --settings /tmp/maven/settings.xml -DskipTests clean package; \
    else \
      ./mvnw -DskipTests clean package; \
    fi

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
