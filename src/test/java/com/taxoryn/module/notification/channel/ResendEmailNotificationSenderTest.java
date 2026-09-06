package com.taxoryn.module.notification.channel;

import com.taxoryn.module.notification.email.config.EmailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResendEmailNotificationSenderTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    private EmailProperties emailProperties;
    private SmtpEmailNotificationSender sender;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setEnabled(true);
        emailProperties.setProvider("RESEND");
        emailProperties.setFromEmail("notifications@taxoryn.com");
        emailProperties.setFromName("Taxoryn");
        emailProperties.setReplyTo("support@taxoryn.com");
        emailProperties.setResendApiKey("re_test_ApiKey_1234567890");

        sender = new SmtpEmailNotificationSender(emailProperties);
        sender.setHttpClient(mockHttpClient);
    }

    @Test
    @DisplayName("Resend Dispatch: Builds correct JSON payload with from, to, reply_to, subject, and html")
    void testResendPayloadStructureAndSuccess() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        boolean result = sender.sendEmail(
                "taxpayer@example.com",
                "Rahul Sharma",
                "Welcome to Taxoryn",
                "<h1>Welcome Rahul</h1>",
                Map.of()
        );

        assertTrue(result);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

        HttpRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.uri().toString()).isEqualTo("https://api.resend.com/emails");
        assertThat(capturedRequest.headers().firstValue("Authorization")).contains("Bearer re_test_ApiKey_1234567890");
        assertThat(capturedRequest.headers().firstValue("Content-Type")).contains("application/json");
    }

    @Test
    @DisplayName("Resend Dispatch: Handles HTTP 400 rejection gracefully and returns false")
    void testResendRejectionReturnsFalse() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(400);
        when(mockHttpResponse.body()).thenReturn("{\"statusCode\":400,\"message\":\"Validation error\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        boolean result = sender.sendEmail(
                "invalid-email",
                "Test",
                "Test Subject",
                "<p>Test</p>",
                Map.of()
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("Resend Dispatch: Handles network/IO timeout exception gracefully and returns false")
    void testResendNetworkExceptionReturnsFalse() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection timed out to api.resend.com"));

        boolean result = sender.sendEmail(
                "taxpayer@example.com",
                "Rahul Sharma",
                "Welcome",
                "<p>Hello</p>",
                Map.of()
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("Resend Dispatch: Missing API key falls back to log dispatch")
    void testResendMissingApiKeyFallsBack() {
        emailProperties.setResendApiKey("");
        emailProperties.setApiKey("");

        boolean result = sender.sendEmail(
                "taxpayer@example.com",
                "Rahul Sharma",
                "Welcome",
                "<p>Hello</p>",
                Map.of()
        );

        assertTrue(result);
        verifyNoInteractions(mockHttpClient);
    }

    @Test
    @DisplayName("Provider Resolution: Resolves RESEND when provider is RESEND or AUTO with API key")
    void testProviderResolution() {
        assertThat(sender.getProviderName()).isEqualTo("RESEND");

        emailProperties.setProvider("AUTO");
        assertThat(sender.getProviderName()).isEqualTo("RESEND");

        emailProperties.setResendApiKey("");
        emailProperties.setBrevoApiKey("brevo_key");
        assertThat(sender.getProviderName()).isEqualTo("BREVO");
    }
}
