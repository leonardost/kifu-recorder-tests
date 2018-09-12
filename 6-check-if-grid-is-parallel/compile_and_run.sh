#!/bin/bash

source_file=check_if_grid_is_parallel

if [ ! -z "processing" ]; then
    mkdir "processing"
fi
if [ ! -z "output" ]; then
    mkdir "output"
fi

rm processing/*
rm output/*

javac -cp .:opencv-341.jar "$source_file".java
java -Djava.library.path=".:/home/leo/" -cp .:opencv-341.jar "$source_file" "$1" > results.log

find . -name \*.class -type f -delete 
