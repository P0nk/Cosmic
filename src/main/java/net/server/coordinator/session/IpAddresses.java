package net.server.coordinator.session;

import config.YamlConfig;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IpAddresses {
    private static final List<Pattern> LOCAL_ADDRESS_PATTERNS = loadLocalAddressPatterns();

    private static List<Pattern> loadLocalAddressPatterns() {
        return Stream.of("10\\.", "192\\.168\\.", "172\\.(1[6-9]|2[0-9]|3[0-1])\\.")
                .map(Pattern::compile)
                .collect(Collectors.toList());
    }

    public static String evaluateRemoteAddress(String inetAddress) {
        if (isLocalAddress(inetAddress) || isLanAddress(inetAddress)) {
            return YamlConfig.config.server.HOST;
        } else {
            return inetAddress;
        }
    }

    public static boolean isLocalAddress(String inetAddress) {
        return inetAddress.startsWith("127.");
    }

    public static boolean isLanAddress(String inetAddress) {
        return LOCAL_ADDRESS_PATTERNS.stream()
                .anyMatch(pattern -> matchesPattern(pattern, inetAddress));
    }

    private static boolean matchesPattern(Pattern pattern, String searchTerm) {
        return pattern.matcher(searchTerm).find();
    }
}
