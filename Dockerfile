# choose some image as the base image
FROM mcr.microsoft.com/openjdk/jdk:25-ubuntu

# navigate to working directory
WORKDIR /app

# copy the entire directory
COPY . .

# create environment variable that can be changed during runtime
ENV MODEL_HOST=http://host.docker.internal:8081

# install system requirements and expose the port
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
EXPOSE 8080

# run the command to run the server
CMD ["mvn", "spring-boot:run"]