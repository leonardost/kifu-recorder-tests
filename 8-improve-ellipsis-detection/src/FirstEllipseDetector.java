package src;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * This detector first preprocesses the image following these steps:
 * - Blur
 * - Detect borders with Canny filter
 * - Dilation
 * - Erosion
 * - Invert
 * - Erosion
 * 
 * Then, the resulting image's contours are detected using
 * Imgproc.findContours. Small contours are removed.
 * 
 * Each resulting contour is then checked to see if it can be an
 * ellipse by following these steps:
 * - Check if it is convex and has at least 5 sides
 * - An ellipse that fits that contour is found with Imgproc.fitEllipse
 * - That ellipse is checked against the original contour by counting
 *   the number of pixels that remain in a XOR operation of them
 * - If the ratio between that number of pixels and the area of the
 *   ellipse is smaller than 15%, that ellipse is stored. This means
 *   the contour is probably an ellipse
 */
public class FirstEllipseDetector implements EllipseDetectorInterface {

    private int imageIndex;
    private Mat image;
    private List<MatOfPoint> approximatedContours;

    public String getName() {
        return "first ellipse detector";
    }

    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
    }

    // https://stackoverflow.com/questions/35121045/find-cost-of-ellipse-in-opencv
    public List<RotatedRect> detectEllipsesIn(Mat image) {
        this.image = image.clone();
        Mat imageWithEllipses = image.clone();
        approximatedContours = new ArrayList<>();

        Mat preprocessedImage = preprocessImage(image.clone());
        List<MatOfPoint> contours = detectContoursIn(preprocessedImage);
        outputImageWithContours(image, contours, "processing/image" + imageIndex + "_all_contours.jpg");
        List<RotatedRect> ellipses = new ArrayList<>();

        for (int i = 0; i < contours.size(); i++) {
            RotatedRect ellipse = getEllipseFrom(contours.get(i));
            if (ellipse == null) continue;

            // Let's increase the ellipse size to encompass the entire stone and some more
            // The perspective should be taken into account here, but let's leave it like this for now
            ellipse.size.width *= 1.4;
            ellipse.size.height *= 1.3;
            ellipses.add(ellipse);
            Imgproc.ellipse(imageWithEllipses, ellipse, new Scalar(0, 255, 0));
        }

        outputImageWithContours(image, approximatedContours, "processing/image" + imageIndex + "_approximated_contours.jpg");
        Imgcodecs.imwrite("processing/image" + imageIndex + "_ellipse_fit.jpg", imageWithEllipses);

        return ellipses;
    }

    private Mat preprocessImage(Mat image) {
        // Blur image to smooth noise
        Imgproc.blur(image, image, new Size(3, 3));
        Imgcodecs.imwrite("processing/image" + imageIndex + "_preprocessed_image_0.jpg", image);
        // Detect borders with Canny filter
        image = detectBordersIn(image);
        Imgcodecs.imwrite("processing/image" + imageIndex + "_preprocessed_image_1.jpg", image);
        Imgproc.dilate(image, image, Mat.ones(3, 3, CvType.CV_32F), new Point(-1, -1), 3);
        Imgproc.erode(image, image, Mat.ones(3, 3, CvType.CV_32F), new Point(-1, -1), 3);
        Imgcodecs.imwrite("processing/image" + imageIndex + "_preprocessed_image_2.jpg", image);
        // Invert regions
        Core.bitwise_not(image, image);
        Imgproc.erode(image, image, Mat.ones(3, 3, CvType.CV_32F), new Point(-1, -1), 1);
        Imgcodecs.imwrite("processing/image" + imageIndex + "_preprocessed_image_3.jpg", image);
        return image;
    }

    private Mat detectBordersIn(Mat image) {
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

    private RotatedRect getEllipseFrom(MatOfPoint contour)
    {
        MatOfPoint approximatedContour = approximateContour(contour);
        if (!canContourBeAnEllipse(approximatedContour)) return null;
        approximatedContours.add(approximatedContour);
        RotatedRect ellipse = fitEllipseInContour(contour); 
        if (!isEllipseAGoodFitAgainstContour(ellipse, contour)) return null;

        return ellipse;
    }

    // A contour that can be an ellipse must be convex and have at least 5 sides
    private boolean canContourBeAnEllipse(MatOfPoint contour)
    {
        return Imgproc.isContourConvex(contour) && contour.rows() >= 5;
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

    private RotatedRect fitEllipseInContour(MatOfPoint contour) {
        MatOfPoint2f contour2f = new MatOfPoint2f();
        contour.convertTo(contour2f, CvType.CV_32FC2);
        return Imgproc.fitEllipse(contour2f);
    }

    private boolean isEllipseAGoodFitAgainstContour(RotatedRect ellipse, MatOfPoint contour) {
        // We plot a mask of the contour we are checking
        Mat maskContour = new Mat(image.rows(), image.cols(), CvType.CV_8U, new Scalar(0));
        List<MatOfPoint> contours = new ArrayList<>();
        contours.add(contour);
        Imgproc.drawContours(maskContour, contours, 0, new Scalar(255), -1);
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

        return leftoverRatio < 0.15;
    }

}