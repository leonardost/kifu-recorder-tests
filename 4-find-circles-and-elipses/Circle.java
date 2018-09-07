import org.opencv.core.Point;

public class Circle {

    public Point center;
    public int radius;

    public Circle() {}
    public Circle(Point center, int radius) { this.center = center; this.radius = radius; }

    public String toString() {
        return "(" + center + ", " + radius + ")";
    }

}
