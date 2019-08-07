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

    public Position position() {
        return new Position(row, column);
    }

    public String sgf() {
        int l = 'a' + row;
        int c = 'a' + column;
        String coordinate = "" + (char)c + (char)l;
        if (isPass) coordinate = "";
        char color = this.color == Board.BLACK_STONE ? 'B' : 'W';
        return ";" + color + "[" + coordinate + "]";
    }

    public String toString() {
        String colorName = color == Board.EMPTY ? "Empty" : color == Board.BLACK_STONE ? "Black" : "White";
        return "(" + row + ", " + column + ", " + colorName + ")";
    }

}
