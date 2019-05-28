package src.cornerDetector;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import src.Ponto;

public class CornerDetector {

    public int imageIndex;

    private HarrisCornerDetector harrisCornerDetector = new HarrisCornerDetector();
    private EllipseCornerDetector ellipseCornerDetector = new EllipseCornerDetector();

    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
        harrisCornerDetector.imageIndex = imageIndex;
        ellipseCornerDetector.imageIndex = imageIndex;
    };

    public List<Corner> findCornerCandidatesIn(Mat image) {
        List<Corner> candidateCorners = new ArrayList<>();
        List<Corner> candidateCornerHarris = harrisCornerDetector.detectCandidateCornersIn(image);
        List<Corner> candidateCornerEllipsis = ellipseCornerDetector.detectCandidateCornersIn(image);
        // List<Corner> candidateCornerEllipsis = new ArrayList<>();

        Mat imageWithCornersPlotted = image.clone();

        // Remove Harris corner candidates that are too close to circle corner candidates
        // This is done to try to remove corner candidates that appear on the edge of circles
        for (Iterator<Corner> it = candidateCornerHarris.iterator(); it.hasNext();) {
            Corner point = it.next();
            for (Corner circlePoint : candidateCornerEllipsis) {
                if (circlePoint.isTooCloseToCircle(point.position)) {
                    it.remove();
                    break;
                }
            }
        }

        for (Corner point : candidateCornerEllipsis) {
            System.out.println("Candidate corner found by circle detection in image " + imageIndex + ": ");
            System.out.println(point);
            System.out.println("width of rotated rectangle:");
            System.out.println(point.stonePosition.size.width);
            System.out.println("height of rotated rectangle:");
            System.out.println(point.stonePosition.size.height);
            System.out.println("angle of rotated rectangle:");
            System.out.println(point.stonePosition.angle);
            Imgproc.circle(imageWithCornersPlotted, new Point(point.getX(), point.getY()), 3, new Scalar(0, 255, 0), -1);
            Imgproc.ellipse(imageWithCornersPlotted, point.stonePosition, new Scalar(0, 255, 255));
        }

        Random random = new Random();
        for (Corner point : candidateCornerHarris) {
            System.out.println("Candidate corner found by corner Harris detection in frame " + imageIndex + ": ");
            System.out.println(point);
            Imgproc.circle(imageWithCornersPlotted, new Point(point.getX(), point.getY()), 1, new Scalar(0, 0, 255), -1);
        }

        Imgcodecs.imwrite("processing/image" + imageIndex + "_candidate_corners.png", imageWithCornersPlotted);
        Imgcodecs.imwrite("processing/image" + imageIndex + "_candidate_corners.jpg", imageWithCornersPlotted);
        candidateCorners.addAll(candidateCornerHarris);
        candidateCorners.addAll(candidateCornerEllipsis);

        // A corner should have at most 4 candidates, be them Harris corners or ellipsis corners
        // More than that probably means something is wrong in the detection, or there's something
        // else in the scene, like a player's hand or something else
        if (candidateCorners.size() > 4) return null;

        return candidateCorners;
    }

}
