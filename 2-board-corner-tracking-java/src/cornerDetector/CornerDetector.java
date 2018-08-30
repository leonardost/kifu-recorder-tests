package src.cornerDetector;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import src.Ponto;
import src.PointCluster;

public class CornerDetector {

    public final static int RADIUS_OF_REGION_OF_INTEREST = 50;
    public int imageIndex;
    private int cornerIndex;

    public Ponto updateCorner(Mat image, Ponto corner, int cornerIndex) {

        this.cornerIndex = cornerIndex;

        Mat regionImage = getRegionOfInterestAround(corner, image);
        Imgcodecs.imwrite("processing/corner_region_" + cornerIndex + "_frame" + imageIndex + ".jpg", regionImage);

        Ponto candidateCornerHarris = detectCornerByHarrisDetection(regionImage);
        // This should have precedence over the Harris Corner detector because a circle
        // in a corner position is great indicative that it is indeed the corner
        Ponto candidateCornerCircle = detectCornerByCircleDetection(regionImage);

        // List<Ponto> candidatePoints = new ArrayList<>();
        // candidatePoints.add(candidateCornerHarris);
        // candidatePoints.add(candidateCornerCircle);
        // Ponto pointClosestToCenterOfRegionOfInterest = getNearestPointToCenterOfRegionOfInterest(candidatePoints);
        Ponto pointClosestToCenterOfRegionOfInterest;
        if (candidateCornerCircle != null) {
            pointClosestToCenterOfRegionOfInterest = candidateCornerCircle;
            System.out.println("Candidate corner found by circle detection in frame " + imageIndex + ": ");
            System.out.println(pointClosestToCenterOfRegionOfInterest);
        } else {
            pointClosestToCenterOfRegionOfInterest = candidateCornerHarris;
        }

        if (pointClosestToCenterOfRegionOfInterest != null) {
            Ponto upperLeftCornerOfRegionOfInterest = corner.add(new Ponto(-50, -50));
            Ponto newCornerPosition = pointClosestToCenterOfRegionOfInterest.add(upperLeftCornerOfRegionOfInterest);
            return newCornerPosition;
        }

        return null;
    }

    private Mat getRegionOfInterestAround(Ponto point, Mat image) {
        int x = point.x - RADIUS_OF_REGION_OF_INTEREST > 0 ? point.x - RADIUS_OF_REGION_OF_INTEREST : 0;
        int y = point.y - RADIUS_OF_REGION_OF_INTEREST > 0 ? point.y - RADIUS_OF_REGION_OF_INTEREST : 0;
        int w = x + 2 * RADIUS_OF_REGION_OF_INTEREST < image.cols() ? 2 * RADIUS_OF_REGION_OF_INTEREST : image.cols() - x;
        int h = y + 2 * RADIUS_OF_REGION_OF_INTEREST < image.rows() ? 2 * RADIUS_OF_REGION_OF_INTEREST : image.rows() - y;

        Rect regionOfInterest = new Rect(x, y, w, h);
        return new Mat(image, regionOfInterest);
    }

    private Ponto detectCornerByHarrisDetection(Mat regionImage) {
        Mat correctColorFormatImage = convertImageToCorrectColorFormat(regionImage);
        Mat grayscaleImage = convertToGrayscale(correctColorFormatImage);
        Mat resultOfCornerHarris = applyCornerHarrisTo(grayscaleImage);
        resultOfCornerHarris = dilateImage(resultOfCornerHarris);
        double harrisThreshold = calculateHarrisCornerThreshold(resultOfCornerHarris);

        List<PointCluster> cornerPointsClusters = findPossibleCornerPointsAndClusterizeThem(regionImage, resultOfCornerHarris, harrisThreshold);
        List<Ponto> possibleCenters = findPossibleCenters(cornerPointsClusters);
        return getNearestPointToCenterOfRegionOfInterest(possibleCenters);
    }

    private Mat convertImageToCorrectColorFormat(Mat image) {
        Mat correctColorFormatImage = new Mat();
        Imgproc.cvtColor(image, correctColorFormatImage, Imgproc.COLOR_BGR2GRAY);
        return correctColorFormatImage;
    }

    private Mat convertToGrayscale(Mat image) {
        Mat grayscaleImage = new Mat();
        image.convertTo(grayscaleImage, CvType.CV_32F);
        return grayscaleImage;
    }

    private Mat applyCornerHarrisTo(Mat image) {
        Mat resultOfCornerHarris = new Mat();
        Imgproc.cornerHarris(image, resultOfCornerHarris, 2, 3, 0.04);
        return resultOfCornerHarris;
    }

    private Mat dilateImage(Mat image) {
        Mat dilatedImage = new Mat();
        Mat emptyKernel = new Mat();
        Imgproc.dilate(image, dilatedImage, emptyKernel);
        return dilatedImage;
    }

    private double calculateHarrisCornerThreshold(Mat resultOfCornerHarris) {
        double max = resultOfCornerHarris.get(0, 0)[0];
        for (int i = 0; i < resultOfCornerHarris.height(); i++) {
            for (int j = 0; j < resultOfCornerHarris.width(); j++) {
                if (resultOfCornerHarris.get(i, j)[0] > max) {
                    max = resultOfCornerHarris.get(i, j)[0];
                }
            }
        }
        return 0.01 * max;
    }

    private void markCornerPointsInImageInRed(Mat image, Mat harrisImage, double threshold) {
        double[] red = {0, 0, 255};
        for (int i = 0; i < harrisImage.height(); i++) {
            for (int j = 0; j < harrisImage.width(); j++) {
                if (harrisImage.get(i, j)[0] > threshold) {
                    image.put(i, j, red);
                }
            }
        }
    }

    private List<PointCluster> findPossibleCornerPointsAndClusterizeThem(Mat image, Mat harrisImage, double threshold) {
        List<PointCluster> pointClusters = new ArrayList<>();

        for (int i = 0; i < harrisImage.height(); i++) {
            for (int j = 0; j < harrisImage.width(); j++) {
                if (harrisImage.get(i, j)[0] > threshold) {
                    addPointToClosestPointClusterOrCreateANewOne(new Ponto(j, i), pointClusters);
                }
            }
        }

        return pointClusters;
    }

    private void addPointToClosestPointClusterOrCreateANewOne(Ponto point, List<PointCluster> pointClusters) {
        boolean foundCluster = false;

        for (PointCluster pointCluster : pointClusters) {
            if (pointCluster.isInsideClusterDistance(point)) {
                pointCluster.add(point);
                foundCluster = true;
            }
        }

        if (!foundCluster) {
            PointCluster pointCluster = new PointCluster();
            pointCluster.add(point);
            pointClusters.add(pointCluster);
        }
    }

    private List<Ponto> findPossibleCenters(List<PointCluster> cornerPointsClusters) {
        List<Ponto> possibleCenters = new ArrayList<>();

        for (PointCluster pointCluster : cornerPointsClusters) {
            possibleCenters.add(pointCluster.getCentroid());
        }

        return possibleCenters;
    }

    private Ponto getNearestPointToCenterOfRegionOfInterest(List<Ponto> points) {
        Ponto center = new Ponto(50, 50);
        Ponto nearestPoint = null;
        double minimumDistance = 999999999;
        for (Ponto point : points) {
            if (point == null) continue;
            if (point.distanceTo(center) < minimumDistance) {
                minimumDistance = point.distanceTo(center);
                nearestPoint = point;
            }
        }
        return nearestPoint;
    }

    // https://docs.opencv.org/3.3.1/d4/d70/tutorial_hough_circle.html
    private Ponto detectCornerByCircleDetection(Mat regionImage) {
        Mat grayscaleImage = convertImageToCorrectColorFormat(regionImage);

        // An image that's so blurry actually helps a lot in finding circles
        Imgproc.medianBlur(grayscaleImage, grayscaleImage, 5);
        Imgcodecs.imwrite("processing/corner_region_" + cornerIndex + "_circle_detection_step_2_frame" + imageIndex + ".jpg", grayscaleImage);
        Mat circles = new Mat();

        // There must be only one stone in a corner, that's why this parameter is so high
        int MIN_DISTANCE_BETWEEN_CIRCLE_CENTERS = 10;
        // Because go stones don't vary much in size, these parameters can be tweaked very nicely to find them
        int MIN_RADIUS = 20;
        int MAX_RADIUS = 40;
        Imgproc.HoughCircles(grayscaleImage, circles, Imgproc.HOUGH_GRADIENT, 1.0,
                MIN_DISTANCE_BETWEEN_CIRCLE_CENTERS,
                100.0, 30.0, MIN_RADIUS, MAX_RADIUS);

        List<Ponto> possibleCenters = new ArrayList<>();

        Mat showCircleDetectionImage = regionImage.clone();
        for (int x = 0; x < circles.cols(); x++) {
            double[] c = circles.get(0, x);
            Point center = new Point(Math.round(c[0]), Math.round(c[1]));
            // circle center
            Imgproc.circle(showCircleDetectionImage, center, 1, new Scalar(0,100,100), 3, 8, 0 );
            // circle outline
            int radius = (int) Math.round(c[2]);
            Imgproc.circle(showCircleDetectionImage, center, radius, new Scalar(255,0,255), 3, 8, 0 );

            possibleCenters.add(new Ponto((int)Math.round(c[0]), (int)Math.round(c[1])));
        }
        Imgcodecs.imwrite("processing/corner_region_" + cornerIndex + "_circle_detection_step_3_frame" + imageIndex + ".jpg", showCircleDetectionImage);

        return getNearestPointToCenterOfRegionOfInterest(possibleCenters);
    }

}

