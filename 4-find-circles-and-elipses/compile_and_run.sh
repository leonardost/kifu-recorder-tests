#!/bin/bash

if [ ! -z "processing" ]; then
    mkdir "processing"
fi
if [ ! -z "output" ]; then
    mkdir "output"
fi

rm processing/*
rm output/*

javac -cp .:opencv-341.jar find_circles.java
java -Djava.library.path=".:/home/leo/" -cp .:opencv-341.jar find_circles > results.log

rm *.class
