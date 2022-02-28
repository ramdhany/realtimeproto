FROM openjdk:17-jdk-alpine AS build 
# ---- 
# Install Maven 
RUN apk add --no-cache curl tar bash 
ARG MAVEN_VERSION=3.8.4 
ARG USER_HOME_DIR="/root" 

RUN wget -q -O /etc/apk/keys/sgerrand.rsa.pub https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub
RUN wget https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.34-r0/glibc-2.34-r0.apk
RUN apk add glibc-2.34-r0.apk

RUN mkdir -p /usr/share/maven && \ 
    curl -fsSL http://apache.osuosl.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar -xzC /usr/share/maven --strip-components=1 && \ 
    ln -s /usr/share/maven/bin/mvn /usr/bin/mvn 
ENV MAVEN_HOME /usr/share/maven 
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2" 

# speed up Maven JVM a bit 
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1" 
# ---- 
# Install project dependencies and keep sources 
# make source folder 
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app 

# install maven dependency packages (keep in image) 
COPY pom.xml /usr/src/app 
RUN mvn -T 1C install && rm -rf target 

# copy other source files (keep in image) 
COPY src/ /usr/src/app/src

RUN mvn -T 1C install

# Base Alpine Linux based image with OpenJDK JRE only 
FROM openjdk:17-jdk-alpine
LABEL version="2.0.0"
# Create app directory
RUN mkdir -p /usr/src/server
WORKDIR /usr/src/server

COPY --from=build /usr/src/app/target/numberseqgenerator-0.0.1-jar-with-dependencies.jar /usr/src/server/numberseqgenerator-0.0.1-jar-with-dependencies.jar


EXPOSE 5000
 
ENTRYPOINT ["java", "-jar", "/usr/src/server/numberseqgenerator-0.0.1-jar-with-dependencies.jar"]
