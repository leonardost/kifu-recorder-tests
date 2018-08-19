import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CornerPositionsFile {

    private List<Ponto[]> cornerPositions;

    public CornerPositionsFile(String imageSequenceFolder) {
        cornerPositions = new ArrayList<>();
        List<String> lines = readLinesFrom(imageSequenceFolder + "/corner_positions.log");

        for (String line : lines) {
            if (line.startsWith("#")) continue;
            Ponto[] points = getPointsFrom(line.trim());
            cornerPositions.add(points);
        }
    }

    private List<String> readLinesFrom(String file) {
        try {
            return Files.readAllLines(Paths.get(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private Ponto[] getPointsFrom(String line) {
        String[] integersAsString = line.split(" ");
        Ponto[] points = new Ponto[4];

        for (int i = 0; i < 4; i++) {
            points[i] = new Ponto();
            points[i].x = Integer.parseInt(integersAsString[i * 2]);
            points[i].y = Integer.parseInt(integersAsString[i * 2 + 1]);
        }

        return points;
    }

    public int getNumberOfImages() {
        return cornerPositions.size() - 1;
    }

    public Ponto[] getInitialCornersPositions() {
        return cornerPositions.get(0);
    }

    public Ponto[] getCornerPositions(int frameNumber) {
        return cornerPositions.get(frameNumber);
    }

}