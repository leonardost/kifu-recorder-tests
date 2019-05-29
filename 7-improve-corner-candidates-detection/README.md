Improve corner candidates detection
===================================

This is one of most important steps done when tracking the board's corners.
This experiment's goal is to improve only this phase of the algorithm.

Samples
-------

### iamge1.jpg

corner_region_2_frame77.jpg from sequence-14, got from running command
`./compile_and_run.sh datasets/sequence-14/` in folder
`2-board-corner-tracking-java` on commit
9604c20ac8c7b7d7382888b088b5907f95fc7fca.

It corresponds to the region from (1139, 131) to (1219, 211) of frame 77.
Shows 3 stones and 1 corner of the board. The detector should detect at
least some of the stones and the board corner as possible candidates.

### image2.jpg

corner_region_4_frame18.jpg from sequence-13, got from running command
`./compile_and_run.sh datasets/sequence-13` in folder
`2-board-corner-tracking-java` on commit
394bd1e107b4863501211a999343745df27b9fe1.

It corresponds to the region from (291, 921) to (371, 1001) of frame 18.
Shows a white stone partially covered by a finger. The detector should
ignore this image or detect a circle. The detector in the aforementioned
commit centered in one of the edges around the circle.

### image3.jpg

corner_region_1_frame40.jpg from sequence-15, got from running command
`./compile_and_run.sh datasets/sequence-13` in folder
`2-board-corner-tracking-java` on commit
394bd1e107b4863501211a999343745df27b9fe1.

It correponds to the region from (405, 123) to (485, 203) of frame 40.
Shows parts of two white stones at the positions adjacent to the top
left corner. The detector should detect the empty corner and maybe the
two circles, but the detector saw an ellipse in the empty square that
forms the corner.

Log
---

#### 26/05/2019 17:26

Corner Harris detection was impproved. The previous PointCluster
implementation used an arbitraty distance to clusterize points that  were
near it, which caused separate corner candidates to be clusterized together.
Now, a depth-first search is done in the image to get the different possible
corner regions.

#### 25/05/2019 23:45

Creating this experiment.
