FROM maven:3.6.3-adoptopenjdk-8 AS build

WORKDIR /usr/src/app
COPY pom.xml .
RUN mvn -B -e -C -T 1C org.apache.maven.plugins:maven-dependency-plugin:3.1.1:go-offline

COPY src src
RUN mvn -B -e -o -T 1C package -DskipTests

FROM tomcat:9-jdk8-adoptopenjdk-hotspot AS publish

ENV JAVA_OPTS="\
        -Dio.personium.configurationFile=/personium/personium-core/conf/personium-unit-config.properties"

WORKDIR /usr/local/tomcat/webapps/
COPY --from=build /usr/src/app/target/personium-core.war .

WORKDIR /usr/local/tomcat/bin
EXPOSE 8080
ENTRYPOINT [ "./catalina.sh", "run" ]