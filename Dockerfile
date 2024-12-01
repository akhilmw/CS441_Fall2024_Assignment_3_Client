# Use a base image with Java and SBT
FROM hseeberger/scala-sbt:11.0.12_1.5.5_2.13.6

# Set working directory
WORKDIR /app

# Copy build.sbt and project files
COPY build.sbt .
COPY project project

# Copy source code
COPY src src

# Build the application
RUN sbt clean assembly

# Command to run the application
CMD ["java", "-jar", "target/scala-2.13/CS441_Fall2024_Assignment_3_Client-assembly-0.1.0-SNAPSHOT.jar"]