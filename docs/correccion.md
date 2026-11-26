# Informe de Corrección — Simulación de Partículas en Paralelo

Fundamentos de Programación Funcional y Concurrente.  
Taller 4: Simulación de partículas en paralelo.

---

## 1. Modelo Matemático del Sistema

Sea $\mathbb{V} = \mathbb{R}^2$ el espacio vectorial bidimensional de posiciones y velocidades. Una partícula se define como una tupla:

```math
p = (\vec{r}, \vec{v}, m) \in \mathbb{V} \times \mathbb{V} \times \mathbb{R}_{\geq 0}
```
donde $\vec{r} = (r_x, r_y)$ es la posición, $\vec{v} = (v_x, v_y)$ es la velocidad y $m$ es la masa. Un sistema de $n$ partículas se modela como una secuencia indexada (arreglo) de longitud $n$:
```math
S = [p_0, p_1, \ldots, p_{n-1}], \quad n = |S| \geq 0
```
Para todo tramo $S[a \dots b)$ con $0 \leq a \leq b \leq n$, denotamos el subarreglo de elementos desde el índice $a$ (incluido) hasta $b$ (excluido) como:

```math
S[a \dots b) = [p_a, p_{a+1}, \ldots, p_{b-1}]
```
