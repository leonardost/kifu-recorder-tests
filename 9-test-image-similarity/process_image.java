import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import src.SimilarityCalculatorInterface;
import src.TemplateMatching;

public class process_image {

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }
    private final static int NUMBER_OF_IMAGES = 91;

    public static void main(String[] args) {

        List<SimilarityCalculatorInterface> similarityCalculators = new ArrayList<>();
        similarityCalculators.add(new TemplateMatching());

        double similarityMatrix[][][] = new double[similarityCalculators.size()][NUMBER_OF_IMAGES][NUMBER_OF_IMAGES];

        Mat[] images = new Mat[NUMBER_OF_IMAGES];
        for (int i = 0; i < NUMBER_OF_IMAGES; i++) {
            images[i] = readImageFile("input/", i);
        }

        for (int i = 0; i < similarityCalculators.size(); i++) {
            System.out.println("Method " + i);

            for (int imageIndex = 0; imageIndex < NUMBER_OF_IMAGES; imageIndex++) {
                System.out.println("    Image " + (imageIndex + 1));

                for (int otherImageIndex = 0; otherImageIndex < NUMBER_OF_IMAGES; otherImageIndex++) {
                    if (imageIndex == otherImageIndex) continue;

                    similarityMatrix[i][imageIndex][otherImageIndex] = similarityCalculators
                        .get(i)
                        .calculateSimilatiryBetween(images[imageIndex], images[otherImageIndex]);
                    similarityMatrix[i][otherImageIndex][imageIndex] = similarityMatrix[i][imageIndex][otherImageIndex];
                    System.out.println("        Image " + otherImageIndex + ": " + similarityMatrix[i][imageIndex][otherImageIndex]);
                }

                System.out.println("=====================================");
                System.out.println();
            }
        }
    }

    private static Mat readImageFile(String inputFolder, int imageNumber) {
        return Imgcodecs.imread(inputFolder + "/corner2_image" + imageNumber + ".png");
    }

}
