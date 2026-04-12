# Initial Docker support thanks to xinyifly
# Optimisation performed by wejrox

FROM maven:3.9.14-amazoncorretto-21-al2023 AS dev

#
# Cosmic JAR creation stage
#
FROM dev AS jar

# Build in a separated location which won't have permissions issues.
WORKDIR /opt/cosmic

# Any changes to the pom will affect the entire build, so it should be copied first.
COPY pom.xml ./pom.xml

# Grab all the dependencies listed in the pom early, since it prevents changes to source code from requiring a complete re-download.
# Skip compiling tests since we don't want all the dependencies to be downloaded.
# RUN mvn -f ./pom.xml clean dependency:go-offline -Dmaven.test.skip -T 1C
# TODO: The above command stopped working as of Java 21 upgrade due to:
# Failed to execute goal org.apache.maven.plugins:maven-dependency-plugin:3.6.1:go-offline (default-cli) on project Cosmic: org.eclipse.aether.resolution.DependencyResolutionException: The following artifacts could
# not be resolved: io.netty:netty-tcnative:jar:${os.detected.classifier}:2.0.65.Final (absent): Could not find artifact io.netty:netty-tcnative:jar:${os.detected.classifier}:2.0.65.Final in central (https://repo.maven.apache.org/maven2) -> [Help 1]

# Source code changes may not change dependencies, so it can go last.
# Skip compiling tests since we don't want all the dependecies to be downloaded for plugins.
COPY src ./src
RUN mvn -f ./pom.xml clean package -Dmaven.test.skip -T 1C

#
# Server creation stage
#
FROM amazoncorretto:21

# Host the server in a location that won't have permissions issues.
WORKDIR /opt/server
# Copy the wizet files first since they're so big and won't change often.
COPY wz ./wz
# Copy the JAR we build earlier.
COPY --from=jar /opt/cosmic/target/Cosmic.jar ./Server.jar
# Scripts are sourced on server startup, so you can mount over them for quicker redeploy.
COPY scripts ./scripts/
# Config is read on server startup, so you can mount over it for quicker redeploy.
COPY config.yaml ./
# Default exposure, although not required if using docker compose.
# This exposes the login server, and channels.
# Format for channels: WWCC, where WW is 75 plus the world number and CC is 75 plus the channel number (both zero indexed).
EXPOSE 8484 7575 7576 7577
ENTRYPOINT ["java", "-jar", "./Server.jar"]
