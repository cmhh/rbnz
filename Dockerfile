FROM ubuntu:20.04

ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update && \
  apt-get --no-install-recommends -y install \
    wget unzip gdebi ca-certificates openjdk-11-jre-headless && \
  wget --quiet \
    https://dl.google.com/linux/chrome/deb/pool/main/g/google-chrome-stable/google-chrome-stable_102.0.5005.61-1_amd64.deb \
    -O chrome.deb && \
  wget --quiet \
    https://chromedriver.storage.googleapis.com/102.0.5005.61/chromedriver_linux64.zip && \
  gdebi --non-interactive chrome.deb && \
  unzip chromedriver_linux64.zip && \
  mv chromedriver /usr/local/bin/ && \
  rm /chrome* 

COPY ./target/scala-2.13/rbnz.jar rbnz.jar

ENTRYPOINT ["java", "-cp", "rbnz.jar"]