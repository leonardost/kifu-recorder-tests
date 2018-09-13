package src.boardDetector;

import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import src.ParallelChecker;

public class BoardDetectorByCountingParallelLines implements BoardDetectorInterface {

    private ParallelChecker parallelChecker;
    private int lastNumberOfParallelLinesWhenBoardWasInside;
    private int lastNumberOfParallelLines;

    public BoardDetectorByCountingParallelLines() {
        lastNumberOfParallelLinesWhenBoardWasInside = -1;
        parallelChecker = new ParallelChecker();
    }

    public void setState(int state) {
        if (state == STATE_BOARD_IS_INSIDE) {
            lastNumberOfParallelLinesWhenBoardWasInside = lastNumberOfParallelLines;
        }
    }

    public void setImageIndex(int imageIndex) {}

    public boolean isBoardContainedIn(Mat ortogonalBoardImage) {
        if (lastNumberOfParallelLinesWhenBoardWasInside == -1) return true;

        lastNumberOfParallelLines = parallelChecker.getNumberOfParallelLinesIn(ortogonalBoardImage);

        return lastNumberOfParallelLinesWhenBoardWasInside - lastNumberOfParallelLines < 3;
    }

}
