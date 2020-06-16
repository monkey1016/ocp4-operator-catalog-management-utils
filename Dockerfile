ARG JAVA_BASE_IMAGE=openjdk:8-jdk-alpine
LABEL maintainer="Lev Shulman <lshulman@redhat.com>"

FROM ${JAVA_BASE_IMAGE}
RUN mkdir /app

COPY operator-utils-api-service /app/ 
EXPOSE 8080
WORKDIR /app/
CMD ./mvnw spring-boot:run
