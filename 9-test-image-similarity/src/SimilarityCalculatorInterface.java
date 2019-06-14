package src;

import org.opencv.core.Mat;

public interface SimilarityCalculatorInterface
{
    public double calculateSimilatiryBetween(Mat image1, Mat image2);
}
