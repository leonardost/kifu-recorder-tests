import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import src.ImageUtils;
import src.Ponto;
import src.ParallelChecker;
import src.cornerDetector.CornerDetectorTemplateMatching;

public class check_if_grid_is_parallel {

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static void main(String[] args) {

        if (args.length != 1) {
            return;
        }

        String imageSequenceFolder = args[0];
        File imageFolder = new File(imageSequenceFolder);
        File[] imageFiles = imageFolder.listFiles();
        Arrays.sort(imageFiles);

        ParallelChecker parallelChecker = new ParallelChecker();
        int imageIndex = 1;

        for (File imageFile : imageFiles) {
            if (imageFile.isDirectory()) continue;

            System.out.println("Processing image " + imageFile.getName());
            parallelChecker.imageIndex = imageIndex;
            long startTime = System.nanoTime();

            Mat image = Imgcodecs.imread(imageFile.getAbsolutePath());
            System.out.println("parallel value = " + parallelChecker.check(image));

            System.out.println();
            System.out.println("Elapsed time = " + (System.nanoTime() - startTime) / 1000000000.0);
            System.out.println("=====");

            imageIndex++;
        }

    }

    private static Mat readImageFile(String inputFolder, int imageNumber) {
        return Imgcodecs.imread(inputFolder + "/frame" + imageNumber + ".jpg");
    }

    private static void printCornerPositions(int imageIndex, Ponto[] corners) {
        for (int i = 0; i < 4; i++) {
            System.out.print("Corner " + (i + 1) + ": ");
            System.out.println(corners[i]);
        }
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
        Mat imageWithContour = image.clone();
        Imgproc.drawContours(imageWithContour, contourPoints, -1, red, 2);
        Imgcodecs.imwrite("output/image" + imageIndex + ".jpg", imageWithContour);
    }

}
