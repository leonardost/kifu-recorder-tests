#!/bin/bash

if [ ! -d "processing" ]; then
    mkdir "processing"
    mkdir "processing/approximated_contours"
fi
if [ ! -d "output" ]; then
    mkdir "output"
fi

rm processing/*
rm processing/approximated_contours/*
rm output/*

javac -cp .:opencv-341.jar find_circles.java
java -Djava.library.path=".:/home/leo/" -cp .:opencv-341.jar find_circles > results.log

rm *.class
