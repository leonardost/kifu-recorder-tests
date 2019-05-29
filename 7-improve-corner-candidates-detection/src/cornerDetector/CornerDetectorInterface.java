package src.cornerDetector;

import java.util.List;
import org.opencv.core.Mat;

public interface CornerDetectorInterface {
    public void setImageIndex(int imageIndex);
    public List<Corner> detectCandidateCornersIn(Mat image);
}