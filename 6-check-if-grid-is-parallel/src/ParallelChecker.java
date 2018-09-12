package src;

import java.util.ArrayList;
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

import src.Line;
import src.LineCluster;

public class ParallelChecker
{
    public int imageIndex;

    public double check(Mat ortogonalBoardImage)
    {
        Mat imageWithBordersDetected = detectBordersIn(ortogonalBoardImage);
        // outputImageWithBorders(imageWithBordersDetected);

        List<Line> detectedLines = detectLinesIn(imageWithBordersDetected);
        System.out.println("Number of ortogonal lines found = " + detectedLines.size());
        outputImageWithDetectedLines(ortogonalBoardImage, detectedLines);

        return 0;
    }

    private Mat detectBordersIn(Mat image)
    {
        Mat imageWithBordersDetected = new Mat();
        Imgproc.Canny(image, imageWithBordersDetected, 50, 100);
        Imgproc.dilate(imageWithBordersDetected, imageWithBordersDetected, Mat.ones(3, 3, CvType.CV_32F));
        return imageWithBordersDetected;
    }

    private void outputImageWithBorders(Mat imageWithBordersDetected)
    {
        Imgcodecs.imwrite("processing/ortogonal_with_borders_detected_" + imageIndex + ".jpg", imageWithBordersDetected);
    }

    private List<Line> detectLinesIn(Mat image)
    {
        Mat detectedLines = new Mat();
        Imgproc.HoughLines(image, detectedLines, 1, Math.PI / 180, 450);
        return getAverageLines(detectedLines);
    }

    private List<Line> getAverageLines(Mat detectedLines)
    {
        List<Line> allLines = new ArrayList<>();
        List<LineCluster> lineClusters = new ArrayList<>();

        // https://stackoverflow.com/questions/47389128/opencv-houghline-only-detect-one-line-in-image
        for (int i = 0; i < detectedLines.rows(); i++) {
            double[] line = detectedLines.get(i, 0);
            double rho = line[0];
            double theta = line[1];
            Line detectedLine = new Line(rho, theta);

            if (!detectedLine.isOrtogonal()) continue;

            allLines.add(detectedLine);
            addLineToNearestClusterOrCreateNew(detectedLine, lineClusters);
        }

        List<Line> averageLines = new ArrayList<>();
        for (LineCluster lineCluster : lineClusters) {
            averageLines.add(lineCluster.getAverageLine());
        }

        // return allLines;
        return averageLines;
    }

    private void addLineToNearestClusterOrCreateNew(Line line, List<LineCluster> lineClusters)
    {
        boolean foundCluster = false;

        for (LineCluster lineCluster : lineClusters) {
            if (lineCluster.isCloseEnoughTo(line)) {
                lineCluster.add(line);
                foundCluster = true;
                break;
            }
        }

        if (!foundCluster) {
            LineCluster lineCluster = new LineCluster();
            lineCluster.add(line);
            lineClusters.add(lineCluster);
        }
    }

    private void outputImageWithDetectedLines(Mat image, List<Line> lines)
    {
        Mat imageWithLinesDrawn = image.clone();

        for (Line line : lines) {
            Point[] points = line.getPoints();
            Imgproc.line(imageWithLinesDrawn, points[0], points[1], new Scalar(0, 0, 255), 2);
        }
        Imgcodecs.imwrite("processing/ortogonal_with_lines_detected_" + imageIndex + ".jpg", imageWithLinesDrawn);
    }

}