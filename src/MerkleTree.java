import java.util.*;
import java.util.concurrent.*;

public class MerkleTree {
    private final List<MerkleNode> leaves;
    private final MerkleNode root;
    private final ConcurrentLinkedQueue<MerkleNode> workQueue = new ConcurrentLinkedQueue<>();

    public MerkleTree(List<String> data) throws InterruptedException {
        this.leaves = new ArrayList<>();
        List<MerkleNode> currentLevel = new ArrayList<>();

        // Build base level (leaves)
        for (String d : data) {
            MerkleNode node = new MerkleNode(d);
            leaves.add(node);
            currentLevel.add(node);
        }

        // Build the tree upward
        while (currentLevel.size() > 1) {
            List<MerkleNode> nextLevel = new ArrayList<>();
            for (int i = 0; i < currentLevel.size(); i += 2) {
                MerkleNode left = currentLevel.get(i);
                MerkleNode right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;
                MerkleNode parent = new MerkleNode(left, right);
                left.parent = parent;
                right.parent = parent;
                nextLevel.add(parent);
            }
            currentLevel = nextLevel;
        }

        root = currentLevel.getFirst();

        // Initialize leaf hashes
        for (MerkleNode leaf : leaves) {
            leaf.hash = MerkleTreeUtil.sha256(leaf.data);
            leaf.done.set(true);
        }

        // Enqueue initial ready nodes (parents of leaves)
        for (MerkleNode leaf : leaves) {
            MerkleNode parent = leaf.parent;
            if (parent != null && isReady(parent)) {
                if (parent.inProgress.compareAndSet(false, true)) {
                    workQueue.add(parent);
                }
            }
        }

        int threadCount = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> workers = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            workers.add(pool.submit(this::computeTree));
        }

        for (Future<?> f : workers) {
            try {
                f.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        pool.shutdown();
        long end = System.currentTimeMillis();
        System.out.println("Execution time: " + (end - start) + " milliseconds");
    }

    public String getRootHash() {
        return root.hash;
    }

    private void computeTree() {
        while (true) {
            MerkleNode task = workQueue.poll();
            if (task == null) break;

            computeAndMarkDone(task);

            MerkleNode parent = task.parent;
            if (parent != null && isReady(parent)) {
                if (parent.inProgress.compareAndSet(false, true)) {
                    workQueue.add(parent);
                }
            }
        }
    }

    private void computeAndMarkDone(MerkleNode node) {
        String combined = node.left.hash + node.right.hash;
        node.hash = MerkleTreeUtil.sha256(combined);
        node.done.set(true);
    }

    private boolean isReady(MerkleNode node) {
        return node.left.done.get() && node.right.done.get();
    }

    public static List<String> generateRandomData(int n) {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            data.add(String.valueOf(i));
        }
        return data;
    }

    public static void main(String[] args) throws InterruptedException {
        MerkleTree tree = new MerkleTree(generateRandomData(32768));
        System.out.println("Root hash: " + tree.getRootHash());
        tree = new MerkleTree(generateRandomData(65536));
        System.out.println("Root hash: " + tree.getRootHash());
        tree = new MerkleTree(generateRandomData(131072));
        System.out.println("Root hash: " + tree.getRootHash());
        tree = new MerkleTree(generateRandomData(262144));
        System.out.println("Root hash: " + tree.getRootHash());
        tree = new MerkleTree(generateRandomData(524288));
        System.out.println("Root hash: " + tree.getRootHash());
        tree = new MerkleTree(generateRandomData(1048576));
        System.out.println("Root hash: " + tree.getRootHash());
    }
}