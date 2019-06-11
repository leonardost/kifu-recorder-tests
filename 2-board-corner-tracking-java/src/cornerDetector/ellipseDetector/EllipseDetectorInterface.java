package src.cornerDetector.ellipseDetector;

import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;

import java.util.List;

public interface EllipseDetectorInterface {
    public void setFilePrefix(String filePrefix);
    public String getName();
    public List<RotatedRect> detectEllipsesIn(Mat image);
}
