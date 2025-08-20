import java.io.*;
import java.util.*;

/**
 * TestHarness.java
 *
 * A simple but practical benchmarking harness to compare:
 *   - SSSP_BMSSP_BlockD (your BMSSP implementation)
 *   - Simple binary-heap Dijkstra (baseline)
 *
 * Features:
 *  - Generates reproducible random directed graphs (no self-loops, no duplicate edges).
 *  - Builds graph objects for both solver implementations.
 *  - Runs multiple trials per (n,m) configuration and prints CSV rows:
 *      n,m,trial,algo,time_ms
 *  - Performs a small warmup run to let the JVM JIT compile hot code paths.
 *
 * Notes about measuring:
 *  - Timings are wall-clock via System.nanoTime() in milliseconds.
 *  - JVM warmup (we run solver once before timed runs) helps JIT fairness.
 *  - For robust comparisons, run several trials and larger graphs.
 */
public class TestHarness {
    static final Random rng = new Random(123456);

    /**
     * Small helper experiment descriptor class: (n, m, trials, maxW)
     *  - n: number of nodes
     *  - m: number of directed edges
     *  - trials: how many random graphs (or re-seeds) to test per configuration
     *  - maxW: maximum integer weight (weights are uniform in [1, maxW])
     *
     * Edit the `experiments` array in main() to change sizes / densities.
     */
    static class Experiment {
        int n;
        int m;
        int trials;
        int maxW;

        Experiment(int n, int m, int t, int w) {
            this.n = n;
            this.m = m;
            this.trials = t;
            this.maxW = w;
        }
    }

    public static void main(String[] args) throws Exception {
        // Example experiments: Edit as needed.
        Experiment[] experiments = new Experiment[] {
            new Experiment(  500,   2000, 10, 10),
            new Experiment( 1000,   4000, 10, 10),
            new Experiment( 2000,  10000,  8, 20),
            new Experiment( 5000,  30000,  6, 50),
            new Experiment(10000,  80000,  5, 50),
            new Experiment(20000, 200000,  4,100),
            new Experiment(50000,1000000,  3,100),
            new Experiment(100000,3000000, 2,100)
        };

        System.out.println("n,m,trial,algo,time_ms");

        for (Experiment e : experiments) {
            for (int trial = 1; trial <= e.trials; trial++) {
                // generate graph edges
                List<int[]> edges = generateRandomEdges(e.n, e.m, e.maxW);

                // build solver graph (SSSP_BMSSP_BlockD.Graph)
                SSSP_BMSSP_BlockD.Graph gSolver = new SSSP_BMSSP_BlockD.Graph(e.n);
                for (int[] ed : edges)
                    gSolver.addEdge(ed[0], ed[1], ed[2]);

                // build simple graph for Dijkstra
                SimpleDijkstra.Graph gDij = new SimpleDijkstra.Graph(e.n);
                for (int[] ed : edges)
                    gDij.addEdge(ed[0], ed[1], ed[2]);

                int s = 0; // choose source vertex 0

                // Warmup (helps JIT)
                if (trial == 1) {
                    runWarmup(gSolver, gDij, s);
                }

                // Measure BMSSP_BlockD solver
                long t0 = System.nanoTime();
                SSSP_BMSSP_BlockD solver = new SSSP_BMSSP_BlockD(gSolver, s);
                double[] distBM = solver.solveFromSource(s);
                long t1 = System.nanoTime();
                double timeMsBM = (t1 - t0) / 1e6;
                System.out.printf("%d,%d,%d,BMSSP,%,.3f\n", e.n, e.m, trial, timeMsBM);

                // Measure Dijkstra
                long t2 = System.nanoTime();
                double[] distDij = SimpleDijkstra.dijkstra(gDij, s);
                long t3 = System.nanoTime();
                double timeMsDij = (t3 - t2) / 1e6;
                System.out.printf("%d,%d,%d,Dijkstra,%,.3f\n", e.n, e.m, trial, timeMsDij);

                // Sanity-check distances match (within eps)
                boolean same = compareDistances(distBM, distDij);
                if (!same)
                    System.err
                            .println("Warning: distance arrays differ for n=" + e.n + " m=" + e.m + " trial=" + trial);
            }
        }
    }

    static void runWarmup(SSSP_BMSSP_BlockD.Graph gSolver, SimpleDijkstra.Graph gDij, int s) {
        // run a quick warmup to let JVM JIT optimize code paths
        SSSP_BMSSP_BlockD solver = new SSSP_BMSSP_BlockD(gSolver, s);
        solver.solveFromSource(s);
        SimpleDijkstra.dijkstra(gDij, s);
        System.gc();
    }

    static List<int[]> generateRandomEdges(int n, int m, int maxW) {
        HashSet<Long> used = new HashSet<>();
        List<int[]> edges = new ArrayList<>(m);
        while (edges.size() < m) {
            int u = rng.nextInt(n);
            int v = rng.nextInt(n);
            if (u == v)
                continue;
            long key = (((long) u) << 32) | (v & 0xffffffffL);
            if (used.contains(key))
                continue;
            used.add(key);
            int w = 1 + rng.nextInt(Math.max(1, maxW));
            edges.add(new int[] { u, v, w });
        }
        return edges;
    }

    static boolean compareDistances(double[] a, double[] b) {
        if (a.length != b.length)
            return false;
        for (int i = 0; i < a.length; i++) {
            double da = a[i];
            double db = b[i];
            if (Double.isInfinite(da) && Double.isInfinite(db))
                continue;
            if (Math.abs(da - db) > 1e-6)
                return false;
        }
        return true;
    }

    // --- Simple Dijkstra implementation used for baseline ---
    static class SimpleDijkstra {
        static class Edge {
            int to;
            int w;

            Edge(int t, int ww) {
                to = t;
                w = ww;
            }
        }

        static class Graph {
            final int n;
            final ArrayList<Edge>[] adj;

            Graph(int n) {
                this.n = n;
                adj = new ArrayList[n];
                for (int i = 0; i < n; i++)
                    adj[i] = new ArrayList<>();
            }

            void addEdge(int u, int v, int w) {
                adj[u].add(new Edge(v, w));
            }
        }

        static double[] dijkstra(Graph g, int s) {
            int n = g.n;
            double[] dist = new double[n];
            Arrays.fill(dist, Double.POSITIVE_INFINITY);
            dist[s] = 0.0;
            PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
            pq.add(new int[] { 0, s });
            boolean[] seen = new boolean[n];
            while (!pq.isEmpty()) {
                int[] top = pq.poll();
                int u = top[1];
                double du = top[0];
                if (seen[u])
                    continue;
                seen[u] = true;
                for (Edge e : g.adj[u]) {
                    int v = e.to;
                    double cand = du + e.w;
                    if (cand + 1e-12 < dist[v]) {
                        dist[v] = cand;
                        pq.add(new int[] { (int) cand, v });
                    }
                }
            }
            return dist;
        }
    }
}

