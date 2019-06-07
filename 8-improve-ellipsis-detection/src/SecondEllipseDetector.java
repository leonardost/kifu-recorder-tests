package src;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Steps:
 * - Blur
 * - Transform to grayscale
 * - Clusterize historgram in 3 groups, which represent the black
 *   stones, the white stones and the board
 * - Mask out white stones and black stones separately
 * - Dilate white stones because they're usually harder to detect
 * - Detect contours
 * - Check if each contour can be an ellipse
 */
public class SecondEllipseDetector implements EllipseDetectorInterface
{
    private EllipseChecker ellipseChecker = new EllipseChecker();
    private int imageIndex;

    private static final int FILTER_UNDER = 0;
    private static final int FILTER_OVER = 1;

    public String getName()
    {
        return "second ellipse detector, uses k-means clustering on grayscale image histogram";
    }

    public void setImageIndex(int imageIndex)
    {
        this.imageIndex = imageIndex;
    }

    public List<RotatedRect> detectEllipsesIn(Mat image)
    {
        ellipseChecker.setImage(image);
        Mat preprocessedImage = preprocessImage(image);

        int numberOfBins = 16;
        Mat histogram = getHistogramFrom(preprocessedImage, numberOfBins);
        System.out.println("histogram with 16 bins = ");
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
        // System.out.println("centers =");
        // System.out.println(centers.dump());
        // System.out.println("labels =");
        // System.out.println(labels.t().dump());

        // Centroid 0 centers around the dark pixels and centroid 1 around the light ones
        int[] centroids = clusterizeHistogramAndReturnCentroids(histogram, numberOfClusters);
        List<RotatedRect> darkEllipses = getPossibleEllipsesByFilteringBelow(centroids[0], preprocessedImage);
        List<RotatedRect> lightEllipses = getPossibleEllipsesByFilteringOver(centroids[1], preprocessedImage);
        List<RotatedRect> ellipses = new ArrayList<>();
        ellipses.addAll(darkEllipses);
        ellipses.addAll(lightEllipses);

        drawEllipses(ellipses, image);

        return ellipses;
    }

    private Mat preprocessImage(Mat image)
    {
        Mat processedImage = image.clone();
        processedImage = blur(processedImage);
        processedImage = convertToGrayscale(processedImage);
        processedImage = adjustBrightnessAndContrast(processedImage);
        return processedImage;
    }

    // Blur image to smooth out noise. Being "myopic" here might be
    // good to smooth out imperfections and focus on the colors
    private Mat blur(Mat image)
    {
        Mat blurredImage = image.clone();
        Imgproc.blur(blurredImage, blurredImage, new Size(5, 5));
        Imgproc.blur(blurredImage, blurredImage, new Size(3, 3));
        Imgproc.blur(blurredImage, blurredImage, new Size(3, 3));
        Imgcodecs.imwrite("processing/second-filter_image" + imageIndex + "_preprocessed_image_1.jpg", blurredImage);
        return blurredImage;
    }

    private Mat convertToGrayscale(Mat image)
    {
        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(image, grayscaleImage, Imgproc.COLOR_BGR2GRAY, 1); // 1 channel
        Imgcodecs.imwrite("processing/second-filter_image" + imageIndex + "_preprocessed_image_2.jpg", grayscaleImage);
        return grayscaleImage;
    }

    // https://docs.opencv.org/3.4/d3/dc1/tutorial_basic_linear_transform.html
    // Adjust brightness and contrast
    private Mat adjustBrightnessAndContrast(Mat image)
    {
        Mat adjustedImage = new Mat();
        double alpha = 1.4; // contrast
        int beta = -50; // brightness
        // image.convertTo(m, rtype, alpha, beta);
        image.convertTo(adjustedImage, -1, alpha, beta);
        Imgcodecs.imwrite("processing/second-filter_image" + imageIndex + "_preprocessed_image_3.jpg", adjustedImage);
        // Don't know if Gamma correction is needed
        return adjustedImage;
    }

    // https://www.programcreek.com/java-api-examples/?class=org.opencv.imgproc.Imgproc&method=calcHist
    // Get histogram from image
    private Mat getHistogramFrom(Mat image, int numberOfBins)
    {
        Mat histogram = new Mat();
        List<Mat> images = new ArrayList<>();
        images.add(image);
        MatOfInt histogramSize = new MatOfInt(numberOfBins); // number of bins
        MatOfFloat ranges = new MatOfFloat(0f, 256f);
        // Imgproc.calcHist(images, channels, mask, hist, histSize, ranges);
        Imgproc.calcHist(images, new MatOfInt(0), new Mat(), histogram, histogramSize, ranges);
        return histogram;
    }

    private int[] clusterizeHistogramAndReturnCentroids(Mat histogram, int numberOfClusters)
    {
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

    private List<RotatedRect> getPossibleEllipsesByFilteringBelow(int centroid, Mat image)
    {
        Mat filteredImage = getFilteredImage(centroid * 16 + 16, FILTER_UNDER, image);
        Imgcodecs.imwrite("processing/second-filter_image" + imageIndex + "_preprocessed_image_3_dark_filter.png", filteredImage);

        return findPossibleEllipsesIn(filteredImage, "dark");
    }

    private List<RotatedRect> getPossibleEllipsesByFilteringOver(int centroid, Mat image)
    {
        Mat filteredImage = getFilteredImage(centroid * 16, FILTER_OVER, image);
        Imgcodecs.imwrite("processing/second-filter_image" + imageIndex + "_preprocessed_image_3_light_filter.png", filteredImage);
        Mat dilatedImage = new Mat();
        // Imgproc.dilate(src, dst, kernel, anchor, iterations, borderType, borderValue);
        Imgproc.dilate(filteredImage, dilatedImage, Mat.ones(5, 5, CvType.CV_8U));
        Imgcodecs.imwrite("processing/second-filter_image" + imageIndex + "_preprocessed_image_3_light_filter_dilated.png", dilatedImage);

        return findPossibleEllipsesIn(dilatedImage, "light");
    }

    private Mat getFilteredImage(int threshold, int operation, Mat image)
    {
        // There are 256 possible pixel intensities and the histogram has 16 bins,
        // so each bin represents a reange of 16 pixels
        Mat filteredImage = image.clone();

        for (int i = 0; i < image.rows(); i++) {
            for (int j = 0; j < image.cols(); j++) {
                if (doesPixelPassFilter(image.get(i, j)[0], threshold, operation)) {
                    filteredImage.put(i, j, new double[]{ 255, 255, 255 });
                } else {
                    filteredImage.put(i, j, new double[]{ 0, 0, 0 });
                }
            }
        }

        return filteredImage;
    }

    private boolean doesPixelPassFilter(double pixelValue, int threshold, int operation)
    {
        return operation == FILTER_UNDER ? pixelValue <= threshold : pixelValue >= threshold;
    }

    private List<RotatedRect> findPossibleEllipsesIn(Mat image, String suffix)
    {
        List<MatOfPoint> contours = findContoursIn(image);
        Mat imageWithContoursDetected = image.clone();
        Imgproc.drawContours(imageWithContoursDetected, contours, -1, new Scalar(255, 255, 255), 2);
        Imgcodecs.imwrite("processing/second-filter_image" + imageIndex + "_preprocessed_image_3_" + suffix + "_filter_contours.png", imageWithContoursDetected);

        List<RotatedRect> ellipses = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            RotatedRect ellipse = ellipseChecker.getEllipseFrom(contour);
            if (ellipse != null) ellipses.add(ellipse);
        }
        return ellipses;
    }

    private List<MatOfPoint> findContoursIn(Mat image)
    {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(image, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
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

    private void drawEllipses(List<RotatedRect> ellipses, Mat image)
    {
        Mat imageWithEllipses = image.clone();
        for (RotatedRect ellipse : ellipses) {
            Imgproc.ellipse(imageWithEllipses, ellipse, new Scalar(0, 255, 0));
        }
        Imgcodecs.imwrite("processing/second-filter_image" + imageIndex + "_ellipses.jpg", imageWithEllipses);
    }

}