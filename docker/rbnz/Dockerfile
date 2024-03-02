FROM ubuntu:23.10

ARG CHROME_VERSION=122.0.6261.57
ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update && \
  apt-get --no-install-recommends -y install \
    wget unzip gdebi ca-certificates openjdk-11-jre-headless && \
  wget --quiet \
    https://dl.google.com/linux/chrome/deb/pool/main/g/google-chrome-stable/google-chrome-stable_${CHROME_VERSION}-1_amd64.deb \
    -O chrome.deb && \
  gdebi --non-interactive chrome.deb && \
  wget  --quiet \
    https://storage.googleapis.com/chrome-for-testing-public/${CHROME_VERSION}/linux64/chromedriver-linux64.zip && \
  unzip chromedriver-linux64.zip && \
  mv chromedriver-linux64/chromedriver /usr/local/bin/ 

# COPY ./target/scala-2.13/rbnz.jar rbnz.jar

ENTRYPOINT ["java", "-cp", "rbnz.jar"]