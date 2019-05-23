package src.cornerDetector;

import src.Ponto;

public class Corner {
    public Ponto position;
    public Ponto displacementToRealCorner;
    public boolean isStone;

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

    public String toString() {
        return position.toString();
    }

}