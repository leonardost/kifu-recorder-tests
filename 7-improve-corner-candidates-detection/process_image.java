import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.List;
import java.util.ArrayList;

import src.cornerDetector.Corner;
import src.cornerDetector.CornerDetector;
import src.Ponto;
import src.ImageUtils;

public class process_image {

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static void main(String[] args) {

        CornerDetector cornerDetector = new CornerDetector();
        int numberOfImages = 1;

        for (int imageIndex = 1; imageIndex <= numberOfImages; imageIndex++) {

            long startTime = System.nanoTime();
            cornerDetector.imageIndex = imageIndex;

            Corner[] possibleNewCorners = new Corner[4];
            boolean wereAllCornersFound = true;
            for (int i = 0; i < 4; i++) {
                cornerDetector.setCornerIndex(i + 1);
                possibleNewCorners[i] = cornerDetector.findNewCornerAround(corners[i], image);
                if (possibleNewCorners[i] == null) {
                    wereAllCornersFound = false;
                }
            }

            printCornerPositions(imageIndex, corners);
            // printDetectionError(cornerPositionsFile, imageIndex, corners);

            System.out.println();
            System.out.println("Elapsed time = " + (System.nanoTime() - startTime) / 1000000000.0);
            System.out.println("=====");
        }

    }

    private static Mat readImageFile(String inputFolder, int imageNumber) {
        return Imgcodecs.imread(inputFolder + "/frame" + imageNumber + ".jpg");
    }

    private static void printCornerPositions(int imageIndex, Corner[] corners) {
        for (int i = 0; i < 4; i++) {
            System.out.print("Corner " + (i + 1) + ": ");
            System.out.println(corners[i]);
        }
    }

    private static void printDetectionError(CornerPositionsFile cornerPositionsFile, int imageIndex, Corner[] corners) {
        int distanceToExpectedPosition = 0;
        Corner[] expectedPoints = cornerPositionsFile.getCornerPositions(imageIndex);
        for (int i = 0; i < 4; i++) {
            distanceToExpectedPosition += corners[i].manhattanDistanceTo(expectedPoints[i]);
        }
        System.out.println("Error: " + distanceToExpectedPosition);
    }

}
