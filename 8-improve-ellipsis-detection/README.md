Improve ellipses detection
==========================

This will improve corner detection.

Samples
-------

### iamge1.jpg

corner_region_2_frame77.jpg from sequence-14, got from running command
`./compile_and_run.sh datasets/sequence-14/` in folder
`2-board-corner-tracking-java` on commit
9604c20ac8c7b7d7382888b088b5907f95fc7fca.

It corresponds to the region from (1139, 131) to (1219, 211) of frame 77.
Shows 3 stones and 1 corner of the board.

### image2.jpg

corner_region_4_frame18.jpg from sequence-13, got from running command
`./compile_and_run.sh datasets/sequence-13` in folder
`2-board-corner-tracking-java` on commit
394bd1e107b4863501211a999343745df27b9fe1.

It corresponds to the region from (291, 921) to (371, 1001) of frame 18.
Shows a white stone partially covered by a finger. The detector should
ignore this image or detect a circle.

### image3.jpg

corner_region_1_frame40.jpg from sequence-15, got from running command
`./compile_and_run.sh datasets/sequence-15` in folder
`2-board-corner-tracking-java` on commit
394bd1e107b4863501211a999343745df27b9fe1.

It correponds to the region from (405, 123) to (485, 203) of frame 40.
Shows parts of two white stones at the positions adjacent to the top
left corner.

### image4.jpg

corner_region_2_frame29.jpg from sequence-14, got from running command
`./compile_and_run.sh datasets/sequence-14/` in folder
`2-board-corner-tracking-java` on commit
95a9c279ee507c5699755d2d43e83534a014536c.

It corresponds to the region from (397, 128) to (477, 208) of frame 29.
Show a single white stone on the upper right corner of the board. The
corner detector detected two Harris corner points on the boundaries of
the stone, which should not have been detected. This happened because
the detected stone ellipse did not cover the stone entirely.

### image5.jpg

corner_region_1_frame17.jpg from sequence-15, got from running command
`./compile_and_run.sh datasets/sequence-15` in folder
`2-board-corner-tracking-java` on commit
ea2b39ab469dca35ab623595a6571f6d0d745284.

It corresponds to the region from (405, 123) to (485, 203) of frame 17.
It shows a black stone and a white stone partially covered by a finger.

### image6.jpg

corner_region_1_frame18.jpg from sequence-15, got from running command
`./compile_and_run.sh datasets/sequence-15` in folder
`2-board-corner-tracking-java` on commit
ea2b39ab469dca35ab623595a6571f6d0d745284.

It shows a black stone, half of a white stone and a little bit of a
shadow cast by a finger outside the image view.

### image7.jpg

corner_region_1_frame38.jpg from sequence-15, got from running command
`./compile_and_run.sh datasets/sequence-15` in folder
`2-board-corner-tracking-java` on commit
ea2b39ab469dca35ab623595a6571f6d0d745284.

It shows a black stone partially covered by a shadow that comes from
outside the board and half of a white stone.

Log
---

#### 30/05/2019 21:52

Creating this experiment. 
