#!/bin/bash

javac -cp .:opencv-341.jar calculate_similarity.java
java -Djava.library.path=".:/home/leo/" -cp .:opencv-341.jar calculate_similarity > results.log

rm *.class
