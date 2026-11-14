# Builds the mod with JDK 25 (the Minecraft 26.x toolchain needs it). docker-compose.yml runs it
# with build/libs bind-mounted, so the jar lands on the host.
FROM eclipse-temurin:25-jdk
WORKDIR /src
COPY . .
CMD ["sh", "./gradlew", "build", "--no-daemon"]
