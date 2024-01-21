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

# Set environment variables at the container level
ENV dbHost=${DB_HOST}
ENV dbUser=${DB_USER}
ENV dbPassword=${DB_PASSWORD}
ENV dbPort=${DB_PORT}
ENV dbName=${DB_NAME}
ENV dbType=${DB_TYPE}
ENV emailFilter=${EMAIL_FILTER}
ENV emailUser=${EMAIL_USER}
ENV emailSummaryCC=${EMAIL_SUMMARY_CC}
ENV emailSummaryTo=${EMAIL_SUMMARY_TO}
#ENV azureStorageConnectionString=${AZURE_STORAGE_CONNECTION_STRING}
#ENV azureStorageContainerName=${AZURE_STORAGE_CONTAINER_NAME}

# Expose ports
EXPOSE 9091

# Set the entry point to run the jar
ENTRYPOINT ["java","-jar","EmailToDb.jar"]
