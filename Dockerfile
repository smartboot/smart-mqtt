FROM openjdk:18-alpine3.7
WORKDIR /
RUN curl -LO https://gitee.com/smartboot/smart-mqtt/releases/download/v0.5/smart-mqtt-broker-bin-0.5.tar.gz