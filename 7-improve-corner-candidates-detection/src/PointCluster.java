package src;

import java.util.ArrayList;
import java.util.List;

public class PointCluster {

    private Ponto centroid;
    private List<Ponto> points;

    public PointCluster() {
        centroid = new Ponto(-1, -1);
        points = new ArrayList<>();
    }

    public Ponto getCentroid() { return centroid; }

    public void add(Ponto point) {
        points.add(point);
        updateCentroid();
    }

    private void updateCentroid() {
        int accumulatedY = 0;
        int accumulatedX = 0;
        for (Ponto point: points) {
            accumulatedY = accumulatedY + point.y;
            accumulatedX = accumulatedX + point.x;
        }
        centroid.y = accumulatedY / points.size();
        centroid.x = accumulatedX / points.size();
    }

    public double distanceTo(Ponto point) {
        if (centroid.y == -1) return 0;
        return centroid.distanceTo(point);
    }

}

