package src.models;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a game board state
 */
public class Board implements Serializable {

    public final static int EMPTY = 0;
    public final static int BLACK_STONE = 1;
    public final static int WHITE_STONE = 2;

    private int dimension;
    private int[][] board;
    // For how many frames each stone was identified on the board
    // The longer, the more probable it is correct
    private int[][] stability;

    public Board(int dimension) {
        this.dimension = dimension;
        this.board = new int[dimension][dimension];
        this.stability = new int[dimension][dimension];
        for (int i = 0; i < dimension; ++i) {
            for (int j = 0; j < dimension; ++j) {
                board[i][j] = EMPTY;
                stability[i][j] = 0;
            }
        }
    }

    public Board(Board board) {
        this.dimension = board.dimension;
        this.board = new int[dimension][dimension];
        this.stability = new int[dimension][dimension];
        for (int i = 0; i < dimension; ++i) {
            for (int j = 0; j < dimension; ++j) {
                this.board[i][j] = board.board[i][j];
                this.stability[i][j] = board.stability[i][j];
            }
        }
    }

    public int getDimension() {
        return dimension;
    }

    public void increaseStability(int row, int column) {
        stability[row][column]++;
        System.out.println("Increasing stability of (" + column + ", " + row + ") - " + stability[row][column]);
    }

    // Returns true if a stone was removed
    public boolean decreaseStability(int row, int column) {
        if (stability[row][column] > 0) {
            stability[row][column]--;
            System.out.println("Decreasing stability of (" + column + ", " + row + ") - " + stability[row][column]);
        }
        if (stability[row][column] == 0) {
            board[row][column] = EMPTY;
            System.out.println("Stone at (" + column + ", " + row + ") removed");
            return true;
        }
        return false;
    }

    public void putStone(int row, int column, int stone) {
        if (stone != BLACK_STONE && stone != WHITE_STONE) {
            throw new RuntimeException("Invalid stone");
        }
        if (isInvalidPosition(row, column)) {
            throw new RuntimeException("Invalid position");
        }
        if (board[row][column] != EMPTY) {
			throw new RuntimeException("There's already a stone in this position");
		}
        board[row][column] = stone;
    }

    private boolean isInvalidPosition(int row, int column) {
        return row < 0 || column < 0 || row >= dimension || column >= dimension;
    }

    public int getPosition(int row, int column) {
        return board[row][column];
    }

    public String toString() {
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < dimension; ++i) {
            for (int j = 0; j < dimension; ++j) {
                if (board[i][j] == EMPTY) output.append('.');
                else if (board[i][j] == BLACK_STONE) output.append('B');
                else if (board[i][j] == WHITE_STONE) output.append('W');
            }
            output.append(System.getProperty("line.separator"));
        }

        return output.toString();
    }

    /**
     * Returns a new board that correponds to the current board rotated clockwise (direction =1)
     * or counter-clockwise (direction = -1)
     */
    public Board rotate(int direction) {
        if (direction == -1) return rotateCounterClockwise();
        else if (direction == 1) return rotateClockwise();
        return null;
    }

    private Board rotateClockwise() {
        Board rotatedBoard = new Board(dimension);
        for (int i = 0; i < dimension; ++i) {
            for (int j = 0; j < dimension; ++j) {
                if (board[dimension - 1 - j][i] != Board.EMPTY) {
                    rotatedBoard.putStone(i, j, board[dimension - 1 - j][i]);
                }
            }
        }
        return rotatedBoard;
    }

    private Board rotateCounterClockwise() {
        Board rotatedBoard = new Board(dimension);
        for (int i = 0; i < dimension; ++i) {
            for (int j = 0; j < dimension; ++j) {
                if (board[j][dimension - 1 - i] != Board.EMPTY) {
                    rotatedBoard.putStone(i, j, board[j][dimension - 1 - i]);
                }
            }
        }
        return rotatedBoard;
    }

    public boolean isIdenticalTo(Board otherBoard) {
        if (dimension != otherBoard.dimension) return false;
        for (int i = 0; i < dimension; ++i) {
            for (int j = 0; j < dimension; ++j) {
                if (getPosition(i, j) != otherBoard.getPosition(i, j)) return false;
            }
        }
        return true;
    }

    /**
     * Verifies if two boards are equal, including rotations
     */
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Board)) return false;
        Board otherBoard = (Board)object;
        if (dimension != otherBoard.dimension) return false;

        Board rotation1 = otherBoard.rotateClockwise();
        Board rotation2 = rotation1.rotateClockwise();
        Board rotation3 = rotation2.rotateClockwise();

        return this.isIdenticalTo(otherBoard) || this.isIdenticalTo(rotation1) || this.isIdenticalTo(rotation2) || this.isIdenticalTo(rotation3);
    }

    /**
     * Returns the move difference between this board and the previous one.
     * If it is not possible to reach the current board status from the
     * previous one, returns null.
     */
    public Move getDifferenceTo(Board previousBoard) {
        int numberOfBlackStonesBefore = previousBoard.getNumberOfStonesOfColor(Board.BLACK_STONE);
        int numberOfWhiteStonesBefore = previousBoard.getNumberOfStonesOfColor(Board.WHITE_STONE);
        int numberOfBlackStonesAfter = getNumberOfStonesOfColor(Board.BLACK_STONE);
        int numberOfWhiteStonesAfter = getNumberOfStonesOfColor(Board.WHITE_STONE);
        int blackStonesDifference = numberOfBlackStonesAfter - numberOfBlackStonesBefore;
        int whiteStonesDifference = numberOfWhiteStonesAfter - numberOfWhiteStonesBefore;
        int moveColor;

        if (blackStonesDifference == 1) moveColor = Board.BLACK_STONE;
        else if (whiteStonesDifference == 1) moveColor = Board.WHITE_STONE;
        else return null;

        Move playedMove = getDifferentMoveBetweenCurrentBoardAnd(previousBoard, moveColor);
        if (previousBoard.generateNewBoardWith(playedMove).isIdenticalTo(this)) {
            return playedMove;
        }
        return null;
    }

    private int getNumberOfStonesOfColor(int color) {
        int numberOfStonesOfThisColor = 0;
        for (int i = 0; i < dimension; ++i) {
            for (int j = 0; j < dimension; ++j) {
                if (board[i][j] == color) ++numberOfStonesOfThisColor;
            }
        }
        return numberOfStonesOfThisColor;
    }

    /**
     * Returns the first different stone of the specified color found between the two boards
     */
    private Move getDifferentMoveBetweenCurrentBoardAnd(Board previousBoard, int color) {
        for (int i = 0; i < previousBoard.getDimension(); ++i) {
            for (int j = 0; j < previousBoard.getDimension(); ++j) {
                if (board[i][j] == color && previousBoard.getPosition(i, j) != board[i][j]) {
                    return new Move(i, j, color);
                }
            }
        }
        return null;
    }

    /**
     * Returns a new board with the given move played. If the move is not valid,
     * returns the last valid board
     */
	public Board generateNewBoardWith(Move move) {
        if (move == null || board[move.row][move.column] != EMPTY) return this;

        Board newBoard = new Board(this);

        for (Group group : retrieveAdjacentGroupsTo(move)) {
            if (group == null) continue;
            if (group.isCapturedBy(move)) newBoard.remove(group);
        }

        newBoard.board[move.row][move.column] = move.color;
        newBoard.stability[move.row][move.column] = 3;

        Group groupOfMove = newBoard.groupAt(move.row, move.column);
        if (groupOfMove.hasNoLiberties()) return this;

        return newBoard;
	}

    private Set<Group> retrieveAdjacentGroupsTo(Move move) {
        Set<Group> adjacentGroupsToMove = new HashSet<>();
        adjacentGroupsToMove.add(groupAt(move.row - 1, move.column));
        adjacentGroupsToMove.add(groupAt(move.row + 1, move.column));
        adjacentGroupsToMove.add(groupAt(move.row, move.column - 1));
        adjacentGroupsToMove.add(groupAt(move.row, move.column + 1));
        return adjacentGroupsToMove;
    }

    private void remove(Group group) {
        for (Position position : group.getPositions()) {
            board[position.row][position.column] = EMPTY;
            stability[position.row][position.column] = 0;
        }
    }

    /**
     * Returns the group that is in a certain position of the board or null if there is none
     */
    public Group groupAt(int row, int column) {
        if (isInvalidPosition(row, column) || board[row][column] == Board.EMPTY) return null;

        boolean[][] visitedPositions = new boolean[dimension][dimension];
        for (int i = 0; i < dimension; ++i) {
            for (int j = 0; j < dimension; ++j) {
                visitedPositions[i][j] = false;
            }
        }

        int color = board[row][column];
        Group group = new Group(color);
        delimitGroup(row, column, visitedPositions, group);
        return group;
    }

    /**
     * Does a depth-first search to find all stones that are part of this group and their
     * liberties
     */
    private void delimitGroup(int row, int column, boolean[][] visitedPositions, Group group) {
        if (isInvalidPosition(row, column) || visitedPositions[row][column]) return;

        visitedPositions[row][column] = true;

        if (board[row][column] == Board.EMPTY) {
            group.addLiberty(new Position(row, column));
        }
        else if (board[row][column] == group.getColor()) {
            group.addPosition(new Position(row, column));

            delimitGroup(row - 1, column, visitedPositions, group);
            delimitGroup(row + 1, column, visitedPositions, group);
            delimitGroup(row, column - 1, visitedPositions, group);
            delimitGroup(row, column + 1, visitedPositions, group);
        }
    }

    public int getNumberOfDifferencesBetweenThisAnd(Board otherBoard)
    {
        if (dimension != otherBoard.dimension) {
            return -1;
        }

        int numberOfDifferences = 0;
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                if (board[i][j] != otherBoard.board[i][j]) {
                    numberOfDifferences++;
                }
            }
        }

        return numberOfDifferences;
    }

}
