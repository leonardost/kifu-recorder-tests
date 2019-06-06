import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import src.EllipseDetectorInterface;
import src.FirstEllipseDetector;
import src.SecondEllipseDetector;

public class process_image {

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }
    private final static int NUMBER_OF_IMAGES = 7;

    public static void main(String[] args) {

        List<EllipseDetectorInterface> ellipseDetectors = new ArrayList<>();
        ellipseDetectors.add(new FirstEllipseDetector());
        ellipseDetectors.add(new SecondEllipseDetector());

        for (int imageIndex = 1; imageIndex <= NUMBER_OF_IMAGES; imageIndex++) {
            System.out.println("Image " + imageIndex);
            Mat image = readImageFile("input/", imageIndex);

            for (EllipseDetectorInterface detector : ellipseDetectors) {
                long startTime = System.nanoTime();
                System.out.println();
                System.out.println("--- " + detector.getName() + " ---");

                detector.setImageIndex(imageIndex);
                detector.detectEllipsesIn(image);

                System.out.println();
                System.out.println("Elapsed time = " + (System.nanoTime() - startTime) / 1000000000.0);
            }

            System.out.println("=====================================");
            System.out.println();
        }

    }

    private static Mat readImageFile(String inputFolder, int imageNumber) {
        return Imgcodecs.imread(inputFolder + "/image" + imageNumber + ".jpg");
    }

}
