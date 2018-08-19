#!/bin/bash

create_and_clear_folder() {
    folder="$1"
    if [ ! -d "$folder" ]; then
        mkdir "$folder"
    else
        rm "$folder"/*
    fi
}

create_and_clear_folder "processing"
create_and_clear_folder "output"

javac -cp .:opencv-341.jar process_image.java

if [ -z "$1" ]; then
    java -Djava.library.path=".:/home/leo/" -cp .:opencv-341.jar process_image datasets/sequence-1
else
    java -Djava.library.path=".:/home/leo/" -cp .:opencv-341.jar process_image "$1"
fi

rm *.class
