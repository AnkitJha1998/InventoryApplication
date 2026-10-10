# Build the service with the repository's Java 21 Maven project.
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

# Keep the runtime image smaller than the build image.
FROM eclipse-temurin:21-jre

WORKDIR /app
RUN useradd --system --create-home spring
COPY --from=build /build/target/*.jar /app/app.jar

USER spring
EXPOSE 8083
ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
