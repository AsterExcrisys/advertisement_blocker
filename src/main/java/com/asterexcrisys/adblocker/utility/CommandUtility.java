package com.asterexcrisys.adblocker.utility;

import com.asterexcrisys.adblocker.resolvers.*;
import com.asterexcrisys.adblocker.types.HttpMethod;
import com.asterexcrisys.adblocker.types.ResolverType;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class CommandUtility {

    private static final Pattern DOMAIN_PATTERN;

    static {
        DOMAIN_PATTERN = Pattern.compile("^(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$");
    }

    public static List<Resolver> parseNameServers(File file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            List<Resolver> nameServers = new ArrayList<>();
            Set<String> lines = new HashSet<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (lines.contains(line)) {
                    continue;
                }
                nameServers.add(parseNameServer(line));
                lines.add(line);
            }
            return nameServers;
        }
    }

    public static List<String> parseFilteredDomains(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            List<String> filteredDomains = new ArrayList<>();
            Set<String> lines = new HashSet<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (lines.contains(line)) {
                    continue;
                }
                filteredDomains.add(parseFilteredDomain(line));
                lines.add(line);
            }
            return filteredDomains;
        }
    }

    private static Resolver parseNameServer(String line) throws Exception {
        if (line.isBlank()) {
            throw new IllegalArgumentException("line must not be empty or blank");
        }
        if (line.trim().length() < 3) {
            throw new IllegalArgumentException("line must contain the resolver type");
        }
        ResolverType type = ResolverType.valueOf(line.trim().substring(0, 3).toUpperCase());
        return switch (type) {
            case STD -> {
                String[] parts = line.split(":");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("line must contain the resolver address (STD)");
                }
                yield new STDResolver(resolveDomainIfNecessary(parts[1]));
            }
            case SEC -> {
                String[] parts = line.split(":");
                if (parts.length != 3) {
                    throw new IllegalArgumentException("line must contain the trust anchor and resolver address (SEC)");
                }
                yield new SECResolver(parts[1], resolveDomainIfNecessary(parts[2]));
            }
            case DOT -> {
                String[] parts = line.split(":");
                if (parts.length < 2 || parts.length > 3) {
                    throw new IllegalArgumentException("line must contain the resolver address and port (optional) (DOT)");
                }
                if (parts.length == 2) {
                    yield new DOTResolver(resolveDomainIfNecessary(parts[1]));
                } else {
                    yield new DOTResolver(resolveDomainIfNecessary(parts[1]), Integer.parseInt(parts[2]));
                }
            }
            case DOQ -> {
                String[] parts = line.split(":");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("line must contain the resolver address (DOQ)");
                }
                yield new DOQResolver(resolveDomainIfNecessary(parts[1]));
            }
            case DOH -> {
                String[] parts = line.split(":");
                if (parts.length < 2 || parts.length > 3) {
                    throw new IllegalArgumentException("line must contain the resolver method (optional) and address (DOT)");
                }
                if (parts.length == 2) {
                    yield new DOHResolver(resolveDomainIfNecessary(parts[1]));
                } else {
                    yield new DOHResolver(HttpMethod.valueOf(parts[1].toUpperCase()), resolveDomainIfNecessary(parts[2]));
                }
            }
        };
    }

    private static String parseFilteredDomain(String line) {
        if (line.isBlank()) {
            throw new IllegalArgumentException("line must not be empty or blank");
        }
        if (line.startsWith("www.")) {
            return line.trim().substring(4);
        }
        return line.trim();
    }

    private static String resolveDomainIfNecessary(String address) throws Exception {
        if (!DOMAIN_PATTERN.matcher(address).matches()) {
            return address;
        }
        return GlobalUtility.resolveDomainAddress(address, true);
    }

}