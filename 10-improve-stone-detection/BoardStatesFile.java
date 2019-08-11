import java.io.IOException;
import java.util.List;

import src.models.Board;

import java.util.ArrayList;
import java.util.Iterator;
import java.nio.file.Files;
import java.nio.file.Paths;

public class BoardStatesFile {

    private List<Board> boardStates;
    private List<Boolean> shouldProcess;
    private int boardDimension;

    public BoardStatesFile(String imageSequenceFolder) {
        boardStates = new ArrayList<>();
        shouldProcess = new ArrayList<>();
        List<String> lines = readLinesFrom(imageSequenceFolder + "/board_sequence");
        Iterator<String> iterator = lines.iterator();

        String line = iterator.next();
        while (line.startsWith("#")) {
            line = iterator.next();
            continue;
        }
        boardDimension = Integer.parseInt(line);

        while (iterator.hasNext()) {
            line = (String)iterator.next();
            if (line.startsWith("#")) continue;

            String[] frameInfo = line.split(" ");
            shouldProcess.add(frameInfo[1].equals("1"));

            Board board = getBoardFrom(iterator);
            boardStates.add(board);
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

    private Board getBoardFrom(Iterator<String> iterator)
    {
        Board board = new Board(boardDimension);

        for (int i = 0; i < boardDimension; i++) {
            String boardLine = iterator.next();

            for (int j = 0; j < boardDimension; j++) {
                if (boardLine.charAt(j) == 'B') {
                    board.putStone(i, j, Board.BLACK_STONE);
                } else if (boardLine.charAt(j) == 'W') {
                    board.putStone(i, j, Board.WHITE_STONE);
                }
            }
        }

        return board;
    }

    public int getNumberOfImages() {
        return boardStates.size() - 1;
    }

    public Board getBoard(int frameNumber) {
        return shouldProcess.get(frameNumber - 1)
            ? boardStates.get(frameNumber - 1)
            : null;
    }

}