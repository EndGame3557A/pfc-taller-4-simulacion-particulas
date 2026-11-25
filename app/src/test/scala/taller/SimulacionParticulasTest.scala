package taller

import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import scala.util.Random

/**
 * Las pruebas no miden tiempos: un tiempo depende de la máquina y de lo que
 * esté corriendo al lado, así que una prueba que lo compare fallaría sin que
 * nada esté mal.
 */
@RunWith(classOf[JUnitRunner])
class SimulacionParticulasTest extends AnyFunSuite {

  val sim = new SimulacionParticulas()
  import sim._

  val eps = 1e-9

  val p = Particula(Vector2(0, 0), Vector2(1, 2), 1.0)

  val s: Sistema = Array(
    Particula(Vector2(0, 0), Vector2(3, 4), 2.0), // norma 5
    Particula(Vector2(4, 0), Vector2(0, 0), 2.0))

  val vacio: Sistema = Array()

  val azar = new Random(7)
  val grande: Sistema = Array.fill(1000)(Particula(
    Vector2(azar.nextDouble() * 10, azar.nextDouble() * 10),
    Vector2(azar.nextDouble() * 2, azar.nextDouble() * 2),
    azar.nextDouble() * 5 + 0.1))

  def cerca(a: Double, b: Double): Boolean = Math.abs(a - b) < 1e-6

  def cerca(a: Vector2, b: Vector2): Boolean =
    Math.abs(a.x - b.x) < 1e-6 && Math.abs(a.y - b.y) < 1e-6

  // Punto 1 -------------------------------------------------------------------

  test("pasoSecuencial: la posición avanza velocidad por dt") {
    assert(pasoSecuencial(Array(p), 2.0).head.pos == Vector2(2.0, 4.0))
  }

  test("pasoSecuencial: con dt cero nada se mueve") {
    assert(pasoSecuencial(Array(p), 0.0).head.pos == Vector2(0.0, 0.0))
  }

  test("pasoSecuencial: la velocidad y la masa no cambian") {
    val despues = pasoSecuencial(Array(p), 3.0).head
    assert(despues.vel == p.vel && despues.masa == p.masa)
  }

  test("pasoSecuencial: sobre el sistema vacío no hay nada que mover") {
    assert(pasoSecuencial(vacio, 1.0).isEmpty)
  }

  test("pasoSegmento: solo calcula el tramo pedido") {
    val tramo = pasoSegmento(s, 1.0, 1, 2)
    assert(tramo.length == 1 && tramo.head.pos == Vector2(4.0, 0.0))
  }

  test("pasoSegmento: el tramo vacío da un sistema vacío") {
    assert(pasoSegmento(s, 1.0, 1, 1).isEmpty)
  }

  test("pasoParalelo: da lo mismo que el secuencial en un caso simple") {
    assert(pasoParalelo(Array(p), 2.0, 1).head.pos == Vector2(2.0, 4.0))
  }

  test("pasoParalelo: coincide con el secuencial para varios umbrales") {
    val umbrales = List(1, 2, 7, 64, 1000, 5000)
    assert(umbrales.forall { u =>
      pasoParalelo(grande, 0.5, u).zip(pasoSecuencial(grande, 0.5))
        .forall { case (a, b) => cerca(a.pos, b.pos) }
    })
  }

  test("pasoParalelo: conserva el orden y el tamaño del sistema") {
    assert(pasoParalelo(grande, 0.5, 32).length == grande.length)
  }

  test("pasoParalelo: con umbral cero no se queda sin fondo") {
    assert(pasoParalelo(Array(p), 1.0, 0).length == 1)
  }

  // Punto 2 -------------------------------------------------------------------

  test("energiaSecuencial: media masa por velocidad al cuadrado") {
    assert(cerca(energiaSecuencial(s), 25.0))
  }

  test("energiaSecuencial: el sistema vacío no tiene energía") {
    assert(energiaSecuencial(vacio) == 0.0)
  }

  test("energiaSecuencial: una partícula quieta no aporta energía") {
    assert(cerca(energiaSecuencial(Array(s(1))), 0.0))
  }

  test("masaTotal: suma las masas") {
    assert(cerca(masaTotal(s), 4.0))
  }

  test("masaTotal: el sistema vacío no tiene masa") {
    assert(masaTotal(vacio) == 0.0)
  }

  test("centroDeMasaSecuencial: el punto medio de dos masas iguales") {
    assert(cerca(centroDeMasaSecuencial(s), Vector2(2.0, 0.0)))
  }

  test("centroDeMasaSecuencial: sin masa el centro es el origen") {
    assert(centroDeMasaSecuencial(vacio) == Vector2(0, 0))
  }

  test("centroDeMasaSecuencial: la masa mayor arrastra el centro") {
    val pesado: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(0, 0), 3.0),
      Particula(Vector2(4, 0), Vector2(0, 0), 1.0))
    assert(cerca(centroDeMasaSecuencial(pesado), Vector2(1.0, 0.0)))
  }

  // Punto 3 -------------------------------------------------------------------

  test("energiaParalela: coincide con la secuencial en el ejemplo") {
    assert(cerca(energiaParalela(s, 1), 25.0))
  }

  test("energiaParalela: coincide con la secuencial para varios umbrales") {
    val esperado = energiaSecuencial(grande)
    assert(List(1, 3, 64, 999, 5000).forall(u => cerca(energiaParalela(grande, u), esperado)))
  }

  test("energiaParalela: sobre el sistema vacío da cero") {
    assert(energiaParalela(vacio, 1) == 0.0)
  }

  test("centroDeMasaParalelo: coincide con el secuencial en el ejemplo") {
    assert(cerca(centroDeMasaParalelo(s, 1), Vector2(2.0, 0.0)))
  }

  test("centroDeMasaParalelo: coincide con el secuencial para varios umbrales") {
    val esperado = centroDeMasaSecuencial(grande)
    assert(List(1, 3, 64, 999, 5000).forall(u => cerca(centroDeMasaParalelo(grande, u), esperado)))
  }

  test("centroDeMasaParalelo: promediar promedios daría otra cosa") {
    // Tres partículas: dos en el origen y una lejos. El promedio de los
    // promedios de las dos mitades no es el centro de masa del conjunto.
    val tres: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(0, 0), 1.0),
      Particula(Vector2(0, 0), Vector2(0, 0), 1.0),
      Particula(Vector2(9, 0), Vector2(0, 0), 1.0))
    assert(cerca(centroDeMasaParalelo(tres, 1), Vector2(3.0, 0.0)))
  }

  test("centroDeMasaParalelo: sin masa el centro es el origen") {
    assert(centroDeMasaParalelo(vacio, 1) == Vector2(0, 0))
  }

  // Punto 4 -------------------------------------------------------------------

  test("trabajo: una operación por partícula") { assert(trabajo(1024) == 1024) }

  test("profundidad: si el umbral cubre todo, no se parte") {
    assert(profundidad(1024, 1024) == 1024)
  }

  test("profundidad: una partición y el tramo que queda") {
    assert(profundidad(1024, 512) == 513)
  }

  test("profundidad: diez particiones y un elemento") {
    assert(profundidad(1024, 1) == 11)
  }

  test("tiempoEstimado: profundidad más trabajo entre procesadores") {
    assert(cerca(tiempoEstimado(1024, 4, 1), 267.0))
  }

  test("tiempoEstimado: con un procesador no hay ganancia") {
    assert(cerca(tiempoEstimado(1024, 1, 1), 11.0 + 1024.0))
  }

  test("profundidad: nunca supera el trabajo") {
    val casos = for (n <- List(1, 7, 64, 1024); u <- List(1, 8, 64)) yield (n, u)
    assert(casos.forall { case (n, u) => profundidad(n, u) <= trabajo(n) })
  }

  // Punto 5 -------------------------------------------------------------------

  test("aceleracion: cuatro veces más rápido da 4.0") {
    assert(cerca(aceleracion(100.0, 25.0), 4.0))
  }

  test("aceleracion: el mismo tiempo da 1.0") {
    assert(cerca(aceleracion(100.0, 100.0), 1.0))
  }

  test("umbralConveniente: sin candidatos devuelve el tamaño del sistema") {
    assert(umbralConveniente(grande, 0.5, Nil) == grande.length)
  }

  test("umbralConveniente: devuelve uno de los candidatos") {
    val candidatos = List(16, 128, 1024)
    assert(candidatos.contains(umbralConveniente(grande, 0.5, candidatos)))
  }


  //====================TEST ADICIONALES===============================

  // Punto 1 (adicionales) -----------------------------------------------------

  test("pasoSecuencial: velocidad negativa retrocede la posición") {
    val q = Particula(Vector2(5, 5), Vector2(-1, -2), 1.0)
    assert(cerca(pasoSecuencial(Array(q), 2.0).head.pos, Vector2(3.0, 1.0)))
  }

  test("pasoSecuencial: dt negativo mueve hacia atrás") {
    assert(cerca(pasoSecuencial(Array(p), -1.0).head.pos, Vector2(-1.0, -2.0)))
  }

  test("pasoSecuencial: no modifica el sistema original") {
    val antes = s(0).pos
    pasoSecuencial(s, 5.0)
    assert(s(0).pos == antes)
  }

  test("pasoSegmento: un tramo central coincide con el mismo tramo del secuencial") {
    val tramo = pasoSegmento(grande, 0.5, 10, 20)
    val esperado = pasoSecuencial(grande, 0.5).slice(10, 20)
    assert(tramo.length == 10 &&
      tramo.zip(esperado).forall { case (a, b) => cerca(a.pos, b.pos) })
  }

  test("pasoParalelo: tamaño impar y vacío coinciden con el secuencial") {
    val impar = grande.take(7)
    val ok = pasoParalelo(impar, 0.5, 2).zip(pasoSecuencial(impar, 0.5))
      .forall { case (a, b) => cerca(a.pos, b.pos) }
    assert(ok && pasoParalelo(vacio, 0.5, 1).isEmpty)
  }

  // Punto 2 (adicionales) -----------------------------------------------------

  test("energiaSecuencial: masas y velocidades distintas") {
    val mixto: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(1, 0), 1.0),  // 0.5
      Particula(Vector2(0, 0), Vector2(0, 3), 4.0))  // 18
    assert(cerca(energiaSecuencial(mixto), 18.5))
  }

  test("energiaSecuencial: el signo de la velocidad no cambia la energía") {
    val negativa: Sistema = Array(Particula(Vector2(0, 0), Vector2(-3, -4), 2.0))
    assert(cerca(energiaSecuencial(negativa), 25.0))
  }

  test("masaTotal: suma masas decimales") {
    val dec: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(0, 0), 0.5),
      Particula(Vector2(1, 1), Vector2(0, 0), 1.5),
      Particula(Vector2(2, 2), Vector2(0, 0), 2.0))
    assert(cerca(masaTotal(dec), 4.0))
  }

  test("centroDeMasaSecuencial: una sola partícula es su propia posición") {
    val sola: Sistema = Array(Particula(Vector2(7, -3), Vector2(1, 1), 5.0))
    assert(cerca(centroDeMasaSecuencial(sola), Vector2(7.0, -3.0)))
  }

  test("centroDeMasaSecuencial: tres partículas con masas distintas en 2D") {
    val tres: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(0, 0), 2.0),
      Particula(Vector2(3, 3), Vector2(0, 0), 1.0),
      Particula(Vector2(0, 6), Vector2(0, 0), 1.0))
    assert(cerca(centroDeMasaSecuencial(tres), Vector2(0.75, 2.25)))
  }

  // Punto 3 (adicionales) -----------------------------------------------------

  test("energiaParalela: con umbral cero coincide con la secuencial") {
    assert(cerca(energiaParalela(grande, 0), energiaSecuencial(grande)))
  }

  test("energiaParalela: tamaño impar coincide con la secuencial") {
    val impar = grande.take(7)
    assert(cerca(energiaParalela(impar, 2), energiaSecuencial(impar)))
  }

  test("centroDeMasaParalelo: mitades con masas muy distintas") {
    val desigual: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(0, 0), 1.0),
      Particula(Vector2(2, 0), Vector2(0, 0), 1.0),
      Particula(Vector2(10, 0), Vector2(0, 0), 100.0),
      Particula(Vector2(10, 10), Vector2(0, 0), 100.0))
    assert(cerca(centroDeMasaParalelo(desigual, 2), Vector2(2002.0 / 202.0, 1000.0 / 202.0)))
  }

  test("centroDeMasaParalelo: con umbral cero coincide con el secuencial") {
    assert(cerca(centroDeMasaParalelo(grande, 0), centroDeMasaSecuencial(grande)))
  }

  test("centroDeMasaParalelo: partículas de masa cero dan el origen") {
    val sinMasa: Sistema = Array(
      Particula(Vector2(5, 5), Vector2(0, 0), 0.0),
      Particula(Vector2(8, 1), Vector2(0, 0), 0.0))
    assert(centroDeMasaParalelo(sinMasa, 1) == Vector2(0, 0))
  }

  // Punto 4 (adicionales) -----------------------------------------------------

  test("trabajo: cero partículas y siete partículas") {
    assert(trabajo(0) == 0 && trabajo(7) == 7)
  }

  test("profundidad: n que no es potencia de 2 (10 -> 5 -> 2 -> 1)") {
    assert(profundidad(10, 1) == 4)
  }

  test("profundidad: un millón de partículas con umbral 1000") {
    assert(profundidad(1000000, 1000) == 986)
  }

  test("tiempoEstimado: la división entre procesadores es decimal") {
    assert(cerca(tiempoEstimado(10, 4, 1), 6.5)) // 10/4 = 2.5, más profundidad 4
  }

  test("tiempoEstimado: con umbral igual a n el tramo queda completo") {
    assert(cerca(tiempoEstimado(1000, 4, 1000), 250.0 + 1000.0))
  }

  // Punto 5 (adicionales) -----------------------------------------------------

  test("aceleracion: un paralelo más lento da menos de 1") {
    assert(cerca(aceleracion(50.0, 100.0), 0.5))
  }

  test("aceleracion: tiempo secuencial cero da cero") {
    assert(cerca(aceleracion(0.0, 10.0), 0.0))
  }

  test("aceleracion: no depende de la escala de los tiempos") {
    assert(cerca(aceleracion(10.0, 2.0), aceleracion(1000.0, 200.0)))
  }

  test("umbralConveniente: con un solo candidato devuelve ese") {
    assert(umbralConveniente(s, 1.0, List(64)) == 64)
  }

  test("umbralConveniente: sin candidatos devuelve el tamaño de un sistema pequeño") {
    assert(umbralConveniente(s, 1.0, Nil) == 2)
  }


}
