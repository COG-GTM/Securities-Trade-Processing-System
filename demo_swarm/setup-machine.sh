#!/usr/bin/env bash
# Presenter or Devin VM prep for demo_swarm: JDK 21, Maven, GCS Maven mirror (Maven Central rate limits), warm cache.
set -euo pipefail
trap 'echo "setup-machine: FAILED at line $LINENO" >&2' ERR
KIT="$(cd "$(dirname "$0")" && pwd)"
JDK=/usr/lib/jvm/java-21-openjdk-amd64
if [ ! -x "$JDK/bin/javac" ] || ! command -v mvn >/dev/null; then
  sudo apt-get update -qq
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-21-jdk-headless maven >/dev/null
fi
mkdir -p ~/.m2
if ! grep -qs maven-central.storage-download.googleapis.com ~/.m2/settings.xml; then
  cp "$KIT/overlay/swarm/maven-settings.xml" ~/.m2/settings.xml
fi
command -v gh >/dev/null || { echo "setup-machine: gh CLI is required" >&2; exit 1; }
echo "READY: JAVA_HOME=$JDK, $(mvn -v | head -1), mirror in ~/.m2/settings.xml"
