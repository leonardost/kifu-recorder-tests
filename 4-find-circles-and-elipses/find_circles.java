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

    public static void main(String[] args) {

        File imageFolder = new File("images");
        File[] imageFiles = imageFolder.listFiles();
        Arrays.sort(imageFiles);
        List<Boolean> isThereACircle = readAnnotations();

        int mostDetections = 0;

        // for (int gauss = 1; gauss <= 5; gauss += 2) { 
        //     for (double param1 = 10.0; param1 <= 200.0; param1 += 10.0) {
        //         for (double param2 = 10.0; param2 <= param1; param2 += 10.0) {

                    int counter = 0;
                    int numberOfCorrectDetections = 0;

                    for (File imageFile : imageFiles) {
                        if (imageFile.isDirectory()) continue;
            
                        Mat image = Imgcodecs.imread(imageFile.getAbsolutePath());
                        System.out.println("Processing image " + imageFile.getName());
            
                        // List<Circle> detectedCircles = detectCirclesIn(image, gauss, param1, param2);
                        // List<Circle> detectedCircles = detectCirclesIn(image, 1, 100.0, 20.0);
                        List<Circle> detectedCircles = detectCirclesIn(image, 3, 50.0, 20.0);

                        // for (Circle circle : detectedCircles) {
                        //     Imgproc.circle(image, circle.center, 1, new Scalar(0,100,100), 3, 8, 0 );
                        //     Imgproc.circle(image, circle.center, circle.radius, new Scalar(255,0,255), 3, 8, 0 );
                        // }
            
                        // for (Circle circle : detectCirclesInImageWithBlobDetector(image)) {
                        //     Imgproc.circle(image, circle.center, 1, new Scalar(0,100,100), 3, 8, 0 );
                        //     Imgproc.circle(image, circle.center, circle.radius, new Scalar(255,0,255), 3, 8, 0 );
                        // }
            
                        // for (Circle circle : detectCirclesInImageWithEllipsisFit(image, imageFile.getName())) {
                        //     Imgproc.circle(image, circle.center, 1, new Scalar(0,100,100), 3, 8, 0 );
                        //     Imgproc.circle(image, circle.center, circle.radius, new Scalar(255,0,255), 3, 8, 0 );
                        // }

                        // for (Circle circle : detectCirclesInImageWithApproxDP(image, imageFile.getName())) {
                        //     Imgproc.circle(image, circle.center, 1, new Scalar(0,100,100), 3, 8, 0);
                        //     Imgproc.circle(image, circle.center, circle.radius, new Scalar(255,0,255), 3, 8, 0);
                        // }

                        for (Circle circle : detectCirclesInImageByRegions(image, imageFile.getName())) {
                            Imgproc.circle(image, circle.center, 1, new Scalar(0,100,100), 3, 8, 0);
                            Imgproc.circle(image, circle.center, circle.radius, new Scalar(255,0,255), 3, 8, 0);
                        }

                        Imgcodecs.imwrite("output/" + imageFile.getName(), image);
            
                        // System.out.println("image " + (counter + 1) + " - ");
                        if (isThereACircle.get(counter) && !detectedCircles.isEmpty()) {
                            // System.out.println("OK - FOUND CIRCLE WHERE THERE SHOULD BE ONE!");
                            numberOfCorrectDetections++;
                        } else if (!isThereACircle.get(counter) && detectedCircles.isEmpty()) {
                            // System.out.println("OK - DID NOT FIND CIRCLE WHERE THERE SHOULD NOT BE ONE!");
                            numberOfCorrectDetections++;
                        } else {
                            // System.out.println("incorrect...");
                        }

                        counter++;
                    }

        //             if (mostDetections <= numberOfCorrectDetections) {
        //                 mostDetections = numberOfCorrectDetections;
        //                 System.out.println("Number of correct detections = " + mostDetections);
        //                 System.out.println("Parameters = ");
        //                 System.out.println(gauss);
        //                 System.out.println(param1);
        //                 System.out.println(param2);
        //                 System.out.println("======");
        //             }

        //         }
        //     }
        // }

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

    private static List<Circle> detectCirclesInImageWithEllipsisFit(Mat image, String filename)
    {
        Mat imageWithBordersDetected = detectBordersIn(image);
        outputImageWithBorders(imageWithBordersDetected, filename);

        List<MatOfPoint> contours = detectContoursIn(imageWithBordersDetected);
        outputImageWithContours(image, contours, filename);

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            // System.out.println("Contour area = " + area);
        }
        
        return new ArrayList<>();
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
            if (Imgproc.contourArea(contour) < 450) {
                it.remove();
            }
        }
    }

    private static void outputImageWithContours(Mat ortogonalBoardImage, List<MatOfPoint> contours, String filename) {
        Mat imageWithContoursDetected = ortogonalBoardImage.clone();
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

    private static List<Circle> detectCirclesInImageWithApproxDP(Mat image, String filename)
    {
        Mat imageWithBordersDetected = detectBordersIn(image);
        outputImageWithBorders(imageWithBordersDetected, filename);

        List<MatOfPoint> contours = detectContoursIn(imageWithBordersDetected);
        outputImageWithContours(image, contours, filename);
        
        List<MatOfPoint> possibleCircles = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f();
            MatOfPoint2f approx2f = new MatOfPoint2f();
            contour.convertTo(contour2f, CvType.CV_32FC2);
            // The 0.1 means this detection is very lenient, as the goal here
            // is to find as most squares as possible inside the image
            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.04, true);
            System.out.println("approx2f " + approx2f.size());

            MatOfPoint approx = new MatOfPoint();
            approx2f.convertTo(approx, CvType.CV_32S);

            if (Imgproc.isContourConvex(approx)) {
                possibleCircles.add(approx);
            }

            // double contourArea = Math.abs(Imgproc.contourArea(approx2f));
        }
        outputImageWithContours(image, possibleCircles, filename);
        
        return new ArrayList<>();
    }

    private static List<Circle> detectCirclesInImageByRegions(Mat image, String filename)
    {
        Mat imageWithBordersDetected = detectBordersInImageWithSimpleDilation(image);
        Mat regions = new Mat();
        Core.bitwise_not(imageWithBordersDetected, regions);
        outputImageWithBorders(regions, filename);

        List<MatOfPoint> contours = detectContoursIn(regions);
        outputImageWithContours(image, contours, filename);
        // Resultados interessantes até agora, tenho que detectar se o contorno encontrado ao redor das pedras é
        // elipsoide para filtrá-lo

        List<MatOfPoint> possibleCircles = new ArrayList<>();
        int index = 1;
        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint2f contour2f = new MatOfPoint2f();
            MatOfPoint2f approx2f = new MatOfPoint2f();
            contours.get(i).convertTo(contour2f, CvType.CV_32FC2);

            // https://stackoverflow.com/questions/35121045/find-cost-of-ellipse-in-opencv
            RotatedRect ellipse = Imgproc.fitEllipse(contour2f);

            Mat maskContour = new Mat(image.rows(), image.cols(), CvType.CV_8U);
            Imgproc.drawContours(maskContour, contours, i, new Scalar(255), 2);

            Mat maskEllipse = new Mat(image.rows(), image.cols(), CvType.CV_8U);
            Imgproc.ellipse(maskEllipse, ellipse, new Scalar(255), 2);
            
            Mat intersection = new Mat(image.rows(), image.cols(), CvType.CV_8U);
            Core.bitwise_and(maskContour, maskEllipse, intersection);

            System.out.println("intersection = " + intersection);
            System.out.println(intersection.size());

            double cnz = Core.countNonZero(intersection);
            // Count number of pixels in the drawn contour
            double n = Core.countNonZero(maskContour);
            // Compute your measure
            double measure = (double)cnz / (double)n;
            System.out.println("cnz = " + cnz);
            System.out.println("n = " + n);
    
            // Draw, color coded: good -> gree, bad -> red
            Imgproc.ellipse(image, ellipse, new Scalar(0, measure * 255, 255 - measure * 255), 3);

            index++;
        }
        outputImageWithContours(image, possibleCircles, filename);
        
        return new ArrayList<>();
    }

}
