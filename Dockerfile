FROM openjdk:11-jre-bullseye

ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update && \
  apt-get --no-install-recommends -y install \
    wget unzip && \
  wget https://dl.google.com/linux/chrome/deb/pool/main/g/google-chrome-stable/google-chrome-stable_100.0.4896.127-1_amd64.deb -O chrome.deb && \
  wget https://chromedriver.storage.googleapis.com/100.0.4896.60/chromedriver_linux64.zip && \
  apt install -y ./chrome.deb && \
  unzip chromedriver_linux64.zip && \
  mv chromedriver /usr/local/bin/ && \
  rm /chrome* && \
  apt-get remove -y wget unzip && \
  apt-get autoremove -y && \
  apt-get clean && \
  rm -rf /var/lib/apt/lists/* 

COPY ./target/scala-2.13/rbnz.jar rbnz.jar

ENTRYPOINT ["java", "-cp", "rbnz.jar"] 