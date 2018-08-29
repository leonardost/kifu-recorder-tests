import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SimilarityCalculator {

    private Mat imageA;
    private Mat imageB;

    public SimilarityCalculator(Mat imageA, Mat imageB) {
        this.imageA = imageA;
        this.imageB = imageB;
        // TODO: If imageA and imageB have different dimensions, don't even check or return infinity
    }

    private double l2similarity(Mat imageA, Mat imageB) {
        double errorL2 = Core.norm(imageA, imageB, Core.NORM_L2);
        return errorL2 / (double)(imageA.rows() * imageB.cols());
    }

    public double l2similarity() {
        return l2similarity(imageA, imageB);
    }

    public double l2similarityGrayscale() {
        Mat imageAGrayscale = convertImageToGrayscale(imageA);
        Mat imageBGrayscale = convertImageToGrayscale(imageB);
        return l2similarity(imageAGrayscale, imageBGrayscale);
    }

    private Mat convertImageToGrayscale(Mat image) {
        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(image, grayscaleImage, Imgproc.COLOR_BGR2GRAY);
        return grayscaleImage;
    }

    private double templateMatching(int method) {
        Mat result = new Mat();
        Imgproc.matchTemplate(imageA, imageB, result, method);
        Core.MinMaxLocResult minMaxLoc = Core.minMaxLoc(result);
        return minMaxLoc.minVal;
    }

    // https://stackoverflow.com/questions/42292685/calculate-similarity-of-picture-and-its-sketch
    public double templateMatchingCcoeff() {
        return templateMatching(Imgproc.TM_CCOEFF_NORMED);
    }

    public double templateMatchingSqdiff() {
        // This method returns a normalized value between [-1, 0]
        return 1 - templateMatching(Imgproc.TM_SQDIFF_NORMED);
    }

    /**
     * https://stackoverflow.com/questions/34762865/comparing-two-images-with-opencv-in-java
     * https://stackoverflow.com/questions/24569386/opencv-filtering-orb-matches
     */
    public double orbFeatureMatching() {
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
        MatOfKeyPoint keypointsA = new MatOfKeyPoint();
        MatOfKeyPoint keypointsB = new MatOfKeyPoint();
        detector.detect(imageA, keypointsA);
        detector.detect(imageB, keypointsB);

        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        Mat descriptorsA = new Mat();
        Mat descriptorsB = new Mat();
        extractor.compute(imageA, keypointsA, descriptorsA);
        extractor.compute(imageB, keypointsB, descriptorsB);

        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(descriptorsA, descriptorsB, matches);

        List<DMatch> matchesList = matches.toList();
        Collections.sort(matchesList, new Comparator<DMatch>() {
            public int compare(DMatch a, DMatch b) {
                if (a.distance < b.distance) return -1;
                else if (a.distance > b.distance) return 1;
                return 0;
            }
        });

        // It seems there isn't a standard way to calculate the similarity between two
        // images based on feature matching, so I created one from the top of my head
        double averageDistanceOfClosestDescriptors = 0;
        int numberOfDescriptorsToConsider = 15;
        for (int i = 0; i < numberOfDescriptorsToConsider; i++) {
            averageDistanceOfClosestDescriptors += matchesList.get(i).distance;
        }

        return 1 - (averageDistanceOfClosestDescriptors / numberOfDescriptorsToConsider) / 100;
    }

}
