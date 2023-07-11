package dsaprojects;

import java.util.*;

public class ShortestPathFinder {
    private Map<Integer, List<Edge>> graph;

    public ShortestPathFinder() {
        graph = new HashMap<>();
    }

    public void addEdge(int source, int destination, int weight) {
        graph.computeIfAbsent(source, ArrayList::new).add(new Edge(destination, weight));
        graph.computeIfAbsent(destination, ArrayList::new).add(new Edge(source, weight));
    }

    public List<Integer> findShortestPath(int source, int destination) {
        Map<Integer, Integer> distance = new HashMap<>();
        Map<Integer, Integer> previous = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(Node::getDistance));

        distance.put(source, 0);
        queue.offer(new Node(source, 0));

        for (int vertex : graph.keySet()) {
            if (vertex != source) {
                distance.put(vertex, Integer.MAX_VALUE);
                previous.put(vertex, null);
            }
        }

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            if (current.getVertex() == destination) {
                break;
            }

            if (current.getDistance() > distance.get(current.getVertex())) {
                continue;
            }

            for (Edge edge : graph.get(current.getVertex())) {
                int newDistance = distance.get(current.getVertex()) + edge.getWeight();

                if (newDistance < distance.get(edge.getDestination())) {
                    distance.put(edge.getDestination(), newDistance);
                    previous.put(edge.getDestination(), current.getVertex());
                    queue.offer(new Node(edge.getDestination(), newDistance));
                }
            }
        }

        List<Integer> path = new ArrayList<>();
        Integer currentVertex = destination;
        int totalDistance = distance.get(destination);
        while (currentVertex!=null) {
            path.add(0, currentVertex);
            currentVertex = previous.get(currentVertex);
        }
        System.out.println("Total Distance: " + totalDistance);
        return path;
    }

    public static void main(String[] args) {
        ShortestPathFinder shortestPathFinder = new ShortestPathFinder();

        shortestPathFinder.addEdge(1, 2, 7);
        shortestPathFinder.addEdge(1, 3, 9);
        shortestPathFinder.addEdge(1, 6, 14);
        shortestPathFinder.addEdge(2, 3, 10);
        shortestPathFinder.addEdge(2, 4, 15);
        shortestPathFinder.addEdge(3, 4, 11);
        shortestPathFinder.addEdge(3, 6, 2);
        shortestPathFinder.addEdge(4, 5, 6);
        shortestPathFinder.addEdge(5, 6, 9);
        Scanner sc=new Scanner(System.in);
        System.out.print("Enter Source: ");
        int source = sc.nextInt();
        System.out.print("Enter Destination: ");
        int destination = sc.nextInt();
        List<Integer> shortestPath = shortestPathFinder.findShortestPath(source, destination);
        System.out.println("Shortest path from " + source + " to " + destination + ": " + shortestPath);
    }

    private static class Edge {
        private int destination;
        private int weight;

        public Edge(int destination, int weight) {
            this.destination = destination;
            this.weight = weight;
        }

        public int getDestination() {
            return destination;
        }

        public int getWeight() {
            return weight;
        }
    }

    private static class Node {
        private int vertex;
        private int distance;

        public Node(int vertex, int distance) {
            this.vertex = vertex;
            this.distance = distance;
        }

        public int getVertex() {
            return vertex;
        }

        public int getDistance() {
            return distance;
        }
    }
}
