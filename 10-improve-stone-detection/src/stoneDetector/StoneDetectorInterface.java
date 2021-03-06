package src.stoneDetector;

import src.models.Board;

public interface StoneDetectorInterface {
    public Board detect();
    public Board detect(Board lastBoard, boolean canBeBlackStone, boolean canBeWhiteStone);
    public void setImageIndex(int imageIndex);
}
