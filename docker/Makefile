VERSION=0.6

init:
	docker buildx create --use --name smartbuilder
	docker buildx inspect smartbuilder --bootstrap

docker:
	docker buildx build -t smartboot/smart-mqtt:$(VERSION) -t smartboot/smart-mqtt:latest --platform=linux/amd64,linux/arm64 . --push

clear:
	docker buildx rm smartbuilder