FROM openjdk:11-jre-slim

WORKDIR /opt

COPY build/libs/*-all.jar ./sys-buyer.jar

CMD ["/bin/sh", "-c", "java -jar sys-buyer.jar"]