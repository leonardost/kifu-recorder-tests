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

        for (int imageIndex = 1; imageIndex <= numberOfImages; imageIndex++) {
            long startTime = System.nanoTime();
            Mat image = readImageFile(imageSequenceFolder + "/images/", imageIndex);
            
            stoneDetector.setBoardImage(image);
            Board detectedBoard = stoneDetector.detect();

            // Imgcodecs.imwrite("processing/ortogonal" + padWithZeroes(imageIndex) + ".png", ortogonalBoardImage);
            System.out.println("Frame " + imageIndex);
            System.out.println();
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
        throw new Exception("File " + filename + " does not exist");
    }

    private static String padWithZeroes(int number) {
        String paddedNumber = Integer.toString(number);
        while (paddedNumber.length() < 3) {
            paddedNumber = "0" + paddedNumber;
        }
        return paddedNumber;
    }

}
