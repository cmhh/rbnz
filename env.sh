#! /usr/bin/env bash

echo "GID=$(id -g)" > .env
echo "UID=$(id -u)" >> .env
chmod 600 .env