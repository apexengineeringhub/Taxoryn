package com.taxoryn.module.notification.email.template;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
public class EmailTemplateRenderer {

    public String renderSubject(EmailTemplateType type, Map<String, Object> data) {
        String template = type.getDefaultSubject();
        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String val = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
                template = template.replace("{{" + entry.getKey() + "}}", val);
            }
        }
        return template;
    }

    public String renderHtml(EmailTemplateType type, Map<String, Object> data) {
        return switch (type) {
            case WELCOME_PRACTITIONER -> renderPractitionerWelcomeHtml(data);
            case WELCOME_INDIVIDUAL -> renderIndividualWelcomeHtml(data);
            case INVOICE_ISSUED -> renderInvoiceIssuedHtml(data);
            case PAYMENT_RECEIVED -> renderPaymentReceivedHtml(data);
            case INVOICE_REMINDER -> renderInvoiceReminderHtml(data);
            case PASSWORD_RESET -> renderPasswordResetHtml(data);
            case DOCUMENT_REQUEST -> renderDocumentRequestHtml(data);
            case DOCUMENT_REMINDER -> renderDocumentReminderHtml(data);
            case DOCUMENT_REJECTED -> renderDocumentRejectedHtml(data);
        };
    }

    private String renderPractitionerWelcomeHtml(Map<String, Object> data) {
        String name = getString(data, "name", "Practitioner");
        String practiceName = getString(data, "practiceName", "Your Practice");
        String email = getString(data, "email", "");
        String mobile = getString(data, "mobile", "");
        String loginUrl = getString(data, "loginUrl", "https://taxoryn.com/login");

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Welcome to Taxoryn</title>
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 0; color: #1e293b; }
            .container { max-width: 600px; margin: 30px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03); border: 1px solid #e2e8f0; }
            .header { background: linear-gradient(135deg, #082e5b 0%%, #07152b 100%%); padding: 32px 40px; text-align: left; }
            .logo { font-size: 24px; font-weight: 900; color: #ffffff; letter-spacing: 2px; display: inline-flex; align-items: center; }
            .logo-accent { color: #00d1a3; }
            .logo-badge { background: #00d1a3; color: #07152b; font-size: 10px; font-weight: 800; padding: 3px 10px; border-radius: 9999px; text-transform: uppercase; margin-left: 12px; letter-spacing: 0.5px; }
            .motto-bar { color: #94a3b8; font-size: 9px; font-weight: 700; letter-spacing: 1.5px; text-transform: uppercase; margin-top: 6px; }
            .content { padding: 36px 40px; }
            h1 { font-size: 22px; font-weight: 800; color: #082e5b; margin-top: 0; margin-bottom: 16px; }
            p { font-size: 15px; line-height: 1.6; color: #475569; margin: 0 0 16px; }
            .account-box { background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; padding: 20px; margin: 24px 0; }
            .account-item { display: flex; justify-content: space-between; font-size: 14px; margin-bottom: 8px; }
            .account-item:last-child { margin-bottom: 0; }
            .account-label { color: #64748b; font-weight: 500; }
            .account-value { color: #082e5b; font-weight: 700; }
            .feature-list { margin: 24px 0; padding-left: 0; list-style: none; }
            .feature-item { font-size: 14px; color: #334155; margin-bottom: 10px; display: flex; align-items: flex-start; }
            .feature-icon { color: #00d1a3; margin-right: 10px; font-weight: bold; }
            .btn-wrapper { text-align: center; margin: 32px 0 16px; }
            .btn { background-color: #00d1a3; color: #07152b !important; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 800; font-size: 15px; display: inline-block; box-shadow: 0 2px 4px rgba(0, 209, 163, 0.2); }
            .btn:hover { background-color: #00b388; }
            .footer { background: #f8fafc; padding: 28px 40px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
            .footer-brand { font-weight: 900; font-size: 14px; color: #082e5b; letter-spacing: 1.5px; margin-bottom: 2px; }
            .footer-motto { font-weight: 700; font-size: 9px; color: #00b388; letter-spacing: 1.8px; text-transform: uppercase; margin-bottom: 12px; }
            .footer a { color: #64748b; text-decoration: underline; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <div class="logo">
                TAXO<span class="logo-accent">RYN</span>
                <span class="logo-badge">Practitioner Suite</span>
              </div>
              <div class="motto-bar">SIMPLIFYING TAX PRACTICE MANAGEMENT</div>
            </div>
            <div class="content">
              <h1>Welcome aboard, %s! 👋</h1>
              <p>Your practice workspace for <strong>%s</strong> has been successfully configured on the Taxoryn Platform.</p>
              
              <div class="account-box">
                <div class="account-item">
                  <span class="account-label">Practice Name:</span>
                  <span class="account-value">%s</span>
                </div>
                <div class="account-item">
                  <span class="account-label">Registered Admin Email:</span>
                  <span class="account-value">%s</span>
                </div>
                <div class="account-item">
                  <span class="account-label">Registered Mobile / WhatsApp:</span>
                  <span class="account-value">%s</span>
                </div>
                <div class="account-item">
                  <span class="account-label">Subscription Tier:</span>
                  <span class="account-value" style="color: #00b388;">Starter Tier (Active)</span>
                </div>
              </div>

              <p><strong>What you can do right now with Taxoryn:</strong></p>
              <ul class="feature-list">
                <li class="feature-item"><span class="feature-icon">✓</span> <strong>Client 360° Management:</strong> Onboard clients, assign PAN/GSTIN profiles, and delegate tasks to staff.</li>
                <li class="feature-item"><span class="feature-icon">✓</span> <strong>Compliance Pipelines:</strong> Track GSTR-1, 3B, ITR-1 to 7, and TDS 24Q/26Q filings seamlessly.</li>
                <li class="feature-item"><span class="feature-icon">✓</span> <strong>Professional Invoicing:</strong> Generate branded tax invoices with automated WhatsApp & email alerts.</li>
                <li class="feature-item"><span class="feature-icon">✓</span> <strong>Marketplace Discovery:</strong> Publish your verified practice profile to attract new tax and audit clients.</li>
              </ul>

              <div class="btn-wrapper">
                <a href="%s" class="btn" target="_blank">Access Practice Dashboard &rarr;</a>
              </div>

              <p style="font-size: 13px; color: #64748b; text-align: center; margin-top: 20px;">
                Direct link: <a href="%s" style="color: #082e5b; font-weight: 600;">%s</a>
              </p>
            </div>
            <div class="footer">
              <div class="footer-brand">TAXO<span style="color: #00d1a3;">RYN</span></div>
              <div class="footer-motto">SIMPLIFYING TAX PRACTICE MANAGEMENT</div>
              <p>&copy; 2026 Taxoryn Technologies Pvt Ltd. All rights reserved.</p>
              <p>Need assistance? Contact our team at <a href="mailto:support@taxoryn.com">support@taxoryn.com</a></p>
            </div>
          </div>
        </body>
        </html>
        """.formatted(escape(name), escape(practiceName), escape(practiceName), escape(email), escape(mobile), escape(loginUrl), escape(loginUrl), escape(loginUrl));
    }

    private String renderIndividualWelcomeHtml(Map<String, Object> data) {
        String name = getString(data, "name", "Valued Customer");
        String email = getString(data, "email", "");
        String mobile = getString(data, "mobile", "");
        String loginUrl = getString(data, "loginUrl", "https://taxoryn.com/login");

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Welcome to Taxoryn</title>
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 0; color: #1e293b; }
            .container { max-width: 600px; margin: 30px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03); border: 1px solid #e2e8f0; }
            .header { background: linear-gradient(135deg, #082e5b 0%%, #07152b 100%%); padding: 32px 40px; text-align: left; }
            .logo { font-size: 24px; font-weight: 900; color: #ffffff; letter-spacing: 2px; display: inline-flex; align-items: center; }
            .logo-accent { color: #00d1a3; }
            .logo-badge { background: #ffffff; color: #082e5b; font-size: 10px; font-weight: 800; padding: 3px 10px; border-radius: 9999px; text-transform: uppercase; margin-left: 12px; letter-spacing: 0.5px; }
            .motto-bar { color: #94a3b8; font-size: 9px; font-weight: 700; letter-spacing: 1.5px; text-transform: uppercase; margin-top: 6px; }
            .content { padding: 36px 40px; }
            h1 { font-size: 22px; font-weight: 800; color: #082e5b; margin-top: 0; margin-bottom: 16px; }
            p { font-size: 15px; line-height: 1.6; color: #475569; margin: 0 0 16px; }
            .account-box { background: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; padding: 20px; margin: 24px 0; }
            .account-item { display: flex; justify-content: space-between; font-size: 14px; margin-bottom: 8px; }
            .account-item:last-child { margin-bottom: 0; }
            .account-label { color: #64748b; font-weight: 500; }
            .account-value { color: #082e5b; font-weight: 700; }
            .feature-list { margin: 24px 0; padding-left: 0; list-style: none; }
            .feature-item { font-size: 14px; color: #334155; margin-bottom: 10px; display: flex; align-items: flex-start; }
            .feature-icon { color: #00d1a3; margin-right: 10px; font-weight: bold; }
            .btn-wrapper { text-align: center; margin: 32px 0 16px; }
            .btn { background-color: #00d1a3; color: #07152b !important; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 800; font-size: 15px; display: inline-block; }
            .footer { background: #f8fafc; padding: 28px 40px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
            .footer-brand { font-weight: 900; font-size: 14px; color: #082e5b; letter-spacing: 1.5px; margin-bottom: 2px; }
            .footer-motto { font-weight: 700; font-size: 9px; color: #00b388; letter-spacing: 1.8px; text-transform: uppercase; margin-bottom: 12px; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <div class="logo">
                TAXO<span class="logo-accent">RYN</span>
                <span class="logo-badge">Customer Portal</span>
              </div>
              <div class="motto-bar">SIMPLIFYING TAX PRACTICE MANAGEMENT</div>
            </div>
            <div class="content">
              <h1>Welcome, %s! 🎉</h1>
              <p>Your Taxoryn taxpayer account is now ready. You can now hire verified Chartered Accountants, track tax filings, and manage compliance from a single secure portal.</p>
              
              <div class="account-box">
                <div class="account-item">
                  <span class="account-label">Email:</span>
                  <span class="account-value">%s</span>
                </div>
                <div class="account-item">
                  <span class="account-label">Mobile Number:</span>
                  <span class="account-value">%s</span>
                </div>
              </div>

              <p><strong>Explore what you can do on Taxoryn:</strong></p>
              <ul class="feature-list">
                <li class="feature-item"><span class="feature-icon">✓</span> <strong>Find Verified CAs & CSs:</strong> Explore top-rated tax professionals near you with transparent pricing.</li>
                <li class="feature-item"><span class="feature-icon">✓</span> <strong>Post Tax Requirements:</strong> Get proposals for ITR filing, GST returns, business setup, and notice resolution.</li>
                <li class="feature-item"><span class="feature-icon">✓</span> <strong>Document Vault:</strong> Securely share financial records and Form 16/26AS with end-to-end encryption.</li>
              </ul>

              <div class="btn-wrapper">
                <a href="%s" class="btn" target="_blank">Access Your Account &rarr;</a>
              </div>
            </div>
            <div class="footer">
              <div class="footer-brand">TAXO<span style="color: #00d1a3;">RYN</span></div>
              <div class="footer-motto">SIMPLIFYING TAX PRACTICE MANAGEMENT</div>
              <p>&copy; 2026 Taxoryn Technologies Pvt Ltd. All rights reserved.</p>
              <p>Questions? Contact us at <a href="mailto:support@taxoryn.com">support@taxoryn.com</a></p>
            </div>
          </div>
        </body>
        </html>
        """.formatted(escape(name), escape(email), escape(mobile), escape(loginUrl));
    }

    private String renderInvoiceIssuedHtml(Map<String, Object> data) {
        String clientName = getString(data, "clientName", "Client");
        String invoiceNumber = getString(data, "invoiceNumber", "");
        String practiceName = getString(data, "organizationName", "Tax Practice");
        String totalAmount = getString(data, "totalAmount", "0.00");
        String dueDate = getString(data, "dueDate", "");
        String invoiceUrl = getString(data, "invoiceUrl", "https://taxoryn.com/login");

        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="utf-8"><title>Invoice %s</title></head>
        <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f8fafc; padding: 20px;">
          <div style="max-width: 600px; margin: 0 auto; background: #fff; border-radius: 12px; padding: 32px; border: 1px solid #e2e8f0;">
            <div style="font-weight: 900; font-size: 20px; color: #082e5b; letter-spacing: 1px; margin-bottom: 2px;">TAXO<span style="color: #00d1a3;">RYN</span></div>
            <div style="font-weight: 700; font-size: 8px; color: #00b388; letter-spacing: 1.5px; text-transform: uppercase; margin-bottom: 20px;">SIMPLIFYING TAX PRACTICE MANAGEMENT</div>
            <h2 style="color: #082e5b; margin-top: 0;">New Invoice from %s</h2>
            <p style="color: #475569;">Dear %s,</p>
            <p style="color: #475569;">An invoice has been issued for your professional tax and compliance services.</p>
            <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin: 20px 0;">
              <p style="margin: 4px 0;"><strong>Invoice Number:</strong> %s</p>
              <p style="margin: 4px 0;"><strong>Total Amount:</strong> ₹%s</p>
              <p style="margin: 4px 0;"><strong>Due Date:</strong> %s</p>
            </div>
            <p><a href="%s" style="background: #00d1a3; color: #07152b; font-weight: 800; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;">View Invoice &rarr;</a></p>
            <hr style="border: none; border-top: 1px solid #e2e8f0; margin-top: 30px;">
            <p style="font-size: 11px; color: #94a3b8; text-align: center; margin-bottom: 0;">&copy; 2026 Taxoryn Technologies Pvt Ltd.</p>
          </div>
        </body>
        </html>
        """.formatted(escape(invoiceNumber), escape(practiceName), escape(clientName), escape(invoiceNumber), escape(totalAmount), escape(dueDate), escape(invoiceUrl));
    }

    private String renderPaymentReceivedHtml(Map<String, Object> data) {
        String clientName = getString(data, "clientName", "Client");
        String invoiceNumber = getString(data, "invoiceNumber", "");
        String amountPaid = getString(data, "amountPaid", "0.00");
        String remainingBalance = getString(data, "remainingBalance", "0.00");
        String paymentReference = getString(data, "paymentReference", "N/A");

        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="utf-8"><title>Payment Receipt</title></head>
        <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f8fafc; padding: 20px;">
          <div style="max-width: 600px; margin: 0 auto; background: #fff; border-radius: 12px; padding: 32px; border: 1px solid #e2e8f0;">
            <div style="font-weight: 900; font-size: 20px; color: #082e5b; letter-spacing: 1px; margin-bottom: 2px;">TAXO<span style="color: #00d1a3;">RYN</span></div>
            <div style="font-weight: 700; font-size: 8px; color: #00b388; letter-spacing: 1.5px; text-transform: uppercase; margin-bottom: 20px;">SIMPLIFYING TAX PRACTICE MANAGEMENT</div>
            <h2 style="color: #00b388; margin-top: 0;">Payment Received Successfully</h2>
            <p style="color: #475569;">Dear %s,</p>
            <p style="color: #475569;">We have successfully recorded your payment of <strong>₹%s</strong> for Invoice <strong>%s</strong>.</p>
            <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin: 20px 0;">
              <p style="margin: 4px 0;"><strong>Reference:</strong> %s</p>
              <p style="margin: 4px 0;"><strong>Remaining Balance:</strong> ₹%s</p>
            </div>
            <hr style="border: none; border-top: 1px solid #e2e8f0; margin-top: 30px;">
            <p style="font-size: 11px; color: #94a3b8; text-align: center; margin-bottom: 0;">&copy; 2026 Taxoryn Technologies Pvt Ltd.</p>
          </div>
        </body>
        </html>
        """.formatted(escape(clientName), escape(amountPaid), escape(invoiceNumber), escape(paymentReference), escape(remainingBalance));
    }

    private String renderInvoiceReminderHtml(Map<String, Object> data) {
        String clientName = getString(data, "clientName", "Client");
        String invoiceNumber = getString(data, "invoiceNumber", "");
        String balanceAmount = getString(data, "balanceAmount", "0.00");
        String dueDate = getString(data, "dueDate", "Overdue");
        String invoiceUrl = getString(data, "invoiceUrl", "https://taxoryn.com/login");

        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="utf-8"><title>Payment Reminder</title></head>
        <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f8fafc; padding: 20px;">
          <div style="max-width: 600px; margin: 0 auto; background: #fff; border-radius: 12px; padding: 32px; border: 1px solid #e2e8f0;">
            <div style="font-weight: 900; font-size: 20px; color: #082e5b; letter-spacing: 1px; margin-bottom: 2px;">TAXO<span style="color: #00d1a3;">RYN</span></div>
            <div style="font-weight: 700; font-size: 8px; color: #00b388; letter-spacing: 1.5px; text-transform: uppercase; margin-bottom: 20px;">SIMPLIFYING TAX PRACTICE MANAGEMENT</div>
            <h2 style="color: #d97706; margin-top: 0;">Payment Reminder: Invoice %s</h2>
            <p style="color: #475569;">Dear %s,</p>
            <p style="color: #475569;">This is a friendly reminder that an outstanding balance of <strong>₹%s</strong> is due for Invoice <strong>%s</strong>.</p>
            <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin: 20px 0;">
              <p style="margin: 4px 0;"><strong>Due Date:</strong> %s</p>
            </div>
            <p><a href="%s" style="background: #082e5b; color: #fff; font-weight: 800; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;">Pay Now &rarr;</a></p>
            <hr style="border: none; border-top: 1px solid #e2e8f0; margin-top: 30px;">
            <p style="font-size: 11px; color: #94a3b8; text-align: center; margin-bottom: 0;">&copy; 2026 Taxoryn Technologies Pvt Ltd.</p>
          </div>
        </body>
        </html>
        """.formatted(escape(invoiceNumber), escape(clientName), escape(balanceAmount), escape(invoiceNumber), escape(dueDate), escape(invoiceUrl));
    }

    private String renderPasswordResetHtml(Map<String, Object> data) {
        String name = getString(data, "name", "Taxoryn User");
        String resetUrl = getString(data, "resetUrl", "https://taxoryn.com/reset-password");
        String expiryMinutes = getString(data, "expiryMinutes", "30");

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Reset Your Taxoryn Password</title>
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 0; color: #1e293b; }
            .container { max-width: 600px; margin: 30px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03); border: 1px solid #e2e8f0; }
            .header { background: linear-gradient(135deg, #082e5b 0%%, #07152b 100%%); padding: 32px 40px; text-align: left; }
            .logo { font-size: 24px; font-weight: 900; color: #ffffff; letter-spacing: 2px; display: inline-flex; align-items: center; }
            .logo-accent { color: #00d1a3; }
            .logo-badge { background: #00d1a3; color: #07152b; font-size: 10px; font-weight: 800; padding: 3px 10px; border-radius: 9999px; text-transform: uppercase; margin-left: 12px; letter-spacing: 0.5px; }
            .motto-bar { color: #94a3b8; font-size: 9px; font-weight: 700; letter-spacing: 1.5px; text-transform: uppercase; margin-top: 6px; }
            .content { padding: 36px 40px; }
            h1 { font-size: 22px; font-weight: 800; color: #082e5b; margin-top: 0; margin-bottom: 16px; }
            p { font-size: 15px; line-height: 1.6; color: #475569; margin: 0 0 16px; }
            .btn-wrapper { text-align: center; margin: 32px 0; }
            .btn { background-color: #00d1a3; color: #07152b !important; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 800; font-size: 15px; display: inline-block; box-shadow: 0 2px 4px rgba(0, 209, 163, 0.2); }
            .btn:hover { background-color: #00b388; }
            .security-box { background: #f8fafc; border-left: 4px solid #00d1a3; border-radius: 4px; padding: 16px; margin: 24px 0; font-size: 13px; color: #64748b; }
            .raw-link { word-break: break-all; font-size: 12px; color: #0284c7; }
            .footer { background: #f8fafc; padding: 28px 40px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
            .footer-brand { font-weight: 900; font-size: 14px; color: #082e5b; letter-spacing: 1.5px; margin-bottom: 2px; }
            .footer-motto { font-weight: 700; font-size: 9px; color: #00b388; letter-spacing: 1.8px; text-transform: uppercase; margin-bottom: 12px; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <div class="logo">
                TAXO<span class="logo-accent">RYN</span>
                <span class="logo-badge">Security</span>
              </div>
              <div class="motto-bar">SIMPLIFYING TAX PRACTICE MANAGEMENT</div>
            </div>
            <div class="content">
              <h1>Password Reset Request</h1>
              <p>Hello <strong>%s</strong>,</p>
              <p>We received a request to reset the password for your Taxoryn account. Click the button below to establish a new password:</p>
              
              <div class="btn-wrapper">
                <a href="%s" class="btn">Reset My Password &rarr;</a>
              </div>

              <div class="security-box">
                <strong>Important Security Notice:</strong>
                <ul style="margin: 6px 0 0; padding-left: 18px;">
                  <li>This link will expire in <strong>%s minutes</strong> and can only be used once.</li>
                  <li>If you did not request this password reset, please ignore this email or contact support immediately. Your password remains safe.</li>
                </ul>
              </div>

              <p style="font-size: 12px; color: #94a3b8; margin-top: 24px;">
                If the button above does not work, copy and paste this link into your browser:<br>
                <a href="%s" class="raw-link">%s</a>
              </p>
            </div>
            <div class="footer">
              <div class="footer-brand">TAXORYN</div>
              <div class="footer-motto">SIMPLIFYING TAX PRACTICE MANAGEMENT</div>
              <p>&copy; 2026 Taxoryn Technologies Pvt Ltd. All rights reserved.</p>
            </div>
          </div>
        </body>
        </html>
        """.formatted(escape(name), escape(resetUrl), escape(expiryMinutes), escape(resetUrl), escape(resetUrl));
    }

    private String renderDocumentRequestHtml(Map<String, Object> data) {
        String name = getString(data, "name", "Valued Client");
        String purpose = getString(data, "purpose", "Tax Preparation");
        String practiceName = getString(data, "practiceName", "Your Tax Consultant");
        String dueDate = getString(data, "dueDate", "Promptly");
        String message = getString(data, "message", "");
        String itemsListHtml = getString(data, "itemsListHtml", "<li>Standard tax compliance documents</li>");
        String uploadUrl = getString(data, "uploadUrl", "https://taxoryn.com/login");

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Documents Required — %s</title>
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 0; color: #1e293b; }
            .container { max-width: 600px; margin: 30px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03); border: 1px solid #e2e8f0; }
            .header { background: linear-gradient(135deg, #082e5b 0%%, #07152b 100%%); padding: 32px 40px; text-align: left; }
            .logo { font-size: 24px; font-weight: 900; color: #ffffff; letter-spacing: 2px; display: inline-flex; align-items: center; }
            .logo-accent { color: #00d1a3; }
            .logo-badge { background: #00d1a3; color: #07152b; font-size: 10px; font-weight: 800; padding: 3px 10px; border-radius: 9999px; text-transform: uppercase; margin-left: 12px; letter-spacing: 0.5px; }
            .motto-bar { color: #94a3b8; font-size: 9px; font-weight: 700; letter-spacing: 1.5px; text-transform: uppercase; margin-top: 6px; }
            .content { padding: 36px 40px; }
            h1 { font-size: 20px; font-weight: 800; color: #082e5b; margin-top: 0; margin-bottom: 12px; }
            p { font-size: 14px; line-height: 1.6; color: #475569; margin: 0 0 16px; }
            .purpose-box { background: #f0fdf9; border-radius: 8px; border: 1px solid #bbf7d0; padding: 16px 20px; margin: 20px 0; }
            .purpose-title { font-size: 15px; font-weight: 800; color: #065f46; margin: 0 0 4px; }
            .due-info { font-size: 13px; color: #047857; font-weight: 600; }
            .message-box { background: #f8fafc; border-left: 4px solid #00d1a3; padding: 12px 16px; margin: 16px 0; font-size: 13px; color: #334155; font-style: italic; }
            .items-container { margin: 20px 0; }
            .items-title { font-size: 14px; font-weight: 700; color: #1e293b; margin-bottom: 8px; }
            .items-list { list-style: none; padding-left: 0; margin: 0; }
            .items-list li { padding: 8px 12px; margin-bottom: 6px; background: #f8fafc; border-radius: 6px; border: 1px solid #e2e8f0; font-size: 13px; font-weight: 600; color: #334155; display: flex; align-items: center; }
            .items-list li::before { content: "📄"; margin-right: 8px; }
            .btn-wrapper { text-align: center; margin: 30px 0 10px; }
            .btn { background-color: #00d1a3; color: #07152b !important; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 800; font-size: 14px; display: inline-block; box-shadow: 0 2px 4px rgba(0, 209, 163, 0.2); }
            .footer { background: #f8fafc; padding: 24px 40px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
            .footer-brand { font-weight: 900; font-size: 13px; color: #082e5b; letter-spacing: 1.5px; margin-bottom: 2px; }
            .footer-motto { font-weight: 700; font-size: 9px; color: #00b388; letter-spacing: 1.8px; text-transform: uppercase; margin-bottom: 10px; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <div class="logo">
                TAXO<span class="logo-accent">RYN</span>
                <span class="logo-badge">Client Portal</span>
              </div>
              <div class="motto-bar">SIMPLIFYING TAX PRACTICE MANAGEMENT</div>
            </div>
            <div class="content">
              <h1>Hello %s, 👋</h1>
              <p>Your tax consultant at <strong>%s</strong> has requested documents from you to complete your filing.</p>
              
              <div class="purpose-box">
                <div class="purpose-title">📋 Purpose: %s</div>
                <div class="due-info">⏰ Submission Due Date: <strong>%s</strong></div>
              </div>

              %s

              <div class="items-container">
                <div class="items-title">Requested Document Checklist:</div>
                <ul class="items-list">
                  %s
                </ul>
              </div>

              <div class="btn-wrapper">
                <a href="%s" class="btn">Upload Requested Documents →</a>
              </div>
            </div>
            <div class="footer">
              <div class="footer-brand">TAXORYN</div>
              <div class="footer-motto">SIMPLIFYING TAX PRACTICE MANAGEMENT</div>
              <p>&copy; 2026 Taxoryn Technologies Pvt Ltd. All rights reserved.</p>
            </div>
          </div>
        </body>
        </html>
        """.formatted(
                escape(purpose),
                escape(name),
                escape(practiceName),
                escape(purpose),
                escape(dueDate),
                StringUtils.hasText(message) ? "<div class=\"message-box\">\"" + escape(message) + "\"</div>" : "",
                itemsListHtml,
                escape(uploadUrl)
        );
    }

    private String renderDocumentReminderHtml(Map<String, Object> data) {
        String name = getString(data, "name", "Valued Client");
        String purpose = getString(data, "purpose", "Tax Preparation");
        String practiceName = getString(data, "practiceName", "Your Tax Consultant");
        String dueDate = getString(data, "dueDate", "Promptly");
        String itemsListHtml = getString(data, "itemsListHtml", "<li>Pending documents</li>");
        String uploadUrl = getString(data, "uploadUrl", "https://taxoryn.com/login");

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Reminder: Documents Required — %s</title>
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 0; color: #1e293b; }
            .container { max-width: 600px; margin: 30px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03); border: 1px solid #e2e8f0; }
            .header { background: linear-gradient(135deg, #b45309 0%%, #78350f 100%%); padding: 32px 40px; text-align: left; }
            .logo { font-size: 24px; font-weight: 900; color: #ffffff; letter-spacing: 2px; display: inline-flex; align-items: center; }
            .logo-accent { color: #fde047; }
            .logo-badge { background: #fde047; color: #78350f; font-size: 10px; font-weight: 800; padding: 3px 10px; border-radius: 9999px; text-transform: uppercase; margin-left: 12px; }
            .motto-bar { color: #fef08a; font-size: 9px; font-weight: 700; letter-spacing: 1.5px; text-transform: uppercase; margin-top: 6px; }
            .content { padding: 36px 40px; }
            h1 { font-size: 20px; font-weight: 800; color: #78350f; margin-top: 0; margin-bottom: 12px; }
            p { font-size: 14px; line-height: 1.6; color: #475569; margin: 0 0 16px; }
            .due-box { background: #fef3c7; border-radius: 8px; border: 1px solid #fde68a; padding: 16px 20px; margin: 20px 0; font-size: 14px; color: #92400e; font-weight: bold; }
            .items-list { list-style: none; padding-left: 0; margin: 16px 0; }
            .items-list li { padding: 8px 12px; margin-bottom: 6px; background: #fffbeb; border-radius: 6px; border: 1px solid #fef3c7; font-size: 13px; font-weight: 600; color: #78350f; }
            .btn-wrapper { text-align: center; margin: 30px 0 10px; }
            .btn { background-color: #d97706; color: #ffffff !important; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 800; font-size: 14px; display: inline-block; }
            .footer { background: #f8fafc; padding: 24px 40px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <div class="logo">
                TAXO<span class="logo-accent">RYN</span>
                <span class="logo-badge">Friendly Reminder</span>
              </div>
              <div class="motto-bar">SIMPLIFYING TAX PRACTICE MANAGEMENT</div>
            </div>
            <div class="content">
              <h1>Document Reminder for %s</h1>
              <p>This is a quick reminder from <strong>%s</strong> regarding pending documents for <strong>%s</strong>.</p>
              
              <div class="due-box">
                ⏰ Submission Due Date: %s
              </div>

              <p>Please upload the following pending items as soon as possible:</p>
              <ul class="items-list">
                %s
              </ul>

              <div class="btn-wrapper">
                <a href="%s" class="btn">Upload Pending Documents →</a>
              </div>
            </div>
            <div class="footer">
              <p>&copy; 2026 Taxoryn Technologies Pvt Ltd. All rights reserved.</p>
            </div>
          </div>
        </body>
        </html>
        """.formatted(
                escape(purpose),
                escape(name),
                escape(practiceName),
                escape(purpose),
                escape(dueDate),
                itemsListHtml,
                escape(uploadUrl)
        );
    }

    private String renderDocumentRejectedHtml(Map<String, Object> data) {
        String name = getString(data, "name", "Valued Client");
        String documentTitle = getString(data, "documentTitle", "Document");
        String purpose = getString(data, "purpose", "Tax Preparation");
        String reason = getString(data, "reason", "Document could not be verified.");
        String practiceName = getString(data, "practiceName", "Your Tax Consultant");
        String uploadUrl = getString(data, "uploadUrl", "https://taxoryn.com/login");

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Document Needs Correction</title>
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 0; color: #1e293b; }
            .container { max-width: 600px; margin: 30px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03); border: 1px solid #e2e8f0; }
            .header { background: linear-gradient(135deg, #991b1b 0%%, #450a0a 100%%); padding: 32px 40px; text-align: left; }
            .logo { font-size: 24px; font-weight: 900; color: #ffffff; letter-spacing: 2px; }
            .content { padding: 36px 40px; }
            h1 { font-size: 20px; font-weight: 800; color: #991b1b; margin-top: 0; }
            p { font-size: 14px; line-height: 1.6; color: #475569; }
            .reason-box { background: #fef2f2; border-left: 4px solid #ef4444; padding: 16px; margin: 20px 0; border-radius: 4px; }
            .reason-title { font-weight: bold; color: #991b1b; margin-bottom: 4px; font-size: 13px; }
            .reason-text { font-size: 14px; color: #7f1d1d; }
            .btn-wrapper { text-align: center; margin: 30px 0 10px; }
            .btn { background-color: #ef4444; color: #ffffff !important; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 800; font-size: 14px; display: inline-block; }
            .footer { background: #f8fafc; padding: 24px 40px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <div class="logo">TAXORYN</div>
            </div>
            <div class="content">
              <h1>Action Required: Document Needs Correction</h1>
              <p>Hello %s,</p>
              <p>Your tax consultant at <strong>%s</strong> has reviewed the uploaded document <strong>%s</strong> for <strong>%s</strong> and indicated that a correction is required:</p>
              
              <div class="reason-box">
                <div class="reason-title">Reason from Practitioner:</div>
                <div class="reason-text">%s</div>
              </div>

              <p>Please upload a corrected replacement document through your Taxoryn Client Portal.</p>

              <div class="btn-wrapper">
                <a href="%s" class="btn">Upload Replacement Document →</a>
              </div>
            </div>
            <div class="footer">
              <p>&copy; 2026 Taxoryn Technologies Pvt Ltd. All rights reserved.</p>
            </div>
          </div>
        </body>
        </html>
        """.formatted(
                escape(name),
                escape(practiceName),
                escape(documentTitle),
                escape(purpose),
                escape(reason),
                escape(uploadUrl)
        );
    }

    private String getString(Map<String, Object> data, String key, String defaultValue) {
        if (data == null || !data.containsKey(key) || data.get(key) == null) {
            return defaultValue;
        }
        String val = String.valueOf(data.get(key));
        return StringUtils.hasText(val) ? val : defaultValue;
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
