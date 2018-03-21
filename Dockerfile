FROM sgametrio/wsd-environment as wsd-env
# Included because has built-in things I need

# source code builder image
FROM maven:3.5-jdk-8 as builder
ENV MAVEN_OPTS=-Dmaven.repo.local=/.m2/
# Set current working directory
WORKDIR /app
COPY pom.xml .
RUN mvn -B install
COPY src src
RUN mvn -B -o package -DskipTests


# Executor image (testing build)
FROM openjdk:8-jre
COPY --from=builder /app/target/wordsensedisambiguation-0.0.1-SNAPSHOT-jar-with-dependencies.jar /app/target/app.jar
COPY --from=wsd-env /usr/local/WordNet-3.0 /usr/local/WordNet-3.0

WORKDIR /app/target
COPY --from=wsd-env /app/evaluation-datasets /app/target/src/main/resources/evaluation-datasets
COPY --from=wsd-env /app/GLKH-1.0 /app/target/src/main/resources/GLKH-1.0

CMD [ "java", "-jar", "app.jar" ]