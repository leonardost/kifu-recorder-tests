import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.util.List;
import java.util.ArrayList;

public class process_image {

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public final static int RADIUS_OF_REGION_OF_INTEREST = 50;

    public static void main(String[] args) {

        if (args.length != 10) {
            printUsage();
            return;
        }

        String inputFolder = args[0];
        int numberOfImages = Integer.parseInt(args[1]);
        List<Mat> boardImageSeries = readImageFiles(inputFolder, numberOfImages);

        Ponto[] corners = new Ponto[4];
        corners[0] = new Ponto(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        corners[1] = new Ponto(Integer.parseInt(args[4]), Integer.parseInt(args[5]));
        corners[2] = new Ponto(Integer.parseInt(args[6]), Integer.parseInt(args[7]));
        corners[3] = new Ponto(Integer.parseInt(args[8]), Integer.parseInt(args[9]));

        int imageIndex = 1;
        for (Mat image : boardImageSeries) {

            for (int i = 0; i < 4; i++) {
                Ponto possibleNewCorner = updateCorner(image, corners[i], i + 1, imageIndex);
                if (possibleNewCorner != null) {
                    corners[i] = possibleNewCorner;
                }
            }

            printCornerPositions(imageIndex, corners);
            drawBoardContourOnImage(image, corners, imageIndex);
            imageIndex++;
        }

    }

    private static void printUsage() {
        System.out.println("Usage: process_image INPUT_FOLDER NUMBER_OF_IMAGES P1 P2 P3 P4");
        System.out.println("The input folder must contain a sequence of images named as \"frameX.jpg\"");
        System.out.println("P1, P2, P3 and P4 are the positions of the corners of the board in the first");
        System.out.println("image, each composed of two integers X and Y separated by spaces.");
        System.out.println();
        System.out.println("Example:");
        System.out.println("process_image input 7 1210 136 1252 984 617 937 582 235");
        System.out.println();
        System.out.println("The outputs of this algorithm are the input images with the go board's contour");
        System.out.println("marked in red");
    }

    private static List<Mat> readImageFiles(String inputFolder, int numberOfImages) {
        ArrayList<Mat> boardImages = new ArrayList<>();
        for (int i = 1; i <= numberOfImages; i++) {
            boardImages.add(Imgcodecs.imread(inputFolder + "/frame" + i + ".jpg"));
        }
        return boardImages;
    }

    private static void printCornerPositions(int imageIndex, Ponto[] corners) {
        System.out.println("Frame " + imageIndex);
        for (int i = 0; i < 4; i++) {
            System.out.print("Corner " + (i + 1) + ": ");
            System.out.println(corners[i]);
        }
        System.out.println("-----");
    }

    private static Ponto updateCorner(Mat image, Ponto corner, int index, int imageIndex) {

        Mat regionImage = getRegionOfInterestAround(corner, image);
        Imgcodecs.imwrite("processing/corner" + index + "_imagem_" + imageIndex + ".jpg", regionImage);

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

    private static Mat getRegionOfInterestAround(Ponto point, Mat image) {
        int x = point.x - RADIUS_OF_REGION_OF_INTEREST > 0 ? point.x - RADIUS_OF_REGION_OF_INTEREST : 0;
        int y = point.y - RADIUS_OF_REGION_OF_INTEREST > 0 ? point.y - RADIUS_OF_REGION_OF_INTEREST : 0;
        int w = x + 2 * RADIUS_OF_REGION_OF_INTEREST < image.cols() ? 2 * RADIUS_OF_REGION_OF_INTEREST : image.cols() - x;
        int h = y + 2 * RADIUS_OF_REGION_OF_INTEREST < image.rows() ? 2 * RADIUS_OF_REGION_OF_INTEREST : image.rows() - y;

        Rect regionOfInterest = new Rect(x, y, w, h);
        return new Mat(image, regionOfInterest);
    }

    private static Mat convertImageToCorrectColorFormat(Mat image) {
        Mat correctColorFormatImage = new Mat();
        Imgproc.cvtColor(image, correctColorFormatImage, Imgproc.COLOR_BGR2GRAY);
        return correctColorFormatImage;
    }

    private static Mat convertToGrayscale(Mat image) {
        Mat grayscaleImage = new Mat();
        image.convertTo(grayscaleImage, CvType.CV_32F);
        return grayscaleImage;
    }

    private static Mat applyCornerHarrisTo(Mat image) {
        Mat resultOfCornerHarris = new Mat();
        Imgproc.cornerHarris(image, resultOfCornerHarris, 2, 3, 0.04);
        return resultOfCornerHarris;
    }

    private static Mat dilateImage(Mat image) {
        Mat dilatedImage = new Mat();
        Mat emptyKernel = new Mat();
        Imgproc.dilate(image, dilatedImage, emptyKernel);
        return dilatedImage;
    }

    private static double calculateHarrisCornerThreshold(Mat resultOfCornerHarris) {
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

    private static void markCornerPointsInImageInRed(Mat image, Mat harrisImage, double threshold) {
        double[] red = {0, 0, 255};
        for (int i = 0; i < harrisImage.height(); i++) {
            for (int j = 0; j < harrisImage.width(); j++) {
                if (harrisImage.get(i, j)[0] > threshold) {
                    image.put(i, j, red);
                }
            }
        }
    }

    private static List<PointCluster> findPossibleCornerPointsAndClusterizeThem(Mat image, Mat harrisImage, double threshold) {
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

    private static void addPointToClosestPointClusterOrCreateANewOne(Ponto point, List<PointCluster> pointClusters) {
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

    private static List<Ponto> findPossibleCenters(List<PointCluster> cornerPointsClusters) {
        List<Ponto> possibleCenters = new ArrayList<>();
        for (PointCluster pointCluster : cornerPointsClusters) {
            possibleCenters.add(pointCluster.getCentroid());
        }
        return possibleCenters;
    }

    private static Ponto getNearestPointToCenterOfRegionOfInterest(List<Ponto> points) {
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

    private static void drawBoardContourOnImage(Mat image, Ponto[] corners, int imageIndex) {
        Point[] boardCorners = new Point[4];
        boardCorners[0] = new Point(corners[0].x, corners[0].y);
        boardCorners[1] = new Point(corners[1].x, corners[1].y);
        boardCorners[2] = new Point(corners[2].x, corners[2].y);
        boardCorners[3] = new Point(corners[3].x, corners[3].y);
        MatOfPoint boardContour = new MatOfPoint(boardCorners);

        List<MatOfPoint> contourPoints = new ArrayList<MatOfPoint>();
        contourPoints.add(boardContour);
        Scalar red = new Scalar(0, 0, 255);
        Imgproc.drawContours(image, contourPoints, -1, red, 2);
        Imgcodecs.imwrite("output/image" + imageIndex + ".jpg", image);
    }

}

class Ponto {
    public int x;
    public int y;

    Ponto() {}
    Ponto(int x, int y) { this.x = x; this.y = y; }

    double distanceTo(Ponto point) {
        return (y - point.y) * (y - point.y) + (x - point.x) * (x - point.x);
    }

    public Ponto add(Ponto point) {
        Ponto newPoint = new Ponto(x, y);
        newPoint.x += point.x;
        newPoint.y += point.y;
        if (newPoint.x < 0) newPoint.x = 0;
        if (newPoint.y < 0) newPoint.y = 0;
        return newPoint;
    }

    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}

class PointCluster {

    private static final double DISTANCE_THRESHOULD_TO_BELONG_TO_A_CLUSTER = 100.0;

    private Ponto centroid;
    private List<Ponto> points;

    public PointCluster() {
        centroid = new Ponto(-1, -1);
        points = new ArrayList<>();
    }

    public Ponto getCentroid() { return centroid; }

    public void add(Ponto point) {
        points.add(point);
        updateCentroid();
    }

    private void updateCentroid() {
        int accumulatedY = 0;
        int accumulatedX = 0;
        for (Ponto point: points) {
            accumulatedY = accumulatedY + point.y;
            accumulatedX = accumulatedX + point.x;
        }
        centroid.y = accumulatedY / points.size();
        centroid.x = accumulatedX / points.size();
    }

    public double distanceTo(Ponto point) {
        if (centroid.y == -1) return 0;
        return centroid.distanceTo(point);
    }

    public boolean isInsideClusterDistance(Ponto point) {
        return distanceTo(point) < DISTANCE_THRESHOULD_TO_BELONG_TO_A_CLUSTER;
    }

}
