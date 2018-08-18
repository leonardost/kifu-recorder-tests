
class PointCluster:

    def __init__(self):
        self.centroid = (-1, -1)
        self.points = []

    def getCentroid(self):
        return self.centroid

    def add(self, point):
        self.points.append(point)
        self.__updateCentroid()

    def __updateCentroid(self):
        accumulatedY = 0
        accumulatedX = 0
        for point in self.points:
            accumulatedY = accumulatedY + point[0]
            accumulatedX = accumulatedX + point[1]
        numberOfPoints = len(self.points)
        self.centroid[0] = accumulatedY / numberOfPoints
        self.centroid[1] = accumulatedX / numberOfPoints

    def distanceTo(self, point):
        if self.centroid[0] == -1:
            return 0
        return (self.centroid[0] - point[0]) * (self.centroid[0] - point[0]) + (self.centroid[1] - point[1]) * (self.centroid[1] - point[1])
