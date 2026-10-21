# DAA Assignment 3 — Graphs

**Beibarys Balykbayev — SE-2516**  
Design and Analysis of Algorithms, Taubakabyl Nurlybek. Deadline: 23 October 2026, 23:59.

Repository: https://github.com/lackoftalentt/assignment3daa  
Submission branch: `main`; release tag: `v1.0`.

## Build, tests, and demonstration

Requires JDK 17+ and Maven 3.8+. Maven downloads JUnit 5 and its build plugins on the first build. Production code has no external dependencies.

```bash
mvn clean verify
java -jar target/daa-assignment3-graphs-1.0.jar demo data/connected.txt
java -jar target/daa-assignment3-graphs-1.0.jar demo data/disconnected.txt
java -jar target/daa-assignment3-graphs-1.0.jar demo data/cyclic.txt
java -jar target/daa-assignment3-graphs-1.0.jar demo data/dag.txt
```

`mvn test` runs 19 JUnit 5 tests. These include 100 random connected MST comparisons, exhaustive MST search, SCC against BFS in both directions, 100 random DAGs against Bellman–Ford, actual path checks, and a 100,000-vertex stack-safety test. Test reports are written to `target/surefire-reports/`.

Expected demonstrations:

| Input | Expected result |
| --- | --- |
| `connected.txt` | MST weight 10, 1 component, 4 selected edges |
| `disconnected.txt` | Forest weight 3, 4 components, 3 selected edges |
| `cyclic.txt` | 4 SCCs; Kahn reports a cycle; condensation critical path length 4 |
| `dag.txt` | Distances `[0,0,-2,-1,-7,-5,0]`; sum -15; path to 5 is `[1,2,3,4,5]` |

## Benchmark and plots

```bash
java -Xmx1g -jar target/daa-assignment3-graphs-1.0.jar benchmark results
python3 -m pip install -r scripts/requirements.txt
python3 scripts/analyze.py
```

To render the printable report again, run `python3 scripts/render_report.py` (requires the same Python dependencies and DejaVu Sans fonts). The submitted PDF already contains five pages.

With Python dependencies already installed, reproduce tests, benchmark, and plots in one command:

```bash
bash scripts/reproduce.sh
```

Each graph kind and case uses a **fresh `Random(42)`**. Sizes are 1,000; 3,162; 10,000; 31,623; 100,000, with `m=4n` and `m=20n`. Each algorithm gets one warmup and five measured runs; the CSV contains the median. Graph generation and external validation are outside timing. For DAG paths, topological sorting and the distance checksum are included in timing, while `edge_scans` records the relaxation pass only.

The benchmark checks all 30 published numerical values, both MST weights and tree sizes, full SCC partition agreement, topological order, repeated counter determinism, and exact scan counts. Failure throws an exception with a nonzero process exit. New main/bonus CSVs replace the previous files only after every case succeeds.

Outputs:

- `results/results.csv`: exactly **50 data rows**, plus the required header; five algorithms per size/density case.
- `results/bonus.csv`: ten separate Tarjan rows, preserving the required main CSV shape.
- `results/benchmark.log`, `environment.txt`: actual reference checks and runtime details.
- `results/ratios.csv`, `analysis.json`: normalizations and fitted log-log slopes.
- `results/plots/`: three PNG charts, reproduced from CSVs.
- `REPORT.md`, `REPORT.pdf`: five-page report with results, proofs, complexity, and algorithm choices.
- `DEFENSE.md`: detailed study guide and defense practice in Russian.

The submission ZIP also includes `bin/daa-assignment3-graphs-1.0.jar`, runnable immediately with `java -jar bin/daa-assignment3-graphs-1.0.jar demo data/dag.txt` after extracting the project folder. To rebuild the ZIP from a clean checkout of the tagged release after `mvn verify`, run `python3 scripts/package_release.py`.

The handout's final reference to “four generated graphs” conflicts with its detailed benchmark specification. This project follows the explicit ten size/density cases, with three graph kinds in each case, giving 50 main rows.

## Implementation map

| Package / class | Responsibility |
| --- | --- |
| `graph/Graph` | One directed/undirected adjacency-list class, input parsing, in-degrees, transpose |
| `algorithms/Dsu` | `makeSet`, iterative compressed `find`, union by size, component count |
| `algorithms/Kruskal` | Own counted stable merge sort and DSU; stops scanning once connected |
| `algorithms/Prim` | Lazy edge-frontier priority queue; starts at 0, then restarts for each remaining component |
| `algorithms/Kosaraju` | Two iterative DFS passes with explicit vertex/cursor frames |
| `algorithms/Kahn` | In-degree queue, cycle detection, strict permutation/order validation |
| `algorithms/DagShortestPath` | All zero-in-degree sources at 0, long distances, parent-based path restoration |
| `algorithms/Tarjan` | Bonus A: single iterative DFS, index/lowlink, active stack |
| `algorithms/Condensation` | Bonus B: SCC DAG and longest component-level path |
| `metrics/Metrics` | Counters updated inside operations |
| `benchmark/GraphGenerator`, `Benchmark` | Exact deterministic generator and self-checking experiments |

The adjacency list uses primitive arrays (`head`, `arcNext`, `arcTo`, `arcEdge`), rather than millions of edge objects. Every input edge has one ID; undirected edges have two adjacency arcs, including self-loops. Input weights are `int`; total weights and distances are `long`. The graph must remain unchanged while an algorithm runs.

Counters have precise scopes:

- `dsu_ops = findCalls + successfulUnions`; `find` counts one public call, not each parent pointer traversal. An unsuccessful union contributes its two find calls.
- `edge_scans`: Kruskal's sorted selection loop; Prim's adjacency arcs; the two Kosaraju DFS passes; Kahn's dequeue expansion; DAG edge relaxations. Transpose construction, validation, and sorting reads are excluded from this counter.
- `comparisons`: actual weight comparisons inside the custom merge sort or priority-queue comparator. Vertex/edge tie-breaks and distance comparisons are excluded.
- `max_depth`: maximum occupied explicit DFS stack; for Tarjan, the maximum of the DFS-frame stack and active-vertex stack. Non-DFS algorithms report 0.

The plotted operation sum combines these recorded counters; it is a work proxy, not an exact count of all JVM instructions. DSU call frequency alone cannot measure or prove the inverse-Ackermann bound. Our lazy Prim can hold O(E) heap entries, so its multigraph bound is O(V + E log(E+1)) time and O(V+E) extra space.

## Git release and upload

The local project contains real feature commits and merges for `feature/dsu`, `feature/mst`, `feature/scc`, `feature/paths`, and `feature/metrics`. Main points to the final working project; `v1.0` marks the submitted release. A `repository.bundle` is included in the ZIP to preserve every branch and the tag without copying build output into Git.

**Remote publication status:** this environment could not resolve GitHub's hostname, so the repository link is configured but remote upload is unverified. From this existing project directory, publish once network access is available:

```bash
git push -u origin main feature/dsu feature/mst feature/scc feature/paths feature/metrics
git push origin v1.0
```

To restore history from the submission ZIP on another computer:

```bash
git clone -b main repository.bundle assignment3daa-restored
cd assignment3daa-restored
git remote set-url origin git@github.com:lackoftalentt/assignment3daa.git
for topic in dsu mst scc paths metrics; do
  git branch "feature/$topic" "origin/feature/$topic"
done
git push -u origin main feature/dsu feature/mst feature/scc feature/paths feature/metrics
git push origin v1.0
```

No force push is needed or used. If the remote already contains unrelated commits, inspect and reconcile them before publishing. Upload `DAA_Assignment3_Beibarys_Balykbayev_SE-2516.zip` to Moodle; Moodle upload remains a student action.

## AI-use acknowledgement

OpenAI Codex assisted in implementing algorithms, designing cross-checks, running the experiments, plotting results, and drafting documentation and defense notes. AI assistance was used for this submission. The benchmark values and test outcomes in the report come from actual program executions. The student should study the implementation and be prepared to explain and modify it independently during the defense.
