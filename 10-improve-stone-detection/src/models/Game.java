package src.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete game, with a sequence of boards and played moves
 */
public class Game implements Serializable {

    private String blackPlayer;
    private String whitePlayer;
    private int boardDimension;
    private List<Move> moves;
    private List<Board> boards;

    public Game(int boardDimension, String blackPlayer, String whitePlayer, String komi) {
        this.boardDimension = boardDimension;
        this.blackPlayer = blackPlayer;
        this.whitePlayer = whitePlayer;
        moves = new ArrayList<>();
        boards = new ArrayList<>();

        Board emptyBoard = new Board(boardDimension);
        boards.add(emptyBoard);
    }

    public int getBoardDimention() {
        return boardDimension;
    }

    public String getBlackPlayer() {
        return blackPlayer;
    }

    public String getWhitePlayer() {
        return whitePlayer;
    }

    public boolean addNewMoveFrom(Board board) {
        Move move = board.getDifferenceTo(getLastBoard());

        if (move == null || repeatsPreviousState(board) || !canNextMoveBe(move.color)) {
            return false;
        }

        boards.add(board);
        moves.add(move);
        return true;
    }

    /**
     * Returns true if the new board repeats some previous game state (superko rule).
     * 
     * Now that StoneDetector can remove previously detected stones, maybe this should
     * be revised, because there might be states which should not even have existed.
     * I guess a simple ko checking would suffice (ensure previous board state is not
     * repeated).
     */
    private boolean repeatsPreviousState(Board newBoard) {
        for (Board board : boards) {
            if (board.equals(newBoard)) return true;
        }
        return false;
    }

    public boolean canNextMoveBe(int color) {
        if (color == Board.BLACK_STONE)
            return isFirstMove() || haveOnlyBlackStonesBeenPlayed() || wasLastMoveWhite();
        else if (color == Board.WHITE_STONE)
            return wasLastMoveBlack();
        return false;
    }

    private boolean isFirstMove() {
        return moves.isEmpty();
    }

    private boolean haveOnlyBlackStonesBeenPlayed() {
        for (Move move : moves) {
            if (move.color == Board.WHITE_STONE) return false;
        }
        return true;
    }

    private boolean wasLastMoveWhite() {
        return !isFirstMove() && getLastMove().color == Board.WHITE_STONE;
    }

    private boolean wasLastMoveBlack() {
        return !isFirstMove() && getLastMove().color == Board.BLACK_STONE;
    }

    public int getNumberOfMoves() {
        return moves.size();
    }

    // Stone removal "moves" are ignored here
    public Move getLastMove() {
        if (moves.isEmpty()) return null;

        int moveIndex = moves.size();
        Move lastMove = null;
        do {
            moveIndex--;
            lastMove = moves.get(moveIndex);
        } while (moveIndex > 0 && lastMove.isRemoval());
        return lastMove;
    }

    public Board getLastBoard() {
        return boards.get(boards.size() - 1);
    }

    /**
     * Disconsider the last move and return it.
     */
    public Move undoLastMove() {
        if (moves.isEmpty()) return null;
        boards.remove(boards.size() - 1);
        Move lastMove = moves.remove(moves.size() - 1);
        return lastMove;
    }

    /**
     * Rotates all boards of this game clockwise (direction = 1) or
     * counter-clockwise (direction = -1)
     */
    public void rotate(int direction) {
        if (direction != -1 && direction != 1) return;

        List<Board> rotatedBoards = new ArrayList<>();
        for (Board board : boards) {
            rotatedBoards.add(board.rotate(direction));
        }

        List<Move> rotatedMoves = new ArrayList<>();
        for (int i = 1; i < rotatedBoards.size(); ++i) {
            Board last = rotatedBoards.get(i);
            Board beforeLast = rotatedBoards.get(i - 1);
            rotatedMoves.add(last.getDifferenceTo(beforeLast));
        }

        boards = rotatedBoards;
        moves = rotatedMoves;
    }

    public void removeStoneAt(int row, int column)
    {
        Move removedStone = Move.createRemoval(row, column);
        moves.add(removedStone);
    }

}
