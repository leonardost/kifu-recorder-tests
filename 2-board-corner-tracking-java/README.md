19/08/2018 12:15

Tests with sequencias 1, 2 and 3 with algorithm 2 (java) version
2c91cb8ea0d0c6081ebcd03f9bcca1ad007c58b0 were 100% accurate, finding corners
correctly. Tests with sequences 4 and 6 had errors when the image moved too
abruptly.

Sequence 4 had a mistake in frame 15, and sequence 6 already had an error in
frame 5, which persisted to the end. This may be the best subject to test
methods to improve robustness of the corner tracking.
