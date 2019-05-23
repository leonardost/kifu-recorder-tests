package src.stoneDetector;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import src.models.Board;
import src.models.Move;
import src.models.MoveHypothesis;

import java.util.ArrayList;
import java.util.List;

public class StoneDetector implements StoneDetectorInterface {

    private int imageIndex;

    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
    }

    // Imagem ortogonal e quadrada do board
    private Mat boardImage = null;
    // Dimensões do board (9x9, 13x13 ou 19x19)
    private int boardDimension = 0;
    // Informações de debug do estado atual sendo visto pelo detector
    public StringBuilder snapshot;

    public void setBoardDimension(int boardDimension) {
        this.boardDimension = boardDimension;
    }

    public void setBoardImage(Mat boardImage) {
        this.boardImage = boardImage;
    }

    /**
     * Stone detection that does not use the previous state of the game
     */
    public Board detect() {

        Board board = new Board(boardDimension);

        double[] averageColorDoBoard = averageColorDoBoard(boardImage);

        for (int i = 0; i < boardDimension; ++i) {
            for (int j = 0; j < boardDimension; ++j) {
                double[] color = getAverageColorAround(i, j);

                int hypothesis = getColorHypothesis(color, averageColorDoBoard);
                if (hypothesis != Board.EMPTY) {
                    board.putStone(i, j, hypothesis);
                }
            }
        }

        return board;
    }

    /**
     * Utiliza a informação do último estado do jogo para melhorar a detecção da
     * última jogada feita. Os parâmetros informam se o detector deve procurar
     * uma pedra preta, branca, ou ambas, de acordo com o estado atual da
     * partida.
     */
    public Board detect(Board lastBoard, boolean canBeBlackStone, boolean canBeWhiteStone) {
        long startTime = System.currentTimeMillis();
        double[][] averageColors = new double[3][boardImage.channels()];
        int[] counters = new int[3];

        snapshot = new StringBuilder();

        findAverageColorsOf(lastBoard, averageColors, counters);

        List<MoveHypothesis> hypothesissDeMovesEncontradas = new ArrayList<>();

        for (int i = 0; i < boardDimension; ++i) {
            for (int j = 0; j < boardDimension; ++j) {

//                Log.i(TestesActivity.TAG, "(" + i + ", " + j + ")\n");
                snapshot.append(String.format("(%1$2d, %2$2d)", i, j) + "\n");

                // Ignora as interseções das jogadas que já foram feitas
                if (lastBoard.getPosition(i, j) != Board.EMPTY) continue;

                double[] colorAroundPosition = getAverageColorAround(i, j);

                double[][] colorsAtAdjacentEmptyPositions = new double[4][];
                colorsAtAdjacentEmptyPositions[0] = (i > 0) ? lastBoard.getPosition(i - 1, j) == Board.EMPTY ? getAverageColorAround(i - 1, j) : null : null;
                colorsAtAdjacentEmptyPositions[1] = (j < boardDimension - 1) ? lastBoard.getPosition(i, j + 1) == Board.EMPTY ? getAverageColorAround(i, j + 1) : null : null;
                colorsAtAdjacentEmptyPositions[2] = (i < boardDimension - 1) ? lastBoard.getPosition(i + 1, j) == Board.EMPTY ? getAverageColorAround(i + 1, j) : null : null;
                colorsAtAdjacentEmptyPositions[3] = (j > 0) ? lastBoard.getPosition(i, j - 1) == Board.EMPTY ? getAverageColorAround(i, j - 1) : null : null;

                snapshot.append("    Cor média ao redor  = " + printColor(colorAroundPosition) + "\n");
                snapshot.append("    Luminancia ao redor = " + calculateLuminanceOf(colorAroundPosition) + "\n");
                snapshot.append("    Variância ao redor  = " + calculateVarianceOf(colorAroundPosition) + "\n");
                snapshot.append("    ---\n");

                MoveHypothesis hypothesis = getColorHypothesis5(colorAroundPosition, averageColors, counters, colorsAtAdjacentEmptyPositions);
                hypothesis.row = i;
                hypothesis.column = j;

                snapshot.append("    Hipótese = " + hypothesis.color + " (confiança: " + hypothesis.confidence + ")\n");

                if (hypothesis.color != Board.EMPTY) {
                    hypothesissDeMovesEncontradas.add(hypothesis);
                }
            }
        }
        
        Move chosenMove = null;
        double biggestConfidence = 0;
        for (MoveHypothesis hypothesis : hypothesissDeMovesEncontradas) {
            if (hypothesis.confidence > biggestConfidence) {
                biggestConfidence = hypothesis.confidence;
                chosenMove = new Move(hypothesis.row, hypothesis.column, hypothesis.color);
            }
        }

        if (chosenMove != null && (canBeBlackStone && chosenMove.color == Board.BLACK_STONE ||
                canBeWhiteStone && chosenMove.color == Board.WHITE_STONE)) {
            snapshot.append("Chosen move = " + chosenMove + " with confidence " + biggestConfidence + "\n");
        }
        else {
            snapshot.append("Nenhuma jogada detectada.\n");
            chosenMove = null;
        }

        return lastBoard.generateNewBoardWith(chosenMove);
    }

    private void findAverageColorsOf(Board lastBoard, double[][] averageColors, int[] counters) {
        long startTime = System.currentTimeMillis();
        counters[Board.EMPTY] = 0;
        counters[Board.BLACK_STONE] = 0;
        counters[Board.WHITE_STONE] = 0;

        for (int i = 0; i < boardDimension; ++i) {
            for (int j = 0; j < boardDimension; ++j) {
                int colorAtPosition = lastBoard.getPosition(i, j);
                counters[colorAtPosition]++;
                double[] averageColorAtPosition = getAverageColorAround(i, j);

                for (int k = 0; k < boardImage.channels(); ++k) {
                    averageColors[colorAtPosition][k] += averageColorAtPosition[k];
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
                snapshot.append("    Luminance = " + calculateLuminanceOf(averageColors[i]) + "\n");
                snapshot.append("    Variance = " + calculateVarianceOf(averageColors[i]) + "\n");
            }
        }
    }

    private double calculateLuminanceOf(double color[]) {
        return 0.299 * color[0] + 0.587 * color[1] + 0.114 * color[2];
    }

    private MoveHypothesis getColorHypothesis5(double[] color, double[][] averageColors, int[] counters, double[][] colorsAtAdjacentPositions) {
        double[] black = {10.0, 10.0, 10.0, 255.0};

        double verifiedLuminance = calculateLuminanceOf(color);
        double verifiedVariance = calculateVarianceOf(color);
        double distanceToBlack = getColorDistance(color, black);
        double luminanceDifferenceToNeighbors = calculateLuminanceDifference(color, colorsAtAdjacentPositions);
        snapshot.append("    Diferença de luminância para as posições adjacentes vazias = " + luminanceDifferenceToNeighbors + "\n");

        double distanceToIntersectionAverage = 999;
        double distanceToIntersectionLuminance = 999;
        double distanceToIntersectionVariance = 999;
        if (counters[Board.EMPTY] > 0) {
            distanceToIntersectionAverage = getColorDistance(color, averageColors[Board.EMPTY]);
            distanceToIntersectionLuminance = Math.abs(verifiedLuminance - calculateLuminanceOf(averageColors[Board.EMPTY])) ;
            distanceToIntersectionVariance = Math.abs(verifiedVariance - calculateVarianceOf(averageColors[Board.EMPTY]));
        }
        double distanceToBlackStonesAverage = 999;
        double distanceToBlackStonesLuminance = 999;
        double distanceToBlackStonesVariance = 999;
        if (counters[Board.BLACK_STONE] > 0) {
            distanceToBlackStonesAverage = getColorDistance(color, averageColors[Board.BLACK_STONE]);
            distanceToBlackStonesLuminance = Math.abs(verifiedLuminance - calculateLuminanceOf(averageColors[Board.BLACK_STONE]));
            distanceToBlackStonesVariance = Math.abs(verifiedVariance - calculateVarianceOf(averageColors[Board.BLACK_STONE]));
        }
        double distanceToWhiteStonesAverage = 999;
        double distanceToWhiteStonesLuminance = 999;
        double distanceToWhiteStonesVariance = 999;
        if (counters[Board.WHITE_STONE] > 0) {
            distanceToWhiteStonesAverage = getColorDistance(color, averageColors[Board.WHITE_STONE]);
            distanceToWhiteStonesLuminance = Math.abs(verifiedLuminance - calculateLuminanceOf(averageColors[Board.WHITE_STONE]));
            distanceToWhiteStonesVariance = Math.abs(verifiedVariance - calculateVarianceOf(averageColors[Board.WHITE_STONE]));
        }

        double distanceToIntersections = distanceToIntersectionAverage + distanceToIntersectionLuminance + distanceToIntersectionVariance;
        double distanceToBlackStones = distanceToBlackStonesAverage + distanceToBlackStonesLuminance + distanceToBlackStonesVariance;
        double distanceToWhiteStones = distanceToWhiteStonesAverage + distanceToWhiteStonesLuminance + distanceToWhiteStonesVariance;

        snapshot.append("    Distância para média das pedras pretas    = " + distanceToBlackStonesAverage + "\n");
        snapshot.append("    Distancia para luminância das pretas      = " + distanceToBlackStonesLuminance + "\n");
        snapshot.append("    Distancia para calculateVarianceOf das pretas       = " + distanceToBlackStonesVariance + "\n");
        snapshot.append("    Distância para pretas                     = " + distanceToBlackStones + "\n");
        snapshot.append("    Distância para média das pedras brancas   = " + distanceToWhiteStonesAverage + "\n");
        snapshot.append("    Distancia para luminância das brancas     = " + distanceToWhiteStonesLuminance + "\n");
        snapshot.append("    Distancia para calculateVarianceOf das brancas      = " + distanceToWhiteStonesVariance + "\n");
        snapshot.append("    Distância para brancas                    = " + distanceToWhiteStones + "\n");
        snapshot.append("    Distância para média das interseções      = " + distanceToIntersectionAverage + "\n");
        snapshot.append("    Distancia para luminância das interseçoes = " + distanceToIntersectionLuminance + "\n");
        snapshot.append("    Distancia para calculateVarianceOf das interseçoes  = " + distanceToIntersectionVariance + "\n");
        snapshot.append("    Distância para interseções                = " + distanceToIntersections + "\n");

        if (counters[Board.BLACK_STONE] == 0 && counters[Board.WHITE_STONE] == 0) {
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

        if (counters[Board.WHITE_STONE] == 0) {
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
            /*else if (color[2] >= 150) {
                return new MoveHypothesis(Board.WHITE_STONE, 0.7);
            }*/
            return new MoveHypothesis(Board.EMPTY, 1);
        }

        // Esta condição foi adicionada porque quando uma pedra preta era jogada de forma inválida
        // (após outra pedra preta e não sendo pedra de handicap) acontecia algo inusitado: as
        // posições ao redor da pedra preta eram vistas como pedras brancas devido à diferença de
        // contraste. Verificar se as interseções se parecem com interseções vazias antes de
        // verificar se se parecem com pedras brancas resolve esse problema.
        if (distanceToIntersectionAverage < 20) {
            return new MoveHypothesis(Board.EMPTY, 1);
        }
        if (distanceToBlack < 30 || luminanceDifferenceToNeighbors < -30) {
            if (distanceToBlackStones < distanceToIntersections && distanceToIntersections - distanceToBlackStones > 100) {
                return new MoveHypothesis(Board.BLACK_STONE, 1);
            }
        }
/*        if (luminanceDifferenceToNeighbors > 30) {
            // O 0.99 é só para os casos em que uma pedra preta é colocada mas uma pedra branca é detectada
            // erroneamente. Com esta confiança em 0.99, a pedra preta tem prioridade.
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

        snapshot.append("    Probabilidade de ser pedra preta  = " + probabilityOfBeing[Board.BLACK_STONE] + "\n");
        snapshot.append("    Probabilidade de ser pedra branca = " + probabilityOfBeing[Board.WHITE_STONE] + "\n");
        snapshot.append("    Probabilidade de ser vazio        = " + probabilityOfBeing[Board.EMPTY] + "\n");

        if (probabilityOfBeing[Board.BLACK_STONE] > probabilityOfBeing[Board.WHITE_STONE] &&
                probabilityOfBeing[Board.BLACK_STONE] > probabilityOfBeing[Board.EMPTY]) {

            if (Math.abs(probabilityOfBeing[Board.BLACK_STONE] - probabilityOfBeing[Board.EMPTY]) < 100) {
                return new MoveHypothesis(Board.EMPTY, 0.5);
            }

            double differences = probabilityOfBeing[Board.BLACK_STONE] - probabilityOfBeing[Board.WHITE_STONE];
            differences += probabilityOfBeing[Board.BLACK_STONE] - probabilityOfBeing[Board.EMPTY];
            snapshot.append("    Hipótese de ser pedra preta com diferenças de " + (differences / 2) + "\n");
            return new MoveHypothesis(Board.BLACK_STONE, differences / 2);
        }

        if (probabilityOfBeing[Board.WHITE_STONE] > probabilityOfBeing[Board.BLACK_STONE] &&
                probabilityOfBeing[Board.WHITE_STONE] > probabilityOfBeing[Board.EMPTY]) {

            // Esta possível pedra branca está quase indistinguível de uma interseção vazia.
            // Para diminuir os falsos positivos, consideramos que é uma interseção bazia.
            if (Math.abs(probabilityOfBeing[Board.WHITE_STONE] - probabilityOfBeing[Board.EMPTY]) < 100) {
                return new MoveHypothesis(Board.EMPTY, 0.5);
            }

            double differences = probabilityOfBeing[Board.WHITE_STONE] - probabilityOfBeing[Board.BLACK_STONE];
            differences += probabilityOfBeing[Board.WHITE_STONE] - probabilityOfBeing[Board.EMPTY];
            snapshot.append("    Hipótese de ser pedra branca com diferenças de " + (differences / 2) + "\n");
            return new MoveHypothesis(Board.WHITE_STONE, differences / 2);
        }

        return new MoveHypothesis(Board.EMPTY, 1);
    }

    private double calculateLuminanceDifference(double color[], double colorAtAdjacentPositions[][]) {
        double difference = 0;
        double centerLuminance = calculateLuminanceOf(color);
        int numberOfValidNeighbors = 0;
        for (int i = 0; i < 4; ++i) {
            if (colorAtAdjacentPositions[i] != null) {
                double luminance = calculateLuminanceOf(colorAtAdjacentPositions[i]);
                difference += (centerLuminance - luminance);
                numberOfValidNeighbors++;
            }
        }
        if (numberOfValidNeighbors > 0) {
            return difference / (double)numberOfValidNeighbors;
        }
        return 0;
    }

    private String printColor(double color[]) {
        StringBuilder output = new StringBuilder("(");
        for (int i = 0; i < color.length; ++i) {
            output.append(color[i] + ", ");
        }
        output.append(")");
        return output.toString();
    }

    private double calculateVarianceOf(double color[]) {
        double average = (color[0] + color[1] + color[2]) / 3;
        double differences[] = {color[0] - average, color[1] - average, color[2] - average};
        return (differences[0] * differences[0] +
                differences[1] * differences[1] +
                differences[2] * differences[2]) / 3;
    }

    private double[] getAverageColorAround(int row, int column) {

        int y = row * (int)boardImage.size().width / (boardDimension - 1);
        int x = column * (int)boardImage.size().height / (boardDimension - 1);

        double[] color = retrieveAverageColors(y, x);
        return color;
    }

    /**
     * Recupera a cor average ao redor de uma posiçao na imagem
     *
     * @param y
     * @param x
     * @return
     */
    private double[] retrieveAverageColors(int y, int x) {
        /**
         * A imagem do board ortogonal tem 500x500 pixels de dimensão.
         * Este cálculo pega mais ou menos o tamanho de pouco menos de metade de uma pedra na imagem do
         * board ortogonal.
         * 9x9 -> 25
         * 13x13 -> 17
         * 19x19 -> 11
         * 
         * Antes o raio sendo utilizado era de 8 pixels. Em um board 9x9 em uma imagme de 500x500
         * pixels, um raio de 8 pixels, em uma interseção que tem o ponto do hoshi, o detector quase
         * achava que havia uma pedra preta ali.
         */ 
        //int radius = 500 / (partida.getDimensaoDoBoard() - 1) * 0.33;
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

        // Não é um círculo, mas pelo speedup, acho que compensa pegar a média
        // de cores assim
        Mat roi = boardImage.submat(
                Math.max(y - radius, 0),
                Math.min(y + radius, boardImage.height()),
                Math.max(x - radius, 0),
                Math.min(x + radius, boardImage.width())
        );
        Scalar scalarAverage = Core.mean(roi);

        double[] averageColor = new double[boardImage.channels()];
        // System.out.println(">>>>> " + boardImage.channels());
        // System.out.println(">>>>> " + scalarAverage.val.length);
        // for (int i = 0; i < scalarAverage.val.length; ++i) {
        for (int i = 0; i < boardImage.channels(); ++i) {
            averageColor[i] = scalarAverage.val[i];
        }

        return averageColor;
    }

    /**
     * Verifica se uma determinada cor está mais próxima de uma pedra preta ou branca.
     *
     * @param cor Cor a ser verificada
     * @param averageColorDoBoard Cor média da imagem do board
     * @return Pedra preta, branca, ou vazio
     */
    private int getColorHypothesis(double[] color, double[] averageColorDoBoard) {
        double[] black = {0.0, 0.0, 0.0, 255.0};
        double[] white = {255.0, 255.0, 255.0, 255.0};
        double distanceToBlack = getColorDistance(color, black);
        double distanceToWhite = getColorDistance(color, white);
        double distanceToAverageColor = getColorDistance(color, averageColorDoBoard);

        // Testando outras hipóteses
        if (distanceToBlack < 80 || distanceToBlack < distanceToAverageColor) {
            return Board.BLACK_STONE;
        }
//        else if (color[2] >= 150) {
        else if (color[2] >= averageColorDoBoard[2] * 1.35) {
            return Board.WHITE_STONE;
        }
        else if (true) {
            return Board.EMPTY;
        }

        // Se a distância para a média for menor que um certo threshold, muito provavelmente é uma
        // intersecção vazia
        if (distanceToAverageColor < 120) {
            return Board.EMPTY;
        }

        if (distanceToBlack < distanceToWhite) {
            return Board.BLACK_STONE;
        }
        else {
            return Board.WHITE_STONE;
        }
    }

    private double getColorDistance(double[] color1, double[] color2) {
        double distancia = 0;
        for (int i = 0; i < Math.min(color1.length, color2.length); ++i) {
            distancia += Math.abs(color1[i] - color2[i]);
        }
        return distancia;
    }

    /**
     * Retorna a cor average do board.
     * 
     * ESTA COR MUDA CONFORME O JOGO PROGRIDE E CONFORME A ILUMINAÇÃO MUDA.
     *
     * @param boardImage
     * @return
     */
    private double[] averageColorDoBoard(Mat boardImage) {
        Scalar scalarAverage = Core.mean(boardImage);

        double[] average = new double[boardImage.channels()];
        for (int i = 0; i < scalarAverage.val.length; ++i) {
            average[i] = scalarAverage.val[i];
        }

        return average;
    }

}
