package src;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;

public class OrbFeaturesMatching implements SimilarityCalculatorInterface
{
    private int imageNumber;

    public void setImageNumber(int imageNumber)
    {
        this.imageNumber = imageNumber;
    }

    /**
     * https://stackoverflow.com/questions/34762865/comparing-two-images-with-opencv-in-java
     * https://stackoverflow.com/questions/24569386/opencv-filtering-orb-matches
     * Copying this from experiment 3
     * https://docs.opencv.org/master/javadoc/index.html
     * https://www.learnopencv.com/image-alignment-feature-based-using-opencv-c-python/
     * https://stackoverflow.com/questions/48972082/how-should-i-replace-featuredetector-function-in-new-opencv
     * 
     * Explicação mais aprofundada sobre o método:
     * https://medium.com/software-incubator/introduction-to-orb-oriented-fast-and-rotated-brief-4220e8ec40cf
     * 
     * I tested and it seems these feature detectors don't work in small images.
     * That's why with 80x80 pictures no keypoints are found.
     * Actually, using the original colores and unaltered images, some keypoints
     * are found, but just a few (just 4 or so).
     */
    public double calculateSimilatiryBetween(Mat image1, Mat image2) {
        // int numberOfFeatures = 100;
        ORB orbDetector = ORB.create();
        Mat mask = new Mat();
        MatOfKeyPoint keypointsA = new MatOfKeyPoint();
        Mat descriptorsA = new Mat();
        orbDetector.detect(image1, keypointsA);
        orbDetector.detectAndCompute(image1, mask, keypointsA, descriptorsA);
        // System.out.println(keypointsA.dump());
        // System.out.println(descriptorsA.dump());

        MatOfKeyPoint keypointsB = new MatOfKeyPoint();
        Mat descriptorsB = new Mat();
        orbDetector.detectAndCompute(image2, mask, keypointsB, descriptorsB);
        // System.out.println(keypointsB.dump());
        // System.out.println(descriptorsB.dump());

        if (descriptorsA.rows() == 0 || descriptorsB.rows() == 0) return 0;

        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(descriptorsA, descriptorsB, matches);

        List<DMatch> matchesList = matches.toList();
        Collections.sort(matchesList, new Comparator<DMatch>() {
            public int compare(DMatch a, DMatch b) {
                return (int)(a.distance - b.distance);
            }
        });

        // It seems there isn't a standard way to calculate the similarity between two
        // images based on feature matching, so I created one from the top of my head
        double averageDistanceOfClosestDescriptors = 0;
        // 15 might be a bad number because in Kifu Recorder the images are mostly similar.
        // Therefore, we want to discard images where a hand is covering part of the board,
        // for example. Picking only the best descriptor matches might make images that
        // should not be labeled as similar to be marked as such. Maybe picking the worst
        // descriptors might be interesting? Because in similar images, even the worst
        // descriptors should be much closer than the worst descriptors in different images.
        int numberOfDescriptorsToConsider = Math.min(15, matchesList.size());
        System.out.println("        Number of descrptors = " + numberOfDescriptorsToConsider);
        for (int i = 0; i < numberOfDescriptorsToConsider; i++) {
            averageDistanceOfClosestDescriptors += matchesList.get(i).distance;
        }
        if (numberOfDescriptorsToConsider == 0) return 0;

        return 1 - (averageDistanceOfClosestDescriptors / numberOfDescriptorsToConsider) / 100;
    }

    public boolean areImagesSimilar(Mat image1, Mat image2)
    {
        return calculateSimilatiryBetween(image1, image2) > 0.3;
    }

}