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
import src.cornerDetector.Corner;
import src.cornerDetector.CornerDetector;
import src.Ponto;
import src.ImageUtils;

public class process_image {

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public final static int RADIUS_OF_REGION_OF_INTEREST = 50;
    // Any corner that moves at least this is considered to have moved
    private final static int MOVEMENT_THRESHOULD = 10;

    public static void main(String[] args) {

        if (args.length != 1) {
            printUsage();
            return;
        }

        String imageSequenceFolder = args[0];
        CornerPositionsFile cornerPositionsFile = new CornerPositionsFile(imageSequenceFolder);

        int numberOfImages = cornerPositionsFile.getNumberOfImages();
        Corner[] corners = cornerPositionsFile.getInitialCornersPositions();

        CornerDetector cornerDetector = new CornerDetector();
        BoardDetector boardDetector = new BoardDetector();

        for (int imageIndex = 1; imageIndex <= numberOfImages; imageIndex++) {

            long startTime = System.nanoTime();
            Mat image = readImageFile(imageSequenceFolder + "/images/", imageIndex);
            cornerDetector.imageIndex = imageIndex;

            Corner[] possibleNewCorners = new Corner[4];
            boolean wereAllCornersFound = true;
            for (int i = 0; i < 4; i++) {
                cornerDetector.setCornerIndex(i + 1);
                Corner possibleNewCorner = cornerDetector.findNewCornerAround(corners[i], image);
                possibleNewCorners[i] = cornerDetector.findNewCornerAround(corners[i], image);
                if (possibleNewCorners[i] == null) {
                    wereAllCornersFound = false;
                }
            }

            Mat ortogonalBoardImage = ImageUtils.generateOrtogonalBoardImage(image, wereAllCornersFound ? possibleNewCorners : corners);
            Imgcodecs.imwrite("processing/ortogonal" + padWithZeroes(imageIndex) + ".jpg", ortogonalBoardImage);
            boardDetector.setImageIndex(imageIndex);

            System.out.println("Frame " + imageIndex);

            if (boardDetector.isBoardContainedIn(ortogonalBoardImage) && wereAllCornersFound) {
                System.out.println("Board is inside countour");
                int numberOfCornersThatMoved = getNumberOfCornersThatMoved(possibleNewCorners, corners);

                for (int i = 0; i < 4; i++) {
                    if (numberOfCornersThatMoved < 4) {
                        // Update relative corner position of possible corners with stones
                        if (possibleNewCorners[i].isStone) {
                            if (!corners[i].isStone) {
                                possibleNewCorners[i].updateDisplacementVectorRelativeTo(corners[i].position);
                            } else if (corners[i].isStone) {
                                possibleNewCorners[i].updateDisplacementVectorRelativeTo(corners[i].getRealCornerPosition());
                            }
                        }
                    } else if (possibleNewCorners[i].isStone) {
                        // All corners moved together, so it was probably a board displacemente and we
                        // don't update the corners's relative position to the real corners
                        possibleNewCorners[i].displacementToRealCorner = corners[i].displacementToRealCorner;
                    }

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

    private static void printCornerPositions(int imageIndex, Corner[] corners) {
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
    // (except if it's a stone placement or the corner detector updating)
    private static boolean isCornerMovementUniform(Corner[] newCorners, Corner[] oldCorners) {
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

    private static int getNumberOfCornersThatMoved(Corner[] possibleNewCorners, Corner[] corners) {
        int numberOfCornersThatMoved = 0;
        for (int i = 0; i < 4; i++) {
            if (possibleNewCorners[i].distanceTo(corners[i]) > MOVEMENT_THRESHOULD) {
                numberOfCornersThatMoved++;
            }
        }
        return numberOfCornersThatMoved;
    }

    private static void printDetectionError(CornerPositionsFile cornerPositionsFile, int imageIndex, Corner[] corners) {
        int distanceToExpectedPosition = 0;
        Corner[] expectedPoints = cornerPositionsFile.getCornerPositions(imageIndex);
        for (int i = 0; i < 4; i++) {
            distanceToExpectedPosition += corners[i].manhattanDistanceTo(expectedPoints[i]);
        }
        System.out.println("Error: " + distanceToExpectedPosition);
    }

    private static void drawBoardContourOnImage(Mat image, Corner[] corners, int imageIndex) {
        Point[] boardCorners = new Point[4];
        boardCorners[0] = new Point(corners[0].position.x, corners[0].position.y);
        boardCorners[1] = new Point(corners[1].position.x, corners[1].position.y);
        boardCorners[2] = new Point(corners[2].position.x, corners[2].position.y);
        boardCorners[3] = new Point(corners[3].position.x, corners[3].position.y);
        MatOfPoint boardContour = new MatOfPoint(boardCorners);

        Point[] realBoardContours = new Point[4];
        realBoardContours[0] = new Point(corners[0].position.x - corners[0].displacementToRealCorner.x, corners[0].position.y - corners[0].displacementToRealCorner.y);
        realBoardContours[1] = new Point(corners[1].position.x - corners[1].displacementToRealCorner.x, corners[1].position.y - corners[1].displacementToRealCorner.y);
        realBoardContours[2] = new Point(corners[2].position.x - corners[2].displacementToRealCorner.x, corners[2].position.y - corners[2].displacementToRealCorner.y);
        realBoardContours[3] = new Point(corners[3].position.x - corners[3].displacementToRealCorner.x, corners[3].position.y - corners[3].displacementToRealCorner.y);
        MatOfPoint realBoardContour = new MatOfPoint(realBoardContours);

        List<MatOfPoint> realContourPoints = new ArrayList<MatOfPoint>();
        realContourPoints.add(realBoardContour);
        Scalar green = new Scalar(0, 255, 0);
        Imgproc.drawContours(image, realContourPoints, -1, green, 2);

        List<MatOfPoint> contourPoints = new ArrayList<MatOfPoint>();
        contourPoints.add(boardContour);
        Scalar red = new Scalar(0, 0, 255);
        Imgproc.drawContours(image, contourPoints, -1, red, 2);

        Imgcodecs.imwrite("output/image" + imageIndex + ".jpg", image);
    }

}
