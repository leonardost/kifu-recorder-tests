import cv2

class HierarquiaDeQuadrilateros:

	def __init__(self, quadrilateros):
		
		self.hierarquia = {}
		self.folhas = []
		self.externos = []

		for quadrilatero in quadrilateros:
			#~ print self.hash(quadrilatero)
			self.hierarquia[self.hash(quadrilatero)] = []
			for outroQuadrilatero in quadrilateros:
				if (outroQuadrilatero == quadrilatero).all():
					continue
				if self.estaDentro(quadrilatero, outroQuadrilatero):
					self.hierarquia[self.hash(quadrilatero)].append(outroQuadrilatero)
		
		#~ print "----"
		
		for quadrilatero in quadrilateros:
			#print self.hash(quadrilatero)
			if len(self.hierarquia[self.hash(quadrilatero)]) == 0:
				self.folhas.append(quadrilatero)
			else:
				self.externos.append(quadrilatero)

	def estaDentro(self, quadrilateroExterno, quadrilateroInterno):
		for ponto in quadrilateroInterno:
			resultado = cv2.pointPolygonTest(quadrilateroExterno, (ponto[0][0], ponto[0][1]), False)
			if resultado != 1:
				return False
		return True
		
	def hash(self, quadrilatero):
		hashString = "";
		for ponto in quadrilatero:
			hashString += str(ponto[0][0])
			hashString += "_"
			hashString += str(ponto[0][1])
		return hashString
