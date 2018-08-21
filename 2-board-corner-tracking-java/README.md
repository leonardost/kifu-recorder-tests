Corner tracking
===============

This experiment's goal is to develop and improve the corner tracking feature
of Kifu Recorder.

Kifu Recorder's (v1.1.8) board detector is able to detect the contour of the
board very well, especially with human assitance to say when it is correct and
when it is not. However, after the game watching phase begins, the contour
of the baord doesn't change anymore, so any movements to the camera or the
board results in many errors ocurring in the detection. Bumps or moves to the
board and camera are commonplace, so this situation must be addressed.

### Hypothesis 1

The contour of the board should not move much if the camera is resting on a
tripod, so we can adjust the corner's position by always checking the possible
corner points in the regions around the last detected corners. In a region of
interest of 50 pixels radius around each corner, we check for possible corner
points and choose the one which is closest to the center of the region.

The Harris corner detector was used to develop this solution.

### Hypothesis 2

On each frame we capture a region of interest around each corner. On the next
frame, we search for a region that most closely resembles the one we captured
in the frame before. When we find that region, we update the corner according
to the position of that region. This approach has not been tested yet.

### Auxiliary method - is the contour surrounding the board?

To check whether a set of corners is valid or not, a method to check if the
board is currently surrounded by the contour could be created. This method
would tell if the new found corners are valid. The feasability of this method
is being tested.

#### Method 1

My first thought is to see if the number of squares found inside the ortogonal
image is too different from the last image. In tests done with the 6 first
sequences of images, a difference of around 15 squares means that something
went wrong and the user should intervene.

#### Method 2

My second idea is to calculate the difference between the current ortogonal
image and the last ortogonal image. If the difference is too big, it means that
the camera must have moved more than it should. A reasonable threshould should
be decided after many empirical tests.

Results
-------

Hypothesis 1 works reasonably well if the camera moves very slowly, but as soon
as any abrupt movement is made, the corners are positioned wrongly. This could
be remedied by checking if the resulting contour really contains the board. If
it does not contain the board, the corners are not updated.

Running
-------

Tests can be run by running the compile_and_run.sh script. One of the
sequence image folders can be passed as a parameter to run the tests on them.

Log
---

#### 19/08/2018 00:15

Tests with sequences 1, 2 and 3 with algorithm 2 (java) version
2c91cb8ea0d0c6081ebcd03f9bcca1ad007c58b0 were 100% accurate, finding corners
correctly.

Sequence 4 had a mistake in frame 15, and sequence 6 already had an error in
frame 5, which persisted to the end. This may be the best subject to test
methods to improve robustness of the corner tracking.

#### 20/08/2018 22:50

I started developing the auxiliary method that checks if the board is contained
in the contour or not by counting the difference in the number of squares
found.
