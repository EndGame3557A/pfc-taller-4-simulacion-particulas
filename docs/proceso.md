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


