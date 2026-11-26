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

## 3. Corrección del Punto 1:Paso de la Simulación
### 3.1. Especificación matemática
Sea $\Delta t \in \mathbb{R}$ el intervalo de tiempo. La función de transición individual $f_{\text{mover}} : \text{Particula} \times \mathbb{R} \to \text{Particula}$ se especifica como:
```math
f_{\text{mover}}((\vec{r}, \vec{v}, m), \Delta t) = (\vec{r} + \vec{v} \cdot \Delta t, \vec{v}, m)
```
Para un sistema $S = [p_0, \ldots, p_{n-1}]$, el paso completo $F_{\text{paso}}(S, \Delta t)$ aplica $f_{\text{mover}}$ a cada partícula:
```math
F_{\text{paso}}(S, \Delta t) = [f_{\text{mover}}(p_0, \Delta t), \ldots, f_{\text{mover}}(p_{n-1}, \Delta t)]
```
### 3.2. Versiones secuenciales ($mover$, $pasoSecuencial$, $pasoSegmento$)
- $mover(p, dt)$ construye directamente $Particula(p.pos + (p.vel * dt), p.vel, p.masa)$, lo cual coincide por definición con $f_{\text{mover}}(p, \Delta t)$.
- $pasoSecuencial(s, dt)$ ejecuta $s.map(mover(_, dt))$. Por definición de la operación funcional $map$ sobre un arreglo de longitud $n$, transforma cada elemento $s(i)$ en $\text{mover}(s(i), dt)$ para todo $0 \leq i < n$, satisfaciendo $\text{pasoSecuencial}(S, \Delta t) = F_{\text{paso}}(S, \Delta t)$.
- $pasoSegmento(s, dt, desde, hasta)$ ejecuta $s.slice(desde, hasta).map(mover(_, dt))$. El método $slice(desde, hasta)$ extrae exactamente el subarreglo $S[\text{desde} \dots \text{hasta})$, y $map$ aplica $f_{\text{mover}}$ a cada elemento de dicho tramo, cumpliendo:
  ```math
    \text{pasoSegmento}(S, \Delta t, a, b) = F_{\text{paso}}(S[a \dots b), \Delta t)
    ```

### 3.3. Versión paralela (`pasoParalelo`)
Demostramos por inducción fuerte sobre la longitud $n = |S|$ que para todo arreglo $S$, todo $\Delta t \in \mathbb{R}$ y todo $\text{umbral} \in \mathbb{Z}$:
```math
\text{pasoParalelo}(S, \Delta t, \text{umbral}) = \text{pasoSecuencial}(S, \Delta t)
```
***Caso base:*** $\text{umbral} \leq 0 \lor n \leq \text{umbral}$.Por definición de la guarda `if (umbral <= 0 || s.length <= umbral)`, el programa evalúa directamente la rama secuencial:
```math
\text{pasoParalelo}(S, \Delta t, \text{umbral}) \rightarrow \text{pasoSecuencial}(S, \Delta t)
```
El caso base se cumple trivialmente.
**Paso inductivo:** 
Supongamos que $\text{umbral} > 0$ y $n > \text{umbral}$ (lo cual implica $n \geq 2$).
Hipótesis de inducción (HI): Para todo sistema $S'$ con $|S'| < n$, se cumple $\text{pasoParalelo}(S', \Delta t, \text{umbral}) = \text{pasoSecuencial}(S', \Delta t)$.
Al ejecutar `pasoParalelo(S, dt, umbral)`, se calcula $\text{mitad} = \lfloor n / 2 \rfloor$. Como $n \geq 2$, tenemos $1 \leq \text{mitad} < n$, de modo que $|S_{\text{izq}}| = \text{mitad} < n$ y $|S_{\text{der}}| = n - \text{mitad} < n$. Evaluando la expresión:
```math
\begin{aligned}
\text{pasoParalelo}(S, \Delta t, \text{umbral}) &\rightarrow \text{pasoParalelo}(S_{\text{izq}}, \Delta t, \text{umbral}) \mathbin{++} \text{pasoParalelo}(S_{\text{der}}, \Delta t, \text{umbral}) \\
&\stackrel{\text{HI}}{=} \text{pasoSecuencial}(S_{\text{izq}}, \Delta t) \mathbin{++} \text{pasoSecuencial}(S_{\text{der}}, \Delta t) \\
&= F_{\text{paso}}(S_{\text{izq}}, \Delta t) \mathbin{++} F_{\text{paso}}(S_{\text{der}}, \Delta t)
\end{aligned}
```
Por la propiedad de partición demostrada en la Sección 2.1 ($S_{\text{izq}} \mathbin{++} S_{\text{der}} = S$) y la distributividad de `map` respecto a la concatenación:
```math
F_{\text{paso}}(S_{\text{izq}}, \Delta t) \mathbin{++} F_{\text{paso}}(S_{\text{der}}, \Delta t) = F_{\text{paso}}(S_{\text{izq}} \mathbin{++} S_{\text{der}}, \Delta t) = F_{\text{paso}}(S, \Delta t) = \text{pasoSecuencial}(S, \Delta t)
```
Concluimos por inducción que `pasoParalelo` es correcto para cualquier entrada.