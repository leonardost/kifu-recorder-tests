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

import src.models.Board;
import src.models.Game;
import src.similarityCalculator.FingerprintMatching;
import src.similarityCalculator.SimilarityCalculatorInterface;
import src.stoneDetector.StoneDetector;

public class process_image {

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public final static int RADIUS_OF_REGION_OF_INTEREST = 50;

    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            // printUsage();
            return;
        }

        String imageSequenceFolder = args[0];
        int numberOfImages = new File(imageSequenceFolder + "/images").list().length;
        SimilarityCalculatorInterface fingerprintMatching = new FingerprintMatching();
        StoneDetector stoneDetector = new StoneDetector();
        stoneDetector.setBoardDimension(19);

        Game game = new Game(19, "", "", "6.5");

        for (int imageIndex = 1; imageIndex <= numberOfImages; imageIndex++) {
            long startTime = System.nanoTime();
            Mat image = readImageFile(imageSequenceFolder + "/images/", imageIndex);
            if (image == null) continue;
            stoneDetector.setBoardImage(image);

            Board detectedBoard = game.getNumberOfMoves() == 0
                ? stoneDetector.detect()
                : stoneDetector.detect(game.getLastBoard(), game.canNextMoveBe(Board.BLACK_STONE), game.canNextMoveBe(Board.WHITE_STONE));
            game.addNewMoveFrom(detectedBoard);
            Board detectedBoardNoInfo = stoneDetector.detect();

            System.out.println("Frame " + imageIndex);
            System.out.println("Detected board with no game information");
            System.out.println(detectedBoardNoInfo);
            System.out.println("Detected board with game information");
            System.out.println(detectedBoard);
            System.out.println();
            System.out.println("Elapsed time = " + (System.nanoTime() - startTime) / 1000000000.0);
            System.out.println("=====");
        }

    }

    private static Mat readImageFile(String inputFolder, int imageNumber) throws Exception {
        String filename = inputFolder + "/ortogonal_2_" + padWithZeroes(imageNumber);
        String[] possibleExtensions = { ".jpg", ".png" };
        for (String extension : possibleExtensions) {
            File file = new File(filename + extension);
            if (file.exists()) {
                return Imgcodecs.imread(file.getAbsolutePath());
            }
        }
        return null;
        // throw new Exception("File " + filename + " does not exist");
    }

    private static String padWithZeroes(int number) {
        String paddedNumber = Integer.toString(number);
        while (paddedNumber.length() < 3) {
            paddedNumber = "0" + paddedNumber;
        }
        return paddedNumber;
    }

}
