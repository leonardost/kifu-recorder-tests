import cv2
import math
import numpy as np
from hierarquia import HierarquiaDeQuadrilateros
























def distance(x1, y1, x2, y2):
	return math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2))

def recuperarCorPredominanteNaPosicao(linha, coluna, imagem, radius):
	cor = np.array([0, 0, 0])

	#~ # este raio deveria ser proporcional ao tamanho das pedras no tabuleiro
	#~ radius = 10;
	contador = 0;
	# http://stackoverflow.com/questions/19098104/python-opencv2-cv2-wrapper-get-image-size
	height, width, channels = imagem.shape

	for yy in range(linha - radius, linha + radius + 1):
		if yy < 0 or yy >= height:
			continue
		for xx in range(coluna - radius, coluna + radius + 1):
			if xx < 0 or xx >= width:
				continue
			if distance(xx, yy, coluna, linha) < radius:
				cor += imagem[yy][xx]
				contador += 1;

	cor /= contador
	#~ print cor;
	return cor;
	#return imagem[linha][coluna]

def distanciaDeCor(cor1, cor2):
	distancia = 0
	for i in range(0, len(cor1)):
		distancia += math.fabs(cor1[i] - cor2[i])
	return distancia

def distanciaDeCor2(cor1, cor2):
	distancia = 0
	distancia = math.fabs(cor1[0] - cor2[0])
	#~ for i in range(0, len(cor1)):
		#~ distancia += math.fabs(cor1[0] - cor2[0])
	return distancia

def hipoteseDeCor(cor, corMediaDoTabuleiro):
	preto = np.array([20, 20, 20])
	branco = np.array([200, 200, 200])
	
	distanciaParaPreto = distanciaDeCor(cor, preto)
	distanciaParaBranco = distanciaDeCor2(cor, branco)
	distanciaParaCorMedia = distanciaDeCor(cor, corMediaDoTabuleiro)

	# Testando algumas coisas
	# BGR
	#if cor[0] <= 20:
	if distanciaParaPreto < 80 or distanciaParaPreto < distanciaParaCorMedia:
		return 1     # pedra preta
	#~ elif cor[0] >= 150:     # este eh um valor totalmente arbitrario, dependendo da foto tenho certeza que nao ira funcionar
		#~ return 2     # pedra branca
	elif cor[0] >= corMediaDoTabuleiro[0] * 1.35:
		return 2
	else:
		return 0     # vazio

	# Se a distancia para a media for menor que um certo threshold, muito provavelmente e uma
	# interseccao vazia
	if distanciaParaCorMedia < 120:
		return 0
	if distanciaParaPreto < distanciaParaBranco:
		return 1;
	else:
		return 2;

def corMedia(imagem):
	media = np.array([0, 0, 0])

	height, width, channels = imagem.shape

	for y in range(0, height):
		for x in range(0, width):
			c = imagem[y][x]
			media += c

	media /= height * width
	return media

def desenharTabuleiro(tabuleiro, tamanhoImagem, indice):
	
	imagem = np.zeros((tamanhoImagem, tamanhoImagem, 3), np.uint8)

	distanciaEntreLinhas = tamanhoImagem / (dimensaoDoTabuleiro + 1);
	fimDasLinhas = tamanhoImagem - distanciaEntreLinhas
	raioDaPedra = 31 - dimensaoDoTabuleiro # estava usando tamanhoImagem / 20 para o 9x9
	p1 = (0, 0)
	p2 = (tamanhoImagem, tamanhoImagem)
	cv2.rectangle(imagem, p1, p2, (255, 255, 255), -1)

	# Desenha linhas horizontais
	for i in range(dimensaoDoTabuleiro):
		inicio = (
			distanciaEntreLinhas,
			distanciaEntreLinhas + distanciaEntreLinhas * i
		)
		fim = (
			#~ int(tamanhoImagem * 0.9),
			fimDasLinhas,
			inicio[1]
		)
		cv2.line(imagem, inicio, fim, (0, 0, 0))

	# Desenha linhas verticais
	for i in range(dimensaoDoTabuleiro):
		inicio = (
			distanciaEntreLinhas + distanciaEntreLinhas * i,
			distanciaEntreLinhas
		)
		fim = (
			inicio[0],
			#~ int(tamanhoImagem * 0.9)
			fimDasLinhas
		)
		cv2.line(imagem, inicio, fim, (0, 0, 0));

	# Desenha pedras
	for i in range(dimensaoDoTabuleiro):
		for j in range(dimensaoDoTabuleiro):
			centro = (
				distanciaEntreLinhas + j * distanciaEntreLinhas,
				distanciaEntreLinhas + i * distanciaEntreLinhas
			)
			if tabuleiro[i][j] == 1:
				cv2.circle(imagem, centro, raioDaPedra, (0, 0, 0), -1)
			elif tabuleiro[i][j] == 2:
				cv2.circle(imagem, centro, raioDaPedra, (255, 255, 255), -1)
				cv2.circle(imagem, centro, raioDaPedra, (0, 0, 0))
	
	#~ cv2.imshow("tabuleiro detectado", imagem)	
	cv2.imwrite("tabuleiro detectado " + str(indice) + ".jpg", imagem)	






























 
# tamanho da imagem no celular: 1080x1776
#~ height = 1080
#~ width = 1776
height = 1080
width = 1440

for indice in xrange(52, 67):

	print "Processando imagem " + str(indice)

	im = cv2.imread("images/imagem" + str(indice) + ".jpg")
	#resized_image = im
	#resized_image = cv2.resize(im, (0, 0), fx=0.28, fy=0.28)
	resized_image = cv2.resize(im, (width, height))

	# bordas
	image_with_borders_in_evidence = cv2.Canny(resized_image, 30, 100)
	image_with_borders_in_evidence = cv2.dilate(image_with_borders_in_evidence, np.ones((3, 3)))
	#~ cv2.imshow("etapa1", image_with_borders_in_evidence)
	cv2.imwrite("etapa1 (" + str(indice) + ").jpg", image_with_borders_in_evidence)

	# contornos
	contours, hierarchy = cv2.findContours(image_with_borders_in_evidence.copy(), cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
	contours = [contour for contour in contours if cv2.contourArea(contour) > 700]

	imagem_contornos = np.zeros((height, width, 3), np.uint8)
	cv2.drawContours(imagem_contornos, contours, -1, (0, 255, 0), 1)
	#~ cv2.imshow("etapa2", imagem_contornos)
	cv2.imwrite("etapa2 (" + str(indice) + ").jpg", imagem_contornos)

	# quadrilateros
	quadrilateros = []
	for contorno in contours:
		quadrilateros.append(cv2.approxPolyDP(contorno, cv2.arcLength(contorno, True) * 0.012, True))
	quadrilateros = [quadrilatero for quadrilatero in quadrilateros if
		len(quadrilatero) == 4 and
		cv2.contourArea(quadrilatero) > 400 and 
		cv2.isContourConvex(quadrilatero)
	]

	imagem_quadrilateros = np.zeros((height, width, 3), np.uint8)
	cv2.drawContours(imagem_quadrilateros, quadrilateros, -1, (0, 0, 255), 1)
	#~ cv2.imshow("etapa3", imagem_quadrilateros)
	cv2.imwrite("etapa3 (" + str(indice) + ").jpg", imagem_quadrilateros)

	hierarquiaDeQuadrados = HierarquiaDeQuadrilateros(quadrilateros)

	contornoMaisProximo = None
	numeroDeFilhos = 9999
	threshould = 10

	if len(hierarquiaDeQuadrados.externos) == 0:
		continue
	
	for contorno in hierarquiaDeQuadrados.externos:
		numeroDeContornosDentro = len(hierarquiaDeQuadrados.hierarquia[hierarquiaDeQuadrados.hash(contorno)])
		if numeroDeContornosDentro < numeroDeFilhos and numeroDeContornosDentro > threshould:
			contornoMaisProximo = contorno
			numeroDeFilhos = numeroDeContornosDentro

	cv2.drawContours(imagem_quadrilateros, [contornoMaisProximo], -1, (255, 0, 0), 1)
	#~ cv2.imshow("etapa4 - quadrilatero do tabuleiro", imagem_quadrilateros)
	cv2.imwrite("etapa4 (" + str(indice) + ").jpg", imagem_quadrilateros)

	areaMedia = 0
	for quadrilatero in hierarquiaDeQuadrados.hierarquia[hierarquiaDeQuadrados.hash(contornoMaisProximo)]:
		areaMedia += cv2.contourArea(quadrilatero)
	areaMedia /= len(hierarquiaDeQuadrados.hierarquia[hierarquiaDeQuadrados.hash(contornoMaisProximo)])
	areaDoTabuleiro = cv2.contourArea(contornoMaisProximo)
	razao = areaMedia / areaDoTabuleiro

	if razao <= 1.0 / 324.0:       # 18 quadrados por 18
		dimensaoDoTabuleiro = 19
	elif razao <= 1.0 / 144.0:
		dimensaoDoTabuleiro = 13   # 12 quadrados por 12
	else:
		dimensaoDoTabuleiro = 9

	# print "dimensao = " + str(dimensaoDoTabuleiro)

	#~ print contornoMaisProximo[0];
	#~ print contornoMaisProximo[1];
	#~ print contornoMaisProximo[2];
	#~ print contornoMaisProximo[3];

	# http://stackoverflow.com/questions/9808601/is-getperspectivetransform-broken-in-opencv-python2-wrapper

	larguraImagem = 1000
	alturaImagem = 1000

	src = np.array([
		[contornoMaisProximo[0][0][0], contornoMaisProximo[0][0][1]],
		[contornoMaisProximo[3][0][0], contornoMaisProximo[3][0][1]],
		[contornoMaisProximo[2][0][0], contornoMaisProximo[2][0][1]],
		[contornoMaisProximo[1][0][0], contornoMaisProximo[1][0][1]]],
		np.float32
	)
	dst = np.array([
		[0, 0],
		[larguraImagem + 1, 0],
		[larguraImagem + 1, alturaImagem + 1],
		[0, alturaImagem + 1]],
		np.float32)
	matrizDeTransformacao = cv2.getPerspectiveTransform(src, dst)
	#~ print matrizDeTransformacao

	tabuleiro_corrigido = cv2.warpPerspective(resized_image, matrizDeTransformacao, (larguraImagem + 1, alturaImagem + 1))

	#~ cv2.imshow("etapa5 - tabuleiro corrigido", tabuleiro_corrigido)
	cv2.imwrite("etapa5 (" + str(indice) + ").jpg", tabuleiro_corrigido)
	tabuleiro = []

	corMediaDoTabuleiro = corMedia(tabuleiro_corrigido)

	#~ print "cor media do tabuleiro = "
	#~ print corMediaDoTabuleiro

	tabuleiro = []
	for i in range(dimensaoDoTabuleiro):
		tabuleiro.append([0 for x in range(dimensaoDoTabuleiro)])

	#~ # Detectar circulos
	# https://extr3metech.wordpress.com/2012/09/23/convert-photo-to-grayscale-with-python-opencv/
	tabuleiro_corrigido_pb = cv2.cvtColor(tabuleiro_corrigido, cv2.COLOR_BGR2GRAY)
	# http://stackoverflow.com/questions/22241474/i-get-a-error-when-using-houghcircles-with-python-opencv-that-a-module-is-missin
	# http://opencv-python-tutroals.readthedocs.org/en/latest/py_tutorials/py_imgproc/py_houghcircles/py_houghcircles.html

	#~ cv2.imshow("tabuleiro corrigido em preto e branco", tabuleiro_corrigido_pb)

	#~ circles = cv2.HoughCircles(tabuleiro_corrigido_pb, cv2.cv.CV_HOUGH_GRADIENT, 1, 20,
			#~ param1 = 50, param2 = 70, minRadius = 0, maxRadius = 200)
	#~ print "----"
	#~ print circles
	#~ print "----"
	#~ if len(circles) > 0:
		#~ for circle in circles[0]:
			#~ # draw the outer circle
			#~ cv2.circle(tabuleiro_corrigido_pb, (circle[0], circle[1]), circle[2], (0, 255, 0), 1)
	#~ cv2.imshow("tabuleiro corrigido com circulos identificados", tabuleiro_corrigido_pb)

	radius = int((larguraImagem / (dimensaoDoTabuleiro - 1)) / 3.5)
	# Teoricamente deveria ser / 2, mas como as pedras nao sao perfeitamente do tamanho
	# dos quadrados do tabuleiro e aparentemente pegar a cor media de uma regiao maior eh
	# pior, parece ser melhor se concentrar na regiao central das pedras
	#~ print "------"
	#~ print "radius = " + str(radius)
	#~ print "------"
	#~ radius = 10

	for i in range(0, dimensaoDoTabuleiro):
		for j in range(0, dimensaoDoTabuleiro):
			#~ print (i, j)

			# Verificar se um circulo esta presente na interseccao
			
			cor = recuperarCorPredominanteNaPosicao(
				(i * alturaImagem / (dimensaoDoTabuleiro - 1)),
				(j * larguraImagem / (dimensaoDoTabuleiro - 1)),
				tabuleiro_corrigido,
				radius)
			hipotese = hipoteseDeCor(cor, corMediaDoTabuleiro)
			if hipotese != 0:
				tabuleiro[i][j] = hipotese
			#~ cv2.circle(tabuleiro_corrigido, (
					#~ (j * larguraImagem / (dimensaoDoTabuleiro - 1)),
					#~ (i * alturaImagem / (dimensaoDoTabuleiro - 1))
					#~ ),
					#~ radius, (0, 255, 0), 1)
				
	#~ cv2.imshow("tabuleiro sadf", tabuleiro_corrigido)
	cv2.imwrite("tabuleiro_ortogonal_" + str(indice) + ".jpg", tabuleiro_corrigido)

	desenharTabuleiro(tabuleiro, 500, indice)
	#~ for i in range(dimensaoDoTabuleiro):
		#~ print tabuleiro[i]

#~ cv2.waitKey(0)












	
