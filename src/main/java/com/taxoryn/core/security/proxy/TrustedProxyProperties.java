package com.taxoryn.core.security.proxy;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Trusted Reverse Proxy identification and Client IP extraction.
 * <p>
 * Controls whether client-supplied forwarding headers (X-Forwarded-For, X-Real-IP, CF-Connecting-IP)
 * are accepted based on whether the connecting TCP remote socket matches a known trusted proxy CIDR range.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "taxoryn.security.trusted-proxy")
public class TrustedProxyProperties {

    /**
     * Whether trusted proxy processing is enabled.
     * When true, forwarding headers are only parsed if the direct connection originates from a trusted proxy.
     */
    private boolean enabled = true;

    /**
     * List of trusted CIDRs or IP addresses for reverse proxies / load balancers.
     * Default includes:
     * - Loopback (127.0.0.1/32, ::1/128)
     * - RFC 1918 Private subnets used by Render, Docker, Kubernetes, AWS VPCs (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
     * - Link-local / Cloud metadata subnets (169.254.0.0/16)
     * - IPv6 Unique Local / Link Local (fc00::/7, fe80::/10)
     */
    private List<String> trustedProxies = new ArrayList<>(List.of(
            "127.0.0.1/32",
            "127.0.0.0/8",
            "::1/128",
            "0:0:0:0:0:0:0:1/128",
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16",
            "169.254.0.0/16",
            "fc00::/7",
            "fe80::/10"
    ));

    /**
     * If true, unconditionally trust forwarding headers regardless of remote address.
     * STRICT WARNING: Must remain false in production to prevent IP spoofing attacks.
     */
    private boolean trustAllProxies = false;

    /**
     * Whether Cloudflare specific headers (CF-Connecting-IP, True-Client-IP) should be prioritized
     * when the request arrives from a trusted proxy.
     */
    private boolean cfHeaderEnabled = true;
}
