package com.asterexcrisys.adblocker.utility;

import com.asterexcrisys.adblocker.resolvers.*;
import com.asterexcrisys.adblocker.types.HttpMethod;
import com.asterexcrisys.adblocker.types.ResolverType;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class CommandUtility {

    public static List<Resolver> parseNameServers(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            List<Resolver> nameServers = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                nameServers.add(parseNameServer(line));
            }
            return nameServers;
        }
    }

    public static List<String> parseFilteredDomains(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            List<String> filteredDomains = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                filteredDomains.add(parseFilteredDomain(line));
            }
            return filteredDomains;
        }
    }

    private static Resolver parseNameServer(String line) {
        if (line.isBlank()) {
            throw new IllegalArgumentException("line must not be empty or blank");
        }
        if (line.length() < 3) {
            throw new IllegalArgumentException("line must contain the resolver type");
        }
        ResolverType type = ResolverType.valueOf(line.substring(0, 3).toUpperCase());
        return switch (type) {
            case STD -> {
                String[] parts = line.split(":");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("line must contain the resolver address (STD)");
                }
                yield new STDResolver(parts[1]);
            }
            case SEC -> {
                String[] parts = line.split(":");
                if (parts.length != 3) {
                    throw new IllegalArgumentException("line must contain the trust anchor and resolver address (SEC)");
                }
                yield new SECResolver(parts[1], parts[2]);
            }
            case DOT -> {
                String[] parts = line.split(":");
                if (parts.length < 2 || parts.length > 3) {
                    throw new IllegalArgumentException("line must contain the resolver address and port (optional) (DOT)");
                }
                if (parts.length == 2) {
                    yield new DOTResolver(parts[1]);
                } else {
                    yield new DOTResolver(parts[1], Integer.parseInt(parts[2]));
                }
            }
            case DOQ -> {
                String[] parts = line.split(":");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("line must contain the resolver address (DOQ)");
                }
                yield new DOQResolver(parts[1]);
            }
            case DOH -> {
                String[] parts = line.split(":");
                if (parts.length < 2 || parts.length > 3) {
                    throw new IllegalArgumentException("line must contain the resolver method (optional) and address (DOT)");
                }
                if (parts.length == 2) {
                    yield new DOHResolver(parts[1]);
                } else {
                    yield new DOHResolver(HttpMethod.valueOf(parts[1].toUpperCase()), parts[2]);
                }
            }
        };
    }

    private static String parseFilteredDomain(String line) {
        if (line.isBlank()) {
            throw new IllegalArgumentException("line must not be empty or blank");
        }
        if (line.startsWith("www.")) {
            return line.substring(4);
        }
        return line;
    }

}