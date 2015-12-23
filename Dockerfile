FROM java:latest

RUN apt-get update && \
    apt-get install -y \
            ant \
            cmake \
            g++ \
            libboost-all-dev \
            libz-dev \
            make


RUN mkdir /opt/joshua
WORKDIR /opt/joshua

# set environment variables
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
ENV JOSHUA=/opt/joshua


# copy Joshua source code to image
COPY . $JOSHUA

RUN ant
