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

java -Djava.library.path=".:/home/leo/" -cp .:lib/opencv-341.jar process_image 1 > results1.log
java -Djava.library.path=".:/home/leo/" -cp .:lib/opencv-341.jar process_image 2 > results2.log
java -Djava.library.path=".:/home/leo/" -cp .:lib/opencv-341.jar process_image 3 > results3.log

find . -name \*.class -type f -delete 
