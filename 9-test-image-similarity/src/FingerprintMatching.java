package src;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class FingerprintMatching implements SimilarityCalculatorInterface
{
    public double calculateSimilatiryBetween(Mat image1, Mat image2)
    {
        Mat smallImage1 = new Mat();
        Mat smallImage2 = new Mat();
        Imgproc.resize(image1, smallImage1, new Size(16, 16));
        Imgproc.resize(image2, smallImage2, new Size(16, 16));
        Imgcodecs.imwrite("processing/image_small1.jpg", smallImage1);
        return 0;
        // Imgproc.matchTemplate(image2, image1, result, Imgproc.TM_SQDIFF_NORMED);
        // Core.MinMaxLocResult minMaxLoc = Core.minMaxLoc(result);
        // System.out.println("        " + minMaxLoc.maxLoc);
        // System.out.println("        " + minMaxLoc.maxVal);
        // return minMaxLoc.maxVal;
    }
}