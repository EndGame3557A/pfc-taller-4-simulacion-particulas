# Informe de proceso — Simulación de partículas en paralelo

## Contenido

1.  Introducción
2.  Contexto y modelo de datos
3.  Punto 1: un paso de la simulación
4.  Punto 2: magnitudes globales en secuencial
5.  Punto 3: las mismas magnitudes en paralelo
6.  Punto 4: trabajo y profundidad
7.  Punto 5: medir la aceleración
8.  Diagramas del proceso
9.  El resultado no depende del umbral
10. Uso de programación funcional
11. Conclusiones

----------

## 1. Introducción

El objetivo de este taller es implementar una simulación de partículas y calcular magnitudes globales del sistema (energía y centro de masa), repartiendo el trabajo entre varios núcleos con las abstracciones `parallel` y `task` del paquete `common`.

El desarrollo se realizó sin variables mutables, sin ciclos iterativos y sin `return`. Todas las operaciones se expresan como transformaciones de colecciones y recursión.

El informe explica, para cada función, cómo se ejecuta paso a paso, qué tramo del sistema resuelve cada llamado en las versiones repartidas y cómo se combinan los resultados parciales. Cada función se acompaña de una traza con un ejemplo pequeño.

----------

## 2. Contexto y modelo de datos

El taller proporciona los siguientes tipos, que se conservan sin modificaciones:

```scala
case class Vector2(x: Double, y: Double) {
  def +(o: Vector2): Vector2 = Vector2(x + o.x, y + o.y)
  def *(k: Double): Vector2  = Vector2(x * k, y * k)
  def norma: Double          = Math.sqrt(x * x + y * y)
}

case class Particula(pos: Vector2, vel: Vector2, masa: Double)

type Sistema = Array[Particula]

```

Un sistema es un arreglo de partículas. Cada partícula tiene una posición, una velocidad y una masa.

Para realizar las trazas se utiliza el siguiente sistema de cinco partículas, con un paso de tiempo `dt = 2.0`:

```scala
val s = Array(
  Particula(Vector2(0, 0), Vector2( 1, 2), 1.0),  // P1
  Particula(Vector2(1, 0), Vector2( 0, 1), 2.0),  // P2
  Particula(Vector2(2, 2), Vector2( 1, 0), 1.0),  // P3
  Particula(Vector2(3, 1), Vector2(-1, 1), 3.0),  // P4
  Particula(Vector2(4, 4), Vector2( 0, 0), 2.0)   // P5
)

```

```
| Elemento | Posición | Velocidad | Masa |
|----------|----------|-----------|------|
| P1 | (0, 0) | (1, 2) | 1.0 |
| P2 | (1, 0) | (0, 1) | 2.0 |
| P3 | (2, 2) | (1, 0) | 1.0 |
| P4 | (3, 1) | (-1, 1) | 3.0 |
| P5 | (4, 4) | (0, 0) | 2.0 |
```

El orden de estos elementos corresponde al orden original del arreglo. Con `dt = 2.0`, la partícula nueva de `Pk` se escribe `Pk'`.

Las funciones están definidas dentro de la clase `SimulacionParticulas`. Además de las funciones pedidas, se usa la función `medir`, que viene en el repositorio y se describe en la sección 7.


## 3. Punto 1: un paso de la simulación

### 3.1. Función auxiliar `mover`

La función `mover` calcula cómo queda una sola partícula después de un paso de tiempo. Las demás funciones de este punto se apoyan en ella.

Código completo:

```scala
def mover(p: Particula, dt: Double): Particula = {
  new Particula(p.pos + (p.vel * dt), p.vel, p.masa)
}

```

En notación matemática, si la partícula $p$ tiene posición $r$ y velocidad $v$:

$$ r' = r + v \cdot dt $$

**Proceso:**

1.  Se multiplica la velocidad por `dt` con el operador `*` de `Vector2`.
2.  Se suma ese desplazamiento a la posición con el operador `+` de `Vector2`.
3.  Se construye una partícula nueva con la posición calculada y la misma velocidad y masa.

La partícula original no se modifica: se devuelve una partícula nueva.

**Traza con `mover(P1, 2.0)`:**

Paso

Operación

Resultado

1

`vel * dt` = (1, 2) · 2.0

(2.0, 4.0)

2

`pos + desplazamiento` = (0, 0) + (2.0, 4.0)

(2.0, 4.0)

3

Partícula nueva

`Particula((2.0, 4.0), (1, 2), 1.0)`



