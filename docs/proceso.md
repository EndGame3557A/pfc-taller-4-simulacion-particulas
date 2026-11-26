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

text

```text
+------+--------------------------------------------+------------------------------------+
| Paso | Operación                                  | Resultado                          |
+------+--------------------------------------------+------------------------------------+
| 1    | vel * dt = (1, 2) * 2.0                    | (2.0, 4.0)                         |
| 2    | pos + desplazamiento = (0, 0) + (2.0, 4.0) | (2.0, 4.0)                         |
| 3    | Partícula nueva                            | Particula((2.0, 4.0), (1, 2), 1.0) |
+------+--------------------------------------------+------------------------------------+
```
### 3.2. Función `pasoSecuencial`

La función `pasoSecuencial` avanza todo el sistema un paso de tiempo `dt`.

Código completo:

scala

```scala
def pasoSecuencial(s: Sistema, dt: Double): Sistema = {
  s.map(mover(_, dt))
}
```

La expresión `mover(_, dt)` equivale a `p => mover(p, dt)`.

**Proceso:**

1.  Se recorre el arreglo de izquierda a derecha.
2.  A cada partícula se le aplica `mover` con el mismo `dt`.
3.  Se construye un arreglo nuevo con los resultados, en el mismo orden y con la misma longitud.

**Traza con `pasoSecuencial(s, 2.0)`:**

text

```text
+-----------+-------------------+----------+----------------+
| Partícula | Posición anterior | vel * dt | Posición nueva |
+-----------+-------------------+----------+----------------+
| P1        | (0, 0)            | (2, 4)   | (2, 4)         |
| P2        | (1, 0)            | (0, 2)   | (1, 2)         |
| P3        | (2, 2)            | (2, 0)   | (4, 2)         |
| P4        | (3, 1)            | (-2, 2)  | (1, 3)         |
| P5        | (4, 4)            | (0, 0)   | (4, 4)         |
+-----------+-------------------+----------+----------------+
```

Resultado: un arreglo con `P1'`, `P2'`, `P3'`, `P4'`, `P5'` en ese orden.

**Casos particulares:**

-   Con `dt = 0.0` todas las posiciones quedan igual.
-   Con un sistema vacío el resultado es un arreglo vacío.
-   El arreglo original no cambia.

### 3.3. Función `pasoSegmento`

La función `pasoSegmento` avanza solo el tramo entre `desde` (incluido) y `hasta` (excluido).

Código completo:

scala

```scala
def pasoSegmento(s: Sistema, dt: Double, desde: Int, hasta: Int): Sistema = {
  s.slice(desde, hasta).map(mover(_, dt))
}
```

**Proceso:**

1.  `slice(desde, hasta)` copia las partículas de las posiciones $desde, desde+1, \dots, hasta-1$.
2.  A cada partícula del tramo se le aplica `mover`.
3.  Se devuelve un arreglo con solo el tramo, de longitud $hasta - desde$. No se devuelve el sistema completo.

**Traza con `pasoSegmento(s, 2.0, 1, 4)`:**

text

```text
+------+------------------+---------------+
| Paso | Operación        | Resultado     |
+------+------------------+---------------+
| 1    | slice(1, 4)      | P2, P3, P4    |
| 2    | mover a cada una | P2', P3', P4' |
+------+------------------+---------------+
```

Resultado: posiciones `(1, 2)`, `(4, 2)`, `(1, 3)`.

Si el tramo es vacío (por ejemplo `desde = hasta`), `slice` devuelve un arreglo vacío y el resultado también lo es.

### 3.4. Función `pasoParalelo`

La función `pasoParalelo` avanza el sistema repartiendo el trabajo. Parte el arreglo por la mitad con `parallel` mientras el tramo tenga más elementos que el umbral. Por debajo de esa cifra resuelve en secuencial.

Código completo:

scala

```scala
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
```

**Proceso:**

1.  Se evalúa el criterio de parada: si `umbral <= 0` o el tramo tiene como máximo `umbral` elementos, se resuelve con `pasoSecuencial`.
2.  En caso contrario se calcula `mitad = s.length / 2`.
3.  Se crean los dos tramos: `[0, mitad)` y `[mitad, s.length)`.
4.  `parallel` ejecuta a la vez el llamado recursivo sobre cada tramo.
5.  Cuando los dos terminan, se concatenan con `++`: primero la mitad izquierda y luego la derecha, de modo que el orden original se conserva.

Cuando $umbral \ge 1$ y el tramo tiene más de `umbral` elementos, tiene al menos dos. Por eso `mitad` es al menos 1 y las dos mitades son no vacías y más cortas que el tramo original. Cada llamado trabaja sobre un arreglo estrictamente más corto, así que la recursión termina.

**Traza con `pasoParalelo(s, 2.0, 2)`:**

text

```text
+---------+--------------------+----------+----------------+----------------------------------------------------+
| Llamado | Tramo              | Longitud | ¿Longitud > 2? | Acción                                             |
+---------+--------------------+----------+----------------+----------------------------------------------------+
| A       | P1, P2, P3, P4, P5 | 5        | Sí             | mitad = 2: se parte en B (P1, P2) y C (P3, P4, P5) |
| B       | P1, P2             | 2        | No             | pasoSecuencial -> P1', P2'                         |
| C       | P3, P4, P5         | 3        | Sí             | mitad = 1: se parte en D (P3) y E (P4, P5)         |
| D       | P3                 | 1        | No             | pasoSecuencial -> P3'                              |
| E       | P4, P5             | 2        | No             | pasoSecuencial -> P4', P5'                         |
+---------+--------------------+----------+----------------+----------------------------------------------------+
```

B y C se ejecutan en paralelo entre sí. Dentro de C, D y E también se ejecutan en paralelo.

**Combinación de los resultados:**

1.  C combina sus dos resultados: `D ++ E` = P3', P4', P5'.
2.  A combina los suyos: `B ++ C` = P1', P2', P3', P4', P5'.

El resultado coincide con el de `pasoSecuencial(s, 2.0)`: mismas partículas, en el mismo orden.

**Casos particulares:**

-   `umbral <= 0`: se resuelve todo en secuencial, sin partir.
-   `umbral >= s.length`: no se reparte nada. Es el caso de control del punto 5, porque se llama a `pasoSecuencial(s, dt)` sin copiar el arreglo.
-   `umbral = 1`: se parte hasta llegar a tramos de una sola partícula.
-   `pasoParalelo(Array(p), 2.0, 1)` con una sola partícula no parte, porque $1 \le 1$.

Cada llamado que parte el tramo crea copias con `slice` y un arreglo nuevo con `++`. Ese costo extra explica por qué un umbral demasiado pequeño no conviene (se retoma en el informe de paralelización).

----------



