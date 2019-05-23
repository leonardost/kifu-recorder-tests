package src.cornerDetector;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import src.Ponto;
import src.PointCluster;

public class CornerDetector {

    public final static int RADIUS_OF_REGION_OF_INTEREST = 40;
    public int imageIndex;
    private int cornerIndex;

    public void setCornerIndex(int cornerIndex) {
        this.cornerIndex = cornerIndex;
    }

    public Corner findNewCornerAround(Corner corner, Mat image) {

        Mat regionImage = getRegionOfInterestAround(corner, image);
        Imgcodecs.imwrite("processing/corner_region_" + cornerIndex + "_frame" + imageIndex + ".jpg", regionImage);

        List<Corner> candidateCorners = new ArrayList<>();
        List<Corner> candidateCornerHarris = detectCandidateCornersByHarrisDetection(regionImage);
        List<Corner> candidateCornerEllipsis = detectCandidateCornersByEllipsisFit(regionImage);

        // If too many Harris corner candidates were found, this probably means the image contains
        // something other than Go stones. Maybe a player's hand or something else.
        // if (candidateCornerHarris.size() > 5) return null;

        Mat imageWithCornersPlotted = regionImage.clone();

        System.out.println("Processing corner " + cornerIndex);

        // Remove Harris corner candidates that are too close to circle corner candidates
        // This is done to try to remove corner candidates that appear on the edge of circles
        for (Iterator<Corner> it = candidateCornerHarris.iterator(); it.hasNext();) {
            Corner point = it.next();
            for (Corner circlePoint : candidateCornerEllipsis) {
                if (point.position.distanceTo(circlePoint.position) <= 25 * 25) {
                    it.remove();
                    break;
                }
            }
        }

        for (Corner point : candidateCornerEllipsis) {
            System.out.println("Candidate corner found by circle detection in frame " + imageIndex + ": ");
            System.out.println(point.position);
            Imgproc.circle(imageWithCornersPlotted, new Point(point.position.x, point.position.y), 3, new Scalar(0, 255, 0), -1);
            Imgproc.circle(imageWithCornersPlotted, new Point(point.position.x, point.position.y), 25, new Scalar(0, 255, 0), 1);
        }

        for (Corner point : candidateCornerHarris) {
            System.out.println("Candidate corner found by corner Harris detection in frame " + imageIndex + ": ");
            System.out.println(point.position);
            Imgproc.circle(imageWithCornersPlotted, new Point(point.position.x, point.position.y), 3, new Scalar(0, 0, 255), -1);
        }

        Imgcodecs.imwrite("processing/corner_region_" + cornerIndex + "_frame" + imageIndex + "_candidate_corners.jpg", imageWithCornersPlotted);
        candidateCorners.addAll(candidateCornerHarris);
        candidateCorners.addAll(candidateCornerEllipsis);

        Corner candidateCorner = getCandidateNearestToCenterOfRegionOfInterest(candidateCorners);

        if (candidateCorner != null) {
            Ponto upperLeftCornerOfRegionOfInterest = corner.position.add(new Ponto(-RADIUS_OF_REGION_OF_INTEREST, -RADIUS_OF_REGION_OF_INTEREST));
            Ponto newCornerPosition = candidateCorner.position.add(upperLeftCornerOfRegionOfInterest);
            return new Corner(newCornerPosition.x, newCornerPosition.y, candidateCorner.isStone);
        }

        return null;
    }

    private Mat getRegionOfInterestAround(Corner point, Mat image) {
        int x = point.position.x - RADIUS_OF_REGION_OF_INTEREST > 0 ? point.position.x - RADIUS_OF_REGION_OF_INTEREST : 0;
        int y = point.position.y - RADIUS_OF_REGION_OF_INTEREST > 0 ? point.position.y - RADIUS_OF_REGION_OF_INTEREST : 0;
        int w = x + 2 * RADIUS_OF_REGION_OF_INTEREST < image.cols() ? 2 * RADIUS_OF_REGION_OF_INTEREST : image.cols() - x;
        int h = y + 2 * RADIUS_OF_REGION_OF_INTEREST < image.rows() ? 2 * RADIUS_OF_REGION_OF_INTEREST : image.rows() - y;

        Rect regionOfInterest = new Rect(x, y, w, h);
        return new Mat(image, regionOfInterest);
    }

    private List<Corner> detectCandidateCornersByHarrisDetection(Mat regionImage) {
        Mat correctColorFormatImage = convertImageToCorrectColorFormat(regionImage);
        Mat grayscaleImage = convertToGrayscale(correctColorFormatImage);
        Mat resultOfCornerHarris = applyCornerHarrisTo(grayscaleImage);
        resultOfCornerHarris = dilateImage(resultOfCornerHarris);
        double harrisThreshold = calculateHarrisCornerThreshold(resultOfCornerHarris);
        List<PointCluster> cornerPointsClusters = findPossibleCornerPointsAndClusterizeThem(resultOfCornerHarris, harrisThreshold);
        List<Ponto> possibleCenters = findPossibleCenters(cornerPointsClusters);
        List<Corner> candidateCorners = new ArrayList<>();
        for (Ponto center : possibleCenters) {
            candidateCorners.add(new Corner(center.x, center.y));
        }
        return candidateCorners;
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
        // 1% of the maximum value in the matrix
        return Core.minMaxLoc(resultOfCornerHarris).maxVal * 0.01;
    }

    private List<PointCluster> findPossibleCornerPointsAndClusterizeThem(Mat harrisImage, double threshold) {
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

    private Corner getCandidateNearestToCenterOfRegionOfInterest(List<Corner> corners) {
        Ponto center = new Ponto(RADIUS_OF_REGION_OF_INTEREST, RADIUS_OF_REGION_OF_INTEREST);
        Corner neasrestCorner = null;
        double minimumDistance = 999999999;
        for (Corner point : corners) {
            if (point == null) continue;
            if (point.position.distanceTo(center) < minimumDistance) {
                minimumDistance = point.position.distanceTo(center);
                neasrestCorner = point;
            }
        }
        return neasrestCorner;
    }

    // https://stackoverflow.com/questions/35121045/find-cost-of-ellipse-in-opencv
    private List<Corner> detectCandidateCornersByEllipsisFit(Mat image)
    {
        Mat imageWithEllipsis = image.clone();
        Mat preprocessedImage = image.clone();

        // Blur image to smooth noise
        Imgproc.blur(preprocessedImage, preprocessedImage, new Size(3, 3));
        // Detect borders
        preprocessedImage = detectBordersIn(preprocessedImage);
        // Imgcodecs.imwrite("processing/corner_region_" + cornerIndex + "_frame" + imageIndex + "_preprocessed_image_1.jpg", preprocessedImage);
        Imgproc.dilate(preprocessedImage, preprocessedImage, Mat.ones(3, 3, CvType.CV_32F), new Point(-1, -1), 3);
        Imgproc.erode(preprocessedImage, preprocessedImage, Mat.ones(3, 3, CvType.CV_32F), new Point(-1, -1), 3);
        // Imgcodecs.imwrite("processing/corner_region_" + cornerIndex + "_frame" + imageIndex + "_preprocessed_image_2.jpg", preprocessedImage);
        // Invert regions
        Core.bitwise_not(preprocessedImage, preprocessedImage);
        Imgproc.erode(preprocessedImage, preprocessedImage, Mat.ones(3, 3, CvType.CV_32F), new Point(-1, -1), 1);
        Imgcodecs.imwrite("processing/corner_region_" + cornerIndex + "_frame" + imageIndex + "_preprocessed_image_3.jpg", preprocessedImage);

        // Detect contours
        List<MatOfPoint> contours = detectContoursIn(preprocessedImage);
        outputImageWithContours(image, contours, "processing/corner_region_" + cornerIndex + "_all_ellipses_" + imageIndex + ".jpg");

        List<MatOfPoint> approximatedContours = new ArrayList<>();
        List<Corner> candidateCorners = new ArrayList<>();

        for (int i = 0; i < contours.size(); i++) {

            if (!canContourBeAnEllipsis(contours.get(i))) continue;
            approximatedContours.add(approximateContour(contours.get(i)));

            MatOfPoint2f contour2f = new MatOfPoint2f();
            contours.get(i).convertTo(contour2f, CvType.CV_32FC2);

            RotatedRect ellipse = Imgproc.fitEllipse(contour2f);
            Ponto center = new Ponto((int)ellipse.center.x, (int)ellipse.center.y);
            if (!isInsideRegionOfInterest(center)) continue;

            Mat maskContour = new Mat(image.rows(), image.cols(), CvType.CV_8U, new Scalar(0));
            Imgproc.drawContours(maskContour, contours, i, new Scalar(255), -1);
            Mat maskEllipse = new Mat(image.rows(), image.cols(), CvType.CV_8U, new Scalar(0));
            Imgproc.ellipse(maskEllipse, ellipse, new Scalar(255), -1);
            Mat leftover = new Mat(image.rows(), image.cols(), CvType.CV_8U, new Scalar(0));
            // The leftover is the difference between the contour found and the ellipse we're trying to fit.
            // The less leftover there is, the more the ellipse fits the contour.
            Core.bitwise_xor(maskContour, maskEllipse, leftover);

            int leftoverCount = Core.countNonZero(leftover);
            int maskEllipseCount = Core.countNonZero(maskEllipse);
            double leftoverRatio = (double)leftoverCount / (double)maskEllipseCount;

            if (leftoverRatio < 0.15) {
                Corner candidateCorner = new Corner(center.x, center.y, true);
                candidateCorners.add(candidateCorner);
                Imgproc.ellipse(imageWithEllipsis, ellipse, new Scalar(0, 255, 0));
            }
        }
        outputImageWithContours(image, approximatedContours, "processing/corner_region_" + cornerIndex + "_approximated_contours_" + imageIndex + ".jpg");
        Imgcodecs.imwrite("processing/corner_region_" + cornerIndex + "_ellipsis_fit_" + imageIndex + ".jpg", imageWithEllipsis);

        return candidateCorners;
    }

    private Mat detectBordersIn(Mat image)
    {
        Mat imageWithBordersDetected = new Mat();
        Imgproc.Canny(image, imageWithBordersDetected, 50, 150);
        return imageWithBordersDetected;
    }

    private List<MatOfPoint> detectContoursIn(Mat imageWithBordersDetected)
    {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(imageWithBordersDetected, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        removeSmallContours(contours);
        return contours;
    }

    private void removeSmallContours(List<MatOfPoint> contours)
    {
        for (Iterator<MatOfPoint> it = contours.iterator(); it.hasNext();) {
            MatOfPoint contour = it.next();
            if (Imgproc.contourArea(contour) < 200) {
                it.remove();
            }
        }
    }

    private static boolean isInsideRegionOfInterest(Ponto point)
    {
        return point.x >= 0 && point.x < RADIUS_OF_REGION_OF_INTEREST * 2
            && point.y >= 0 && point.y < RADIUS_OF_REGION_OF_INTEREST * 2;
    }

    private static void outputImageWithContours(Mat image, List<MatOfPoint> contours, String filename) {
        Mat imageWithContoursDetected = image.clone();
        Random random = new Random();
        for (MatOfPoint contour : contours) {
            int color1 = random.nextInt(255);
            int color2 = random.nextInt(255);
            int color3 = random.nextInt(255);
            List<MatOfPoint> c = new ArrayList<>();
            c.add(contour);
            Imgproc.drawContours(imageWithContoursDetected, c, -1, new Scalar(color1, color2, color3), 2);
        }

        Imgcodecs.imwrite(filename, imageWithContoursDetected);
    }

    private boolean canContourBeAnEllipsis(MatOfPoint contour)
    {
        MatOfPoint approximatedContour = approximateContour(contour);
        return Imgproc.isContourConvex(approximatedContour) && approximatedContour.rows() > 4;
    }

    private MatOfPoint approximateContour(MatOfPoint contour)
    {
        MatOfPoint2f contour2f = new MatOfPoint2f();
        MatOfPoint2f approx2f = new MatOfPoint2f();
        contour.convertTo(contour2f, CvType.CV_32FC2);
        // The lower epsilon is, the more exact the approximation has to be
        Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.03, true);
        MatOfPoint approx = new MatOfPoint();
        approx2f.convertTo(approx, CvType.CV_32S);
        return approx;
    }

}
