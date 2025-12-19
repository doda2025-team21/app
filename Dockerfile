# choose some image as the base image
FROM eclipse-temurin:25-jdk-jammy
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*

# navigate to working directory
WORKDIR /app

# Build arguments to pull jar file from GitHub Packages
ARG GH_ACTOR
ARG GH_TOKEN
ARG VERSION

# install system requirements and expose the port
RUN curl -u "${GH_ACTOR}:${GH_TOKEN}" -L\
    "https://maven.pkg.github.com/doda2025-team21/app/com/doda/app/app/${VERSION}/app-${VERSION}.jar"\
    -o app.jar

# Configure Maven settings for GitHub Packages authentication
WORKDIR /app

# create environment variable that can be changed during runtime
ENV MODEL_HOST=http://host.docker.internal:8081

ENV APP_PORT=8080 \
    METRICS_PORT=9090

EXPOSE 8080 9090

# run the command to run the server
# CMD ["mvn", "spring-boot:run"]

# Env is set in the application.prperties file
CMD ["java","-jar","app.jar"] 