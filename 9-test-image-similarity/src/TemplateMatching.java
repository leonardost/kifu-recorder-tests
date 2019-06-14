package src;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class TemplateMatching implements SimilarityCalculatorInterface
{
    public double calculateSimilatiryBetween(Mat image1, Mat image2)
    {
        // Imgproc.matchTemplate(image, templ, result, method);
        Mat result = new Mat();
        Imgproc.matchTemplate(image2, image1, result, Imgproc.TM_SQDIFF_NORMED);
        Core.MinMaxLocResult minMaxLoc = Core.minMaxLoc(result);
        System.out.println(minMaxLoc.minLoc);
        return 0;
    }
}