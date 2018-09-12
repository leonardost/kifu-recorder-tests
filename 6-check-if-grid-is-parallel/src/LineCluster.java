package src;

import java.util.ArrayList;
import java.util.List;

public class LineCluster
{

    public static final double DISTANCE_THRESHOULD = 2;

    private List<Line> lines;
    private Line averageLine;

    public LineCluster()
    {
        lines = new ArrayList<>();
    }

    public Line getAverageLine()
    {
        return averageLine;
    }

    public boolean isCloseEnoughTo(Line line)
    {
        return Math.abs(line.rho - averageLine.rho) < DISTANCE_THRESHOULD && Math.abs(line.theta - averageLine.theta) < 0.0001;
    }

    public void add(Line line)
    {
        lines.add(line);
        recalculateMedianLine();
    }

    private void recalculateMedianLine()
    {
        double averageRho = 0;
        double averageTheta = 0;
        for (Line line : lines) {
            averageRho += line.rho;
            averageTheta += line.theta;
        }
        averageLine = new Line(averageRho, averageTheta);
    }

}