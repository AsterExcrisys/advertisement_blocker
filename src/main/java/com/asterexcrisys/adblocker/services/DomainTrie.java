package com.asterexcrisys.adblocker.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class DomainTrie {

    private final DomainTrieNode root;
    private final String wildcard;

    public DomainTrie(String prefix, String wildcard) {
        root = DomainTrieNode.of(Objects.requireNonNull(prefix), new HashMap<>());
        this.wildcard = Objects.requireNonNull(wildcard);
    }

    public boolean has(String word, String separator) {
        if (word == null || word.isBlank() || separator == null || separator.isBlank()) {
            throw new IllegalArgumentException("word and separator must not be null, empty, or blank");
        }
        if (word.contains(wildcard)) {
            throw new IllegalArgumentException("word must not contain the wildcard");
        }
        String[] parts = ignoreBlankParts(ignoreTriePrefix(word), separator);
        return has(root, parts, 0);
    }

    public void add(String word, String separator) {
        if (word == null || word.isBlank() || separator == null || separator.isBlank()) {
            throw new IllegalArgumentException("word and separator must not be null, empty, or blank");
        }
        String[] parts = ignoreBlankParts(ignoreTriePrefix(word), separator);
        DomainTrieNode current = root;
        for (String part : parts) {
            current.children().putIfAbsent(part, DomainTrieNode.of(part, new HashMap<>()));
            current = current.children().get(part);
        }
    }

    public void remove(String word, String separator) {
        if (word == null || word.isBlank() || separator == null || separator.isBlank()) {
            throw new IllegalArgumentException("word and separator must not be null, empty, or blank");
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

    private boolean has(DomainTrieNode current, String[] parts, int index) {
        if (index == parts.length) {
            return current.children().isEmpty();
        }
        String part = parts[index];
        DomainTrieNode exact = current.children().get(part);
        DomainTrieNode wildcard = current.children().get(this.wildcard);
        if (exact != null && has(exact, parts, index + 1)) {
            return true;
        }
        return wildcard != null && has(wildcard, parts, index + 1);
    }

    private boolean remove(DomainTrieNode current, String[] parts, int index) {
        if (index == parts.length) {
            return current.children().isEmpty();
        }
        String part = parts[index];
        DomainTrieNode child = current.children().get(part);
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
record DomainTrieNode(String label, Map<String, DomainTrieNode> children) implements Comparable<DomainTrieNode> {

    public DomainTrieNode {
        Objects.requireNonNull(label);
        Objects.requireNonNull(children);
    }

    @Override
    public int compareTo(DomainTrieNode other) {
        return label.compareTo(other.label);
    }

    public static DomainTrieNode of(String label, Map<String, DomainTrieNode> children) {
        return new DomainTrieNode(label, children);
    }

}