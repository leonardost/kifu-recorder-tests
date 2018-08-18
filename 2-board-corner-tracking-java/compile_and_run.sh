#!/bin/bash

if [ ! -d "processing" ]; then
    mkdir processing
else
    rm processing/*
fi

if [ ! -d "output" ]; then
    mkdir output
else
    rm output/*
fi

javac -cp .:opencv-341.jar process_image.java
java -Djava.library.path=".:/home/leo/" -cp .:opencv-341.jar process_image \
    input 10 1193 202 1294 822 563 855 601 203
rm *.class
