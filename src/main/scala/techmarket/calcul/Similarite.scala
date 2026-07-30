package techmarket.calcul

object Similarite:

  def cosinus(a: Vector[Double], b: Vector[Double]): Double =
    if a.length != b.length || a.isEmpty then 0.0
    else
      val produitScalaire = a.lazyZip(b).map(_ * _).sum
      val normeA = math.sqrt(a.map(x => x * x).sum)
      val normeB = math.sqrt(b.map(x => x * x).sum)
      if normeA == 0.0 || normeB == 0.0 then 0.0
      else produitScalaire / (normeA * normeB)
