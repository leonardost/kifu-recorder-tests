import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

public class calculate_similarity {

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static void main(String[] args) {
        for (int i = 0; i <= 6; i++) {
            Mat imageA = Imgcodecs.imread("image" + i + "a.jpg");
            Mat imageB = Imgcodecs.imread("image" + i + "b.jpg");

            SimilarityCalculator similarityCalculator = new SimilarityCalculator(imageA, imageB);

            System.out.println("Similarity results for image pair " + i);
            System.out.println();
            System.out.println("L2 similarity:");
            System.out.println(similarityCalculator.l2similarity());
            System.out.println("L2 similarity in grayscale:");
            System.out.println(similarityCalculator.l2similarityGrayscale());
            System.out.println("Template matching ccoeff:");
            System.out.println(similarityCalculator.templateMatchingCcoeff());
            System.out.println("Template matching sqdiff:");
            System.out.println(similarityCalculator.templateMatchingSqdiff());
            System.out.println("ORB feature matching:");
            System.out.println(similarityCalculator.orbFeatureMatching());
            System.out.println();
            System.out.println("==========");
            System.out.println();
        }
    }

    private static void printUsage() {
        System.out.println("Usage: calculate_similarity");
        System.out.println();
        System.out.println("The output of this algorithm is the image similarity according to different similarity metrics");
    }

}
