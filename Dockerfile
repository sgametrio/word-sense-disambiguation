# Builder image
FROM maven:3.5-jdk-8 as builder
ENV MAVEN_OPTS=-Dmaven.repo.local=/.m2/
# Set current working directory
WORKDIR /app
COPY pom.xml .
RUN mvn -B install
COPY . .
RUN mvn -B -o -T 1C package -DskipTests

# Executor image
FROM openjdk:8-jre-alpine
COPY --from=builder /app/target/*.jar /app/app.jar
WORKDIR /app
# RUN java command to execute jar
RUN java -jar ./app.jar

# Use an official Python runtime as a parent image
FROM maven:3.5.3-jdk-10

# Set the working directory to /app
#WORKDIR /app

# Copy the current directory contents into the container at /app
#ADD . /app

# Install any needed packages specified in requirements.txt
#RUN pip install --trusted-host pypi.python.org -r requirements.txt

# Make port 80 available to the world outside this container
#EXPOSE 80

# Define environment variable
#ENV NAME World

# Run app.py when the container launches
#CMD ["python", "app.py"]