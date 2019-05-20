import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.List;
import java.util.ArrayList;

import src.boardDetector.BoardDetector;
import src.cornerDetector.CornerDetector;
import src.Ponto;
import src.ImageUtils;

public class process_image {

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public final static int RADIUS_OF_REGION_OF_INTEREST = 50;

    public static void main(String[] args) {

        if (args.length != 1) {
            printUsage();
            return;
        }

        String imageSequenceFolder = args[0];
        CornerPositionsFile cornerPositionsFile = new CornerPositionsFile(imageSequenceFolder);

        int numberOfImages = cornerPositionsFile.getNumberOfImages();
        Ponto[] corners = cornerPositionsFile.getInitialCornersPositions();
        Ponto[] cornersVerification = corners.clone();
        int[] numberOfFramesWithouModification = { 0, 0, 0, 0 };

        CornerDetector cornerDetector = new CornerDetector();
        BoardDetector boardDetector = new BoardDetector();

        for (int imageIndex = 1; imageIndex <= numberOfImages; imageIndex++) {

            long startTime = System.nanoTime();
            Mat image = readImageFile(imageSequenceFolder + "/images/", imageIndex);
            cornerDetector.imageIndex = imageIndex;

            Ponto[] possibleNewCorners = new Ponto[4];
            boolean wereAllCornersFound = true;
            for (int i = 0; i < 4; i++) {
                possibleNewCorners[i] = cornerDetector.updateCorner(image, corners[i], i + 1);
                if (possibleNewCorners[i] == null) {
                    wereAllCornersFound = false;
                } else {
                    numberOfFramesWithouModification[i] = possibleNewCorners[i].distanceTo(corners[i]) < 300 ?
                        numberOfFramesWithouModification[i] + 1 : 0;
                }
            }

            Mat ortogonalBoardImage = ImageUtils.generateOrtogonalBoardImage(image, wereAllCornersFound ? possibleNewCorners : corners);
            Imgcodecs.imwrite("processing/ortogonal" + padWithZeroes(imageIndex) + ".jpg", ortogonalBoardImage);
            boardDetector.setImageIndex(imageIndex);

            System.out.println("Frame " + imageIndex);

            // for (int i = 0; i < 4; i++) {
            //     if (numberOfFramesWithouModification[i] > 1) {
            //         System.out.println("Updating corner " + (i + 1));
            //         corners[i] = possibleNewCorners[i];
            //         numberOfFramesWithouModification[i] = 0;
            //     }
            // }

            if (wereAllCornersFound && isCornerMovementUniform(possibleNewCorners, corners) && boardDetector.isBoardContainedIn(ortogonalBoardImage)) {
            // if (wereAllCornersFound && isNewContourValid(possibleNewCorners, corners) && boardDetector.isBoardContainedIn(ortogonalBoardImage)) {
            // if (boardDetector.isBoardContainedIn(ortogonalBoardImage)) {
                System.out.println("Board is inside countour");
                for (int i = 0; i < 4; i++) {
                    corners[i] = possibleNewCorners[i];
                }
            } else {
                System.out.println("Board is NOT inside countour");
            }

            printCornerPositions(imageIndex, corners);
            // printDetectionError(cornerPositionsFile, imageIndex, corners);
            drawBoardContourOnImage(image, corners, imageIndex);

            System.out.println();
            System.out.println("Elapsed time = " + (System.nanoTime() - startTime) / 1000000000.0);
            System.out.println("=====");
        }

    }

    private static void printUsage() {
        System.out.println("Usage: process_image IMAGE_SEQUENCE_FOLDER");
        System.out.println("The image sequence folder must contain a sub-folder named \"images\", containing");
        System.out.println("a sequence of images named as \"frameX.jpg\". It must also contain a file named");
        System.out.println("\"corner_positions.log\", which should contain one line for each image in the");
        System.out.println("sequence. Each line is composed of 8 integers, representing the corner positions");
        System.out.println("of the Go board within each image. It must also contain an extra line at the");
        System.out.println("beginning, containing the initial position of the corners.");
        System.out.println();
        System.out.println("Example:");
        System.out.println("process_image datasets/sequence-1");
        System.out.println();
        System.out.println("The outputs of this algorithm are the input images with the go board's contour");
        System.out.println("marked in red");
    }

    private static Mat readImageFile(String inputFolder, int imageNumber) {
        return Imgcodecs.imread(inputFolder + "/frame" + imageNumber + ".jpg");
    }

    private static String padWithZeroes(int number) {
        String paddedNumber = Integer.toString(number);
        while (paddedNumber.length() < 3) {
            paddedNumber = "0" + paddedNumber;
        }
        return paddedNumber;
    }

    private static void printCornerPositions(int imageIndex, Ponto[] corners) {
        for (int i = 0; i < 4; i++) {
            System.out.print("Corner " + (i + 1) + ": ");
            System.out.println(corners[i]);
        }
    }

    // Checks if the newly found corners are at an uniform distnace from the old ones
    private static boolean isNewContourValid(Ponto[] newCorners, Ponto[] oldCorners) {
        int THRESHOULD = 300;
        double[] distanceToNewPoint = new double[4];
        int numberOfPointsThatMovedMoreThanThreshould = 0;
        for (int i = 0; i < 4; i++) {
            distanceToNewPoint[i] = oldCorners[i].distanceTo(newCorners[i]);
            System.out.println("Distance to old corner point " + (i + 1) + " = " + distanceToNewPoint[i]);
            if (distanceToNewPoint[i] > THRESHOULD) {
                numberOfPointsThatMovedMoreThanThreshould++;
            }
        }
        return numberOfPointsThatMovedMoreThanThreshould == 0
            || numberOfPointsThatMovedMoreThanThreshould == 4;
    }

    // Checks if the distance of the old corners to the new corners is uniform
    // There's no situation where only one corner of the board changes location
    private static boolean isCornerMovementUniform(Ponto[] newCorners, Ponto[] oldCorners) {
        double[] distanceToNewPoint = new double[4];
        for (int i = 0; i < 4; i++) {
            distanceToNewPoint[i] = oldCorners[i].distanceTo(newCorners[i]);
            System.out.println("Distance to old corner point " + (i + 1) + " = " + distanceToNewPoint[i]);
        }
        double standardDeviationOfDistances = calculateStandardDeviationOf(distanceToNewPoint);
        System.out.println("Standard deviation of distances = " + standardDeviationOfDistances);
        return standardDeviationOfDistances <= 100;
    }

    private static double calculateStandardDeviationOf(double[] distribution) {
        double sum = 0;
        for (int i = 0; i < distribution.length; i++) {
            sum += distribution[i];
        }
        double mean = sum / distribution.length;
        double sumOfDistancesToMean = 0;
        for (int i = 0; i < distribution.length; i++) {
            sumOfDistancesToMean += Math.pow((distribution[i] - mean), 2);
        }
        return Math.sqrt(sumOfDistancesToMean / distribution.length);
    }

    private static void printDetectionError(CornerPositionsFile cornerPositionsFile, int imageIndex, Ponto[] corners) {
        int distanceToExpectedPosition = 0;
        Ponto[] expectedPoints = cornerPositionsFile.getCornerPositions(imageIndex);
        for (int i = 0; i < 4; i++) {
            distanceToExpectedPosition += corners[i].manhattanDistanceTo(expectedPoints[i]);
        }
        System.out.println("Error: " + distanceToExpectedPosition);
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
