# Stage 1: Build with Maven
FROM maven:3.8.4-openjdk-17 as builder

# Copy your project's source code into the Docker image
COPY . /usr/src/myapp

# List the contents of the /usr/src/myapp directory to verify the copy
RUN ls -lrt /usr/src/myapp

# Set the working directory
WORKDIR /usr/src/myapp

# Run Maven install to build your application
RUN mvn clean install -DskipTests

# Stage 2: Setup the runtime environment
FROM openjdk:17

# Copy the JAR file from the builder stage
COPY --from=builder /usr/src/myapp/target/EmailToDb-0.0.1-SNAPSHOT.jar /usr/app/EmailToDb.jar

# Set the working directory
WORKDIR /usr/app

# Set the entry point to run the jar
ENTRYPOINT ["java","-jar","EmailToDb.jar"]