import cv2
import math
import numpy as np
from hierarquia import HierarquiaDeQuadrilateros
 
#~ height = 1080
#~ width = 1776
height = 1000
width = 1000
 
imagem = cv2.imread("etapa5.jpg")

#~ imagem = cv2.resize(imagem, (width, height))

#~ # Detectar circulos
# https://extr3metech.wordpress.com/2012/09/23/convert-photo-to-grayscale-with-python-opencv/
imagem_pb = cv2.cvtColor(imagem, cv2.COLOR_BGR2GRAY)
# http://stackoverflow.com/questions/22241474/i-get-a-error-when-using-houghcircles-with-python-opencv-that-a-module-is-missin
# http://opencv-python-tutroals.readthedocs.org/en/latest/py_tutorials/py_imgproc/py_houghcircles/py_houghcircles.html

#~ cv2.imshow("tabuleiro corrigido em preto e branco", tabuleiro_corrigido_pb)

#~ circles = cv2.HoughCircles(imagem_pb, cv2.cv.CV_HOUGH_GRADIENT, 1, 40,
		#~ param1 = 50, param2 = 70, minRadius = 5, maxRadius = 200)
		
image_with_borders_in_evidence = cv2.Canny(imagem, 30, 100)
image_with_borders_in_evidence = cv2.dilate(image_with_borders_in_evidence, np.ones((3, 3)))
#~ image_with_borders_in_evidence = cv2.dilate(image_with_borders_in_evidence, np.ones((2, 2)))
#~ image_with_borders_in_evidence = cv2.dilate(image_with_borders_in_evidence, np.ones((2, 2)))
		
cv2.imshow("filtro canny", image_with_borders_in_evidence)
		
#~ circles = cv2.HoughCircles(imagem_pb, cv2.cv.CV_HOUGH_GRADIENT, 1, 40,
		#~ param1 = 30, param2 = 50, minRadius = 20, maxRadius = 200)
circles = cv2.HoughCircles(image_with_borders_in_evidence, cv2.cv.CV_HOUGH_GRADIENT, 1, 30,
		param1 = 50, param2 = 15, minRadius = 20, maxRadius = 29)
print "----"
print circles
print "----"
if circles is not None:
	if len(circles) > 0:
		for circle in circles[0]:
			if circle[2] > 40:
				continue
			cv2.circle(imagem, (circle[0], circle[1]), 2, (0, 0, 255), 2)
			# draw the outer circle
			cv2.circle(imagem, (circle[0], circle[1]), circle[2], (0, 255, 0), 2)

cv2.imshow("tabuleiro corrigido com circulos identificados", imagem)
cv2.waitKey(0)
