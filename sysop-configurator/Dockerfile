FROM openjdk:11-jre-slim

WORKDIR /opt

COPY build/libs/*-all.jar ./sysop-configurator.jar

CMD ["/bin/sh", "-c", "java -jar sysop-configurator.jar"]