### 下载代码
FROM alpine AS download
ENV VERSION=0.6
WORKDIR /
RUN apk add git curl
RUN curl -LO https://gitee.com/smartboot/smart-mqtt/releases/download/v${VERSION}/smart-mqtt-broker-bin-${VERSION}.tar.gz
RUN tar xzvf smart-mqtt-broker-bin-${VERSION}.tar.gz
RUN mv smart-mqtt-broker-bin-${VERSION} smart-mqtt


FROM openjdk:8-alpine3.9
WORKDIR /
COPY --from=download /smart-mqtt /smart-mqtt
EXPOSE 1883
ENTRYPOINT ["sh","/smart-mqtt/bin/start.sh"]