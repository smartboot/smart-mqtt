package:
	mvn clean install
	mvn -f smart-mqtt-broker/pom.xml clean install
	mvn -f smart-mqtt-maven-plugin/pom.xml clean install


clean:
	mvn clean
	mvn -f smart-mqtt-broker/pom.xml clean
	mvn -f smart-mqtt-maven-plugin/pom.xml clean
	mvn -f smart-mqtt-plugin-enterprise/pom.xml clean