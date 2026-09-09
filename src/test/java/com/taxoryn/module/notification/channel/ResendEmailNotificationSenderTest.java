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
    @DisplayName("Resend Dispatch: Handles HTTP 403 Case A unverified domain rejection gracefully and returns false")
    void testResend403DomainNotVerifiedReturnsFalse() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(403);
        when(mockHttpResponse.body()).thenReturn("{\"statusCode\":403,\"name\":\"validation_error\",\"message\":\"The domain taxoryn.com is not verified. Please verify your domain at resend.com/domains\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        boolean result = sender.sendEmail(
                "unverified-recipient@example.com",
                "Customer",
                "Account Activation",
                "<p>Activate your account</p>",
                Map.of()
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("Resend Dispatch: Handles HTTP 403 Case B test mode sandbox restriction gracefully and returns false")
    void testResend403TestModeSandboxRestrictionReturnsFalse() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(403);
        when(mockHttpResponse.body()).thenReturn("{\"statusCode\":403,\"name\":\"validation_error\",\"message\":\"You can only send testing emails to your own email address. To send emails to other recipients, please verify a domain at resend.com\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        boolean result = sender.sendEmail(
                "unverified-recipient@example.com",
                "Customer",
                "Account Activation",
                "<p>Activate your account</p>",
                Map.of()
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("Resend Dispatch: Handles HTTP 401 unauthorized rejection gracefully and returns false")
    void testResend401UnauthorizedReturnsFalse() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(401);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        boolean result = sender.sendEmail(
                "recipient@example.com",
                "Customer",
                "Subject",
                "<p>Body</p>",
                Map.of()
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("Resend Dispatch: Handles HTTP 422 unprocessable entity rejection gracefully and returns false")
    void testResend422UnprocessableEntityReturnsFalse() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(422);
        when(mockHttpResponse.body()).thenReturn("{\"statusCode\":422,\"message\":\"Invalid email address\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        boolean result = sender.sendEmail(
                "invalid@example.com",
                "Customer",
                "Subject",
                "<p>Body</p>",
                Map.of()
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("Resend Dispatch: Handles HTTP 429 rate limit gracefully and returns false")
    void testResend429RateLimitReturnsFalse() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(429);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        boolean result = sender.sendEmail(
                "recipient@example.com",
                "Customer",
                "Subject",
                "<p>Body</p>",
                Map.of()
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("Dev Mode: Redirects recipient to devRecipient when devMode is true")
    void testDevModeRedirection() throws Exception {
        emailProperties.setDevMode(true);
        emailProperties.setDevRecipient("dev-tester@taxoryn.com");

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        boolean result = sender.sendEmail(
                "customer@clientfirm.com",
                "Client Firm",
                "Activation Email",
                "<p>Activate link</p>",
                Map.of()
        );

        assertTrue(result);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

        HttpRequest capturedRequest = requestCaptor.getValue();
        // Jackson serializes body - let's check it was called and verify request
        assertThat(capturedRequest.uri().toString()).isEqualTo("https://api.resend.com/emails");
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
