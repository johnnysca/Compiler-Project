public class Node<T> {
    T data; // stores whatever we saw, either an identifier name or a constant number
    boolean constant; // true or false
    Node left; // left child
    Node right; // right child
    int adda; // will hold adda instruction num if an array else -1
    Node(T data, boolean constant){
        this.data = data;
        this.constant = constant;
        this.adda = -1;
    }
    Node(T data, boolean constant, int adda){
        this.data = data;
        this.constant = constant;
        this.adda = adda;
    }
}