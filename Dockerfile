FROM ubuntu:15.04

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y  software-properties-common && \
    add-apt-repository ppa:webupd8team/java -y && \
    apt-get update && \
    echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
    apt-get install -y oracle-java8-installer && \
    apt-get clean

ADD . /dead-code-detector

WORKDIR /dead-code-detector

ENV MEMORY 512

EXPOSE 8080

RUN ./gradlew clean build

CMD java -Xmx${MEMORY}m -Djava.security.egd=file:/dev/./urandom -jar build/libs/dead-code-detector.jar