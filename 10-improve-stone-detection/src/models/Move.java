package src.models;

import java.io.Serializable;

/**
 * Represents a move in the game
 */
public class Move implements Serializable {

    public boolean isPass;
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
        return new Move(row, column, Board.EMPTY);
    }

    public Position position() {
        return new Position(row, column);
    }

    public boolean isRemoval() {
        return color == Board.EMPTY;
    }

    public String sgf() {
        int l = 'a' + row;
        int c = 'a' + column;
        String coordinate = "" + (char)c + (char)l;
        if (isPass) coordinate = "";
        return ";" + getSgfPropertyName() + "[" + coordinate + "]";
    }

    private String getSgfPropertyName() {
        if (color == Board.BLACK_STONE) return "B";
        else if (color == Board.WHITE_STONE) return "W";
        // Remove stone (Add Empty space)
        return "AE";
    }

    public String toString() {
        String colorName = color == Board.EMPTY ? "Empty" : color == Board.BLACK_STONE ? "Black" : "White";
        return "(" + row + ", " + column + ", " + colorName + ")";
    }

}
