FROM maven:3.6.3-adoptopenjdk-8 AS build

WORKDIR /usr/src/app

# Firstly, copy only pom.xml and run `mvn go-offline`
# to cache dependencies for building.
COPY pom.xml .
RUN mvn -B -e -C -T 1C org.apache.maven.plugins:maven-dependency-plugin:3.1.1:go-offline

# Secondly, copy src folder and run `mvn package`.
# Unless you edit pom.xml, docker build starts from here.
COPY src src
RUN mvn -B -e -o -T 1C package -DskipTests

# Thirdly, create executable container image
# by copying built war file to Tomcat contianer image
FROM tomcat:9-jdk8-adoptopenjdk-hotspot AS publish

ENV JAVA_OPTS="\
        -Dio.personium.configurationFile=/personium/personium-core/conf/personium-unit-config.properties"

WORKDIR /usr/local/tomcat/webapps/
COPY --from=build /usr/src/app/target/personium-core.war .

WORKDIR /usr/local/tomcat/bin
EXPOSE 8080
ENTRYPOINT [ "./catalina.sh", "run" ]