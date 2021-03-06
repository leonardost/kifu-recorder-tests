import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
            return;
        }

        String imageSequenceFolder = args[0];
        int numberOfImages = new File(imageSequenceFolder + "/images").list().length;
        BoardStatesFile boardStatesFile = new BoardStatesFile(imageSequenceFolder);
        SimilarityCalculatorInterface fingerprintMatching = new FingerprintMatching();
        StoneDetector stoneDetector = new StoneDetector();
        stoneDetector.setBoardDimension(19);

        Game game = new Game(19, "", "", "6.5");

        System.out.println("Running experiment 10 on " + imageSequenceFolder);
        System.out.println("=====");
        long totalStartTime = System.nanoTime();

        for (int imageIndex = 1; imageIndex <= numberOfImages; imageIndex++) {

            Board expectedBoard = boardStatesFile.getBoard(imageIndex);
            if (expectedBoard == null) continue;

            long startTime = System.nanoTime();
            Mat image = readImageFile(imageSequenceFolder + "/images/", imageIndex);
            if (image == null) continue;

            // Transform image to HSV and split channels
            // Mat hsvImage = new Mat();
            // Imgproc.cvtColor(image, hsvImage, Imgproc.COLOR_BGR2HSV, 3); // 3 channels
            // Imgcodecs.imwrite("processing/image" + imageIndex + "_hsv.png", hsvImage);
            // List<Mat> hsvChannels = new ArrayList<Mat>(3);
            // Core.split(hsvImage, hsvChannels);
            // Imgcodecs.imwrite("processing/image" + imageIndex + "_hsv_hue.png", hsvChannels.get(0));
            // Imgcodecs.imwrite("processing/image" + imageIndex + "_hsv_saturation.png", hsvChannels.get(1));
            // Imgcodecs.imwrite("processing/image" + imageIndex + "_hsv_value.png", hsvChannels.get(2));

            stoneDetector.setBoardImage(image);
            Board detectedBoard = stoneDetector.detectBasedOn(game);

            System.out.println("Frame " + imageIndex);
            System.out.println("Detected board");
            System.out.println(detectedBoard);
            System.out.println("Expected board");
            System.out.println(expectedBoard);
            System.out.println("Number of differences = " + expectedBoard.getNumberOfDifferencesBetweenThisAnd(detectedBoard));
            System.out.println();
            System.out.println("Elapsed time = " + (System.nanoTime() - startTime) / 1000000000.0);
            System.out.println("=====");
        }

        System.out.println("Total elapsed time = " + (System.nanoTime() - totalStartTime) / 1000000000.0);
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
