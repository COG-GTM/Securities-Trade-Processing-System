#!/usr/bin/env bash
# Prepare a fresh Devin VM to build and test STPS: JDK 21, Maven, GCS Maven mirror, warm dependency cache.
set -euo pipefail
trap 'echo "setup-child: FAILED at line $LINENO" >&2' ERR
cd "$(dirname "$0")/.."
JDK=/usr/lib/jvm/java-21-openjdk-amd64
if [ ! -x "$JDK/bin/javac" ] || ! command -v mvn >/dev/null; then
  sudo apt-get update -qq
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-21-jdk-headless maven >/dev/null
fi
mkdir -p ~/.m2
[ -f ~/.m2/settings.xml ] && grep -q maven-central.storage-download.googleapis.com ~/.m2/settings.xml || cp swarm/maven-settings.xml ~/.m2/settings.xml
export JAVA_HOME=$JDK
mvn -q -B -s swarm/maven-settings.xml -Pswarm test-compile
mvn -q -B -s swarm/maven-settings.xml -Pswarm test -Dtest='TradeLifecycleRegressionTest' >/dev/null
echo "READY: JDK 21 + Maven warm; run tests with: export JAVA_HOME=$JDK; python3 swarm/swarm.py confirm"
