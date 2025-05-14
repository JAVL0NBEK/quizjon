# Use an appropriate base image that contains the JDK and other necessary tools
FROM openjdk:19-jdk-alpine

# Set the working directory in the container
# Set the working directory in the container
WORKDIR /quiz

# Copy the Maven dependency file and other necessary files
COPY build.gradle .
COPY src /quiz/src

# Copy the JAR file to the desired location
COPY build/libs/quiz-0.0.1-SNAPSHOT.jar quiz-0.0.1-SNAPSHOT.jar

# Expose the port the application runs on
EXPOSE 8080

# Run the jar file
CMD ["java", "-jar", "quiz-0.0.1-SNAPSHOT.jar", "--spring.profiles.active=prod"]
