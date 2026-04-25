package com.asterexcrisys.adblocker.services.domains;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class DomainTrie {

    private final String prefix;
    private final String wildcard;
    private final String separator;
    private final DomainTrieNode root;

    public DomainTrie(String prefix, String wildcard, String separator, Collection<String> domains) {
        this.prefix = Objects.requireNonNull(prefix);
        this.wildcard = Objects.requireNonNull(wildcard);
        this.separator = Objects.requireNonNull(separator);
        root = initialize(Objects.requireNonNull(domains));
    }

    public boolean contains(String domain) {
        if (domain == null || domain.isBlank()) {
            throw new IllegalArgumentException("domain must not be null, empty, or blank");
        }
        if (domain.contains(wildcard)) {
            throw new IllegalArgumentException("domain must not contain the wildcard");
        }
        String[] parts = ignoreBlankParts(ignoreTriePrefix(domain));
        return contains(root, parts, 0);
    }

    private DomainTrieNode initialize(Collection<String> domains) {
        DomainTrieNode root = DomainTrieNode.of(prefix, new HashMap<>());
        for (String domain : domains) {
            String[] parts = ignoreBlankParts(ignoreTriePrefix(domain));
            root = initialize(root, parts, 0);
        }
        return root;
    }

    private DomainTrieNode initialize(DomainTrieNode current, String[] parts, int index) {
        if (index == parts.length) {
            return current;
        }
        String part = parts[index];
        DomainTrieNode child = current.children().getOrDefault(part, DomainTrieNode.empty(part));
        DomainTrieNode updatedChild = initialize(child, parts, index + 1);
        Map<String, DomainTrieNode> newChildren = new HashMap<>(current.children());
        newChildren.put(part, updatedChild);
        return DomainTrieNode.of(current.label(), newChildren);
    }

    private boolean contains(DomainTrieNode current, String[] parts, int index) {
        if (index == parts.length) {
            return current.children().isEmpty();
        }
        String part = parts[index];
        DomainTrieNode exact = current.children().get(part);
        DomainTrieNode wildcard = current.children().get(this.wildcard);
        if (exact != null && contains(exact, parts, index + 1)) {
            return true;
        }
        return wildcard != null && contains(wildcard, parts, index + 1);
    }

    private String ignoreTriePrefix(String domain) {
        if (domain.startsWith(prefix)) {
            return domain.substring(prefix.length());
        }
        return domain;
    }

    private String[] ignoreBlankParts(String domain) {
        return Stream.of(domain.split(separator))
                .filter((parts) -> !parts.isBlank())
                .toArray(String[]::new);
    }

}