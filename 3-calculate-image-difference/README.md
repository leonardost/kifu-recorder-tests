Calculate image similarity
==========================

This experiment's goal is to compare various image similarity metrics in order
to improve Kifu Recorder's corner tracking algorithm. This metric will be used
to check if the board is still inside the image contour or not, by measuring
how different the images are.

Image pairs 1, 2 and 3 should be very similar, almost identical, and image
pairs 4 and 5 should have more differences. Image pair 0 is identical, they are
literally the same image, and can be used as a comparison baseline. Image pair
8 is very different, one is a go board and the other is a cat (image taken from
https://www.pexels.com/photo/cat-whiskers-kitty-tabby-20787/), so this should
be the most dissimilar pair. However, the cat has somewhat the same color of
the board, so, although from a image feature point of view it is very different
from the board, from a histogram perspective it may be close enough (which was
confirmed by the experiment). Image pairs 6 and 7 have some brightness
differences and an added stone.

Image similarity metrics that were tested
-----------------------------------------

- L2 relative error
- L2 relative error with grayscale images
- Template matching (ccoeff)
- Template matching (sqdiff)
- ORB feature matching (average distance of the best 15 descriptor matches)

Results
-------

L2 relative error is not a good measure of similarity. According to it, image
pair 6 (the cat) is more similar than image pair 5, which features a go baord
with a person's arm over it. Actually, even template matching thought the cat
was more similar to the board than image pair 5, which is funny.

The only metric that really noticed the cat was very different from the other
images was feature matching.

Running
-------

Tests can be run by running the compile_and_run.sh script without parameters.
