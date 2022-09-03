VERSION=0.5
docker:
	docker build --no-cache -t smart-mqtt:$(VERSION) .