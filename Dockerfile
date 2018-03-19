FROM sgametrio/wsd-environment as wsd-env
# Included because has built-in things I need

# source code builder image
FROM maven:3.5-jdk-8 as builder
ENV MAVEN_OPTS=-Dmaven.repo.local=/.m2/
# Set current working directory
WORKDIR /app
COPY pom.xml .
RUN mvn -B install
COPY . .
# Include resources in .jar?
COPY --from=wsd-env /app/evaluation-datasets src/main/resources
COPY --from=wsd-env /app/GLKH-1.0 src/main/resources
RUN mvn -B -o -T 1C package -DskipTests


# Executor image (testing build)
FROM openjdk:8-jre-alpine
COPY --from=builder /app/target/*.jar /app/app.jar
COPY --from=wsd-env /usr/local/WordNet-3.0 /usr/local/

WORKDIR /app
# RUN java command to execute jar
CMD [ "java", "-jar", "./app.jar" ]