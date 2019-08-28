package src.stoneDetector;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import src.models.Board;
import src.models.Game;
import src.models.Move;
import src.models.MoveHypothesis;

/**
 * Detects the stones on the board that's in the image and returns a Board
 * object corresponding to the current game state.
 */
public class StoneDetector {

    // Orthogonal square image of the board
    private Mat boardImage = null;
    // Dimensions of the board (9x9, 13x13 or 19x19)
    private int boardDimension = 0;
    // Debug information of the current state seen by the detector
    public StringBuilder snapshot = new StringBuilder();

    public void setBoardDimension(int boardDimension) {
        this.boardDimension = boardDimension;
    }

    public void setBoardImage(Mat boardImage) {
        this.boardImage = boardImage;
    }

    // Now I think using Game here makes more sense than using Board, because
    // the stone detection code uses the current game information to know
    // if it has to look for black or white stones.
    //
    // Take notice that Game can be modified, this is not good. This should be
    // refactored later.
    public Board detectBasedOn(Game game)
    {
        Board detectedBoard = game.getNumberOfMoves() == 0
            ? detect()
            : detect(game);
        game.addNewMoveFrom(detectedBoard);
        return detectedBoard;
    }

    /**
     * Stone detection that does not use the previous state of the game.
     */
    private Board detect() {

        Board board = new Board(boardDimension);

        double[] averageColorOfBoard = calculateAverageColorOfBoard(boardImage);

        for (int i = 0; i < boardDimension; ++i) {
            for (int j = 0; j < boardDimension; ++j) {
                double[] color = calculateAverageColorOnPosition(i, j);

                int hypothesis = calculateColorHypothesis(color, averageColorOfBoard);
                if (hypothesis != Board.EMPTY) {
                    board.putStone(i, j, hypothesis);
                }
            }
        }

        return board;
    }

    /**
     * Returns the average color of the board
     *
     * THIS COLOR CHANGES AS THE GAME PROGRESSES AND AS THE AMBIENT ILLUMINATION CHANGES.
     *
     * @param boardImage
     * @return
     */
    private double[] calculateAverageColorOfBoard(Mat boardImage) {
        Scalar scalarAverage = Core.mean(boardImage);

        double[] average = new double[boardImage.channels()];
        for (int i = 0; i < boardImage.channels(); ++i) {
            average[i] = scalarAverage.val[i];
        }

        return average;
    }

    /**
     * Checks if a certain color is closer to a black or white stone.
     *
     * @param color Cor a ser verificada
     * @param averageBoardColor Cor média da imagem do tabuleiro
     * @return Pedra preta, branca, ou vazio
     */
    private int calculateColorHypothesis(double[] color, double[] averageBoardColor) {
        double[] black = {0.0, 0.0, 0.0, 255.0};
        double[] white = {255.0, 255.0, 255.0, 255.0};
        double distanceToBlack = getColorDistance(color, black);
        // double distanceToWhite = getColorDistance(color, white);
        double distanceToAverageColor = getColorDistance(color, averageBoardColor);

        // Testing other hypothesis
        if (distanceToBlack < 80 || distanceToBlack < distanceToAverageColor) {
            return Board.BLACK_STONE;
        }
//        else if (color[2] >= 150) {
        else if (color[2] >= averageBoardColor[2] * 1.35) {
            return Board.WHITE_STONE;
        }

        return Board.EMPTY;

        // If the distance to the average color is below a certain threshold, it's very probable
        // that it's an empty intersection
        // if (distanceToAverageColor < 120) {
        //     return Board.EMPTY;
        // }

        // if (distanceToBlack < distanceToWhite) {
        //     return Board.BLACK_STONE;
        // }
        // else {
        //     return Board.WHITE_STONE;
        // }
    }

    private double getColorDistance(double[] color1, double[] color2) {
        double distance = 0;
        for (int i = 0; i < Math.min(color1.length, color2.length); ++i) {
            distance += Math.abs(color1[i] - color2[i]);
        }
        return distance;
    }

    /**
     * Uses the information of the last game state to improve the detection
     * precision of the last move made
     */
    private Board detect(Game game) {
        Board lastBoard = game.getLastBoard();
        boolean canBeBlackStone = game.canNextMoveBe(Board.BLACK_STONE);
        boolean canBeWhiteStone = game.canNextMoveBe(Board.WHITE_STONE);
        double[][] averageColors = new double[3][boardImage.channels()];
        double[] averageHue = new double[3];
        double[] averageSaturation = new double[3];
        double[] averageValue = new double[3];
        double[][] averageHsv = new double[3][3];
        
        int[] counters = new int[3];

        Mat hsvImage = new Mat();
        Imgproc.cvtColor(boardImage, hsvImage, Imgproc.COLOR_BGR2HSV, 3);
        List<Mat> hsvChannels = new ArrayList<Mat>(3);
        Core.split(hsvImage, hsvChannels);

        getAverageColors(lastBoard, averageColors, counters);
        getAverageOfChannel(lastBoard, averageHue, counters, hsvChannels.get(0), "hue");
        getAverageOfChannel(lastBoard, averageSaturation, counters, hsvChannels.get(1), "saturation");
        getAverageOfChannel(lastBoard, averageValue, counters, hsvChannels.get(2), "value");

        List<MoveHypothesis> moveHypothesesFound = new ArrayList<>();
        List<MoveHypothesis> hsvHypothesesFound = new ArrayList<>();

        snapshot = new StringBuilder();

        for (int i = 0; i < boardDimension; ++i) {
            for (int j = 0; j < boardDimension; ++j) {

                snapshot.append(String.format("(%1$2d, %2$2d)", i, j) + "\n");

                // Ignores the intersections of moves that were already made
                // if (lastBoard.getPosition(i, j) != Board.EMPTY) continue;

                double[] colorAroundPosition = calculateAverageColorOnPosition(i, j);

                // System.out.println("    HSV on " + String.format("(%d, %d)", i, j) + " = "
                //     + String.format("(%f, %f, %f)", hsvOnPosition[0], hsvOnPosition[1], hsvOnPosition[2]));

                double[][] colorsOnEmptyAdjacentPositions = new double[4][];
                colorsOnEmptyAdjacentPositions[0] = (i > 0) ? lastBoard.getPosition(i - 1, j) == Board.EMPTY ? calculateAverageColorOnPosition(i - 1, j) : null : null;
                colorsOnEmptyAdjacentPositions[1] = (j < boardDimension - 1) ? lastBoard.getPosition(i, j + 1) == Board.EMPTY ? calculateAverageColorOnPosition(i, j + 1) : null : null;
                colorsOnEmptyAdjacentPositions[2] = (i < boardDimension - 1) ? lastBoard.getPosition(i + 1, j) == Board.EMPTY ? calculateAverageColorOnPosition(i + 1, j) : null : null;
                colorsOnEmptyAdjacentPositions[3] = (j > 0) ? lastBoard.getPosition(i, j - 1) == Board.EMPTY ? calculateAverageColorOnPosition(i, j - 1) : null : null;

                snapshot.append("    Average color around = " + printColor(colorAroundPosition) + "\n");
                snapshot.append("    Luminance around     = " + luminance(colorAroundPosition) + "\n");
                snapshot.append("    Variance around      = " + variance(colorAroundPosition) + "\n");
                snapshot.append("    ---\n");

                MoveHypothesis hypothesis = calculateColorHypothesis5(colorAroundPosition, averageColors, counters, colorsOnEmptyAdjacentPositions);
                hypothesis.row = i;
                hypothesis.column = j;

                double[] hsvOnPosition = calculateAverageHsvOnPosition(i, j, hsvImage);
                MoveHypothesis hypothesisHsv = calculateHypothesisHsv(hsvOnPosition, averageHue, averageValue, counters);
                hsvHypothesesFound.add(hypothesisHsv);
                if (hypothesisHsv.color != Board.EMPTY) {
                    System.out.println("    hypothesis hsv(" + j + ", " + i + ") = " + hypothesisHsv.color + " (confidence: " + hypothesisHsv.confidence + ")");
                }

                snapshot.append("    Hypothesis = " + hypothesis.color + " (confidence: " + hypothesis.confidence + ")\n");

                // if (hypothesis.color != Board.EMPTY) {
                    moveHypothesesFound.add(hypothesis);
                // }
                /*
                Ao invés de filtrar as jogadas por cor antes, vamos filtrá-las depois, acho que faz mais
                sentido. Pega-se a jogada mais provável e verifica-se se ela é possível.
                if (hypothesis.color != Board.EMPTY) {
                    if (canBeBlackStone && hypothesis.color == Board.BLACK_STONE ||
                            canBeWhiteStone && hypothesis.color == Board.WHITE_STONE) {
                        hipotesesDeJogadasEncontradas.add(hypothesis);
                    }
                }
                */

            }
        }

        // Chooses the move that obtained highest confidence
        // IMPORTANT: Could verify if the difference in confidence between the
        // two most probable moves is too small, discard both, because that's
        // a sign that the detector is confused
        
        Move chosenMove = null;
        double biggestConfidence = 0;
        for (MoveHypothesis hypothesis : moveHypothesesFound) {
            if (hypothesis.color != Board.EMPTY) System.out.println(hypothesis);
            // We use all the move hypotheses to increase or decrease the confidence of all the
            // previously detected stones
            if (lastBoard.getPosition(hypothesis.row, hypothesis.column) != Board.EMPTY) {
                if (hypothesis.color == lastBoard.getPosition(hypothesis.row, hypothesis.column)) {
                    lastBoard.increaseStability(hypothesis.row, hypothesis.column);
                } else {
                    boolean wasStoneRemoved = lastBoard.decreaseStability(hypothesis.row, hypothesis.column);
                    if (wasStoneRemoved) {
                        Move lastMove = game.getLastMove();
                        if (hypothesis.row == lastMove.row && hypothesis.column == lastMove.column) {
                            // The stone that was removed was the last played move
                            game.undoLastMove();
                            System.out.println("Undoing last move of the game");
                            canBeBlackStone = game.canNextMoveBe(Board.BLACK_STONE);
                            canBeWhiteStone = game.canNextMoveBe(Board.WHITE_STONE);
                        } else {
                            game.removeStoneAt(hypothesis.row, hypothesis.column);
                            System.out.println("Removing move of the game");
                        }
                    }
                }
            } else if (hypothesis.color != Board.EMPTY && hypothesis.confidence > biggestConfidence) {
                biggestConfidence = hypothesis.confidence;
                chosenMove = new Move(hypothesis.row, hypothesis.column, hypothesis.color);
            }
        }

        if (isMovePossiblyPartOfAShadow(chosenMove, moveHypothesesFound, game.getLastBoard())) {
            System.out.println("Move is possibly part of a shadow");
            chosenMove = null;
        }

        if (chosenMove != null && (
                canBeBlackStone && chosenMove.color == Board.BLACK_STONE ||
                canBeWhiteStone && chosenMove.color == Board.WHITE_STONE
        )) {
            System.out.println("Chosen move = " + chosenMove + " with confidence " + biggestConfidence);
            snapshot.append("Chosen move = " + chosenMove + " with confidence " + biggestConfidence + "\n");
        }
        else {
            if (chosenMove == null) {
                snapshot.append("No move detected.");
                System.out.println("No move detected.");
            } else {
                snapshot.append("Chosen move that is invalid = " + chosenMove);
                System.out.println("Chosen move that is invalid = " + chosenMove);
            }
        }

        // System.out.println(snapshot);

        return lastBoard.generateNewBoardWith(chosenMove);
    }

    // Shadows are often times detected as black stones. This method tries to detect when
    // that happens by checking if a black mass of stones suddenly appeared on the board.
    // 
    // This is more useful in the beginning of the game, because from the middle to the
    // ebd the board starts getting full of stones and shadows may not be detected anymore.
    private boolean isMovePossiblyPartOfAShadow(Move move, List<MoveHypothesis> hypotheses, Board currentBoard)
    {
        if (move == null || move.color != Board.BLACK_STONE) return false;

        final int dimension = currentBoard.getDimension();
        char[][] board = new char[dimension][dimension];
        Boolean[][] visited = new Boolean[dimension][dimension];
        for (int i = 0; i < dimension; i++)
            for (int j = 0; j < dimension; j++) {
                board[i][j] = '.';
                visited[i][j] = false;
            }

        // Filter out hypotheses which are on positions where black stones are already present
        for (MoveHypothesis hypothesis : hypotheses) {
            board[hypothesis.row][hypothesis.column] =
                currentBoard.getPosition(hypothesis.row, hypothesis.column) == Board.BLACK_STONE
                ? '.'
                : getColorLetter(hypothesis.color);
        }

        int numberOfVisits = dfs(move.row, move.column, board, visited, dimension);

        return numberOfVisits >= 4;
    }

    private char getColorLetter(int color)
    {
        if (color == Board.BLACK_STONE) return 'B';
        else if (color == Board.WHITE_STONE) return 'W';
        return '.';
    }

    private int dfs(int row, int column, char[][] board, Boolean[][] visited, int dimension)
    {
        if (row < 0 || column < 0 || row >= dimension || column >= dimension
                || visited[row][column] || board[row][column] != 'B') return 0;

        visited[row][column] = true;
        int numberOfVisits = 1;
        numberOfVisits += dfs(row - 1, column, board, visited, dimension);
        numberOfVisits += dfs(row, column + 1, board, visited, dimension);
        numberOfVisits += dfs(row + 1, column, board, visited, dimension);
        numberOfVisits += dfs(row, column - 1, board, visited, dimension);
        return numberOfVisits;
    }

    private void getAverageColors(Board lastBoard, double[][] averageColors, int[] counters) {
        counters[Board.EMPTY] = 0;
        counters[Board.BLACK_STONE] = 0;
        counters[Board.WHITE_STONE] = 0;

        for (int i = 0; i < boardDimension; ++i) {
            for (int j = 0; j < boardDimension; ++j) {
                int colorOnPosition = lastBoard.getPosition(i, j);
                counters[colorOnPosition]++;
                double[] averageColorOnPosition = calculateAverageColorOnPosition(i, j);

                for (int k = 0; k < boardImage.channels(); ++k) {
                    averageColors[colorOnPosition][k] += averageColorOnPosition[k];
                }
            }
        }

        for (int i = 0; i < 3; ++i) {
            if (counters[i] > 0) {
                for (int j = 0; j < boardImage.channels(); ++j) {
                    averageColors[i][j] /= counters[i];
                }
                snapshot.append("Average color (");
                if (i == Board.EMPTY) {
                    snapshot.append("empty intersections");
                }
                else if (i == Board.BLACK_STONE) {
                    snapshot.append("black stones");
                }
                else if (i == Board.WHITE_STONE) {
                    snapshot.append("white stones");
                }
                snapshot.append(") = " + printColor(averageColors[i]) + "\n");
                snapshot.append("    Luminance = " + luminance(averageColors[i]) + "\n");
                snapshot.append("    Variance = " + variance(averageColors[i]) + "\n");
            }
        }
    }

    private double luminance(double cor[]) {
        return 0.299 * cor[0] + 0.587 * cor[1] + 0.114 * cor[2];
    }

    private double variance(double color[]) {
        double average = (color[0] + color[1] + color[2]) / 3;
        double differences[] = {color[0] - average, color[1] - average, color[2] - average};
        return (differences[0] * differences[0] +
                differences[1] * differences[1] +
                differences[2] * differences[2]) / 3;
    }
    
    private void getAverageOfChannel(Board lastBoard, double[] averageHue, int[] counters, Mat hueChannelImage, String label)
    {
        for (int i = 0; i < boardDimension; ++i) {
            for (int j = 0; j < boardDimension; ++j) {
                int colorOnPosition = lastBoard.getPosition(i, j);
                double averageColorOnPosition = calculateAverageHueOnPosition(i, j, hueChannelImage);
                averageHue[colorOnPosition] += averageColorOnPosition;
            }
        }

        for (int i = 0; i < 3; ++i) {
            if (counters[i] > 0) {
                averageHue[i] /= counters[i];
                snapshot.append("Average " + label + " (");
                System.out.print("Average " + label + " (");
                if (i == Board.EMPTY) System.out.print("empty intersections");
                else if (i == Board.BLACK_STONE) System.out.print("black stones");
                else if (i == Board.WHITE_STONE) System.out.print("white stones");
                System.out.println(") = " + averageHue[i]);
            }
        }
    }

    private MoveHypothesis calculateColorHypothesis5(double[] cor, double[][] coresMedias, int[] contadores, double[][] coresNasPosicoesAdjacentes) {
        double[] preto = {10.0, 10.0, 10.0, 255.0};

        double luminanceBeingChecked = luminance(cor);
        double varianceBeingChecked = variance(cor);
        double distanceToBlack = getColorDistance(cor, preto);
        double luminanceDifferenceToNeighbors = calculateLuminanceDifference(cor, coresNasPosicoesAdjacentes);
        snapshot.append("    Luminance difference to adjacent empty positions = " + luminanceDifferenceToNeighbors + "\n");

        double distanceToIntersectionAverage = 999;
        double distanceToIntersectionsLuminance = 999;
        double distanceToIntersectionVariance = 999;
        if (contadores[Board.EMPTY] > 0) {
            distanceToIntersectionAverage = getColorDistance(cor, coresMedias[Board.EMPTY]);
            distanceToIntersectionsLuminance = Math.abs(luminanceBeingChecked - luminance(coresMedias[Board.EMPTY])) ;
            distanceToIntersectionVariance = Math.abs(varianceBeingChecked - variance(coresMedias[Board.EMPTY]));
        }
        double distanceToBlackStonesAverage = 999;
        double distanceToBlackStonesLuminance = 999;
        double distanceToBlackStonesVariance = 999;
        if (contadores[Board.BLACK_STONE] > 0) {
            distanceToBlackStonesAverage = getColorDistance(cor, coresMedias[Board.BLACK_STONE]);
            distanceToBlackStonesLuminance = Math.abs(luminanceBeingChecked - luminance(coresMedias[Board.BLACK_STONE]));
            distanceToBlackStonesVariance = Math.abs(varianceBeingChecked - variance(coresMedias[Board.BLACK_STONE]));
        }
        double distanceToWhiteStonesAverage = 999;
        double distanceToWhiteStonesLuminance = 999;
        double distanceToWhiteStonesVariance = 999;
        if (contadores[Board.WHITE_STONE] > 0) {
            distanceToWhiteStonesAverage = getColorDistance(cor, coresMedias[Board.WHITE_STONE]);
            distanceToWhiteStonesLuminance = Math.abs(luminanceBeingChecked - luminance(coresMedias[Board.WHITE_STONE]));
            distanceToWhiteStonesVariance = Math.abs(varianceBeingChecked - variance(coresMedias[Board.WHITE_STONE]));
        }

        double distanceToIntersections = distanceToIntersectionAverage + distanceToIntersectionsLuminance + distanceToIntersectionVariance;
        double distanceToBlackStones   = distanceToBlackStonesAverage + distanceToBlackStonesLuminance + distanceToBlackStonesVariance;
        double distanceToWhiteStones   = distanceToWhiteStonesAverage + distanceToWhiteStonesLuminance + distanceToWhiteStonesVariance;

        snapshot.append("    Distance to black stones average    = " + distanceToBlackStonesAverage + "\n");
        snapshot.append("    Distance to black stones luminance  = " + distanceToBlackStonesLuminance + "\n");
        snapshot.append("    Distance to black stones variance   = " + distanceToBlackStonesVariance + "\n");
        snapshot.append("    Distance to black stones            = " + distanceToBlackStones + "\n");
        snapshot.append("    Distance to white stones average    = " + distanceToWhiteStonesAverage + "\n");
        snapshot.append("    Distance to white stones luminance  = " + distanceToWhiteStonesLuminance + "\n");
        snapshot.append("    Distance to white stones variance   = " + distanceToWhiteStonesVariance + "\n");
        snapshot.append("    Distance to white stones            = " + distanceToWhiteStones + "\n");
        snapshot.append("    Distance to intersections average   = " + distanceToIntersectionAverage + "\n");
        snapshot.append("    Distance to intersections luminance = " + distanceToIntersectionsLuminance + "\n");
        snapshot.append("    Distance to intersections variance  = " + distanceToIntersectionVariance + "\n");
        snapshot.append("    Distance to intersections           = " + distanceToIntersections + "\n");

        if (contadores[Board.BLACK_STONE] == 0 && contadores[Board.WHITE_STONE] == 0) {
            if (luminanceDifferenceToNeighbors < -30) {
                return new MoveHypothesis(Board.BLACK_STONE, 1);
            }
            if (distanceToBlack < 50) {
                return new MoveHypothesis(Board.BLACK_STONE, 0.9);
            }
            if (distanceToBlack < distanceToIntersectionAverage) {
                return new MoveHypothesis(Board.BLACK_STONE, 0.7);
            }
            return new MoveHypothesis(Board.EMPTY, 1);
        }

        if (contadores[Board.WHITE_STONE] == 0) {
            if (luminanceDifferenceToNeighbors < -30) {
                return new MoveHypothesis(Board.BLACK_STONE, 1);
            }
            if (distanceToBlack < 50) {
                return new MoveHypothesis(Board.BLACK_STONE, 0.9);
            }
            if (distanceToBlack < distanceToIntersectionAverage) {
                return new MoveHypothesis(Board.BLACK_STONE, 0.7);
            }
            if (luminanceDifferenceToNeighbors > 30) {
                return new MoveHypothesis(Board.WHITE_STONE, 1);
            }
            if (luminanceDifferenceToNeighbors > 15) {
                return new MoveHypothesis(Board.WHITE_STONE, 0.9);
            }
            // Estes valores para pedras brancas precisariam ser revistos
            /*else if (cor[2] >= 150) {
                return new MoveHypothesis(Board.WHITE_STONE, 0.7);
            }*/
            return new MoveHypothesis(Board.EMPTY, 1);
        }

        // This condition was added because when an invalid black stone was played (after another
        // black stone and not as a handicap stone) something unexpected happened: the positions
        // around the black stone were seen as white stones because of the difference in contrast.
        // Verifying if the positions look like empty intersections before verifying if they look
        // like white stones solves this problem.
        if (distanceToIntersectionAverage < 20) {
            return new MoveHypothesis(Board.EMPTY, 1);
        }
        if (distanceToBlack < 30 || luminanceDifferenceToNeighbors < -30) {
            if (distanceToBlackStones < distanceToIntersections && distanceToIntersections - distanceToBlackStones > 100) {
                return new MoveHypothesis(Board.BLACK_STONE, 1);
            }
        }
/*        if (luminanceDifferenceToNeighbors > 30) {
            // The 0.99 is just for cases when a black stone is played but a white sotne is detected
            // erroneously. With this 0.99 confidence, the black stone has priority.
            return new MoveHypothesis(Board.WHITE_STONE, 0.99);
        }*/
        if (luminanceDifferenceToNeighbors > 15) {
            // Esta verificação é importante, por isso resolvi deixar apenas esta condição de > 15 e tirar a de cima
            if (distanceToWhiteStones < distanceToIntersections && distanceToIntersections - distanceToWhiteStones > 100) {
                return new MoveHypothesis(Board.WHITE_STONE, 0.99);
            }
        }

        double[] probabilityOfBeing = new double[3];

        probabilityOfBeing[Board.BLACK_STONE] = 1 - (distanceToBlackStones);
        probabilityOfBeing[Board.WHITE_STONE] = 1 - (distanceToWhiteStones);
        probabilityOfBeing[Board.EMPTY] = 1 - (distanceToIntersections);

        snapshot.append("    Probability of being a black stone = " + probabilityOfBeing[Board.BLACK_STONE] + "\n");
        snapshot.append("    Probability of being a white stone = " + probabilityOfBeing[Board.WHITE_STONE] + "\n");
        snapshot.append("    Probability of being empty         = " + probabilityOfBeing[Board.EMPTY] + "\n");

        if (probabilityOfBeing[Board.BLACK_STONE] > probabilityOfBeing[Board.WHITE_STONE] &&
                probabilityOfBeing[Board.BLACK_STONE] > probabilityOfBeing[Board.EMPTY]) {

            if (Math.abs(probabilityOfBeing[Board.BLACK_STONE] - probabilityOfBeing[Board.EMPTY]) < 100) {
                return new MoveHypothesis(Board.EMPTY, 0.5);
            }

            double diferencas = probabilityOfBeing[Board.BLACK_STONE] - probabilityOfBeing[Board.WHITE_STONE];
            diferencas += probabilityOfBeing[Board.BLACK_STONE] - probabilityOfBeing[Board.EMPTY];
            snapshot.append("    Hypothesis of being black stone with difference " + (diferencas / 2) + "\n");
            return new MoveHypothesis(Board.BLACK_STONE, diferencas / 2);
        }

        if (probabilityOfBeing[Board.WHITE_STONE] > probabilityOfBeing[Board.BLACK_STONE] &&
                probabilityOfBeing[Board.WHITE_STONE] > probabilityOfBeing[Board.EMPTY]) {

            // This possible white stone is almost indistinguishable from an empty intersection.
            // To reduce false positives, we consider it's an empty intersection.
            if (Math.abs(probabilityOfBeing[Board.WHITE_STONE] - probabilityOfBeing[Board.EMPTY]) < 100) {
                return new MoveHypothesis(Board.EMPTY, 0.5);
            }

            double differences = probabilityOfBeing[Board.WHITE_STONE] - probabilityOfBeing[Board.BLACK_STONE];
            differences += probabilityOfBeing[Board.WHITE_STONE] - probabilityOfBeing[Board.EMPTY];
            snapshot.append("    Hypothesis of being a white stone with differences " + (differences / 2) + "\n");
            return new MoveHypothesis(Board.WHITE_STONE, differences / 2);
        }

        return new MoveHypothesis(Board.EMPTY, 1);
    }

    private double calculateLuminanceDifference(double color[], double colorsOnAdjacentPositions[][]) {
        double difference = 0;
        double centerLuminance = luminance(color);
        int numberOfValidNeighbors = 0;
        for (int i = 0; i < 4; ++i) {
            if (colorsOnAdjacentPositions[i] != null) {
                double luminancia = luminance(colorsOnAdjacentPositions[i]);
                difference += (centerLuminance - luminancia);
                numberOfValidNeighbors++;
            }
        }
        if (numberOfValidNeighbors > 0) {
            return difference / (double)numberOfValidNeighbors;
        }
        return 0;
    }

    private String printColor(double color[]) {
        StringBuilder saida = new StringBuilder("(");
        for (int i = 0; i < color.length; ++i) {
            saida.append(color[i] + ", ");
        }
        saida.append(")");
        return saida.toString();
    }

    private MoveHypothesis calculateHypothesisHsv(double[] hsvOnPosition, double[] averageHue, double[] averageValue, int[] counters)
    {
        double[] black = { 90.0, 60.0, 20.0 };
        double[] white = { 100.0, 30.0, 210.0 };

        double distanceToIntersectionAverage = 999;
        if (counters[Board.EMPTY] > 0) {
            distanceToIntersectionAverage = getHsvDistance(hsvOnPosition, new double[] { averageHue[Board.EMPTY], 0, averageValue[Board.EMPTY] });
        }
        double distanceToBlackStonesAverage = 999;
        if (counters[Board.BLACK_STONE] > 0) {
            distanceToBlackStonesAverage = getHsvDistance(hsvOnPosition, new double[] { averageHue[Board.BLACK_STONE], 0, averageValue[Board.BLACK_STONE] });
        }
        double distanceToWhiteStonesAverage = 999;
        if (counters[Board.WHITE_STONE] > 0) {
            distanceToWhiteStonesAverage = getHsvDistance(hsvOnPosition, new double[] { averageHue[Board.WHITE_STONE], 0, averageValue[Board.WHITE_STONE] });
        }

        double distanceToHypotheticalBlackStone = getHsvDistance(hsvOnPosition, black);
        double distanceToHypotheticalWhiteStone = getHsvDistance(hsvOnPosition, white);
        double distanceToIntersections = distanceToIntersectionAverage;
        double distanceToBlackStones   = distanceToBlackStonesAverage;
        double distanceToWhiteStones   = distanceToWhiteStonesAverage;

        snapshot.append("    Distance to black stones average    = " + distanceToBlackStonesAverage + "\n");
        snapshot.append("    Distance to black stones            = " + distanceToBlackStones + "\n");
        snapshot.append("    Distance to white stones average    = " + distanceToWhiteStonesAverage + "\n");
        snapshot.append("    Distance to white stones            = " + distanceToWhiteStones + "\n");
        snapshot.append("    Distance to intersections average   = " + distanceToIntersectionAverage + "\n");
        snapshot.append("    Distance to intersections           = " + distanceToIntersections + "\n");

        if (counters[Board.BLACK_STONE] == 0 && counters[Board.WHITE_STONE] == 0) {
            if (distanceToHypotheticalBlackStone < distanceToIntersections && distanceToHypotheticalBlackStone < distanceToHypotheticalWhiteStone) {
                return new MoveHypothesis(Board.BLACK_STONE, 1);
            }
            return new MoveHypothesis(Board.EMPTY, 1);
        }

        if (counters[Board.WHITE_STONE] == 0) {
            if (distanceToBlackStones < distanceToIntersections && distanceToBlackStones < distanceToHypotheticalWhiteStone) {
                return new MoveHypothesis(Board.BLACK_STONE, 0.99);
            }
            if (distanceToHypotheticalWhiteStone < distanceToIntersections && distanceToHypotheticalWhiteStone < distanceToBlackStones) {
                return new MoveHypothesis(Board.WHITE_STONE, 0.99);
            }
            return new MoveHypothesis(Board.EMPTY, 1);
        }

        double[] probabilityOfBeing = new double[3];

        probabilityOfBeing[Board.BLACK_STONE] = 1 - (distanceToBlackStones);
        probabilityOfBeing[Board.WHITE_STONE] = 1 - (distanceToWhiteStones);
        probabilityOfBeing[Board.EMPTY] = 1 - (distanceToIntersections);

        snapshot.append("    Probability of being a black stone = " + probabilityOfBeing[Board.BLACK_STONE] + "\n");
        snapshot.append("    Probability of being a white stone = " + probabilityOfBeing[Board.WHITE_STONE] + "\n");
        snapshot.append("    Probability of being empty         = " + probabilityOfBeing[Board.EMPTY] + "\n");

        if (probabilityOfBeing[Board.BLACK_STONE] > probabilityOfBeing[Board.WHITE_STONE] &&
                probabilityOfBeing[Board.BLACK_STONE] > probabilityOfBeing[Board.EMPTY]) {
            return new MoveHypothesis(Board.BLACK_STONE, 0.9);
        }

        if (probabilityOfBeing[Board.WHITE_STONE] > probabilityOfBeing[Board.BLACK_STONE] &&
                probabilityOfBeing[Board.WHITE_STONE] > probabilityOfBeing[Board.EMPTY]) {
            return new MoveHypothesis(Board.WHITE_STONE, 0.9);
        }

        return new MoveHypothesis(Board.EMPTY, 1);
    }

    // Let's put a bigger weight on hue because it is the de facto color
    private double getHsvDistance(double[] color1, double[] color2)
    {
        return (3 * Math.abs(color1[0] - color2[0]) + Math.abs(color1[2] - color2[2])) / 4;
    }

    private double calculateAverageHueOnPosition(int row, int column, Mat hueChannelImage)
    {
        int y = row * (int) boardImage.size().width / (boardDimension - 1);
        int x = column * (int) boardImage.size().height / (boardDimension - 1);
        double color[] = calculateAverageColors(y, x, hueChannelImage);
        return color[0];
    }

    // TODO: Transformar hipóteses de recuperação de cor em classes separadas
    private double[] calculateAverageColorOnPosition(int row, int column) {

        int y = row * (int) boardImage.size().width / (boardDimension - 1);
        int x = column * (int) boardImage.size().height / (boardDimension - 1);

        double[] color = calculateAverageColors(y, x, boardImage);
        // Would a Gaussian average bring much improvement?
//        double[] color = recuperarMediaGaussianaDeCores(boardImage, row, column);
        return color;
    }

    private double[] calculateAverageHsvOnPosition(int row, int column, Mat image) {

        int y = row * (int) image.size().width / (boardDimension - 1);
        int x = column * (int) image.size().height / (boardDimension - 1);
        double[] hsv = calculateAverageColors(y, x, image);
        return hsv;
    }

    /**
     * Calculates the average color around a position in the image
     *
     * @param y
     * @param x
     * @return
     */
    private double[] calculateAverageColors(int y, int x, Mat image) {
        /**

         * The othogonal board image has a size of 500x500 pixels.
         * This calculation returns more or less the size of a little less than half of a stone in
         * the orthogonal board image.
         * 9x9 -> 25
         * 13x13 -> 17
         * 19x19 -> 11
         *
         * Before, the radius used was 8 pixels. In a 9x9 board on a 500x500 image, a radius of 8px,
         * on an intersection that has the hoshi point, the detector almost thought there was a black
         * stone there.
         */ 
        //int radius = 500 / (partida.getBoardDimension() - 1) * 0.33;
        int radius = 0;
        if (boardDimension == 9) {
            radius = 21;
        }
        else if (boardDimension == 13) {
            radius = 14;
        }
        else if (boardDimension == 19) {
            radius = 9;
        }

        // It's not a circle, but with the speedup gain, I think it's worth it to calculate the
        // average of colors this way
        Mat roi = image.submat(
                Math.max(y - radius, 0),
                Math.min(y + radius, image.height()),
                Math.max(x - radius, 0),
                Math.min(x + radius, image.width())
        );
        Scalar averageScalar = Core.mean(roi);

        double[] averageColor = new double[image.channels()];
        for (int i = 0; i < image.channels(); ++i) {
            averageColor[i] = averageScalar.val[i];
            snapshot.append("Cor média ao redor de (" + x + ", " + y + ") = " + printColor(averageColor) + "\n");
        }

        return averageColor;
    }

}
