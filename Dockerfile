FROM openjdk:8-jdk-alpine
RUN mkdir /app
COPY mvnw /app/mvnw
COPY .mvn /app/.mvn
COPY pom.xml /app/pom.xml
COPY src /app/src
EXPOSE 8080
WORKDIR /app/
CMD ./mvnw spring-boot:run
