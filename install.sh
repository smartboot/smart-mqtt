#!/bin/sh

mvn clean install -DskipTests
mvn -f smart-mqtt-broker/pom.xml clean install -DskipTests
mvn -f smart-mqtt-maven-plugin/pom.xml clean install -DskipTests