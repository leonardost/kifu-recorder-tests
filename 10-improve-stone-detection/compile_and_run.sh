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

javac -cp .:lib/opencv-341.jar process_image.java

if [ -z "$1" ]; then
    java -Djava.library.path=".:/home/leo/" -cp .:lib/opencv-341.jar process_image datasets/orthogonal-boards-sequence-15 > results.log
else
    java -Djava.library.path=".:/home/leo/" -cp .:lib/opencv-341.jar process_image "$1" > results.log
fi

find . -name \*.class -type f -delete 
