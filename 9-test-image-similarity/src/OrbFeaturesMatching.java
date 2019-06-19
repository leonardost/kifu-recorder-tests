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
     */
    public double calculateSimilatiryBetween(Mat image1, Mat image2) {
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        detector.detect(image1, keypoints1);
        detector.detect(image2, keypoints2);

        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        Mat descriptorsA = new Mat();
        Mat descriptorsB = new Mat();
        extractor.compute(image1, keypoints1, descriptorsA);
        extractor.compute(image2, keypoints2, descriptorsB);

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
        int numberOfDescriptorsToConsider = 15;
        for (int i = 0; i < numberOfDescriptorsToConsider; i++) {
            averageDistanceOfClosestDescriptors += matchesList.get(i).distance;
        }

        return 1 - (averageDistanceOfClosestDescriptors / numberOfDescriptorsToConsider) / 100;
    }

    public boolean areImagesSimilar(Mat image1, Mat image2)
    {
        return calculateSimilatiryBetween(image1, image2) > 0.3;
    }

}