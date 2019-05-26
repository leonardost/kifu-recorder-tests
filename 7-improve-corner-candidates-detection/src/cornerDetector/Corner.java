package src.cornerDetector;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;
import src.Ponto;

public class Corner {
    public Ponto position;
    public Ponto displacementToRealCorner;
    public boolean isStone;
    // Stores the bounding rectangle that corresponds to the detected stone ellipsis
    public RotatedRect stonePosition;

    public Corner() {
        displacementToRealCorner = new Ponto(0, 0);
        isStone = false;
    }

    public Corner(int x, int y) {
        displacementToRealCorner = new Ponto(0, 0);
        position = new Ponto(x, y);
        isStone = false;
    }

    public Corner(int x, int y, boolean isStone) {
        displacementToRealCorner = new Ponto(0, 0);
        position = new Ponto(x, y);
        this.isStone = isStone;
    }

    public int getX() {
        return position.x;
    }

    public int getY() {
        return position.y;
    }

    public double distanceTo(Corner otherCorner) {
        return position.distanceTo(otherCorner.position);
    }

    public double manhattanDistanceTo(Corner otherCorner) {
        return position.manhattanDistanceTo(otherCorner.position);
    }

    // Gets the displacement vector from this point to the other
    public Ponto getDifferenceTo(Corner otherCorner) {
        Ponto displacement = new Ponto(position.x, position.y);
        displacement.x -= otherCorner.position.x;
        displacement.y -= otherCorner.position.y;
        return displacement;
    }

    public void updateDisplacementVectorRelativeTo(Ponto point) {
        displacementToRealCorner = new Ponto(position.x, position.y);
        displacementToRealCorner.x -= point.x;
        displacementToRealCorner.y -= point.y;
    }

    public Ponto getRealCornerPosition() {
        Ponto realCornerPosition = new Ponto(position.x, position.y);
        realCornerPosition.x -= displacementToRealCorner.x;
        realCornerPosition.y -= displacementToRealCorner.y;
        return realCornerPosition;
    }

    // Checks if a point lies too close to the stone position
    public boolean isTooCloseToCircle(Ponto position) {
        if (stonePosition == null) return false;

        // Circular distance, this is how it was before
        // return position.distanceTo(position) <= 25 * 25);

        // Let's increase the bounding rectangle's size by some proportion, say, 1.2
        Point[] points = new Point[4];
        RotatedRect expandedStonePosition = stonePosition.clone();
        expandedStonePosition.size.width *= 1.4;
        expandedStonePosition.size.height *= 1.3;
        expandedStonePosition.points(points);
        MatOfPoint2f expandedStoneContour = new MatOfPoint2f(points);
        Point p = new Point(position.x, position.y);

        return Imgproc.pointPolygonTest(expandedStoneContour, p, false) >= 0;
    }

    public String toString() {
        return position.toString() + " (is" + (!isStone ? " not " : " ") + "stone)";
    }

}