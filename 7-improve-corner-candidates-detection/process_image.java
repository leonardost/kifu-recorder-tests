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
        int numberOfImages = 3;

        for (int imageIndex = 1; imageIndex <= numberOfImages; imageIndex++) {

            long startTime = System.nanoTime();
            cornerDetector.setImageIndex(imageIndex);
            Mat image = readImageFile("input/", imageIndex);

            List<Corner> possibleNewCorners = cornerDetector.detectCandidateCornersIn(image);

            // printDetectionError(cornerPositionsFile, imageIndex, corners);

            System.out.println();
            System.out.println("Elapsed time = " + (System.nanoTime() - startTime) / 1000000000.0);
            System.out.println("=====");
        }

    }

    private static Mat readImageFile(String inputFolder, int imageNumber) {
        return Imgcodecs.imread(inputFolder + "/image" + imageNumber + ".jpg");
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
