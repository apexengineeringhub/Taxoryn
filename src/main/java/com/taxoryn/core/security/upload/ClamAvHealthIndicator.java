package com.taxoryn.core.security.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Spring Boot Actuator health indicator for ClamAV daemon connectivity.
 * Safe and production-ready: never exposes credentials or sensitive system information.
 */
@Slf4j
@Component("clamAvHealthIndicator")
@RequiredArgsConstructor
public class ClamAvHealthIndicator implements HealthIndicator {

    private final MalwareScannerProperties properties;

    @Override
    public Health health() {
        if (properties == null || properties.getClamav() == null || !properties.getClamav().isEnabled()) {
            return Health.up()
                    .withDetail("provider", "ClamAV")
                    .withDetail("enabled", false)
                    .withDetail("status", "INACTIVE (Dev mode)")
                    .build();
        }

        MalwareScannerProperties.ClamAvConfig config = properties.getClamav();
        String host = StringUtils.hasText(config.getHost()) ? config.getHost().trim() : "localhost";
        int port = config.getPort() > 0 ? config.getPort() : 3310;
        int timeout = config.getConnectionTimeoutMs() > 0 ? config.getConnectionTimeoutMs() : 2000;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            socket.setSoTimeout(timeout);

            try (OutputStream out = socket.getOutputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))) {

                // ClamAV PING command: "zPING\0"
                out.write("zPING\0".getBytes(StandardCharsets.US_ASCII));
                out.flush();

                String response = reader.readLine();
                if (response != null && response.trim().contains("PONG")) {
                    return Health.up()
                            .withDetail("provider", "ClamAV")
                            .withDetail("host", host)
                            .withDetail("port", port)
                            .withDetail("ping", "PONG")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("provider", "ClamAV")
                            .withDetail("host", host)
                            .withDetail("port", port)
                            .withDetail("error", "Unexpected response: " + response)
                            .build();
                }
            }
        } catch (Exception e) {
            log.warn("ClamAV health indicator ping failed for {}:{}: {}", host, port, e.getMessage());
            return Health.down()
                    .withDetail("provider", "ClamAV")
                    .withDetail("host", host)
                    .withDetail("port", port)
                    .withDetail("error", "ClamAV daemon unreachable: " + e.getMessage())
                    .build();
        }
    }
}
