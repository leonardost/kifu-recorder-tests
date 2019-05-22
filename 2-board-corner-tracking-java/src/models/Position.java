package src.models;

public class Position {
    public int row;
    public int column;

    public Position(int row, int column) {
        this.row = row;
        this.column = column;
    }

    @Override
    public String toString() {
        int l = 'a' + skipLetterI(row);
        int c = 'a' + skipLetterI(column);
        return "[" + (char)l + (char)c + "]";
    }

    private char skipLetterI(int index) {
        final int INDEX_OF_LETTER_I = 8;
        if (index >= INDEX_OF_LETTER_I) index++;
        return (char)index;
    }

    @Override
    public boolean equals(Object position) {
        if (!(position instanceof Position)) return false;
        return row == ((Position)position).row
            && column == ((Position)position).column;
    }

    @Override
    public int hashCode() {
        return row * 39 + column;
    }
}
