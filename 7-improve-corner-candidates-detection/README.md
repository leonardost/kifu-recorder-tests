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
