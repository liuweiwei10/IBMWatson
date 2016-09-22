#!/bin/bash
javac -classpath .:lucene-core-5.3.1.jar:lucene-queryparser-5.3.1.jar:lucene-analyzers-common-5.3.1.jar TestAll.java

java -classpath .:lucene-core-5.3.1.jar:lucene-queryparser-5.3.1.jar:lucene-analyzers-common-5.3.1.jar TestAll "$@"
