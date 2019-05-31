import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import src.EllipseDetector;

public class process_image {

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static void main(String[] args) {

        int numberOfImages = 4;

        for (int imageIndex = 1; imageIndex <= numberOfImages; imageIndex++) {
            long startTime = System.nanoTime();
            Mat image = readImageFile("input/", imageIndex);

            EllipseDetector ellipseDetector = new EllipseDetector();
            ellipseDetector.setImageIndex(imageIndex);
            ellipseDetector.detectEllipsisIn(image);

            System.out.println();
            System.out.println("Elapsed time = " + (System.nanoTime() - startTime) / 1000000000.0);
            System.out.println("=====");
        }

    }

    private static Mat readImageFile(String inputFolder, int imageNumber) {
        return Imgcodecs.imread(inputFolder + "/image" + imageNumber + ".jpg");
    }

}
