##首次编译
build:
	mvn clean install
	mvn -f smart-mqtt-broker/pom.xml clean install
	mvn -f smart-mqtt-maven-plugin/pom.xml clean install
	mvn -f plugins/pom.xml clean install


clean:
	mvn clean
	mvn -f smart-mqtt-broker/pom.xml clean
	mvn -f smart-mqtt-maven-plugin/pom.xml clean
	mvn -f plugins/pom.xml clean

# 当需要升级版本时，执行该命令
version=1.3.0
update_version:
	sed -i  '' 's/public static final String VERSION = ".*";/public static final String VERSION = "v${version}";/' smart-mqtt-plugin-spec/src/main/java/tech/smartboot/mqtt/plugin/spec/Options.java
	mvn versions:set -DnewVersion=${version} versions:commit
	mvn -f smart-mqtt-broker/pom.xml versions:set -DnewVersion=${version} versions:commit
	mvn -f smart-mqtt-bench/pom.xml versions:set -DnewVersion=${version} versions:commit
	mvn -f plugins/pom.xml versions:set -DnewVersion=${version} versions:commit
	mvn -f smart-mqtt-test/pom.xml versions:set -DnewVersion=${version} versions:commit
	mvn clean install
	mvn -f smart-mqtt-broker/pom.xml clean install
	mvn -f smart-mqtt-maven-plugin/pom.xml clean install
	mvn -f smart-mqtt-maven-plugin/pom.xml versions:use-dep-version -Dincludes=tech.smartboot.mqtt:smart-mqtt-broker -DdepVersion=${version} versions:commit