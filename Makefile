version=1.1.1

update_version:
	mvn versions:set -DnewVersion=${version}
	mvn -f smart-mqtt-broker/pom.xml versions:set -DnewVersion=${version}
	mvn -f smart-mqtt-bench/pom.xml versions:set -DnewVersion=${version}
	mvn -f plugins/pom.xml versions:set -DnewVersion=${version}

package:
	mvn clean install
	mvn -f smart-mqtt-broker/pom.xml clean install
	mvn -f smart-mqtt-maven-plugin/pom.xml clean install


clean:
	mvn clean
	mvn -f smart-mqtt-broker/pom.xml clean
	mvn -f smart-mqtt-maven-plugin/pom.xml clean
	mvn -f plugins/pom.xml clean