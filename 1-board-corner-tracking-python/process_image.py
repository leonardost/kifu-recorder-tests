import cv2
import math
import numpy as np
from hierarquia import HierarquiaDeQuadrilateros
from PointCluster import PointCluster
 
#~ height = 1080
#~ width = 1776
height = 800
width = 1200

imagem = cv2.imread("b-a_2018-04-25_jogada0_camera_(1).jpg")
# imagem = cv2.imread("b-a_2018-04-25_jogada0_camera_(5).jpg")
# imagem = cv2.imread("b-a_2018-04-25_jogada0_camera_(11).jpg")

def distance(point1, point2):
	return (point1[0] - point2[0]) * (point1[0] - point2[0]) + (point1[1] - point2[1]) * (point1[1] - point2[1])

def findCorners(image, index):

	# Colocar bordas ao redor da imagem
	# cv2.line(image, (0, 0), (99, 0), (0, 0, 0), 10)
	# cv2.line(image, (0, 0), (0, 99), (0, 0, 0), 10)
	# cv2.line(image, (99, 0), (99, 99), (0, 0, 0), 10)
	# cv2.line(image, (0, 99), (99, 99), (0, 0, 0), 10)
	# cv2.imshow('imagem ' + str(index), image)

	center = (50, 50)

	blackWhiteImage = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
	# image_with_borders_in_evidence = cv2.Canny(image, 30, 100)
	# image_with_borders_in_evidence = cv2.dilate(image_with_borders_in_evidence, np.ones((3, 3)))

	# https://docs.opencv.org/3.0-beta/doc/py_tutorials/py_feature2d/py_features_harris/py_features_harris.html#harris-corners
	gray = np.float32(blackWhiteImage)
	dst = cv2.cornerHarris(gray, 2, 3, 0.04)
	#result is dilated for marking the corners, not important
	dst = cv2.dilate(dst, None)
	# dst = cv2.dilate(dst, np.ones((3, 3)))
	# Threshold for an optimal value, it may vary depending on the image.
	threshouldCorner = 0.01 * dst.max()
	image[dst > threshouldCorner] = [0, 0, 255]
	cv2.imshow('image' + str(index), image)

	centerCornerImage = np.zeros((100, 100, 3), np.uint8)
	smallestDistance = 999999999
	closestPoint = (-1, -1)
	pointClusters = []
	pointClusters.append(PointCluster())

	for i in range(0, 100):
		for j in range(0, 100):
			if image[i][j][0] == 0 and image[i][j][1] == 0 and image[i][j][2] == 255:
				addPointToClosestPointCluster((i, j), pointClusters)
				if distance((i, j), center) < smallestDistance:
					smallestDistance = distance((i, j), center)
					closestPoint = (i, j)
	centerCornerImage[closestPoint[0]][closestPoint[1]] = [0, 0, 255]
	for pointCluster in pointClusters:
		centroid = pointCluster.getCentroid()
		centerCornerImage[centroid[0]][centroid[1]] = [255, 0, 0]
	cv2.imshow('centro' + str(index), centerCornerImage)

	# for line in image:
	# 	for pixel in line:
	# 		if pixel[0] == 0 and pixel[1] == 0 and pixel[2] == 255:
	# 			print pixel
	print "-----"
	# print image
	# print "-----"

	# image_with_borders_in_evidence = cv2.dilate(image_with_borders_in_evidence, np.ones((2, 2)))
	# cv2.imshow('canny', image_with_borders_in_evidence)

	bwContours = cv2.Canny(blackWhiteImage, 30, 100)
	bwContours = cv2.dilate(bwContours, np.ones((3, 3)))
	cv2.imshow('bwContours' + str(index), bwContours)

	# contours, hierarchy = cv2.findContours(image_with_borders_in_evidence.copy(), cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
	# contours = []
	contours, hierarchy = cv2.findContours(bwContours.copy(), cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
	# contours = [contour for contour in contours if cv2.contourArea(contour) > 700]
	imagem_contornos = np.zeros((100, 100, 3), np.uint8)
	cv2.drawContours(imagem_contornos, contours, -1, (0, 255, 0), 1)
	cv2.imshow('contornos' + str(index), imagem_contornos)

	# quadrilateros
	imagem_quadrilateros_temp = np.zeros((100, 100, 3), np.uint8)
	quadrilateros = []
	for contorno in contours:
		quadrilateros.append(cv2.approxPolyDP(contorno, cv2.arcLength(contorno, True) * 0.0012, True))
		cv2.drawContours(imagem_quadrilateros_temp, quadrilateros, -1, (255, 0, 0), 1)
	cv2.imshow("quadrilateros_temp (" + str(index) + ")", imagem_quadrilateros_temp)
	quadrilateros = [quadrilatero for quadrilatero in quadrilateros if
		# len(quadrilatero) == 4 and
		cv2.isContourConvex(quadrilatero)
	]

	imagem_quadrilateros = np.zeros((100, 100, 3), np.uint8)
	cv2.drawContours(imagem_quadrilateros, quadrilateros, -1, (255, 0, 0), 1)
	#~ cv2.imshow("etapa3", imagem_quadrilateros)
	# cv2.imshow("quadrilateros (" + str(index) + ")", imagem_quadrilateros)

	# cv2.findContours(bwContours.copy(), cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE, contours)
	# cv2.drawContours(bwContours, contours, -1, [255, 0, 0], 10)
	# cv2.imshow('contours' + str(index), contours)

def addPointToClosestPointCluster(point, pointClusters):
	distanceThreshouldForPointCluster = 100
	foundCluster = False

	for pointCluster in pointClusters:
		if pointCluster.distanceTo(point) < distanceThreshouldForPointCluster:
			pointCluster.add((point))
			foundCluster = True

	if not foundCluster:
		pointCluster = PointCluster()
		pointCluster.add(point)
		pointClusters.append(pointCluster)



cantos = [
	(512, 115),
	(1367, 97),
	(1723, 943),
	(265, 1005)
]


	# cv2.circle(imagem, cantos[0], 10, [255, 0, 0], 5)
	# cv2.circle(imagem, cantos[1], 10, [255, 0, 0], 5)
	# cv2.circle(imagem, cantos[2], 10, [255, 0, 0], 5)
	# cv2.circle(imagem, cantos[3], 10, [255, 0, 0], 5)

padding = 50

regioesCanto = []
for i in range(0, 4):
	regioesCanto.append(imagem[cantos[i][1] - padding:cantos[i][1] + padding, cantos[i][0] - padding:cantos[i][0] + padding])
	findCorners(regioesCanto[i], i)
# regiaoCanto1 = 
# regiaoCanto2 = imagem[cantos[1][1] - padding:cantos[1][1] + padding, cantos[1][0] - padding:cantos[1][0] + padding]
# regiaoCanto3 = imagem[cantos[2][1] - padding:cantos[2][1] + padding, cantos[2][0] - padding:cantos[2][0] + padding]
# regiaoCanto4 = imagem[cantos[3][1] - padding:cantos[3][1] + padding, cantos[3][0] - padding:cantos[3][0] + padding]

imagem = cv2.resize(imagem, (width, height))




# # Initiate FAST object with default values
# fast = cv2.FastFeatureDetector()

# # find and draw the keypoints
# kp = fast.detect(imagem, None)
# img2 = cv2.drawKeypoints(imagem, kp, color=(255,0,0))
# cv2.imshow("filtro canny", img2)
# # cv2.imshow("filtro canny", image_with_borders_in_evidence)





cv2.waitKey(0)
