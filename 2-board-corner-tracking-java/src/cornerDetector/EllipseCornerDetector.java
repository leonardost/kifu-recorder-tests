package src.cornerDetector;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;

import src.cornerDetector.ellipseDetector.EllipseDetectorInterface;
import src.cornerDetector.ellipseDetector.FirstEllipseDetector;
import src.cornerDetector.ellipseDetector.SecondEllipseDetector;

public class EllipseCornerDetector implements CornerDetectorInterface {

    private int imageIndex;
    private int cornerIndex;

    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
    }

    public void setCornerIndex(int cornerIndex) {
        this.cornerIndex = cornerIndex;
    }

    public List<Corner> detectCandidateCornersIn(Mat image) {
        List<EllipseDetectorInterface> ellipseDetectors = new ArrayList<>();
        EllipseDetectorInterface firstEllipseDetector = new FirstEllipseDetector();
        EllipseDetectorInterface secondEllipseDetector = new SecondEllipseDetector();
        String prefix = "processing/corner" + cornerIndex + "_frame" + imageIndex;
        firstEllipseDetector.setFilePrefix(prefix + "_first-filter");
        secondEllipseDetector.setFilePrefix(prefix + "_second-filter");
        ellipseDetectors.add(firstEllipseDetector);
        ellipseDetectors.add(secondEllipseDetector);

        List<Corner> candidateCorners = new ArrayList<>();

        for (EllipseDetectorInterface ellipseDetector : ellipseDetectors) {
            List<RotatedRect> ellipses = ellipseDetector.detectEllipsesIn(image);
            for (RotatedRect ellipse : ellipses) {
                Corner corner = new Corner((int)ellipse.center.x, (int)ellipse.center.y, true);
                corner.stonePosition = ellipse;
                candidateCorners.add(corner);
            }
        }

        return candidateCorners;
    }

}