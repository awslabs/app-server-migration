#!/usr/bin/env bash

set -o pipefail
set -o nounset
set -o errexit

# DESCRIPTION:
# 
# Tools Installed:
#   Java 8
#   Docker
#   Maven
#   git
# 
# USAGE: 
# chmod +x setup.sh
# ./setup.sh

## java
java_install(){
if type -p java; then
    echo found java executable in PATH
else
    echo "no java present on machine, installing openjdk"
    sudo $PACKAGE_MANAGER install -y $JAVA_LIB
    echo "export JAVA_HOME=`dirname $(dirname $(readlink -f $(which javac)))`" >> ~/.bashrc
fi
version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo version of java "$version"
}
## docker, git
#sudo amazon-linux-extras install -y docker
docker_git_install(){
#$PACKAGE_MANAGER update -y
$PACKAGE_MANAGER install -y $DOCKER_LIB git
  sudo service docker start
  sudo usermod -a -G docker `whoami`
}


## maven
maven_install() {
if type -p mvn; then
    echo found mvn executable in PATH
else
   $PACKAGE_MANAGER install maven -y
fi
}

app_server_mig_install() {
# app-server-migration
mvn clean package install
}

arch=$(uname -m)
if [[ $arch == x86_64* ]]; then
    echo "X64 Architecture"
else
  echo "supporting only x86 architecture" && exit 1
fi

if command -v apt-get >/dev/null; then
    echo "apt-get is used"
    PACKAGE_MANAGER="sudo apt-get"
    JAVA_LIB="openjdk-8-jdk"
    DOCKER_LIB="docker.io"
    java_install
    docker_git_install
    maven_install
elif command -v yum >/dev/null; then
    echo "yum is used"
    PACKAGE_MANAGER="sudo yum"
    JAVA_LIB="java-1.8.0-openjdk java-1.8.0-openjdk-devel.x86_64"
    DOCKER_LIB="docker"
    java_install
    docker_git_install
    maven_install
elif command -v brew >/dev/null; then
    echo "brew is used"
    PACKAGE_MANAGER="brew"
    JAVA_LIB="java"
    DOCKER_LIB="docker"
    java_install
    brew install docker git maven
    open -a docker
else
  echo "package manager not known"
fi

app_server_mig_install