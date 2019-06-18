import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import src.FingerprintMatching;
import src.SimilarityCalculatorInterface;
import src.TemplateMatching;

public class process_image {

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private final static int NUMBER_OF_IMAGES = 93;

    public static void main(String[] args) {

        List<SimilarityCalculatorInterface> similarityCalculators = new ArrayList<>();
        similarityCalculators.add(new TemplateMatching());
        similarityCalculators.add(new FingerprintMatching());

        double similarityMatrix[][][] = new double[similarityCalculators.size()][NUMBER_OF_IMAGES][NUMBER_OF_IMAGES];

        Mat[] images = new Mat[NUMBER_OF_IMAGES];
        for (int i = 0; i < NUMBER_OF_IMAGES; i++) {
            images[i] = readImageFile("input/", i + 1);
            images[i] = convertToGrayscale(images[i], i + 1);
            images[i] = blur(images[i], i + 1);
        }

        for (int i = 0; i < similarityCalculators.size(); i++) {
            SimilarityCalculatorInterface similarityCalculator = similarityCalculators.get(i);
            System.out.println("Method " + i);

            for (int imageIndex = 0; imageIndex < NUMBER_OF_IMAGES; imageIndex++) {
                System.out.println("    Image " + (imageIndex + 1));

                for (int otherImageIndex = imageIndex; otherImageIndex < NUMBER_OF_IMAGES; otherImageIndex++) {
                    similarityMatrix[i][imageIndex][otherImageIndex] = similarityCalculator
                        .calculateSimilatiryBetween(images[imageIndex], images[otherImageIndex]);
                    similarityMatrix[i][otherImageIndex][imageIndex] = similarityMatrix[i][imageIndex][otherImageIndex];
                    System.out.println("        Image " + (otherImageIndex + 1) + ": " + similarityMatrix[i][imageIndex][otherImageIndex]);
                }

                System.out.println("=====================================");
                System.out.println();
            }
        }
    }

    private static Mat readImageFile(String inputFolder, int imageNumber) {
        return Imgcodecs.imread(inputFolder + "/corner2_frame" + imageNumber + ".png");
    }

    private static Mat convertToGrayscale(Mat image, int imageIndex)
    {
        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(image, grayscaleImage, Imgproc.COLOR_BGR2GRAY, 1); // 1 channel
        // Imgcodecs.imwrite("processing/image" + imageIndex + "_preprocessed1.jpg", grayscaleImage);
        return grayscaleImage;
    }

    private static Mat blur(Mat image, int imageIndex)
    {
        Mat blurredImage = image.clone();
        Imgproc.blur(blurredImage, blurredImage, new Size(5, 5));
        // Imgcodecs.imwrite("processing/image" + imageIndex + "_preprocessed2.jpg", blurredImage);
        return blurredImage;
    }

}
