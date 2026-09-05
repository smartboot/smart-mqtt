#!/bin/sh
dir=$(dirname "$0")
#export SMART_MQTT_PLUGINS=${dir}/plugins
export BROKER_PORT=1883
export BROKER_HOST="0.0.0.0"
#export BROKER_THREADNUM=16

# A larger maxInflight value helps improve the communication performance of QoS 1 and QoS 2 in high-concurrency scenarios,
# but it also increases memory overhead to a certain extent. default is 8
#export BROKER_MAXINFLIGHT=256

export BROKER_MAXPACKETSIZE=8192

# 检查是否已经有 smart-mqtt.jar 进程在运行
if pgrep -f "smart-mqtt.jar" > /dev/null; then
    echo "smart-mqtt.jar 进程已存在，请先退出该进程。"
    exit 1
fi

# 检查是否有 -d 参数
if [ "$1" = "-d" ]; then
    # 如果有该参数，将 Java 进程放到后台运行
    java -jar "${dir}/smart-mqtt.jar" &
else
    # 没有该参数，正常运行
    java -jar "${dir}/smart-mqtt.jar"
fi