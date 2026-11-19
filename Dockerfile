# Build the Java 11 Maven WAR
FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# Run on Tomcat 9 because this application uses javax.servlet
FROM tomcat:9.0-jdk11-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /workspace/target/advocate-portal.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
