package taller

import common._
import org.scalameter._

/**
 * Taller 4 — simulación de partículas repartida entre varios núcleos.
 *
 * Complete cada función. Las firmas y los tipos no se modifican.
 *
 * Las abstracciones `parallel` y `task` vienen en el paquete `common`, que ya
 * está en el proyecto.
 */
class SimulacionParticulas {

  type Sistema = Array[Particula]

  // Punto 1: un paso de la simulación -----------------------------------------

  /** La posición nueva es la anterior más la velocidad por dt. */
  def mover(p: Particula, dt: Double): Particula = {
    new Particula(p.pos+(p.vel*dt),p.vel,p.masa)
  }

  /** Avanza todo el sistema un paso de tiempo dt. */
  def pasoSecuencial(s: Sistema, dt: Double): Sistema = {
    s.map(mover(_,dt))
  }

  /** Avanza solo el tramo entre `desde` incluido y `hasta` excluido. */
  def pasoSegmento(s: Sistema, dt: Double, desde: Int, hasta: Int): Sistema = {
    s.slice(desde,hasta).map(mover(_,dt))
  }

  /**
   * Parte el sistema por la mitad con `parallel` mientras el tramo tenga más
   * elementos que el umbral, y por debajo de esa cifra resuelve en secuencial.
   */
  def pasoParalelo(s: Sistema, dt: Double, umbral: Int): Sistema = {
    if (umbral <= 0 || s.length <= umbral)
      pasoSecuencial(s, dt)
    else {
      val mitad = s.length / 2
      val (izquierda, derecha) = parallel(
        pasoParalelo(s.slice(0, mitad), dt, umbral),
        pasoParalelo(s.slice(mitad, s.length), dt, umbral))
      izquierda ++ derecha
    }
  }

  // Punto 2: magnitudes globales ----------------------------------------------

  /** La suma de las energías cinéticas, media masa por velocidad al cuadrado. */
  def energiaSecuencial(s: Sistema): Double = {
    s.map(p => 0.5 * p.masa * math.pow(p.vel.norma, 2)).sum
  }

  /** La suma de las masas. */
  def masaTotal(s: Sistema): Double = {
    s.map(p => p.masa).sum
  }

  /**
   * La suma de las posiciones ponderadas por la masa, dividida entre la masa
   * total. Sin masa, el origen.
   */
  def centroDeMasaSecuencial(s: Sistema): Vector2 ={
    if(masaTotal(s)==0) Vector2(0.0,0.0)
    else s.map(p => p.pos*p.masa).reduce(_+_)*(1/masaTotal(s))
  }

  // Punto 3: las mismas magnitudes en paralelo --------------------------------

  /** La energía del sistema, repartida con el mismo umbral del punto 1. */
  def energiaParalela(s: Sistema, umbral: Int): Double = {
    if(s.length<=umbral || umbral<=0) energiaSecuencial(s)
    else {
      val mitad = s.length / 2
      val (izq,dech) = parallel(
        energiaParalela(s.slice(0,mitad), umbral),
        energiaParalela(s.slice(mitad,s.length), umbral)
      )
      izq + dech
    }
  }

  /**
   * El centro de masa, repartido. Promediar los promedios de cada mitad da
   * otra cosa cuando las dos mitades no pesan igual.
   */
  def centroDeMasaParalelo(s: Sistema, umbral: Int): Vector2 = {
    def auxiliar(sub: Sistema): (Vector2, Double) = {
      if (sub.length <= umbral || umbral <= 0) {
        val m = masaTotal(sub)
        val sumaPonderada = if (m == 0) Vector2(0.0, 0.0) else centroDeMasaSecuencial(sub) * m
        (sumaPonderada, m)
      } else {
        val mitad = sub.length / 2
        val ((vecIzq, masaIzq), (vecDech, masaDech)) = parallel(
          auxiliar(sub.slice(0, mitad)),
          auxiliar(sub.slice(mitad, sub.length))
        )
        (vecIzq + vecDech, masaIzq + masaDech)
      }
    }

    val (sumaTotal, masaTot) = auxiliar(s)
    if (masaTot == 0) Vector2(0.0, 0.0) else sumaTotal * (1 / masaTot)
  }
  // Punto 4: trabajo y profundidad --------------------------------------------

  /** El número total de operaciones, una por partícula. */
  def trabajo(n: Int): Int = {
    n
  }

  /** Cuántas veces se parte a la mitad, más el tramo que queda sin partir. */
  def profundidad(n: Int, umbral: Int): Int = {
      if (n <= umbral) n
      else 1 + profundidad(n / 2, umbral)
  }

  /** La profundidad más el trabajo entre p procesadores. */
  def tiempoEstimado(n: Int, p: Int, umbral: Int): Double = {
    n.toDouble / p + profundidad(n,umbral)
  }

  // Punto 5: medir la aceleración ---------------------------------------------

  /** Mediana de varias mediciones de ScalaMeter, en milisegundos. */
  def medir[A](repeticiones: Int)(cuerpo: => A): Double = {
    val muestras = Vector.fill(repeticiones) {
      (withWarmer(new Warmer.Default) measure { cuerpo }).value
    }
    muestras.sorted.apply(muestras.length / 2)
  }

  /** El cociente entre el tiempo secuencial y el paralelo. */
  def aceleracion(tSecuencial: Double, tParalelo: Double): Double = {
    tSecuencial / tParalelo
  }

  /**
   * El umbral que menos tarda de una lista de candidatos. Con la lista vacía,
   * el tamaño del sistema, que equivale a no repartir.
   */
  def umbralConveniente(s: Sistema, dt: Double, umbrales: List[Int]): Int = {

      if (umbrales.isEmpty) s.length
      else umbrales.minBy(u => medir(3)(pasoParalelo(s, dt, u)))
  }
}
