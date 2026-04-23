package com.asterexcrisys.adblocker.services.domains;

import java.util.Map;
import java.util.Objects;

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