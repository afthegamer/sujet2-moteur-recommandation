package techmarket

import techmarket.calcul.Similarite

class SimilariteTest extends munit.FunSuite:

  private val tolerance = 1e-9

  test("cosinus de deux vecteurs identiques vaut 1"):
    assertEqualsDouble(Similarite.cosinus(Vector(1.0, 2.0, 3.0), Vector(1.0, 2.0, 3.0)), 1.0, tolerance)

  test("cosinus de deux vecteurs orthogonaux vaut 0"):
    assertEqualsDouble(Similarite.cosinus(Vector(1.0, 0.0), Vector(0.0, 1.0)), 0.0, tolerance)

  test("cosinus est insensible à l'échelle des vecteurs"):
    assertEqualsDouble(Similarite.cosinus(Vector(1.0, 2.0), Vector(10.0, 20.0)), 1.0, tolerance)

  test("cosinus vaut 0 si l'un des vecteurs est nul"):
    assertEqualsDouble(Similarite.cosinus(Vector(0.0, 0.0), Vector(1.0, 1.0)), 0.0, tolerance)

  test("cosinus vaut 0 si les tailles diffèrent"):
    assertEqualsDouble(Similarite.cosinus(Vector(1.0), Vector(1.0, 2.0)), 0.0, tolerance)

  test("cosinus vaut 0 sur des vecteurs vides"):
    assertEqualsDouble(Similarite.cosinus(Vector.empty, Vector.empty), 0.0, tolerance)

  test("cosinus reste dans [-1, 1]"):
    val a = Vector(3.0, 1.0, 4.0)
    val b = Vector(1.0, 5.0, 9.0)
    val resultat = Similarite.cosinus(a, b)
    assert(resultat >= -1.0 && resultat <= 1.0, s"hors bornes : $resultat")

  test("cosinus est symétrique"):
    val a = Vector(2.0, 0.0, 1.0)
    val b = Vector(1.0, 3.0, 0.0)
    assertEqualsDouble(Similarite.cosinus(a, b), Similarite.cosinus(b, a), tolerance)
