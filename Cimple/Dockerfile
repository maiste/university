FROM ubuntu:bionic

COPY . root/

RUN apt update \
      && apt install -y \
      gcc \
      valgrind \
      libsdl2-dev \
      libsdl2-image-dev \
      libcmocka-dev \
      libjpeg-dev \
      libpng-dev \
      libreadline-dev
