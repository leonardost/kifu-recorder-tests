import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;

import src.Ponto;
import src.cornerDetector.Corner;

public class CornerPositionsFile {

    private List<Corner[]> cornerPositions;

    public CornerPositionsFile(String imageSequenceFolder) {
        cornerPositions = new ArrayList<>();
        List<String> lines = readLinesFrom(imageSequenceFolder + "/corner_positions.log");

        for (String line : lines) {
            if (line.startsWith("#")) continue;
            Corner[] points = getCornersFrom(line.trim());
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

    private Corner[] getCornersFrom(String line) {
        String[] integersAsString = line.split(" ");
        Corner[] corners = new Corner[4];

        for (int i = 0; i < 4; i++) {
            corners[i] = new Corner();
            corners[i].position = new Ponto();
            corners[i].position.x = Integer.parseInt(integersAsString[i * 2]);
            corners[i].position.y = Integer.parseInt(integersAsString[i * 2 + 1]);
        }

        return corners;
    }

    public int getNumberOfImages() {
        return cornerPositions.size() - 1;
    }

    public Corner[] getInitialCornersPositions() {
        return cornerPositions.get(0);
    }

    public Corner[] getCornerPositions(int frameNumber) {
        return cornerPositions.get(frameNumber);
    }

}