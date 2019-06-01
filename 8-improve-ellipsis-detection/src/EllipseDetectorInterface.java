package src;

import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;

import java.util.List;

public interface EllipseDetectorInterface {
    public void setImageIndex(int imageIndex);
    public String getName();
    public List<RotatedRect> detectEllipsesIn(Mat image);
}
