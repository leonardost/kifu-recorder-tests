package src.cornerDetector;

import org.opencv.core.Point;
import org.opencv.core.RotatedRect;

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
    public boolean isTooCloseToCircle(Ponto point) {
        if (stonePosition == null) return false;

        RotatedRect expandedStonePosition = stonePosition.clone();

        // https://stackoverflow.com/questions/7946187/point-and-ellipse-rotated-position-test-algorithm
        // Some checks should be done to see if this is correct
        double cos = Math.cos(Math.toRadians(expandedStonePosition.angle));
        double sin = Math.sin(Math.toRadians(expandedStonePosition.angle));
        double minorAxis = (expandedStonePosition.size.width / 2) * (expandedStonePosition.size.width / 2);
        double majorAxis = (expandedStonePosition.size.height / 2) * (expandedStonePosition.size.height / 2);
        double a = cos * (point.x - getX()) + sin * (point.y - getY());
        double b = sin * (point.x - getX()) - cos * (point.y - getY());
        return (a * a / minorAxis) + (b * b / majorAxis) <= 1;
    }

    public String toString() {
        return position.toString() + " (is" + (!isStone ? " not " : " ") + "stone)";
    }

 }