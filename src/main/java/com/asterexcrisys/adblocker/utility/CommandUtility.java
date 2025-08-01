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
            List<Resolver> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(new STDResolver(line));
            }
            return lines;
        }
    }

    public static List<String> parseFilteredDomains(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        }
    }

}