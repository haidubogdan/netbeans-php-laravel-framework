
package org.netbeans.modules.php.laravel;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author bogdan
 */
public class LaravelConfigTree<T> {

    private Node<T> root;

    public LaravelConfigTree(T rootData) {
        root = new Node<T>();
        root.data = rootData;
        root.children = new ArrayList<Node<T>>();
    }

    public static class Node<T> {

        private T data;
        private Node<T> parent;
        private List<Node<T>> children;
    }
}
