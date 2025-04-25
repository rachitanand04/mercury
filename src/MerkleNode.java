import java.util.concurrent.atomic.AtomicBoolean;

public class MerkleNode {
    public final MerkleNode left, right;
    public MerkleNode parent;
    public final String data; // only for leaf nodes
    public volatile String hash;
    public final AtomicBoolean done = new AtomicBoolean(false);
    public final AtomicBoolean inProgress = new AtomicBoolean(false);

    // Leaf node
    public MerkleNode(String data) {
        this.left = null;
        this.right = null;
        this.data = data;
    }

    // Internal node
    public MerkleNode(MerkleNode left, MerkleNode right) {
        this.left = left;
        this.right = right;
        this.data = null;
    }
}