package com.taxoryn.core.security.proxy;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Production-ready Client IP Resolver with Trusted Proxy Verification.
 * <p>
 * Defense-in-Depth against IP Spoofing and Rate Limit Evasion:
 * 1. Inspects client-supplied forwarding headers (X-Forwarded-For, X-Real-IP, CF-Connecting-IP, Forwarded)
 *    ONLY IF the connecting socket (remoteAddr) originates from a trusted reverse proxy (e.g. Render, Cloudflare, Docker, AWS ALB).
 * 2. If the request connects directly from an untrusted client, all forwarding headers are discarded and remoteAddr is enforced.
 * 3. Safely sanitizes IP addresses against header injection, port numbers, IPv6 brackets, and malformed strings.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private final TrustedProxyProperties properties;

    private final List<IpMatcher> trustedProxyMatchers = new ArrayList<>();

    private static final Pattern FORWARDED_FOR_PATTERN = Pattern.compile("for=\"?\\[?([^\";\\]\\s,]+)\\]?\"?", Pattern.CASE_INSENSITIVE);
    private static final Pattern IPV4_PATTERN = Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.){3}(25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)$");
    private static final Pattern IPV6_PATTERN = Pattern.compile("^([0-9a-fA-F]{0,4}:){1,7}[0-9a-fA-F]{0,4}$");

    @PostConstruct
    public void init() {
        reloadMatchers();
    }

    public synchronized void reloadMatchers() {
        trustedProxyMatchers.clear();
        if (properties.getTrustedProxies() != null) {
            for (String cidrOrIp : properties.getTrustedProxies()) {
                try {
                    trustedProxyMatchers.add(parseIpMatcher(cidrOrIp.trim()));
                } catch (Exception e) {
                    log.warn("Invalid trusted proxy CIDR or IP configuration '{}': {}", cidrOrIp, e.getMessage());
                }
            }
        }
        log.info("Initialized {} trusted proxy matchers for client IP resolution", trustedProxyMatchers.size());
    }

    /**
     * Resolves the verified client IP address from an incoming HTTP request.
     *
     * @param request the HttpServletRequest
     * @return the resolved, verified client IP (never null)
     */
    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }

        String remoteAddr = sanitizeIp(request.getRemoteAddr());
        if (!StringUtils.hasText(remoteAddr)) {
            remoteAddr = "127.0.0.1";
        }

        // If trusted proxy filtering is disabled or remoteAddr is trusted, parse proxy headers
        if (properties.isTrustAllProxies() || isTrustedProxy(remoteAddr)) {
            // 1. Cloudflare header priority (CF-Connecting-IP, True-Client-IP)
            if (properties.isCfHeaderEnabled()) {
                String cfIp = sanitizeIp(request.getHeader("CF-Connecting-IP"));
                if (isValidIp(cfIp)) {
                    return cfIp;
                }
                String trueClientIp = sanitizeIp(request.getHeader("True-Client-IP"));
                if (isValidIp(trueClientIp)) {
                    return trueClientIp;
                }
            }

            // 2. X-Forwarded-For traversal
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(xForwardedFor)) {
                String extracted = extractFromXForwardedFor(xForwardedFor);
                if (isValidIp(extracted)) {
                    return extracted;
                }
            }

            // 3. X-Real-IP
            String xRealIp = sanitizeIp(request.getHeader("X-Real-IP"));
            if (isValidIp(xRealIp)) {
                return xRealIp;
            }

            // 4. RFC 7239 Forwarded header
            String forwarded = request.getHeader("Forwarded");
            if (StringUtils.hasText(forwarded)) {
                String extracted = extractFromForwarded(forwarded);
                if (isValidIp(extracted)) {
                    return extracted;
                }
            }
        }

        // Default to direct socket remote address
        return remoteAddr;
    }

    /**
     * Checks if a given IP address is within the configured trusted proxies.
     */
    public boolean isTrustedProxy(String ip) {
        if (!properties.isEnabled() || !StringUtils.hasText(ip)) {
            return false;
        }

        String cleanIp = sanitizeIp(ip);
        if (!isValidIp(cleanIp)) {
            return false;
        }

        try {
            InetAddress address = InetAddress.getByName(cleanIp);
            for (IpMatcher matcher : trustedProxyMatchers) {
                if (matcher.matches(address)) {
                    return true;
                }
            }
        } catch (UnknownHostException ignored) {
            return false;
        }

        return false;
    }

    private String extractFromXForwardedFor(String xForwardedFor) {
        String[] parts = xForwardedFor.split(",");
        List<String> validIps = new ArrayList<>();
        for (String part : parts) {
            String clean = sanitizeIp(part);
            if (isValidIp(clean)) {
                validIps.add(clean);
            }
        }

        if (validIps.isEmpty()) {
            return null;
        }

        // Traverse right-to-left: discard trusted reverse proxies and pick the rightmost untrusted client IP
        for (int i = validIps.size() - 1; i >= 0; i--) {
            String candidate = validIps.get(i);
            if (!isTrustedProxy(candidate)) {
                return candidate;
            }
        }

        // If all IPs in chain are in trusted proxy range, pick the original leftmost IP
        return validIps.get(0);
    }

    private String extractFromForwarded(String forwardedHeader) {
        Matcher matcher = FORWARDED_FOR_PATTERN.matcher(forwardedHeader);
        while (matcher.find()) {
            String candidate = sanitizeIp(matcher.group(1));
            if (isValidIp(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public String sanitizeIp(String rawIp) {
        if (!StringUtils.hasText(rawIp)) {
            return null;
        }

        String ip = rawIp.trim();

        // Strip enclosing IPv6 brackets e.g. [::1] or [2001:db8::1]
        if (ip.startsWith("[") && ip.contains("]")) {
            int closeBracket = ip.indexOf(']');
            ip = ip.substring(1, closeBracket);
        } else if (ip.contains(":") && ip.indexOf(':') == ip.lastIndexOf(':')) {
            // IPv4 with port e.g. 192.168.1.1:8080
            ip = ip.substring(0, ip.indexOf(':'));
        }

        // Normalize localhost representations
        if ("localhost".equalsIgnoreCase(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "127.0.0.1";
        }

        return ip;
    }

    public boolean isValidIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        if ("localhost".equalsIgnoreCase(ip)) {
            return true;
        }
        if (IPV4_PATTERN.matcher(ip).matches()) {
            return true;
        }
        if (ip.contains(":") && (IPV6_PATTERN.matcher(ip).matches() || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip))) {
            return true;
        }
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private IpMatcher parseIpMatcher(String cidrOrIp) throws UnknownHostException {
        if (cidrOrIp.contains("/")) {
            String[] parts = cidrOrIp.split("/");
            InetAddress baseAddress = InetAddress.getByName(parts[0].trim());
            int prefixLength = Integer.parseInt(parts[1].trim());
            return new CidrMatcher(baseAddress, prefixLength);
        } else {
            InetAddress exactAddress = InetAddress.getByName(cidrOrIp.trim());
            return new ExactIpMatcher(exactAddress);
        }
    }

    // =========================================================================
    // IP Matcher Internal Abstractions
    // =========================================================================

    private interface IpMatcher {
        boolean matches(InetAddress address);
    }

    private static class ExactIpMatcher implements IpMatcher {
        private final byte[] targetBytes;

        public ExactIpMatcher(InetAddress address) {
            this.targetBytes = address.getAddress();
        }

        @Override
        public boolean matches(InetAddress address) {
            byte[] addrBytes = address.getAddress();
            if (addrBytes.length != targetBytes.length) {
                return false;
            }
            for (int i = 0; i < targetBytes.length; i++) {
                if (targetBytes[i] != addrBytes[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    private static class CidrMatcher implements IpMatcher {
        private final byte[] networkBytes;
        private final int prefixLength;

        public CidrMatcher(InetAddress baseAddress, int prefixLength) {
            this.networkBytes = baseAddress.getAddress();
            this.prefixLength = prefixLength;
        }

        @Override
        public boolean matches(InetAddress address) {
            byte[] addrBytes = address.getAddress();
            if (addrBytes.length != networkBytes.length) {
                return false;
            }

            int fullBytes = prefixLength / 8;
            for (int i = 0; i < fullBytes && i < networkBytes.length; i++) {
                if (addrBytes[i] != networkBytes[i]) {
                    return false;
                }
            }

            int remainingBits = prefixLength % 8;
            if (remainingBits > 0 && fullBytes < networkBytes.length) {
                int mask = (0xFF << (8 - remainingBits)) & 0xFF;
                return (addrBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
            }

            return true;
        }
    }
}
