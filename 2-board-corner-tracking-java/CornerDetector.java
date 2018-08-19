import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class CornerDetector {

    public final static int RADIUS_OF_REGION_OF_INTEREST = 50;

    public Ponto updateCorner(Mat image, Ponto corner, int cornerIndex) {

        Mat regionImage = getRegionOfInterestAround(corner, image);
//        DebugHelper.writeJpgImage(regionImage, Imgproc.COLOR_RGBA2BGR, "debug_1_regiao_ao_redor_do_canto_" + cornerIndex + ".jpg");
        Mat correctColorFormatImage = convertImageToCorrectColorFormat(regionImage);
        Mat grayscaleImage = convertToGrayscale(correctColorFormatImage);
        Mat resultOfCornerHarris = applyCornerHarrisTo(grayscaleImage);
        resultOfCornerHarris = dilateImage(resultOfCornerHarris);
        double harrisThreshold = calculateHarrisCornerThreshold(resultOfCornerHarris);

        List<PointCluster> cornerPointsClusters = findPossibleCornerPointsAndClusterizeThem(regionImage, resultOfCornerHarris, harrisThreshold);
        List<Ponto> possibleCenters = findPossibleCenters(cornerPointsClusters);
        Ponto pointClosestToCenterOfRegionOfInterest = getNearestPointToCenterOfRegionOfInterest(possibleCenters);

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
            if (point.distanceTo(center) < minimumDistance) {
                minimumDistance = point.distanceTo(center);
                nearestPoint = point;
            }
        }
        return nearestPoint;
    }

}

