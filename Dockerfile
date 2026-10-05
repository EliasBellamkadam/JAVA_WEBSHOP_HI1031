FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode package

FROM tomcat:10.1.60-jdk25-temurin
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /build/target/webshop.war /usr/local/tomcat/webapps/webshop.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
