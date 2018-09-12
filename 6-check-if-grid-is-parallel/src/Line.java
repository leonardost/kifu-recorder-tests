package src;

import org.opencv.core.Point;

public class Line
{

    public double rho;
    public double theta;

    public Line(double rho, double theta)
    {
        this.rho = rho;
        this.theta = theta;
    }

    public boolean isOrtogonal()
    {
        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);
        return areClose(Math.abs(sinTheta), 1) || areClose(Math.abs(cosTheta), 1);
    }

    private boolean areClose(double number1, double number2)
    {
        return Math.abs(number1 - number2) < 0.0000001;
    }

    public Point[] getPoints()
    {
        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);
        double x = cosTheta * rho;
        double y = sinTheta * rho;
        Point[] points = {
            new Point(x + 10000 * (-sinTheta), y + 10000 * cosTheta),
            new Point(x - 10000 * (-sinTheta), y - 10000 * cosTheta)
        };
        return points;
    }

}
