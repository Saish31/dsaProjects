import java.io.*;
import java.util.*;

/**
* Practical Java implementation of the BMSSP SSSP algorithm described in:
 *  "Breaking the Sorting Barrier for Directed Single-Source Shortest Paths"
 *
 * Key components:
 *  - findPivots(B, S)  -> Algorithm 1 (limited k-step relaxations + tight-edge forest)
 *  - baseCase(B, x)    -> Algorithm 2 (limited Dijkstra-like exploration)
 *  - BMSSP(l, B, S)    -> Algorithm 3 (recursive driver)
 *  - BlockD            -> Practical approximation of the paper's D0/D1 (Lemma 3.3)
 *
 * Practical differences from the paper are documented inline where relevant:
 *  - BlockD uses a Buffered approach (D1) + occasional rebuilds into sorted blocks (D0).
 *  - No constant-degree vertex splitting is applied here (code runs on general graphs).
 *  - Parameter heuristics (k,t,lMax,block sizes) are chosen pragmatically.
 * 
 * Compile:
 * javac SSSP_BMSSP_BlockD.java TestHarness.java
 * Run:
 * java -Xmx4g TestHarness > results.csv
 *
 * Input format (example):
 * n m
 * u1 v1 w1
 * ...
 * um vm wm
 * s
 */
public class SSSP_BMSSP_BlockD {

    static final double INF = Double.POSITIVE_INFINITY;
    static final double EPS = 1e-12;

    static class Edge {
        int to;
        double w;

        Edge(int t, double ww) {
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

        void addEdge(int u, int v, double w) {
            adj[u].add(new Edge(v, w));
        }
    }

    // Algorithm state
    final Graph G;            // input graph
    final int n;              // number of vertices
    final double[] dHat;      // tentative distances (the algorithm's primary state)
    final int[] pred;         // predecessor for tie-breaking / path reconstruction
    final boolean[] complete; // marker used in some flows (not essential here)
    final int[] pathLen;      // path length to chosen predecessor (used for tie-breaks)
    

    final int k; // ~ (log n)^{1/3}
    final int t; // ~ (log n)^{2/3}
    final int lMax; // ~ log n / t

    public SSSP_BMSSP_BlockD(Graph g, int source) {
        this.G = g;
        this.n = g.n;
        dHat = new double[n];
        Arrays.fill(dHat, INF);
        pred = new int[n];
        Arrays.fill(pred, -1);
        complete = new boolean[n];
        pathLen = new int[n];

        // Use natural log base e as a stable measure; ensure logn >= 2 to avoid degenerate results
        double logn = Math.max(2.0, Math.log(Math.max(2, n)));
        k = Math.max(2, (int) Math.floor(Math.pow(logn, 1.0 / 3.0))); // k := floor((log n)^{1/3})
        t = Math.max(1, (int) Math.floor(Math.pow(logn, 2.0 / 3.0))); // t := floor((log n)^{2/3})
        lMax = Math.max(0, (int) Math.ceil(Math.log(Math.max(2, n)) / Math.max(1.0, t)));

        // initialize source vertex
        dHat[source] = 0.0;
        pred[source] = -1;
        pathLen[source] = 1;
        complete[source] = true;
    }
    
    // Compare two vertices lexicographically by (dHat, pathLen, id) for deterministic ties.
    private int compareDistTie(int a, int b) {
        if (dHat[a] < dHat[b])
            return -1;
        if (dHat[a] > dHat[b])
            return 1;
        if (pathLen[a] < pathLen[b])
            return -1;
        if (pathLen[a] > pathLen[b])
            return 1;
        return Integer.compare(a, b);
    }

    /**
     * Standard relaxation (u -> v with weight w).
     * Returns true if it improved pred or dHat in a meaningful way.
     *
     * Differences from a textbook relax:
     *  - Uses EPS for numeric stability.
     *  - Updates pred & pathLen for deterministic tie-breaking when distances equal.
     */
    private boolean relax(int u, int v, double w) {
        double cand = dHat[u] + w;
        if (cand < dHat[v] - EPS) {
            dHat[v] = cand;
            pred[v] = u;
            pathLen[v] = pathLen[u] + 1;
            return true;
        } else if (Math.abs(cand - dHat[v]) <= EPS) {
            // break ties deterministically: prefer lexicographically smaller predecessor
            if (pred[v] == -1 || compareEndpoints(u, pred[v], v) < 0) {
                pred[v] = u;
                pathLen[v] = pathLen[u] + 1;
                return true;
            }
        }
        return false;
    }

    // Compare endpoints used for tie-breaking when relaxing equally-valued distances.
    private int compareEndpoints(int a, int b, int v) {
        if (dHat[a] < dHat[b])
            return -1;
        if (dHat[a] > dHat[b])
            return 1;
        if (pathLen[a] < pathLen[b])
            return -1;
        if (pathLen[a] > pathLen[b])
            return 1;
        return Integer.compare(a, b);
    }


    // Block-based D structure (practical D0/D1)
    //
    // The paper uses a special 'D' data-structure (Lemma 3.3) with two sequences:
    //  - D0: sorted blocks (linked list of blocks)
    //  - D1: small append-only buffer
    //
    // Purpose: allow many cheap inserts / batch prepends and occasional pulls
    // without paying O(log n) per operation. The implementation below approximates
    // that behavior:
    //  - current: authoritative HashMap key -> best value
    //  - D1: append-only ArrayList buffer for cheap inserts
    //  - D0: rebuilt from current by sorting into blocks when D1 reaches threshold
    //
    // Practical tradeoff: merges (rebuild D0) are O(n log n) but happen rarely if
    // many inserts are buffered. pull() simplifies correctness by rebuilding D0
    // when necessary. This is a pragmatic engineering approximation of the paper's
    // theoretical D, not a formal reproduction of every invariant.
    static class BlockD {
        static class Item {
            int key;
            double val;

            Item(int k, double v) {
                key = k;
                val = v;
            }
        }

        static class Block {
            ArrayList<Item> items = new ArrayList<>();
            int head = 0;
        }

        final LinkedList<Block> D0 = new LinkedList<>(); // sorted blocks (may be empty)
        final ArrayList<Item> D1 = new ArrayList<>(); // append-only buffer for recent inserts
        final HashMap<Integer, Double> current = new HashMap<>(); // authoritative mapping
        final int blockSize; // target size for blocks in D0
        final int mergeThreshold; // when D1 reaches this size, rebuild D0
        final double Bglobal; // global bound used when D is empty on pull


        BlockD(int n, int blockSizeHint, double Bglobal) {
            this.blockSize = Math.max(16, blockSizeHint);
            this.mergeThreshold = Math.max(8, blockSize);
            this.Bglobal = Bglobal;
        }

        /**
         * Insert key→val into the structure.
         * Only keep the best value in `current`.
         * Append to D1 (cheap); when D1 grows too large we rebuild D0 from current.
         */
        void insert(int key, double val) {
            Double cur = current.get(key);
            if (cur == null || val + EPS < cur) {
                current.put(key, val);
                D1.add(new Item(key, val));
                if (D1.size() >= mergeThreshold)
                    mergeBufferIntoD0();
            }
        }

        // Add multiple key→val pairs in one batch.
        void batchPrepend(Collection<Map.Entry<Integer, Double>> entries) {
            for (Map.Entry<Integer, Double> e : entries) {
                int key = e.getKey();
                double val = e.getValue();
                Double cur = current.get(key);
                if (cur == null || val + EPS < cur) {
                    current.put(key, val);
                    D1.add(new Item(key, val));
                }
            }
            if (D1.size() >= mergeThreshold)
                mergeBufferIntoD0();
        }

        /**
         * Merge D1 (buffer) into D0: rebuild blocks from `current`.
         * Implementation:
         *  - collect all (key,val) pairs from current
         *  - sort by (val,key)
         *  - chop into blocks of size blockSize
         *
         * This is the expensive operation (O(|current| log |current|)). The idea
         * is that if many inserts happen before mergeThreshold, the amortized
         * cost per insert is low.
         */
        private void mergeBufferIntoD0() {
            int sz = current.size();
            ArrayList<Item> all = new ArrayList<>(sz);
            for (Map.Entry<Integer, Double> e : current.entrySet())
                all.add(new Item(e.getKey(), e.getValue()));
            Collections.sort(all, (a, b) -> {
                int c = Double.compare(a.val, b.val);
                if (c != 0)
                    return c;
                return Integer.compare(a.key, b.key);
            });
            D0.clear();
            for (int i = 0; i < all.size(); i += blockSize) {
                Block block = new Block();
                int end = Math.min(all.size(), i + blockSize);
                for (int j = i; j < end; j++)
                    block.items.add(all.get(j));
                D0.add(block);
            }
            D1.clear();
        }

        /**
         * Pull up to M smallest keys currently present in `current`.
         * Returns a Pair: (listOfKeysTaken, Bi) where Bi is smallest remaining value (or Bglobal).
         *
         * Practical notes:
         *  - The code must skip stale entries: D1 contains historic values that might be outdated.
         *  - For simplicity this implementation may rebuild D0 from current after removals
         *    to keep block heads consistent (tradeoff between simplicity and performance).
         */
        Pair<List<Integer>, Double> pull(int M) {
            ArrayList<Integer> out = new ArrayList<>();
            if (current.isEmpty())
                return new Pair<>(out, Bglobal);

            // If D0 empty but D1 has items, merge to have efficient pulls.
            if (D0.isEmpty() && !D1.isEmpty())
                mergeBufferIntoD0();

            // Build min-heap over current heads of blocks and over D1 items (if any left)
            class HeapEntry implements Comparable<HeapEntry> {
                double val;
                int key;
                int blockIndex; // blockIndex == -1 for D1 items
                Item item;

                HeapEntry(double v, int k, int b, Item it) {
                    val = v;
                    key = k;
                    blockIndex = b;
                    item = it;
                }

                public int compareTo(HeapEntry o) {
                    int c = Double.compare(val, o.val);
                    if (c != 0)
                        return c;
                    return Integer.compare(key, o.key);
                }
            }

            PriorityQueue<HeapEntry> heap = new PriorityQueue<>();
            // add heads of D0 blocks
            int bIdx = 0;
            for (Block b : D0) {
                if (b.head < b.items.size()) {
                    Item it = b.items.get(b.head);
                    Double cur = current.get(it.key);
                    if (cur != null)
                        heap.add(new HeapEntry(it.val, it.key, bIdx, it));
                }
                bIdx++;
            }
            // also add D1 items (they are probably few since we merge periodically)
            for (Item it : D1) {
                Double cur = current.get(it.key);
                if (cur != null)
                    heap.add(new HeapEntry(it.val, it.key, -1, it));
            }

            double maxTaken = -1;
            // Pop up to M valid smallest items (skip stale entries)
            while (!heap.isEmpty() && out.size() < M) {
                HeapEntry he = heap.poll();
                Double cur = current.get(he.key);
                if (cur == null)
                    continue; // key removed
                if (Math.abs(cur - he.val) > 1e-9)
                    continue; // stale entry
                // valid
                out.add(he.key);
                maxTaken = Math.max(maxTaken, he.val);
                // advance in its block if from D0
                if (he.blockIndex >= 0) {
                    // advance head for that block and push new head if exists
                    int idx = 0;
                    ListIterator<Block> itb = D0.listIterator();
                    while (idx <= he.blockIndex && itb.hasNext()) {
                        itb.next();
                        idx++;
                    }
                }
                current.remove(he.key);
            }

            // After removing selected keys from current, rebuild D0 from remaining current entries
            // to keep the block structure consistent for the next pulls. This rebuild is expensive,
            // but simplifies implementation and ensures correctness at the cost of potential extra cost.
            if (!out.isEmpty()) {
                ArrayList<Item> remain = new ArrayList<>(current.size());
                for (Map.Entry<Integer, Double> e : current.entrySet())
                    remain.add(new Item(e.getKey(), e.getValue()));
                Collections.sort(remain, (a, b) -> {
                    int c = Double.compare(a.val, b.val);
                    if (c != 0)
                        return c;
                    return Integer.compare(a.key, b.key);
                });
                D0.clear();
                for (int i = 0; i < remain.size(); i += blockSize) {
                    Block block = new Block();
                    int end = Math.min(remain.size(), i + blockSize);
                    for (int j = i; j < end; j++)
                        block.items.add(remain.get(j));
                    D0.add(block);
                }
                D1.clear();
            }

            // Compute Bi: smallest remaining value in current (or Bglobal if nothing left)
            double nextMin = Bglobal;
            for (Double v : current.values()) {
                if (v < nextMin)
                    nextMin = v;
            }
            double Bi = nextMin;
            return new Pair<>(out, Bi);
        }

        boolean isEmpty() {
            return current.isEmpty();
        }
    }

    // simple pair
    static class Pair<A, B> {
        A fst;
        B snd;

        Pair(A a, B b) {
            fst = a;
            snd = b;
        }
    }


    // findPivots(B, S) — Algorithm 1
    //
    // Perform up to k rounds of local relaxations from S; collect all vertices W
    // that become reachable with tentative distances < B.
    // If W grows quickly (|W| > k * |S|) return P = S (cheap pivot case).
    // Else build tight-edge forest on W (edges u->v with dHat[v] == dHat[u] + w)
    // and compute subtree sizes to pick P ⊆ S with subtree size >= k.
    //
    // This method updates dHat during the k rounds.
    private static class PivotResult {
        Set<Integer> W;
        Set<Integer> P;

        PivotResult(Set<Integer> W, Set<Integer> P) {
            this.W = W;
            this.P = P;
        }
    }

    private PivotResult findPivots(double B, Set<Integer> S) {
        Set<Integer> W = new HashSet<>(S);
        Set<Integer> prev = new HashSet<>(S);
        Set<Integer> next = new HashSet<>();
        for (int step = 1; step <= k; step++) {
            next.clear();
            for (int u : prev) {
                for (Edge e : G.adj[u]) {
                    int v = e.to;
                    // if relaxing from u to v is allowed under current dHat values
                    if (dHat[u] + e.w <= dHat[v] + EPS) {
                        // update dHat[v] if we found an improvement
                        if (dHat[u] + e.w < dHat[v] - EPS) {
                            dHat[v] = dHat[u] + e.w;
                            pred[v] = u;
                            pathLen[v] = pathLen[u] + 1;
                        }
                         // if this improvement is under the bound B, add to W & next frontier
                        if (dHat[u] + e.w < B - EPS) {
                            if (!W.contains(v))
                                next.add(v);
                            W.add(v);
                        }
                    }
                }
            }
            // Cheap-case: if W grew superlinearly relative to S, prefer P=S
            if (W.size() > (long) k * S.size())
                return new PivotResult(W, new HashSet<>(S));
            prev = new HashSet<>(next); // move frontier one step further
        }

        // Build tight-edge forest F on W: edges (u->v) where dHat[v] == dHat[u] + w
        Map<Integer, List<Integer>> fAdj = new HashMap<>();
        for (int u : W)
            fAdj.put(u, new ArrayList<>());
        for (int u : W)
            for (Edge e : G.adj[u])
                if (W.contains(e.to)) {
                    if (Math.abs(dHat[e.to] - (dHat[u] + e.w)) <= EPS)
                        fAdj.get(u).add(e.to);
                }
        // Compute subtree sizes in F for each root in S (post-order)
        Map<Integer, Integer> subtreeSize = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        for (int s : S)
            if (W.contains(s) && !visited.contains(s)) {
                Deque<Integer> stack = new ArrayDeque<>();
                Deque<Integer> order = new ArrayDeque<>();
                stack.push(s);
                while (!stack.isEmpty()) {
                    int u = stack.pop();
                    if (visited.contains(u))
                        continue;
                    visited.add(u);
                    order.push(u);
                    for (int v : fAdj.get(u))
                        if (!visited.contains(v))
                            stack.push(v);
                }
                while (!order.isEmpty()) { 
                    int u = order.pop(); 
                    int size = 1;
                    for (int v : fAdj.get(u))
                        size += subtreeSize.getOrDefault(v, 0);
                    subtreeSize.put(u, size);
                }
            }
        // Choose pivots P as those s in S whose subtree size >= k
        Set<Integer> P = new HashSet<>();
        for (int s : S) 
            if (W.contains(s)) {
                int size = subtreeSize.getOrDefault(s, 0);
                if (size >= k)
                    P.add(s);
            }
        return new PivotResult(W, P);
    }


    // baseCase(B, x) — Algorithm 2 (small Dijkstra-like)
    //
    // When recursion level l == 0, select pivot x with smallest dHat and
    // perform a small Dijkstra style exploration limited to (k+1) vertices or B bound.
    // If <= k vertices found, return (B, thoseVertices).
    // If k+1 found, compute B' = max dHat among found vertices and return (B', U)
    // where U = {v found | dHat[v] < B'}.
    //
    // This resolves a small group of nodes precisely.
    private Pair<Double, Set<Integer>> baseCase(double B, int x) {
        Set<Integer> U0 = new HashSet<>();
        PriorityQueue<Integer> pq = new PriorityQueue<>((a, b) -> {
            if (dHat[a] < dHat[b])
                return -1;
            if (dHat[a] > dHat[b])
                return 1;
            if (pathLen[a] < pathLen[b])
                return -1;
            if (pathLen[a] > pathLen[b])
                return 1;
            return Integer.compare(a, b);
        });
        pq.add(x);
        U0.add(x);
        while (!pq.isEmpty() && U0.size() < k + 1) {
            int u = pq.poll();
            for (Edge e : G.adj[u]) {
                int v = e.to;
                // only consider edges that don't increase beyond current dHat[v] and below B
                if (dHat[u] + e.w <= dHat[v] + EPS && dHat[u] + e.w < B - EPS) {
                    relax(u, v, e.w);
                    if (!U0.contains(v)) {
                        pq.add(v);
                        U0.add(v);
                    }
                }
            }
        }
        if (U0.size() <= k)
            return new Pair<>(B, U0);
        double Bprime = -1;
        for (int v : U0)
            if (dHat[v] > Bprime)
                Bprime = dHat[v];
        Set<Integer> U = new HashSet<>();
        for (int v : U0)
            if (dHat[v] < Bprime - EPS)
                U.add(v);
        return new Pair<>(Bprime, U);
    }


    // BMSSP(l, B, S) — Algorithm 3 (recursive driver)
    //
    // If l == 0: pick best x in S and run baseCase.
    // Else: find pivots P using findPivots(B,S), create D with P,
    // and repeatedly pull small batches Si from D and recurse on them with a smaller bound Bi.
    // After recursion returns Ui (settled vertices), relax outward and insert new candidates into D
    // or collect them and batch-prepend into D (depending on which band they fall into).
    // Heuristically stop when U grows sufficiently large, or when D empties.
    private Pair<Double, Set<Integer>> BMSSP(int l, double B, Set<Integer> S) {
        if (S.isEmpty())
            return new Pair<>(B, new HashSet<>());
        if (l == 0) {
            int x = -1;
            for (int s : S)
                if (x == -1 || compareDistPair(s, x) < 0)
                    x = s;
            complete[x] = true;
            return baseCase(B, x);
        }

        // 1) find pivots and W
        PivotResult pr = findPivots(B, S);
        Set<Integer> P = pr.P;
        Set<Integer> W = pr.W;

        // 2) prepare D (use heuristics for M and block size)
        int M = Math.max(1, 2 * (l - 1) * t); // how many keys to pull at a time (heuristic)
        int blockSizeHint = Math.max(32, (int) Math.max(32, Math.pow(Math.log(Math.max(2, n)), 2.0 / 3.0)));
        BlockD D = new BlockD(n, blockSizeHint, B);

        // Insert pivots into D (with their current dHat values)
        for (int x : P)
            D.insert(x, dHat[x]);
        double Bprime0 = P.isEmpty() ? B : Double.POSITIVE_INFINITY;
        if (!P.isEmpty())
            for (int x : P)
                Bprime0 = Math.min(Bprime0, dHat[x]);
        Set<Integer> U = new HashSet<>();

        // Main loop: process small batches from D until we reach stopping criteria
        while (U.size() < (long) k * k * Math.max(2, l) && !D.isEmpty()) {
            Pair<List<Integer>, Double> pulled = D.pull(M);
            List<Integer> Si = pulled.fst;
            double Bi = pulled.snd;
            if (Si.isEmpty())
                break;
            Set<Integer> SiSet = new HashSet<>(Si);

            // Recurse: process Si at lower recursion level with bound Bi
            Pair<Double, Set<Integer>> rec = BMSSP(l - 1, Bi, SiSet);
            double Bpi = rec.fst;
            Set<Integer> Ui = rec.snd;
            U.addAll(Ui);

            // For each settled vertex, relax outgoing edges and decide where to place new candidates
            List<Map.Entry<Integer, Double>> Krecords = new ArrayList<>();
            for (int u : Ui)
                for (Edge e : G.adj[u]) {
                    int v = e.to;
                    double cand = dHat[u] + e.w;
                    if (cand <= dHat[v] + EPS) {
                        relax(u, v, e.w);
                        if (cand >= Bi - EPS && cand < B - EPS)
                            D.insert(v, cand);
                        else if (cand >= Bpi - EPS && cand < Bi - EPS)
                            Krecords.add(new AbstractMap.SimpleEntry<>(v, cand));
                    }
                }

            // Also include Si items falling into [Bpi, Bi) as Krecords
            for (int x : Si)
                if (dHat[x] >= Bpi - EPS && dHat[x] < Bi - EPS)
                    Krecords.add(new AbstractMap.SimpleEntry<>(x, dHat[x]));

            D.batchPrepend(Krecords);

            // Optional early exit: if U became large enough, we return early adding accessible W nodes
            if (U.size() >= (long) k * k * l * t) {
                double retB = Math.min(Bpi, B);
                for (int x : W)
                    if (dHat[x] < retB - EPS)
                        U.add(x);
                return new Pair<>(retB, U);
            }
        }

        // At the end add all W vertices with dHat < B to U
        double finalB = B;
        for (int x : W)
            if (dHat[x] < finalB - EPS)
                U.add(x);
        return new Pair<>(finalB, U);
    }

    // helper used to pick smallest by dHat with tie-breaking
    private int compareDistPair(int a, int b) {
        if (dHat[a] < dHat[b])
            return -1;
        if (dHat[a] > dHat[b])
            return 1;
        if (pathLen[a] < pathLen[b])
            return -1;
        if (pathLen[a] > pathLen[b])
            return 1;
        return Integer.compare(a, b);
    }

    // Public solve method
    public double[] solveFromSource(int s) {
        dHat[s] = 0.0;
        complete[s] = true;
        pred[s] = -1;
        pathLen[s] = 1;
        Set<Integer> S = new HashSet<>();
        S.add(s);
        BMSSP(lMax, Double.POSITIVE_INFINITY, S);
        return dHat;
    }

    // --------------- main / IO ---------------
    public static void main(String[] args) throws Exception {
        FastScanner fs = new FastScanner(System.in);
        int n = fs.nextInt();
        int m = fs.nextInt();
        Graph g = new Graph(n);
        for (int i = 0; i < m; i++) {
            int u = fs.nextInt(), v = fs.nextInt();
            double w = fs.nextDouble();
            g.addEdge(u, v, w);
        }
        int s = fs.nextInt();
        SSSP_BMSSP_BlockD solver = new SSSP_BMSSP_BlockD(g, s);
        double[] dist = solver.solveFromSource(s);
        PrintWriter out = new PrintWriter(System.out);
        for (int i = 0; i < n; i++) {
            if (Double.isInfinite(dist[i]))
                out.println("INF");
            else
                out.println(dist[i]);
        }
        out.flush();
    }

    static class FastScanner {
        private final InputStream in;
        private final byte[] buffer = new byte[1 << 16];
        private int ptr = 0, len = 0;

        FastScanner(InputStream is) {
            in = is;
        }

        private int read() throws IOException {
            if (ptr >= len) {
                len = in.read(buffer);
                ptr = 0;
                if (len <= 0)
                    return -1;
            }
            return buffer[ptr++];
        }

        String next() throws IOException {
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = read()) <= ' ') {
                if (c == -1)
                    return null;
            }
            do {
                sb.append((char) c);
                c = read();
            } while (c > ' ');
            return sb.toString();
        }

        int nextInt() throws IOException {
            return Integer.parseInt(next());
        }

        double nextDouble() throws IOException {
            return Double.parseDouble(next());
        }
    }
}
