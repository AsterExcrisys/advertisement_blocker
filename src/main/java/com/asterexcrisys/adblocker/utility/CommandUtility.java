package com.asterexcrisys.adblocker.utility;

import com.asterexcrisys.adblocker.resolvers.Resolver;
import com.asterexcrisys.adblocker.resolvers.STDResolver;
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
                nameServers.add(new STDResolver(line));
            }
            return nameServers;
        }
    }

    public static List<String> parseFilteredDomains(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            List<String> filteredDomains = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                filteredDomains.add(line);
            }
            return filteredDomains;
        }
    }

}