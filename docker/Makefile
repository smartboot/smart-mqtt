VERSION=0.6

init:
	docker buildx create --use --name smartbuilder
	docker buildx inspect smartbuilder --bootstrap

docker:
	docker buildx build --platform=linux/amd64,linux/arm64 -t smartboot/smart-mqtt:$(VERSION) . --push

clear:
	docker buildx rm smartbuilder