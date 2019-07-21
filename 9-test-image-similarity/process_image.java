import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import src.FingerprintMatching;
import src.OrbFeaturesMatching;
import src.SimilarityCalculatorInterface;
import src.TemplateMatching;

public class process_image {

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private final static int[] NUMbER_OF_IMAGES = { 93, 114, 91 };

    public static void main(String[] args) throws Exception {
        List<SimilarityCalculatorInterface> similarityCalculators = new ArrayList<>();
        similarityCalculators.add(new TemplateMatching());
        similarityCalculators.add(new FingerprintMatching());
        // similarityCalculators.add(new OrbFeaturesMatching());

        int testNumber = Integer.parseInt(args[0]);
        String inputFolder = "input" + testNumber;
        int numberOfImages = NUMbER_OF_IMAGES[testNumber - 1];

        Mat[] images = new Mat[numberOfImages];
        Mat[] originalImages = new Mat[numberOfImages];
        double similarityMatrix[][][] = new double[similarityCalculators.size()][numberOfImages][numberOfImages];        

        for (int i = 0; i < numberOfImages; i++) {
            originalImages[i] = readImageFile(inputFolder, i + 1);
            images[i] = readImageFile(inputFolder, i + 1);
            images[i] = convertToGrayscale(images[i], i + 1);
            images[i] = blur(images[i], i + 1);
        }

        SimilarityCalculatorInterface similarityCalculator;
        for (int imageIndex = 0; imageIndex < numberOfImages; imageIndex++) {
            System.out.println("Image " + (imageIndex + 1));

            for (int otherImageIndex = imageIndex; otherImageIndex < numberOfImages && otherImageIndex < imageIndex + 10; otherImageIndex++) {
                System.out.println("    Image " + (otherImageIndex + 1));

                for (int i = 0; i < similarityCalculators.size(); i++) {
                    similarityCalculator = similarityCalculators.get(i);
                    similarityCalculator.setImageNumber(imageIndex);

                    if (i == 2) {
                        similarityMatrix[i][imageIndex][otherImageIndex] = similarityCalculator
                            .calculateSimilatiryBetween(originalImages[imageIndex], originalImages[otherImageIndex]);
                    } else {
                        similarityMatrix[i][imageIndex][otherImageIndex] = similarityCalculator
                            .calculateSimilatiryBetween(images[imageIndex], images[otherImageIndex]);
                    }

                    System.out.println("        Method " + i + ": " + similarityMatrix[i][imageIndex][otherImageIndex]);
                }
            }

            System.out.println("=====================================");
            System.out.println();
        }
    }

    private static Mat readImageFile(String inputFolder, int imageNumber) throws Exception {
        String[] filenames = {
            inputFolder + "/corner2_frame" + imageNumber + ".png",
            inputFolder + "/ortogonal_2_" + padWithZeroes(imageNumber) + ".png"
        };
        for (String filename : filenames) {
            File file = new File(filename);
            if (file.exists()) {
                return Imgcodecs.imread(file.getAbsolutePath());
            }
        }
        throw new Exception("File does not exist");
    }

    private static String padWithZeroes(int number) {
        String paddedNumber = Integer.toString(number);
        while (paddedNumber.length() < 3) {
            paddedNumber = "0" + paddedNumber;
        }
        return paddedNumber;
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
