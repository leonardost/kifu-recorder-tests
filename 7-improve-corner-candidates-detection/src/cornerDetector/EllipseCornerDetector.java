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

public class EllipseCornerDetector {

    public int imageIndex;

    // https://stackoverflow.com/questions/35121045/find-cost-of-ellipse-in-opencv
    public List<Corner> detectCandidateCornersIn(Mat image) {
        Mat imageWithEllipsis = image.clone();
        Mat preprocessedImage = image.clone();

        // Blur image to smooth noise
        Imgproc.blur(preprocessedImage, preprocessedImage, new Size(3, 3));
        // Detect borders
        preprocessedImage = detectBordersIn(preprocessedImage);
        // Imgcodecs.imwrite("processing/image" + imageIndex + "_preprocessed_image_1.jpg", preprocessedImage);
        Imgproc.dilate(preprocessedImage, preprocessedImage, Mat.ones(3, 3, CvType.CV_32F), new Point(-1, -1), 3);
        Imgproc.erode(preprocessedImage, preprocessedImage, Mat.ones(3, 3, CvType.CV_32F), new Point(-1, -1), 3);
        // Imgcodecs.imwrite("processing/image" + imageIndex + "_preprocessed_image_2.jpg", preprocessedImage);
        // Invert regions
        Core.bitwise_not(preprocessedImage, preprocessedImage);
        Imgproc.erode(preprocessedImage, preprocessedImage, Mat.ones(3, 3, CvType.CV_32F), new Point(-1, -1), 1);
        Imgcodecs.imwrite("processing/image" + imageIndex + "_preprocessed_image_3.jpg", preprocessedImage);

        // Detect contours
        List<MatOfPoint> contours = detectContoursIn(preprocessedImage);
        outputImageWithContours(image, contours, "processing/image" + imageIndex + "_all_contours.jpg");
        // If there are more than 5 contours found, there's probably a finger or some other
        // kind of interference in the scene
        if (contours.size() > 5) return new ArrayList<>();

        List<MatOfPoint> approximatedContours = new ArrayList<>();
        List<Corner> candidateCorners = new ArrayList<>();

        for (int i = 0; i < contours.size(); i++) {

            if (!canContourBeAnEllipsis(contours.get(i))) continue;
            approximatedContours.add(approximateContour(contours.get(i)));

            MatOfPoint2f contour2f = new MatOfPoint2f();
            contours.get(i).convertTo(contour2f, CvType.CV_32FC2);

            RotatedRect ellipse = Imgproc.fitEllipse(contour2f);
            Ponto center = new Ponto((int)ellipse.center.x, (int)ellipse.center.y);
            // if (!isInsideRegionOfInterest(center)) continue;

            // We plot a mask of the contour we are checking
            Mat maskContour = new Mat(image.rows(), image.cols(), CvType.CV_8U, new Scalar(0));
            Imgproc.drawContours(maskContour, contours, i, new Scalar(255), -1);
            // We then plot the found ellipse
            Mat maskEllipse = new Mat(image.rows(), image.cols(), CvType.CV_8U, new Scalar(0));
            Imgproc.ellipse(maskEllipse, ellipse, new Scalar(255), -1);
            // we check the pixels that are only in one or the other image
            Mat leftover = new Mat(image.rows(), image.cols(), CvType.CV_8U, new Scalar(0));
            // The leftover is the difference between the contour found and the ellipse we're trying to fit.
            // The less leftover there is, the more the ellipse fits the contour.
            Core.bitwise_xor(maskContour, maskEllipse, leftover);

            int leftoverCount = Core.countNonZero(leftover);
            int maskEllipseCount = Core.countNonZero(maskEllipse);
            double leftoverRatio = (double)leftoverCount / (double)maskEllipseCount;

            if (leftoverRatio < 0.15) {
                Corner candidateCorner = new Corner(center.x, center.y, true);
                candidateCorner.stonePosition = ellipse;
                candidateCorners.add(candidateCorner);
                Imgproc.ellipse(imageWithEllipsis, ellipse, new Scalar(0, 255, 0));
            }
        }
        outputImageWithContours(image, approximatedContours, "processing/image" + imageIndex + "_approximated_contours.jpg");
        Imgcodecs.imwrite("processing/image" + imageIndex + "_ellipsis_fit.jpg", imageWithEllipsis);

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
        // Imgproc.findContours(imageWithBordersDetected, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        Imgproc.findContours(imageWithBordersDetected, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        removeSmallContours(contours);
        System.out.println("Number of contours found in scene: " + contours.size());
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