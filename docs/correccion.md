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

## 2. Propiedades Estructurales del Reparto
Antes de demostrar la corrección de cada función recursiva paralela, establecemos los dos invariantes fundamentales que garantizan la validez de la estrategia divide y vencerás sobre el arreglo.
### 2.1. Partición sin pérdida ni repetición de partículas
En las funciones $pasoParalelo$, $energiaParalela$ y $centroDeMasaParalelo$, cuando el tamaño del arreglo actual $S$ cumple $|S| = k > \text{umbral}$ (con $\text{umbral} \geq 1$, por lo tanto $k \geq 2$), se calcula el punto de corte entero:
```math
\text{mitad} = \left\lfloor \frac{k}{2} \right\rfloor
```
Dado que $k \geq 2$, se cumple estrictamente que $1 \leq \text{mitad} < k$. El arreglo $S[0 \dots k)$ se divide en dos subarreglos disjuntos:
```math
S_{\text{izq}} = S[0 \dots \text{mitad}) = [p_0, \ldots, p_{\text{mitad}-1}]
S_{\text{der}} = S[\text{mitad} \dots k) = [p_{\text{mitad}}, \ldots, p_{k-1}]
```
#### 1. Disyunción (sin repetición):
El conjunto de índices de $S_{\text{izq}}$ es $I_{\text{izq}} = \{0, 1, \ldots, \text{mitad}-1\}$ y el de $S_{\text{der}}$ es $I_{\text{der}} = \{\text{mitad}, \ldots, k-1\}$. Claramente $I_{\text{izq}} \cap I_{\text{der}} = \emptyset$, por lo que ninguna partícula se procesa dos veces.
#### 2. Exhaustividad (sin pérdida): 
La unión de ambos conjuntos de índices es:
```math
I_{\text{izq}} \cup I_{\text{der}} = \{0, 1, \ldots, \text{mitad}-1, \text{mitad}, \ldots, k-1\} = \{0, 1, \ldots, k-1\}
```
Además, $|S_{\text{izq}}| + |S_{\text{der}}| = \text{mitad} + (k - \text{mitad}) = k = |S|$. La concatenación preserva exactamente el orden y la totalidad de los elementos:
```math
S_{\text{izq}} \mathbin{++} S_{\text{der}} = S
```
### 2.2. Asociatividad de las operaciones de combinación
Para que un árbol de computación paralelo produzca el mismo resultado independientemente de cómo se agrupen las tareas, el operador binario $\oplus$ que combina los resultados parciales debe ser asociativo: $(x \oplus y) \oplus z = x \oplus (y \oplus z)$.
- **Concatenación de arreglos ($\mathbin{++}$):** Para tres secuencias cualesquiera $A, B, C$, se cumple $(A \mathbin{++} B) \mathbin{++} C = A \mathbin{++} (B \mathbin{++} C)$, con elemento neutro el arreglo vacío $[]$.
- **Suma en los reales ($+$):** Para la energía cinética total y la masa total, la adición escalar cumple $(a + b) + c = a + (b + c)$ con neutro $0.0$.
- **Suma de pares ponderados en $\mathbb{V} \times \mathbb{R}$:** Definimos la operación de combinación para el centro de masa como:
    ```math
    (\vec{R}_1, M_1) \oplus (\vec{R}_2, M_2) = (\vec{R}_1 + \vec{R}_2, M_1 + M_2)
    ```
  Dado que la suma vectorial en $\mathbb{R}^2$ y la suma escalar en $\mathbb{R}$ son asociativas componente a componente, $\oplus$ es asociativa con elemento neutro $(\vec{0}, 0.0)$.