package com.asterexcrisys.gab.utility;

import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public class Trie {

    private final TrieNode root;

    public Trie(String prefix) {
        root = TrieNode.of(Objects.requireNonNull(prefix), new HashMap<>());
    }

    public boolean has(String word, String separator) {
        if (word == null || word.isBlank() || separator == null || separator.isBlank()) {
            throw new IllegalArgumentException();
        }
        String[] parts = word.split(separator);
        TrieNode current = root;
        for (String part : parts) {
            if (!current.children().containsKey(part)) {
                return false;
            }
            current = current.children().get(part);
        }
        return true;
    }

    public void add(String word, String separator) {
        if (word == null || word.isBlank() || separator == null || separator.isBlank()) {
            throw new IllegalArgumentException();
        }
        String[] parts = word.split(separator);
        TrieNode current = root;
        for (String part : parts) {
            current.children().putIfAbsent(part, TrieNode.of(part, new HashMap<>()));
            current = current.children().get(part);
        }
    }

    public void remove(String word, String separator) {
        if (word == null || word.isBlank() || separator == null || separator.isBlank()) {
            throw new IllegalArgumentException();
        }
        String[] parts = word.split(separator);
        remove(root, parts, 0);
    }

    public void clear() {
        root.children().clear();
    }

    private boolean remove(TrieNode current, String[] parts, int index) {
        if (index == parts.length) {
            return current.children().isEmpty();
        }
        String part = parts[index];
        TrieNode child = current.children().get(part);
        if (child == null) {
            return false;
        }
        boolean shouldDelete = remove(child, parts, index + 1);
        if (shouldDelete) {
            current.children().remove(part);
            return current.children().isEmpty();
        }
        return false;
    }

}

@SuppressWarnings("unused")
record TrieNode(String label, Map<String, TrieNode> children) implements Comparable<TrieNode> {

    @Override
    public int compareTo(@NotNull TrieNode other) {
        return label.compareTo(other.label);
    }

    public static TrieNode of(String label, Map<String, TrieNode> children) {
        return new TrieNode(label, children);
    }

}