public class Node<T> {
    T data; // stores whatever we saw, either an identifier name or a constant number
    boolean constant; // 0 means identifier, 1 means constant, 2 means operator
    Node left; // left child
    Node right; // right child
    Node(T data, boolean constant){
        this.data = data;
        this.constant = constant;
    }
}
