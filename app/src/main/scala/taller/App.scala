package taller

object App {

  def main(args: Array[String]): Unit = {
    val sim = new SimulacionParticulas()
    import sim._

    val dt = 0.5
    val repeticiones = 3

    println(s"Núcleos disponibles: ${Runtime.getRuntime.availableProcessors}")

    List(1000000, 2000000, 4000000).foreach { n =>
      val sistema: Sistema = Array.tabulate(n) { i =>
        Particula(
          Vector2(i.toDouble, (i % 100).toDouble),
          Vector2((i % 7).toDouble, (i % 13).toDouble),
          1.0 + (i % 5))
      }

      val tSec = medir(repeticiones) { pasoSecuencial(sistema, dt) }
      println(f"n=$n%8d  Secuencial: $tSec%.2f ms")

      List(1000, 10000, 100000, n / 2, n).foreach { u =>
        val tPar = medir(repeticiones) { pasoParalelo(sistema, dt, u) }
        println(f"n=$n%8d  umbral=$u%8d  par=$tPar%.2f ms  aceleración=${aceleracion(tSec, tPar)}%.2f")
      }
    }
  }
}