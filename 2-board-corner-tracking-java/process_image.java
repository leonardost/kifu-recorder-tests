import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.List;
import java.io.File;
import java.util.ArrayList;

import src.boardDetector.BoardDetector;
import src.cornerDetector.Corner;
import src.cornerDetector.CornerDetector;
import src.similarityCalculator.FingerprintMatching;
import src.similarityCalculator.SimilarityCalculatorInterface;
import src.Ponto;
import src.ImageUtils;

public class process_image {

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public final static int RADIUS_OF_REGION_OF_INTEREST = 50;
    // Any corner that moves at least this is considered to have moved
    private final static int MOVEMENT_THRESHOULD = 10;

    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            printUsage();
            return;
        }

        String imageSequenceFolder = args[0];
        CornerPositionsFile cornerPositionsFile = new CornerPositionsFile(imageSequenceFolder);

        int numberOfImages = cornerPositionsFile.getNumberOfImages();
        Corner[] corners = cornerPositionsFile.getInitialCornersPositions();
        BoardDetector boardDetector = new BoardDetector();
        SimilarityCalculatorInterface fingerprintMatching = new FingerprintMatching();
        Mat lastValidOrtogonalBoardImage = null;

        // Let's assign a corner detector for each corner separately,
        // each one responsible for tracking its corner
        CornerDetector[] cornerDetector = new CornerDetector[4];
        for (int i = 0; i < 4; i++) {
            cornerDetector[i] = new CornerDetector();
        }

        for (int imageIndex = 1; imageIndex <= numberOfImages; imageIndex++) {

            long startTime = System.nanoTime();
            Mat image = readImageFile(imageSequenceFolder + "/images/", imageIndex);

            Corner[] possibleNewCorners = new Corner[4];
            boolean wereAllCornersFound = true;
            for (int i = 0; i < 4; i++) {
                cornerDetector[i].setImageIndex(imageIndex);
                cornerDetector[i].setCornerIndex(i + 1);
                cornerDetector[i].setCorner(corners[i]);
                possibleNewCorners[i] = cornerDetector[i].detectCornerIn(image);
                if (possibleNewCorners[i] == null) {
                    wereAllCornersFound = false;
                }
            }

            Mat ortogonalBoardImage = ImageUtils.generateOrtogonalBoardImage(image, wereAllCornersFound ? possibleNewCorners : corners);
            Imgcodecs.imwrite("processing/ortogonal" + padWithZeroes(imageIndex) + ".png", ortogonalBoardImage);
            boardDetector.setImageIndex(imageIndex);

            System.out.println("Frame " + imageIndex);

            if (boardDetector.isBoardContainedIn(ortogonalBoardImage) && wereAllCornersFound) {
                System.out.println("Board is inside countour");
                int numberOfCornersThatMoved = getNumberOfCornersThatMoved(possibleNewCorners, corners);
                System.out.println("Number of corners that moved: " + numberOfCornersThatMoved);
                int numberOfEmptyCornersThatMoved = getNumberOfEmptyCornersThatMoved(possibleNewCorners, corners);
                System.out.println("Number of empty corners that moved: " + numberOfCornersThatMoved);

                double[] distanceToNewPoint = new double[4];
                for (int i = 0; i < 4; i++) {
                    distanceToNewPoint[i] = possibleNewCorners[i].distanceTo(corners[i]);
                    System.out.println("Distance to old corner point " + (i + 1) + " = " + distanceToNewPoint[i]);
                }

                for (int i = 0; i < 4; i++) {
                    if (numberOfCornersThatMoved < 4) {
                        // Not all corners moved, so this is probably a corner adjustment
                        // Update relative corner position of possible corners with stones
                        if (possibleNewCorners[i].isStone) {
                            if (!corners[i].isStone) {
                                possibleNewCorners[i].updateDisplacementVectorRelativeTo(corners[i].position);
                            } else if (corners[i].isStone) {
                                possibleNewCorners[i].updateDisplacementVectorRelativeTo(corners[i].getRealCornerPosition());
                            }
                        }
                    } else if (possibleNewCorners[i].isStone) {
                        // All corners moved together, so this is probably a board displacement and we
                        // don't update the corners's relative position to the real corners
                        possibleNewCorners[i].displacementToRealCorner = corners[i].displacementToRealCorner;
                    }
                }

                Mat ortogonalBoardImage2 = ImageUtils.generateOrtogonalBoardImage(image, possibleNewCorners);
                double similarity = lastValidOrtogonalBoardImage != null ? fingerprintMatching.calculateSimilatiryBetween(lastValidOrtogonalBoardImage, ortogonalBoardImage2) : -1;
                System.out.println("Similarity between new ortogonal board image to last valid one = " + similarity);

                if (lastValidOrtogonalBoardImage == null || fingerprintMatching.areImagesSimilar(lastValidOrtogonalBoardImage, ortogonalBoardImage2)) {
                    System.out.println("New ortogonal board image is similar to last valid one");
                    for (int i = 0; i < 4; i++) {
                        if (!corners[i].isStone && !possibleNewCorners[i].isStone && numberOfCornersThatMoved < 3 && numberOfEmptyCornersThatMoved == 1) {
                            // This means a single empty corner moved by itself, which is not possible. This addresses a wrong
                            // corner detection in frame 70 of sequence 16.
                            System.out.println("This empty corner moved by itself");
                            continue;
                        }
                        // if (!possibleNewCorners[i].isStone && corners[i].isStone && possibleNewCorners[i].distanceTo(corners[i].getRealCornerPosition()) > MOVEMENT_THRESHOULD) {
                        //     // If a corner was a stone and is not anymore, the new empty corner should match the real corner
                        //     // position that the stone was on. This addresses a wrong corner detection in frame 74 of sequence 14.
                        //     System.out.println("This now empty corner is in a wrong position");
                        //     continue;
                        // }
                        corners[i] = possibleNewCorners[i];
                    }
                    lastValidOrtogonalBoardImage = ortogonalBoardImage2.clone();
                } else {
                    System.out.println("New ortogonal board image is NOT similar to last valid one");
                }

            } else {
                System.out.println("Board is NOT inside countour");
            }

            Mat ortogonalBoardImage2 = ImageUtils.generateOrtogonalBoardImage(image, corners);
            Imgcodecs.imwrite("processing/ortogonal_2_" + padWithZeroes(imageIndex) + ".png", ortogonalBoardImage2);

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

    private static Mat readImageFile(String inputFolder, int imageNumber) throws Exception {
        String filename = inputFolder + "/frame" + imageNumber;
        String[] possibleExtensions = { ".jpg", ".png" };
        for (String extension : possibleExtensions) {
            File file = new File(filename + extension);
            if (file.exists()) {
                return Imgcodecs.imread(file.getAbsolutePath());
            }
        }
        throw new Exception("File " + filename + " does not exist");
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

    private static int getNumberOfEmptyCornersThatMoved(Corner[] possibleNewCorners, Corner[] corners) {
        int numberOfEmptyCornersThatMoved = 0;
        for (int i = 0; i < 4; i++) {
            if (!possibleNewCorners[i].isStone
                    // && !corners[i].isStone
                    && possibleNewCorners[i].distanceTo(corners[i]) > MOVEMENT_THRESHOULD) {
                numberOfEmptyCornersThatMoved++;
            }
        }
        return numberOfEmptyCornersThatMoved;
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
        realBoardContours[0] = new Point(corners[0].getRealCornerPosition().x, corners[0].getRealCornerPosition().y);
        realBoardContours[1] = new Point(corners[1].getRealCornerPosition().x, corners[1].getRealCornerPosition().y);
        realBoardContours[2] = new Point(corners[2].getRealCornerPosition().x, corners[2].getRealCornerPosition().y);
        realBoardContours[3] = new Point(corners[3].getRealCornerPosition().x, corners[3].getRealCornerPosition().y);
        MatOfPoint realBoardContour = new MatOfPoint(realBoardContours);

        List<MatOfPoint> realContourPoints = new ArrayList<MatOfPoint>();
        realContourPoints.add(realBoardContour);
        Scalar green = new Scalar(0, 255, 0);
        Imgproc.drawContours(image, realContourPoints, -1, green, 2);

        List<MatOfPoint> contourPoints = new ArrayList<MatOfPoint>();
        contourPoints.add(boardContour);
        Scalar red = new Scalar(0, 0, 255);
        Imgproc.drawContours(image, contourPoints, -1, red, 2);

        Imgcodecs.imwrite("output/image" + imageIndex + ".png", image);
    }

}
