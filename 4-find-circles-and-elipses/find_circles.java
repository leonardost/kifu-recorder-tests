import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class find_circles {

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static final Scalar RED = new Scalar(0, 0, 255);
    public static final Scalar GREEN = new Scalar(0, 255, 0);
    public static final Scalar BLUE = new Scalar(255, 0, 0);
    public static final Scalar PURPLE = new Scalar(255, 0, 255);
    public static final Scalar CYAN = new Scalar(0, 255, 255);
    public static final Scalar YELLOW = new Scalar(255, 255, 0);

    public static void main(String[] args) {

        File imageFolder = new File("images");
        File[] imageFiles = imageFolder.listFiles();
        Arrays.sort(imageFiles);
        List<Boolean> isThereACircle = readAnnotations();

        int counter = 0;
        int numberOfCorrectDetections = 0;

        for (File imageFile : imageFiles) {
            if (imageFile.isDirectory()) continue;

            Mat image = Imgcodecs.imread(imageFile.getAbsolutePath());
            Mat imageWithCircles = image.clone();
            System.out.println("Processing image " + imageFile.getName());

            // These were the best parameters to find most circles with Hough circles.
            // Another good set of values is (1, 100.0, 20.0).
            for (Circle circle : detectCirclesIn(image, 3, 50.0, 20.0)) {
                Imgproc.circle(imageWithCircles, circle.center, 1, RED, 3, 8, 0);
                Imgproc.circle(imageWithCircles, circle.center, circle.radius, RED, 3, 8, 0);
            }

            for (Circle circle : detectCirclesInImageWithBlobDetector(image)) {
                Imgproc.circle(imageWithCircles, circle.center, 1, RED, 3, 8, 0);
                Imgproc.circle(imageWithCircles, circle.center, circle.radius, BLUE, 3, 8, 0);
            }

            for (Circle circle : detectCirclesInImageWithApproxDP(image, imageFile.getName())) {
                Imgproc.circle(imageWithCircles, circle.center, 1, RED, 3, 8, 0);
                Imgproc.circle(imageWithCircles, circle.center, circle.radius, PURPLE, 3, 8, 0);
            }

            Circle circle = detectCircleInImageWithEllipsisFit(image, imageFile.getName());
            if (circle != null) {
                Imgproc.circle(imageWithCircles, circle.center, 1, RED, 3, 8, 0);
                Imgproc.circle(imageWithCircles, circle.center, circle.radius, GREEN, 3, 8, 0);
            }

            Imgcodecs.imwrite("output/" + imageFile.getName(), imageWithCircles);

            // // System.out.println("image " + (counter + 1) + " - ");
            // if (isThereACircle.get(counter) && !detectedCircles.isEmpty()) {
            //     // System.out.println("OK - FOUND CIRCLE WHERE THERE SHOULD BE ONE!");
            //     numberOfCorrectDetections++;
            // } else if (!isThereACircle.get(counter) && detectedCircles.isEmpty()) {
            //     // System.out.println("OK - DID NOT FIND CIRCLE WHERE THERE SHOULD NOT BE ONE!");
            //     numberOfCorrectDetections++;
            // } else {
            //     // System.out.println("incorrect...");
            // }

            counter++;
        }

    }

    private static List<Boolean> readAnnotations() {
        List<Boolean> isThereAcircle = new ArrayList<>();
        List<String> lines = readLinesFrom(new File("manual_tagging.log").getAbsolutePath());

        for (String line : lines) {
            if (line.startsWith("#")) continue;
            isThereAcircle.add(new Boolean(line.startsWith("1")));
        }

        return isThereAcircle;
    }

    private static List<String> readLinesFrom(String file) {
        try {
            return Files.readAllLines(Paths.get(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private static List<Circle> detectCirclesIn(Mat image, int gaussianBlur, double param1, double param2) {
        // Mat grayscaleImage = convertImageToGrayscale(image);
        // Imgproc.medianBlur(grayscaleImage, grayscaleImage, gaussianBlur);
        Mat imageWithBordersDetected = new Mat();
        Imgproc.Canny(image, imageWithBordersDetected, 50, 100);
        Imgproc.dilate(imageWithBordersDetected, imageWithBordersDetected, Mat.ones(3, 3, CvType.CV_32F));

        Mat circlesMatrix = new Mat();
        int MIN_DISTANCE_BETWEEN_CIRCLE_CENTERS = 30;
        int MIN_RADIUS = 10;
        int MAX_RADIUS = 30;
        Imgproc.HoughCircles(imageWithBordersDetected, circlesMatrix, Imgproc.HOUGH_GRADIENT, 1.0,
                MIN_DISTANCE_BETWEEN_CIRCLE_CENTERS,
                param1, param2, MIN_RADIUS, MAX_RADIUS);

        return generateCircleListFrom(circlesMatrix);
    }

    private static Mat convertImageToGrayscale(Mat image) {
        Mat correctColorFormatImage = new Mat();
        Imgproc.cvtColor(image, correctColorFormatImage, Imgproc.COLOR_BGR2GRAY);
        return correctColorFormatImage;
    }

    private static List<Circle> generateCircleListFrom(Mat circlesMatrix) {
        List<Circle> detectedCircles = new ArrayList<>();

        int X_INDEX = 0;
        int Y_INDEX = 1;
        int RADIUS_INDEX = 2;
        for (int i = 0; i < circlesMatrix.cols(); i++) {
            double[] circle = circlesMatrix.get(0, i);
            Point center = new Point(Math.round(circle[X_INDEX]), Math.round(circle[Y_INDEX]));
            int radius = (int) Math.round(circle[RADIUS_INDEX]);
            detectedCircles.add(new Circle(center, radius));
        }

        return detectedCircles;
    }

    // http://answers.opencv.org/question/38885/how-to-detect-ellipse-and-get-centers-of-ellipse/
    private static List<Circle> detectCirclesInImageWithBlobDetector(Mat image) {
        FeatureDetector blobsDetector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);
        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        Mat grayscaleImage = convertImageToGrayscale(image);
        Imgproc.medianBlur(grayscaleImage, grayscaleImage, 5);
        blobsDetector.detect(grayscaleImage, keypoints);

        List<Circle> detectedCircles = new ArrayList<>();
        for (KeyPoint keypoint : keypoints.toList()) {
            detectedCircles.add(new Circle(keypoint.pt, 10));
        }
        return detectedCircles;
    }

    private static Mat detectBordersIn(Mat image) {
        Mat imageWithBordersDetected = new Mat();
        Imgproc.Canny(image, imageWithBordersDetected, 50, 100);
        // Imgproc.dilate(imageWithBordersDetected, imageWithBordersDetected, Mat.ones(3, 3, CvType.CV_32F), new Point(-1, -1), 2);
        Imgproc.dilate(imageWithBordersDetected, imageWithBordersDetected, Mat.ones(3, 3, CvType.CV_32F));
        return imageWithBordersDetected;
    }

    private static Mat detectBordersInImageWithSimpleDilation(Mat image) {
        Mat imageWithBordersDetected = new Mat();
        Imgproc.Canny(image, imageWithBordersDetected, 50, 100);
        Imgproc.dilate(imageWithBordersDetected, imageWithBordersDetected, Mat.ones(3, 3, CvType.CV_32F));
        return imageWithBordersDetected;
    }

    private static void outputImageWithBorders(Mat imageWithBordersDetected, String filename) {
        Imgcodecs.imwrite("processing/" + filename + "_with_borders_detected.jpg", imageWithBordersDetected);
    }

    private static List<MatOfPoint> detectContoursIn(Mat imageWithBordersDetected)
    {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(imageWithBordersDetected, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        removeSmallContours(contours);
        return contours;
    }

    private static void removeSmallContours(List<MatOfPoint> contours) {
        for (Iterator<MatOfPoint> it = contours.iterator(); it.hasNext();) {
            MatOfPoint contour = it.next();
            if (Imgproc.contourArea(contour) < 200) {
                it.remove();
            }
        }
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

        Imgcodecs.imwrite("processing/" + filename + "_with_contours_detected.jpg", imageWithContoursDetected);
    }

    // This method didn't work so well, approxDP transformed the circle contours into squares
    private static List<Circle> detectCirclesInImageWithApproxDP(Mat image, String filename)
    {
        Mat imageWithBordersDetected = detectBordersIn(image);
        outputImageWithBorders(imageWithBordersDetected, filename);

        Mat imageRegions = new Mat();
        Core.bitwise_not(imageWithBordersDetected, imageRegions);
        List<MatOfPoint> contours = detectContoursIn(imageRegions);
        outputImageWithContours(image, contours, filename);
        
        List<MatOfPoint> possibleCircles = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f();
            MatOfPoint2f approx2f = new MatOfPoint2f();
            contour.convertTo(contour2f, CvType.CV_32FC2);
            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.04, true);
            // System.out.println("approx2f " + approx2f.size());

            MatOfPoint approx = new MatOfPoint();
            approx2f.convertTo(approx, CvType.CV_32S);

            if (Imgproc.isContourConvex(approx) && approx.rows() > 4) {
                possibleCircles.add(approx);
            }

            // double contourArea = Math.abs(Imgproc.contourArea(approx2f));
        }
        outputImageWithContours(image, possibleCircles, filename + "ASDFASDF");

        return new ArrayList<>();
    }

    private static Circle detectCircleInImageWithEllipsisFit(Mat image, String filename)
    {
        Mat blurredImage = image.clone();
        Imgproc.medianBlur(blurredImage, blurredImage, 11);
        Imgcodecs.imwrite("processing/" + filename + "_blurred_image.jpg", blurredImage);

        Mat imageWithBordersDetected = detectBordersInImageWithSimpleDilation(blurredImage);
        Mat regions = new Mat();
        Core.bitwise_not(imageWithBordersDetected, regions);
        outputImageWithBorders(regions, filename);

        List<MatOfPoint> contours = detectContoursIn(regions);
        outputImageWithContours(image, contours, filename);

        Circle bestCircle = null;
        int minimumPointsFound = 999999999;
        double threshould = 200;

        for (int i = 0; i < contours.size(); i++) {

            if (!canContourCanBeAnEllipsis(contours.get(i))) continue;

            MatOfPoint2f contour2f = new MatOfPoint2f();
            contours.get(i).convertTo(contour2f, CvType.CV_32FC2);

            // https://stackoverflow.com/questions/35121045/find-cost-of-ellipse-in-opencv
            RotatedRect ellipse = Imgproc.fitEllipse(contour2f);

            Mat maskContour = new Mat(image.rows(), image.cols(), CvType.CV_8U, new Scalar(0));
            Imgproc.drawContours(maskContour, contours, i, new Scalar(255), -1);
            Imgcodecs.imwrite("processing/" + filename + "_contour_" + i + ".jpg", maskContour);

            Mat maskEllipse = new Mat(image.rows(), image.cols(), CvType.CV_8U, new Scalar(0));
            Imgproc.ellipse(maskEllipse, ellipse, new Scalar(255), -1);
            // Imgcodecs.imwrite("processing/" + filename + "_ellipse_" + i + ".jpg", maskEllipse);
            
            // Mat intersection = new Mat(image.rows(), image.cols(), CvType.CV_8U, new Scalar(0));
            // Core.bitwise_and(maskContour, maskEllipse, intersection);
            // Imgcodecs.imwrite("processing/" + filename + "_ellipse_intersection_with_contours_" + i + ".jpg", intersection);

            Mat leftover = new Mat(image.rows(), image.cols(), CvType.CV_8U, new Scalar(0));
            Core.bitwise_xor(maskContour, maskEllipse, leftover);
            // Imgcodecs.imwrite("processing/" + filename + "_leftover_" + i + ".jpg", leftover);

            int leftoverCount = Core.countNonZero(leftover);
            if (leftoverCount < minimumPointsFound && leftoverCount < threshould) {
            // if (leftoverCount < minimumPointsFound) {
                minimumPointsFound = leftoverCount;
                Point center = new Point(ellipse.center.x, ellipse.center.y);
                bestCircle = new Circle(center, (int)(ellipse.size.width) / 2);
            }
        }

        return bestCircle;
    }

    private static boolean canContourCanBeAnEllipsis(MatOfPoint contour)
    {
        MatOfPoint2f contour2f = new MatOfPoint2f();
        MatOfPoint2f approx2f = new MatOfPoint2f();
        contour.convertTo(contour2f, CvType.CV_32FC2);
        Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.04, true);
        System.out.println("approx2f " + approx2f.size());

        MatOfPoint approx = new MatOfPoint();
        approx2f.convertTo(approx, CvType.CV_32S);

        return Imgproc.isContourConvex(approx) && approx.rows() > 4;
    }

}
