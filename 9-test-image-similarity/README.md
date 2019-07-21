Test image similarity
=====================

This experiment tests image similarity metrics.

The corner image samples are from running
`./compile_and_run.sh datasets/sequence-14` on experiment 2 on commit
9a4513a022f649b319faf63034c2dfdcd93395a6.

Input images in folder `input2` were obtained by running
`./compile_and_run.sh datasets/sequence-16` on experiment 2 on commit
ef51c5c2f2091b0977a0898c2fb2fd1ba80f3b95.

Input images in folder `input3` were obtained by running
`./compile_and_run.sh datasets/sequence-14` on experiment 2 on commit
acdd7d04c2f74b4507cdadba44585f95010358a6

Log
---

#### 25/06/2019 20:40

Feature matching does not seem to work well with small images (80x80).
Template matching and fingerprint matching did better in our tests.

#### 18/06/2019 20:44

FinderprintMatching is very sensitive to brightness, as it compares
pixel values directly, but it seems like a very good detector to see
when something strange is in the scene. Images where hands appear are
considered very different from images where only the board and stones
are visible.

#### 17/06/2019 23:20

Adding images 92 (doge) and 93 (apple) as baselines to check what
results of comparisons with really different images look like.

#### 13/06/2019 23:26

Creating this experiment.
