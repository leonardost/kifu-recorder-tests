import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.util.List;
import java.util.ArrayList;

public class process_image {

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public final static int RADIUS_OF_REGION_OF_INTEREST = 50;

    public static void main(String[] args) {

        if (args.length != 10) {
            printUsage();
            return;
        }

        String inputFolder = args[0];
        int numberOfImages = Integer.parseInt(args[1]);
        List<Mat> boardImageSeries = readImageFiles(inputFolder, numberOfImages);

        Ponto[] corners = new Ponto[4];
        corners[0] = new Ponto(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        corners[1] = new Ponto(Integer.parseInt(args[4]), Integer.parseInt(args[5]));
        corners[2] = new Ponto(Integer.parseInt(args[6]), Integer.parseInt(args[7]));
        corners[3] = new Ponto(Integer.parseInt(args[8]), Integer.parseInt(args[9]));

        CornerDetector cornerDetector = new CornerDetector();

        int imageIndex = 1;
        for (Mat image : boardImageSeries) {

            for (int i = 0; i < 4; i++) {
                Ponto possibleNewCorner = cornerDetector.updateCorner(image, corners[i], i + 1);
                if (possibleNewCorner != null) {
                    corners[i] = possibleNewCorner;
                }
            }

            printCornerPositions(imageIndex, corners);
            drawBoardContourOnImage(image, corners, imageIndex);
            imageIndex++;
        }

    }

    private static void printUsage() {
        System.out.println("Usage: process_image INPUT_FOLDER NUMBER_OF_IMAGES P1 P2 P3 P4");
        System.out.println("The input folder must contain a sequence of images named as \"frameX.jpg\"");
        System.out.println("P1, P2, P3 and P4 are the positions of the corners of the board in the first");
        System.out.println("image, each composed of two integers X and Y separated by spaces.");
        System.out.println();
        System.out.println("Example:");
        System.out.println("process_image input 7 1210 136 1252 984 617 937 582 235");
        System.out.println();
        System.out.println("The outputs of this algorithm are the input images with the go board's contour");
        System.out.println("marked in red");
    }

    private static List<Mat> readImageFiles(String inputFolder, int numberOfImages) {
        ArrayList<Mat> boardImages = new ArrayList<>();
        for (int i = 1; i <= numberOfImages; i++) {
            boardImages.add(Imgcodecs.imread(inputFolder + "/frame" + i + ".jpg"));
        }
        return boardImages;
    }

    private static void printCornerPositions(int imageIndex, Ponto[] corners) {
        // System.out.println("Frame " + imageIndex);
        // for (int i = 0; i < 4; i++) {
        //     System.out.print("Corner " + (i + 1) + ": ");
        //     System.out.println(corners[i]);
        // }
        // System.out.println("-----");
        for (int i = 0; i < 4; i++) {
            System.out.print(corners[i].x + " " + corners[i].y + " ");
        }
        System.out.println();
    }

    private static void drawBoardContourOnImage(Mat image, Ponto[] corners, int imageIndex) {
        Point[] boardCorners = new Point[4];
        boardCorners[0] = new Point(corners[0].x, corners[0].y);
        boardCorners[1] = new Point(corners[1].x, corners[1].y);
        boardCorners[2] = new Point(corners[2].x, corners[2].y);
        boardCorners[3] = new Point(corners[3].x, corners[3].y);
        MatOfPoint boardContour = new MatOfPoint(boardCorners);

        List<MatOfPoint> contourPoints = new ArrayList<MatOfPoint>();
        contourPoints.add(boardContour);
        Scalar red = new Scalar(0, 0, 255);
        Imgproc.drawContours(image, contourPoints, -1, red, 2);
        Imgcodecs.imwrite("output/image" + imageIndex + ".jpg", image);
    }

}
