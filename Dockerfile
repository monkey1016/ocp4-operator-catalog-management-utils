ARG JAVA_BASE_IMAGE=openjdk:8-jdk-alpine
FROM ${JAVA_BASE_IMAGE}

LABEL maintainer="Lev Shulman <lshulman@redhat.com>"

RUN mkdir /app && mv operator-catalog-tools-*.jar /app/app.jar
EXPOSE 8080
WORKDIR /app/
CMD ["java", "-jar", "/app/app.jar"]
