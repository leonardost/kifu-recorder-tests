package src.boardDetector;

import org.opencv.core.Mat;

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
        System.out.println("Number of parallel lines = " + lastNumberOfParallelLines);
        return lastNumberOfParallelLinesWhenBoardWasInside - lastNumberOfParallelLines < 3;
    }

}
