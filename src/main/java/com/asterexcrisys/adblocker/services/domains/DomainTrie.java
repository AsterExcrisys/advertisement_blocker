package com.asterexcrisys.adblocker.services.domains;

import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class DomainTrie {

    private final DomainTrieNode root;
    private final String wildcard;
    private final String separator;

    public DomainTrie(String prefix, String wildcard, String separator, Collection<String> domains) {
        root = DomainTrieNode.of(prefix, new HashMap<>());
        this.wildcard = wildcard;
        this.separator = separator;
        initialize(domains);
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

    private void initialize(Collection<String> domains) {
        for (String domain : domains) {
            String[] parts = ignoreBlankParts(ignoreTriePrefix(domain));
            DomainTrieNode current = root;
            for (String part : parts) {
                current.children().putIfAbsent(part, DomainTrieNode.of(part, new HashMap<>()));
                current = current.children().get(part);
            }
        }
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
        if (domain.startsWith(root.label())) {
            return domain.substring(root.label().length());
        }
        return domain;
    }

    private String[] ignoreBlankParts(String domain) {
        return Stream.of(domain.split(separator))
                .filter((parts) -> !parts.isBlank())
                .toArray(String[]::new);
    }

}