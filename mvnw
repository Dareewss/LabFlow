#!/bin/bash
MAVEN_HOME="/tmp/apache-maven-3.9.5"
export PATH="$MAVEN_HOME/bin:$PATH"
"$MAVEN_HOME/bin/mvn" "$@"
