# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first (only re-downloads when pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build the application
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create a non-root user
RUN groupadd -r millapp && useradd -r -g millapp millapp

COPY --from=build /app/target/mill-system-backend-1.0.0.jar app.jar
RUN chown -R millapp:millapp /app
USER millapp

EXPOSE 3001

ENTRYPOINT ["java", "-jar", "app.jar"]
