package src.cornerDetector;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import src.Ponto;

public class CornerDetectorTemplateMatching {

    public final static boolean SHOULD_APPLY_CANNY_FILTER = true;
    public final static int RADIUS_OF_REGION_OF_INTEREST = 100;

    public int imageIndex;
    private Mat[] lastCornerRegionImages;

    private Mat detectBordersIn(Mat image) {
        Mat imageWithBordersDetected = new Mat();
        Imgproc.Canny(image, imageWithBordersDetected, 50, 100);
        Imgproc.dilate(imageWithBordersDetected, imageWithBordersDetected, Mat.ones(3, 3, CvType.CV_32F));
        return imageWithBordersDetected;
    }

    public Ponto[] findPossibleNewCorners(Mat image, Ponto[] corners) {
        if (lastCornerRegionImages == null) {
            lastCornerRegionImages = new Mat[4];
            for (int i = 0; i < 4; i++) {
                lastCornerRegionImages[i] = SHOULD_APPLY_CANNY_FILTER ? 
                    detectBordersIn(getRegionOfInterestAround(corners[i], image)) :
                    getRegionOfInterestAround(corners[i], image);
                Imgcodecs.imwrite("processing/corner_region_" + (i + 1) + "_frame" + imageIndex + ".jpg", lastCornerRegionImages[i]);
            }
            return corners;
        }

        Mat imageWithBordersIndentified = detectBordersIn(image);
        Ponto[] possibleNewCorners = new Ponto[4];
        for (int i = 0; i < 4; i++) {
            if (SHOULD_APPLY_CANNY_FILTER) {
                possibleNewCorners[i] = updateCorner(imageWithBordersIndentified, corners[i], i);
            } else {
                possibleNewCorners[i] = updateCorner(image, corners[i], i);
            }
        }
        return possibleNewCorners;
    }

    private Ponto updateCorner(Mat image, Ponto corner, int cornerIndex) {
        Mat regionImage = SHOULD_APPLY_CANNY_FILTER ?
            detectBordersIn(getRegionOfInterestAround(corner, image)) :
            getRegionOfInterestAround(corner, image);
        Imgcodecs.imwrite("processing/corner_region_" + (cornerIndex + 1) + "_frame" + imageIndex + ".jpg", regionImage);
        Ponto candidateTemplateMatching = detectCornerByTemplateMatching(lastCornerRegionImages[cornerIndex], image);

        lastCornerRegionImages[cornerIndex] = SHOULD_APPLY_CANNY_FILTER ?
            detectBordersIn(getRegionOfInterestAround(candidateTemplateMatching, image)) :
            getRegionOfInterestAround(candidateTemplateMatching, image);

        return candidateTemplateMatching;
    }

    private Mat getRegionOfInterestAround(Ponto point, Mat image) {
        int x = point.x - RADIUS_OF_REGION_OF_INTEREST > 0 ? point.x - RADIUS_OF_REGION_OF_INTEREST : 0;
        int y = point.y - RADIUS_OF_REGION_OF_INTEREST > 0 ? point.y - RADIUS_OF_REGION_OF_INTEREST : 0;
        int w = x + 2 * RADIUS_OF_REGION_OF_INTEREST < image.cols() ? 2 * RADIUS_OF_REGION_OF_INTEREST : image.cols() - x;
        int h = y + 2 * RADIUS_OF_REGION_OF_INTEREST < image.rows() ? 2 * RADIUS_OF_REGION_OF_INTEREST : image.rows() - y;

        Rect regionOfInterest = new Rect(x, y, w, h);
        return new Mat(image, regionOfInterest);
    }

    private Ponto detectCornerByTemplateMatching(Mat regionImage, Mat fullImage) {
        Mat result = new Mat();
        Imgproc.matchTemplate(fullImage, regionImage, result, Imgproc.TM_SQDIFF_NORMED);
        Core.MinMaxLocResult minMaxLoc = Core.minMaxLoc(result);
        System.out.println(minMaxLoc.minLoc);
        return new Ponto(
            (int)minMaxLoc.minLoc.x + RADIUS_OF_REGION_OF_INTEREST,
            (int)minMaxLoc.minLoc.y + RADIUS_OF_REGION_OF_INTEREST
        );
    }

}

