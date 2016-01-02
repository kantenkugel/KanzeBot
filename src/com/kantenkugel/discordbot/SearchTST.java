package com.kantenkugel.discordbot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Created by Michael Ritter on 06.12.2015.
 */
public class SearchTST<V> {
    private static class Node<V> {
        V value;
        char key;
        Node<V> left, mid, right;
    }

    private Node<V> root;

    public SearchTST() {
        root = new Node<>();
    }

    private Node<V> getNode(String prefix) {
        Node<V> curr = root;
        for(char c : prefix.toCharArray()) {
            if(curr == null || curr.mid == null) {
                return null;
            }
            curr = curr.mid;
            while(curr != null && curr.key != c) {
                if(curr.key < c) {
                    curr = curr.right;
                } else {
                    curr = curr.left;
                }
            }
        }
        return curr;
    }

    public void put(String key, V value) {
        Node<V> curr = root;
        for(char c : key.toCharArray()) {
            if(curr.mid == null) {
                curr.mid = new Node<>();
                curr = curr.mid;
                curr.key = c;
                continue;
            }
            curr = curr.mid;
            while(curr.key != c) {
                if(curr.key < c) {
                    if(curr.right == null) {
                        curr.right = new Node<>();
                        curr.right.key = c;
                    }
                    curr = curr.right;
                } else {
                    if(curr.left == null) {
                        curr.left = new Node<>();
                        curr.left.key = c;
                    }
                    curr = curr.left;
                }
            }
        }
        curr.value = value;
    }

    public V get(String key) {
        Node<V> curr = getNode(key);
        return curr == null ? null : curr.value;
    }

    public Iterable<String> collect(String prefix) {
        List<String> collection = new ArrayList<>();
        Node<V> root = getNode(prefix);
        if(root == null) {
            return collection;
        }
        if(root.value != null) {
            collection.add(prefix);
        }
        if(root.mid == null) {
            return collection;
        }
        Stack<StackElem<V>> stack = new Stack<>();
        stack.push(new StackElem<V>(prefix, root.mid));
        StackElem<V> curr;
        while(!stack.empty()) {
            curr = stack.pop();
            if(curr.node.value != null) {
                collection.add(curr.prefix+curr.node.key);
            }
            if(curr.node.left != null) {
                stack.push(new StackElem<V>(curr.prefix, curr.node.left));
            }
            if(curr.node.mid != null) {
                stack.push(new StackElem<V>(curr.prefix + curr.node.key, curr.node.mid));
            }
            if(curr.node.right != null) {
                stack.push(new StackElem<V>(curr.prefix, curr.node.right));
            }
        }
        Collections.sort(collection);
        return collection;
    }

    private static class StackElem<V> {
        private final String prefix;
        private final Node<V> node;

        private StackElem(String pre, Node<V> node) {
            this.prefix = pre;
            this.node = node;
        }
    }

    public static void main(String[] args) {
        SearchTST<Integer> tri = new SearchTST<>();
        tri.put("by", 4);
        tri.put("sea", 6);
        tri.put("sells", 1);
        tri.put("she", 0);
        tri.put("shells", 3);
        tri.put("shore", 7);
        tri.put("the", 5);

        System.out.println(tri.collect("sh"));
        System.out.println(tri.get("sea"));
    }
}
