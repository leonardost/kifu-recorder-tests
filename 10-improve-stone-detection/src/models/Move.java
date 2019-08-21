package src.models;

import java.io.Serializable;

/**
 * Represents a move in the game
 */
public class Move implements Serializable {

    public boolean isPass;
    // used when some mistake in the stone detection must be corrected
    public boolean isRemoval;
    public int row;
    public int column;
    public int color;

    public Move(int row, int column, int color) {
        this.isPass = false;
        this.row = row;
        this.column = column;
        this.color = color;
    }

    public static Move createRemoval(int row, int column)
    {
        Move move = new Move(row, column, 0);
        move.isRemoval = true;
        return move;
    }

    public Position position() {
        return new Position(row, column);
    }

    public String sgf() {
        int l = 'a' + row;
        int c = 'a' + column;
        String coordinate = "" + (char)c + (char)l;
        if (isPass) coordinate = "";
        String property = this.color == Board.BLACK_STONE ? "B" : "W";
        if (isRemoval) property = "AE";
        return ";" + property + "[" + coordinate + "]";
    }

    public String toString() {
        String colorName = color == Board.EMPTY ? "Empty" : color == Board.BLACK_STONE ? "Black" : "White";
        return "(" + row + ", " + column + ", " + colorName + ")";
    }

}
