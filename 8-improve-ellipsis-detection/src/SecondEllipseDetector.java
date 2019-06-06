package src;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Steps:
 * - Blur
 * - Transform to grayscale
 * - Clusterize historgram in 3 groups, which represent the black
 *   stones, the white stones and the board
 * - Mask out white stones and black stones separately
 * - Detect contours
 * - Check if each contour can be an ellipse
 */
public class SecondEllipseDetector implements EllipseDetectorInterface {

    private int imageIndex;
    private Mat image;

    public String getName() {
        return "second ellipse detector, uses histogram";
    }

    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
    }

    public List<RotatedRect> detectEllipsesIn(Mat image) {
        Mat preprocessedImage = preprocessImage(image.clone());

        // Calculate histogram
        // https://www.programcreek.com/java-api-examples/?class=org.opencv.imgproc.Imgproc&method=calcHist
        List<Mat> images = new ArrayList<>();
        images.add(preprocessedImage);
        Mat histogram = new Mat();
        MatOfInt histogramSize = new MatOfInt(16); // number of bins
        MatOfFloat ranges = new MatOfFloat(0f, 256f);
        Imgproc.calcHist(images, new MatOfInt(0), new Mat(), histogram, histogramSize, ranges);
        // Imgproc.calcHist(images, channels, mask, hist, histSize, ranges);
        System.out.println("histogram = ");
        System.out.println(histogram.t().dump());

        // Clsuterize these points using k-means
        // https://docs.opencv.org/3.0-beta/doc/py_tutorials/py_ml/py_kmeans/py_kmeans_opencv/py_kmeans_opencv.html
        // https://docs.opencv.org/3.4/d1/d5c/tutorial_py_kmeans_opencv.html
        int numberOfClusters = 3;
        Mat labels = new Mat();
        Mat centers = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 100, 1);
        // https://stackoverflow.com/questions/44386786/k-means-clustering-for-color-based-segmentation-using-opencv-in-android
        // Core.kmeans(data, K, bestLabels, criteria, attempts, flags, centers);
        Core.kmeans(histogram.t(), numberOfClusters, labels, criteria, 10, Core.KMEANS_RANDOM_CENTERS, centers);
        System.out.println("centers =");
        System.out.println(centers.dump());
        System.out.println("labels =");
        System.out.println(labels.t().dump());

        int[] centroids = clusterizeHistogramAndReturnCentroids(histogram, numberOfClusters);

        List<RotatedRect> darkEllipses = getPossibleEllipsesByFilteringBelow(centroids[0], preprocessedImage);
        List<RotatedRect> lightEllipses = getPossibleEllipsesByFilteringOver(centroids[1], preprocessedImage);

        // List<MatOfPoint> contours = detectContoursIn(preprocessedImage);
        // outputImageWithContours(image, contours, "processing/image" + imageIndex + "_all_contours.jpg");
        List<RotatedRect> ellipses = new ArrayList<>();

        // for (int i = 0; i < contours.size(); i++) {
        //     RotatedRect ellipse = getEllipseFrom(contours.get(i));
        //     if (ellipse == null) continue;

        //     // Let's increase the ellipse size to encompass the entire stone and some more
        //     // The perspective should be taken into account here, but let's leave it like this for now
        //     ellipse.size.width *= 1.4;
        //     ellipse.size.height *= 1.3;
        //     ellipses.add(ellipse);
        //     Imgproc.ellipse(imageWithEllipses, ellipse, new Scalar(0, 255, 0));
        // }

        // // outputImageWithContours(image, approximatedContours, "processing/image" + imageIndex + "_approximated_contours.jpg");
        // Imgcodecs.imwrite("processing/image" + imageIndex + "_ellipse_fit.jpg", imageWithEllipses);

        return ellipses;
    }

    private Mat preprocessImage(Mat image) {
        // Blur image to smooth noise
        Imgproc.blur(image, image, new Size(3, 3));
        Imgcodecs.imwrite("processing/second-filter_image" + imageIndex + "_preprocessed_image_1.jpg", image);
        // Convert to grayscale
        image = convertToGrayscale(image);
        Imgcodecs.imwrite("processing/second-filter_image" + imageIndex + "_preprocessed_image_2.jpg", image);
        return image;

        // // Detect borders with Canny filter
        // image = detectBordersIn(image);
        // Imgcodecs.imwrite("processing/image" + imageIndex + "_preprocessed_image_1.jpg", image);
        // Imgproc.dilate(image, image, Mat.ones(3, 3, CvType.CV_32F), new Point(-1, -1), 3);
        // Imgproc.erode(image, image, Mat.ones(3, 3, CvType.CV_32F), new Point(-1, -1), 3);
        // Imgcodecs.imwrite("processing/image" + imageIndex + "_preprocessed_image_2.jpg", image);
        // // Invert regions
        // Core.bitwise_not(image, image);
        // Imgproc.erode(image, image, Mat.ones(3, 3, CvType.CV_32F), new Point(-1, -1), 1);
        // Imgcodecs.imwrite("processing/image" + imageIndex + "_preprocessed_image_3.jpg", image);
    }

    private Mat convertToGrayscale(Mat image) {
        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(image, grayscaleImage, Imgproc.COLOR_BGR2GRAY, 1); // 1 channel
        return grayscaleImage;
    }

    private int[] clusterizeHistogramAndReturnCentroids(Mat histogram, int numberOfClusters) {
        if (numberOfClusters < 2) {
            // This number should be at least 2
            return null;
        }

        int numberOfRows = histogram.rows();
        int[] centroids = new int[numberOfClusters];

        // Let's initialize the centroids at each extreme of the histogram
        centroids[0] = 0;
        centroids[1] = numberOfRows - 1;
        if (numberOfClusters > 2) {
            centroids[2] = numberOfRows / 2;
        }

        boolean converged = false;
        int[] labels = new int[numberOfRows];
        int[] oldCentroids = new int[numberOfClusters];
        oldCentroids[0] = 0;
        oldCentroids[1] = numberOfRows - 1;
        oldCentroids[2] = numberOfRows / 2;

        while (!converged) {
            int[][] distancesToCentroids = new int[numberOfClusters][numberOfRows];

            for (int i = 0; i < numberOfClusters; i++) {
                for (int j = 0; j < numberOfRows; j++) {
                    distancesToCentroids[i][ centroids[i] ] = 0;
                    for (int k = centroids[i] - 1; k >= 0; k--) {
                        distancesToCentroids[i][k] = distancesToCentroids[i][k + 1]
                            + (int)histogram.get(k, 0)[0] * (centroids[i] - k);
                    }
                    for (int k = centroids[i] + 1; k < histogram.rows(); k++) {
                        distancesToCentroids[i][k] = distancesToCentroids[i][k - 1]
                            + (int)histogram.get(k, 0)[0] * (k - centroids[i]);
                    } 
                }
            }

            // System.out.println("Distances = ");
            // for (int i = 0; i < numberOfClusters; i++) {
            //     System.out.println("Centroid " + i + ", which is " + centroids[i]);
            //     for (int j = 0; j < numberOfRows; j++) {
            //         System.out.println("Distance to row " + j + " = " + distancesToCentroids[i][j]);
            //     }
            // }
            System.out.println("-------");

            int[] sumOfElementsOfEachCluster = new int[numberOfClusters];

            for (int i = 0; i < numberOfRows; i++) {
                int smallestDistance = 999999999;
                int nearestCluster = -1;
                for (int j = 0; j < numberOfClusters; j++) {
                    if (distancesToCentroids[j][i] < smallestDistance) {
                        smallestDistance = distancesToCentroids[j][i];
                        nearestCluster = j;
                    }
                }
                sumOfElementsOfEachCluster[ nearestCluster ] += histogram.get(i, 0)[0];

                labels[i] = nearestCluster;
            }

            converged = true;

            for (int i = 0; i < numberOfClusters; i++) {
                int sum = 0;
                int medianOfCluster = sumOfElementsOfEachCluster[i] / 2;

                for (int j = 0; j < numberOfRows; j++) {
                    if (labels[j] != i) continue;

                    sum += histogram.get(j, 0)[0];

                    if (sum >= medianOfCluster) {
                        System.out.println("Centroid " + i + " is now " + j);
                        if (centroids[i] != j) converged = false;
                        centroids[i] = j;
                        break;
                    }
                }
            }

        }

        for (int i = 0; i < numberOfRows; i++) {
            System.out.println("Row " + i + " label = " + labels[i]);
        }

        System.out.println("Centroids = ");
        for (int i = 0; i < numberOfClusters; i++) {
            System.out.println(i + " - " + centroids[i]);
        }

        return centroids;
    }

    private List<RotatedRect> getPossibleEllipsesByFilteringBelow(int centroid, Mat image) {
        // There are 256 possible pixel intensities and the histogram has 16 bins,
        // so each bin represents a reange of 16 pixels
        Mat filteredImage = image.clone();
        int threshold = centroid * 16 + 16;
        for (int i = 0; i < image.rows(); i++) {
            for (int j = 0; j < image.cols(); j++) {
                if (image.get(i, j)[0] <= threshold) {
                    filteredImage.put(i, j, new double[]{ 255, 255, 255 });
                } else {
                    filteredImage.put(i, j, new double[]{ 0, 0, 0 });
                }
            }
        }

        Imgcodecs.imwrite("processing/second-filter_image" + imageIndex + "_preprocessed_image_3_dark_filter.png", filteredImage);

        // https://stackoverflow.com/questions/10167534/how-to-find-out-what-type-of-a-mat-object-is-with-mattype-in-opencv/17820615
        System.out.println("depth = ");
        System.out.println(filteredImage.depth());
        // https://stackoverflow.com/questions/15245262/opencv-mat-element-types-and-their-sizes
        System.out.println("channels = ");
        System.out.println(filteredImage.channels());

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(filteredImage, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        // removeSmallContours(contours);
        // System.out.println("Number of contours found in scene: " + contours.size());
        Mat imageWithContoursDetected = image.clone();
        for (MatOfPoint contour : contours) {
            List<MatOfPoint> c = new ArrayList<>();
            c.add(contour);
            Imgproc.drawContours(imageWithContoursDetected, c, -1, new Scalar(255, 255, 255), 2);
        }
        Imgcodecs.imwrite("processing/second-filter_image" + imageIndex + "_preprocessed_image_3_dark_filter_contours.png", imageWithContoursDetected);

        return null;
    }

    private List<RotatedRect> getPossibleEllipsesByFilteringOver(int centroid, Mat image) {
        // There are 256 possible pixel intensities and the histogram has 16 bins,
        // so each bin represents a reange of 16 pixels
        Mat filteredImage = image.clone();
        int threshold = centroid * 16;
        for (int i = 0; i < image.rows(); i++) {
            for (int j = 0; j < image.cols(); j++) {
                if (image.get(i, j)[0] >= threshold) {
                    filteredImage.put(i, j, new double[]{ 255, 255, 255 });
                } else {
                    filteredImage.put(i, j, new double[]{ 0, 0, 0 });
                }
            }
        }
        Imgcodecs.imwrite("processing/second-filter_image" + imageIndex + "_preprocessed_image_3_light_filter.png", filteredImage);

        System.out.println("depth = ");
        System.out.println(filteredImage.depth());
        System.out.println("channels = ");
        System.out.println(filteredImage.channels());

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(filteredImage, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        // removeSmallContours(contours);
        // System.out.println("Number of contours found in scene: " + contours.size());
        Mat imageWithContoursDetected = image.clone();
        for (MatOfPoint contour : contours) {
            List<MatOfPoint> c = new ArrayList<>();
            c.add(contour);
            Imgproc.drawContours(imageWithContoursDetected, c, -1, new Scalar(0, 0, 0), 2);
        }
        Imgcodecs.imwrite("processing/second-filter_image" + imageIndex + "_preprocessed_image_3_light_filter_contours.png", imageWithContoursDetected);

        return null;
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

    // private RotatedRect getEllipseFrom(MatOfPoint contour)
    // {
    //     MatOfPoint approximatedContour = approximateContour(contour);
    //     if (!canContourBeAnEllipse(approximatedContour)) return null;
    //     approximatedContours.add(approximatedContour);
    //     RotatedRect ellipse = fitEllipseInContour(contour); 
    //     if (!isEllipseAGoodFitAgainstContour(ellipse, contour)) return null;

    //     return ellipse;
    // }

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