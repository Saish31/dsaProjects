# BMSSP — Practical Implementation of a Faster SSSP Algorithm

**Repository:** Practical Java implementation of the BMSSP (block-based) single-source shortest paths method described in **"Breaking the Sorting Barrier for Directed Single-Source Shortest Paths"**. This project contains the solver, a test harness for benchmarking against Dijkstra, and data processing scripts to reproduce the experiments.

---

## Contents

- **`SSSP_BMSSP_BlockD.java`** — Java implementation of the BMSSP algorithm with a practical `BlockD` structure (D0/D1 approximation). Contains:
  - `findPivots(B, S)` — pivot discovery (Algorithm 1).
  - `baseCase(B, x)` — limited Dijkstra base case (Algorithm 2).
  - `BMSSP(l, B, S)` — recursive driver (Algorithm 3).
  - `BlockD` — buffered priority structure approximating the paper’s D0/D1 data structure.
- **`TestHarness.java`** — benchmarking harness that:
  - generates reproducible random directed graphs,
  - runs the BMSSP solver and a baseline Dijkstra implementation,
  - prints CSV output: `n,m,trial,algo,time_ms`.
- **`results.csv`** — example benchmark output.
- **`Paper.pdf`** — the research paper implemented.

---

## Overview

**Purpose.** Implement and evaluate a research SSSP algorithm that reduces the cost of globally sorting tentative vertices by batching work using pivots and a special D structure. The implementation aims to be practical and reproducible on commodity machines.

**Key idea.** The BMSSP algorithm proceeds in bands of distance values and uses small local explorations plus a buffered priority structure (`BlockD`) to avoid paying a full priority queue cost per relaxation. This can give better practical performance than Dijkstra on large graphs.

**What this implementation is:** a faithful, practical translation of the algorithms in the paper, with engineering compromises (documented below) to keep the code maintainable and runnable.

---

## Requirements

- **Java JDK 11+** (for compiling and running Java sources)
- Recommended: allocate sufficient JVM heap when running large experiments; examples use `-Xmx4g` or larger.

---

## Build & Run

### Compile
From the project root folder (where the `.java` files are located):

```bash
javac SSSP_BMSSP_BlockD.java TestHarness.java
```

### Run the harness and save CSV results
Overwrite `results.csv`:

```bash
java -Xmx4g TestHarness > results.csv
```

Append new runs to an existing file (avoid duplicate headers):

```bash
# bash: append without header
if [ -f results.csv ]; then
  java -Xmx4g TestHarness | sed '1d' >> results.csv
else
  java -Xmx4g TestHarness > results.csv
fi
```

---


## How to Add / Tune Experiments

Open `TestHarness.java` and edit the `Experiment[] experiments` block. Each `Experiment` takes:

```java
new Experiment(n, m, trials, maxW)
```

- `n` — number of vertices
- `m` — number of directed edges
- `trials` — number of trial graphs to average per config
- `maxW` — maximum integer edge weight (weights sampled in `[1, maxW]`)

Recommended procedure:
1. Start with small sizes to ensure correctness.
2. Increase `n` and `m` gradually to profile scaling and crossover points.
3. Use `-Xmx` to increase heap for large graphs (e.g., `-Xmx8g`).

---

## Implementation notes & differences from the paper

**Paper mapping.** The code implements:
- `findPivots` → Algorithm 1
- `baseCase` → Algorithm 2
- `BMSSP` → Algorithm 3
- `BlockD` → practical approximation of the paper’s `D` (Lemma 3.3)

**Important practical differences (documented):**
- **`BlockD` is an engineering approximation.** It keeps an authoritative `current` map and a buffer `D1` for cheap inserts, and periodically rebuilds `D0` by sorting `current` into blocks. This is simpler to implement and robust in practice, but some formal amortized guarantees in the paper rely on a more intricate block maintenance strategy.
- **No mandatory constant-degree transform.** The paper sometimes assumes a constant-degree form (via vertex splitting). This implementation runs on general graphs; if you require the exact theoretical setting, add the transform as a preprocessing step.
- **Practical parameter choices.** `k`, `t`, and recursion depth `lMax` are chosen numerically from `n` (following the paper’s asymptotic guidelines). These are tunable and may be tuned for better empirical performance on your graphs.
- **Floating point arithmetic.** The code uses `double` and an `EPS` tolerance for comparisons. The paper’s model is abstract (exact comparisons), so be aware of numeric issues on graphs with extremely fine weights.

---

## Recommended Experiments & Interpretation

- **Small graphs:** BMSSP can show overhead and be slower due to data-structure costs.
- **Medium / large graphs:** BMSSP often outperforms Dijkstra as graph size and density increase — look for a crossover point where BMSSP runtime drops below Dijkstra’s.

---

## Validation & Tests

- **Correctness:** Use the harness to compare returned distances from `SSSP_BMSSP_BlockD` against the baseline `SimpleDijkstra` implementation included in `TestHarness.java`. The harness prints a warning if distances differ.
- **Edge cases to test:** zero-weight edges, multiple edges between same nodes, disconnected graphs, graphs with negative cycles (not supported — algorithm assumes nonnegative weights).

---

## Performance tips

- Increase JVM heap with `-Xmx` for large graphs (e.g., `java -Xmx8g TestHarness`).
- Tune `BlockD` parameters (block size hint and merge threshold) in `SSSP_BMSSP_BlockD` if merges occur too frequently.
- If profiling shows frequent `mergeBufferIntoD0` rebuilds in `BlockD`, consider increasing `mergeThreshold` or implementing more advanced incremental maintenance of block heads.

---

## Limitations

- The `BlockD` implementation trades some worst-case guarantees for simplicity: occasional global sorts may be expensive for certain workloads.
- The current code does not implement the vertex-splitting constant-degree reduction from the paper; this may impact asymptotic constants for very high-degree graphs.
- Floating point comparisons may affect tie handling on specially constructed graphs.

---

## Attribution & Citation

If you use this repository for research or in publications, please cite the original paper:

**"Breaking the Sorting Barrier for Directed Single-Source Shortest Paths"**, authors as listed in the `Paper.pdf`.

---

