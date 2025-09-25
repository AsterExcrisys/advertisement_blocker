package com.asterexcrisys.adblocker.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class Trie {

    private final TrieNode root;
    private final String wildcard;

    public Trie(String prefix, String wildcard) {
        root = TrieNode.of(Objects.requireNonNull(prefix), new HashMap<>());
        this.wildcard = Objects.requireNonNull(wildcard);
    }

    public boolean has(String word, String separator) {
        if (word == null || word.isBlank() || separator == null || separator.isBlank()) {
            throw new IllegalArgumentException();
        }
        if (word.contains(wildcard)) {
            throw new IllegalArgumentException();
        }
        String[] parts = ignoreBlankParts(ignoreTriePrefix(word), separator);
        return has(root, parts, 0);
    }

    public void add(String word, String separator) {
        if (word == null || word.isBlank() || separator == null || separator.isBlank()) {
            throw new IllegalArgumentException();
        }
        String[] parts = ignoreBlankParts(ignoreTriePrefix(word), separator);
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
        String[] parts = ignoreBlankParts(ignoreTriePrefix(word), separator);
        remove(root, parts, 0);
    }

    public void clear() {
        root.children().clear();
    }

    private String ignoreTriePrefix(String word) {
        if (word.startsWith(root.label())) {
            return word.substring(root.label().length());
        }
        return word;
    }

    private String[] ignoreBlankParts(String word, String separator) {
        return Stream.of(word.split(separator)).filter(String::isBlank).toArray(String[]::new);
    }

    private boolean has(TrieNode current, String[] parts, int index) {
        if (index == parts.length) {
            return current.children().isEmpty();
        }
        String part = parts[index];
        TrieNode exact = current.children().get(part);
        TrieNode wildcard = current.children().get(this.wildcard);
        if (exact != null && has(exact, parts, index + 1)) {
            return true;
        }
        return wildcard != null && has(wildcard, parts, index + 1);
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
    public int compareTo(TrieNode other) {
        return label.compareTo(other.label);
    }

    public static TrieNode of(String label, Map<String, TrieNode> children) {
        return new TrieNode(label, children);
    }

}