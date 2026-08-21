import { Invoice } from '../types';

function numberToWordsINR(num: number): string {
  if (!num || num <= 0) return 'Zero Rupees Only';
  const a = ['', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine', 'Ten', 'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen', 'Seventeen', 'Eighteen', 'Nineteen'];
  const b = ['', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety'];

  const inWords = (n: number): string => {
    if (n < 20) return a[n];
    if (n < 100) return b[Math.floor(n / 10)] + (n % 10 !== 0 ? ' ' + a[n % 10] : '');
    if (n < 1000) return a[Math.floor(n / 100)] + ' Hundred' + (n % 100 !== 0 ? ' and ' + inWords(n % 100) : '');
    if (n < 100000) return inWords(Math.floor(n / 1000)) + ' Thousand' + (n % 1000 !== 0 ? ' ' + inWords(n % 1000) : '');
    if (n < 10000000) return inWords(Math.floor(n / 100000)) + ' Lakh' + (n % 100000 !== 0 ? ' ' + inWords(n % 100000) : '');
    return inWords(Math.floor(n / 10000000)) + ' Crore' + (n % 10000000 !== 0 ? ' ' + inWords(n % 10000000) : '');
  };

  const integerPart = Math.floor(num);
  const words = inWords(integerPart);
  return `Rupees ${words} Only`;
}

export function printTaxInvoice(invoice: Invoice, practiceName: string = 'APEX TAX ADVISORS LLP') {
  // Remove any existing print iframe
  const existing = document.getElementById('taxoryn-print-frame');
  if (existing) {
    existing.remove();
  }

  const iframe = document.createElement('iframe');
  iframe.id = 'taxoryn-print-frame';
  iframe.style.position = 'fixed';
  iframe.style.right = '0';
  iframe.style.bottom = '0';
  iframe.style.width = '0';
  iframe.style.height = '0';
  iframe.style.border = '0';
  document.body.appendChild(iframe);

  const doc = iframe.contentWindow?.document;
  if (!doc) return;

  const currencyFmt = (val: number = 0) => {
    return '₹' + Number(val || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  };

  const items = (invoice.items && invoice.items.length > 0) ? invoice.items : [
    {
      service: 'TAX_ADVISORY' as any,
      description: 'Professional Legal & Tax Practice Services Rendered',
      hsnSacCode: '998231',
      quantity: 1,
      unitPrice: invoice.subtotal || invoice.total,
      taxRate: 18,
      amount: invoice.total,
    },
  ];

  const itemsRows = items.map((it, idx) => `
    <tr style="border-bottom: 1px solid #e2e8f0;">
      <td style="padding: 8px 10px; text-align: center; font-family: monospace; font-size: 11px; border-right: 1px solid #cbd5e1;">${idx + 1}</td>
      <td style="padding: 8px 10px; border-right: 1px solid #cbd5e1;">
        <div style="font-weight: 700; font-size: 12px; color: #0f172a;">${it.service || 'PROFESSIONAL_SERVICE'}</div>
        <div style="font-size: 11px; color: #475569; margin-top: 2px;">${it.description || 'Tax and compliance consulting services'}</div>
      </td>
      <td style="padding: 8px 10px; text-align: center; font-family: monospace; font-size: 11px; color: #475569; border-right: 1px solid #cbd5e1;">${it.hsnSacCode || '998231'}</td>
      <td style="padding: 8px 10px; text-align: center; font-family: monospace; font-size: 11px; border-right: 1px solid #cbd5e1;">${it.quantity || 1}</td>
      <td style="padding: 8px 10px; text-align: right; font-family: monospace; font-size: 11px; border-right: 1px solid #cbd5e1;">${currencyFmt(it.unitPrice)}</td>
      <td style="padding: 8px 10px; text-align: right; font-family: monospace; font-size: 11px; border-right: 1px solid #cbd5e1;">${currencyFmt(Number(it.quantity || 1) * Number(it.unitPrice || 0))}</td>
      <td style="padding: 8px 10px; text-align: center; font-family: monospace; font-size: 11px; border-right: 1px solid #cbd5e1;">${it.taxRate || 18}%</td>
      <td style="padding: 8px 10px; text-align: right; font-family: monospace; font-size: 12px; font-weight: 700; color: #0f172a;">${currencyFmt(it.amount || invoice.total)}</td>
    </tr>
  `).join('');

  const paymentsHtml = invoice.payments && invoice.payments.length > 0 ? `
    <div style="margin-top: 16px; padding: 10px 14px; background-color: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 6px;">
      <div style="font-weight: 700; font-size: 11px; color: #166534; margin-bottom: 4px;">RECORDED PAYMENT RECEIPTS:</div>
      ${invoice.payments.map((p) => `
        <div style="display: flex; justify-content: space-between; font-size: 11px; font-family: monospace; color: #14532d; padding: 2px 0;">
          <span>${p.paymentDate} &bull; ${p.paymentMode} ${p.referenceNumber ? `(Ref: ${p.referenceNumber})` : ''}</span>
          <strong style="color: #166534;">${currencyFmt(p.amount)}</strong>
        </div>
      `).join('')}
    </div>
  ` : '';

  const htmlContent = `
    <!DOCTYPE html>
    <html>
      <head>
        <meta charset="utf-8" />
        <title>Tax Invoice - ${invoice.invoiceNumber}</title>
        <style>
          @page {
            size: A4 portrait;
            margin: 12mm 15mm;
          }
          * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
          }
          body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            color: #0f172a;
            background: #ffffff;
            font-size: 12px;
            line-height: 1.4;
            padding: 10px;
          }
          .invoice-box {
            max-width: 800px;
            margin: 0 auto;
            border: 1px solid #cbd5e1;
            padding: 24px;
            background: #ffffff;
          }
          .header-table {
            width: 100%;
            border-bottom: 2px solid #0f172a;
            padding-bottom: 14px;
            margin-bottom: 16px;
          }
          .badge {
            background-color: #0f172a;
            color: #ffffff;
            font-weight: 800;
            font-size: 13px;
            letter-spacing: 1px;
            padding: 4px 12px;
            border-radius: 4px;
            display: inline-block;
            text-transform: uppercase;
          }
          .section-box {
            width: 100%;
            background-color: #f8fafc;
            border: 1px solid #e2e8f0;
            border-radius: 6px;
            padding: 12px;
            margin-bottom: 16px;
          }
          .items-table {
            width: 100%;
            border-collapse: collapse;
            border: 1px solid #cbd5e1;
            margin-bottom: 16px;
          }
          .items-table th {
            background-color: #f1f5f9;
            color: #1e293b;
            font-size: 11px;
            font-weight: 700;
            padding: 8px 10px;
            border: 1px solid #cbd5e1;
            text-transform: uppercase;
          }
          .totals-table {
            width: 100%;
            border-collapse: collapse;
            border: 1px solid #cbd5e1;
          }
          .totals-table td {
            padding: 6px 10px;
            font-size: 11px;
            border-bottom: 1px solid #e2e8f0;
          }
          .bank-box {
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            padding: 10px 12px;
            background-color: #ffffff;
            font-size: 11px;
          }
          .footer-section {
            margin-top: 20px;
            border-top: 1px solid #cbd5e1;
            padding-top: 14px;
          }
        </style>
      </head>
      <body>
        <div class="invoice-box">
          <!-- Practice & Invoice Header -->
          <table class="header-table">
            <tr>
              <td style="vertical-align: top; width: 60%;">
                <h1 style="font-size: 18px; font-weight: 900; color: #0f172a; text-transform: uppercase; margin-bottom: 2px;">
                  ${practiceName}
                </h1>
                <div style="font-size: 11px; color: #475569; font-weight: 600;">Chartered Accountants & Tax Practitioners</div>
                <div style="font-size: 10px; color: #64748b; margin-top: 6px; line-height: 1.5;">
                  <div>Corporate Office: Express Towers, Nariman Point, Mumbai - 400021</div>
                  <div>PAN: <strong style="font-family: monospace; color: #1e293b;">AABFA1234K</strong> | GSTIN: <strong style="font-family: monospace; color: #1e293b;">27AABFA1234K1Z5</strong></div>
                  <div>Email: billing@taxpractice.com | Phone: +91 98201 12233</div>
                </div>
              </td>
              <td style="vertical-align: top; text-align: right; width: 40%;">
                <div class="badge">TAX INVOICE</div>
                <div style="font-size: 9px; color: #64748b; font-weight: 700; margin-top: 2px; text-transform: uppercase;">Original for Recipient</div>
                <div style="font-size: 11px; font-family: monospace; margin-top: 8px; line-height: 1.6;">
                  <div><strong>Invoice No:</strong> ${invoice.invoiceNumber}</div>
                  <div><strong>Date:</strong> ${invoice.invoiceDate}</div>
                  <div><strong>Due Date:</strong> ${invoice.dueDate || 'Immediate'}</div>
                  <div><strong>State Code:</strong> 27 (Maharashtra)</div>
                </div>
              </td>
            </tr>
          </table>

          <!-- Bill-To Client & Place of Supply -->
          <table style="width: 100%; margin-bottom: 16px;">
            <tr>
              <td style="vertical-align: top; width: 55%; padding-right: 10px;">
                <div class="section-box" style="margin-bottom: 0;">
                  <div style="font-size: 9px; font-weight: 800; text-transform: uppercase; color: #64748b; margin-bottom: 4px;">BILLED TO (CLIENT DETAILS):</div>
                  <div style="font-size: 13px; font-weight: 800; color: #0f172a;">${invoice.clientName || 'Practice Client'}</div>
                  <div style="font-size: 11px; color: #475569; margin-top: 4px; line-height: 1.4;">
                    <div>Address: Registered Business Office / Commercial Address</div>
                    <div>PAN: <strong style="font-family: monospace; color: #0f172a;">${invoice.clientPan || 'N/A'}</strong></div>
                    <div>GSTIN: <strong style="font-family: monospace; color: #0f172a;">${invoice.clientGstin || 'Unregistered'}</strong></div>
                  </div>
                </div>
              </td>
              <td style="vertical-align: top; width: 45%; padding-left: 10px;">
                <div class="section-box" style="margin-bottom: 0;">
                  <div style="font-size: 9px; font-weight: 800; text-transform: uppercase; color: #64748b; margin-bottom: 4px;">SUPPLY DETAILS & STATUS:</div>
                  <div style="font-size: 11px; color: #1e293b; line-height: 1.5;">
                    <div><strong>Place of Supply:</strong> Maharashtra (27)</div>
                    <div><strong>Service Category:</strong> Legal & Tax Advisory (SAC 998231)</div>
                    <div><strong>Status:</strong> <span style="font-weight: 700; color: ${invoice.status === 'PAID' ? '#166534' : '#b45309'};">${invoice.status}</span></div>
                  </div>
                </div>
              </td>
            </tr>
          </table>

          <!-- Service Line Items Table -->
          <table class="items-table">
            <thead>
              <tr>
                <th style="width: 35px; text-align: center;">#</th>
                <th style="text-align: left;">Description of Professional Services</th>
                <th style="width: 65px; text-align: center;">SAC</th>
                <th style="width: 45px; text-align: center;">Qty</th>
                <th style="width: 80px; text-align: right;">Rate (₹)</th>
                <th style="width: 80px; text-align: right;">Taxable (₹)</th>
                <th style="width: 55px; text-align: center;">GST</th>
                <th style="width: 90px; text-align: right;">Amount (₹)</th>
              </tr>
            </thead>
            <tbody>
              ${itemsRows}
            </tbody>
          </table>

          <!-- Totals, Bank Details & Tax Summary -->
          <table style="width: 100%; margin-top: 4px;">
            <tr>
              <td style="vertical-align: top; width: 52%; padding-right: 12px;">
                <!-- Amount in Words -->
                <div style="padding: 8px 10px; background: #f8fafc; border: 1px solid #cbd5e1; border-radius: 6px; margin-bottom: 12px;">
                  <div style="font-size: 9px; font-weight: 800; text-transform: uppercase; color: #64748b; margin-bottom: 2px;">Total Amount in Words:</div>
                  <div style="font-size: 11px; font-weight: 700; color: #0f172a; font-style: italic;">
                    ${numberToWordsINR(invoice.total)}
                  </div>
                </div>

                <!-- Bank Transfer Box -->
                <div class="bank-box">
                  <div style="font-weight: 800; font-size: 11px; color: #0f172a; border-bottom: 1px solid #e2e8f0; padding-bottom: 4px; margin-bottom: 6px;">
                    BANK PAYMENT DETAILS (NEFT / RTGS / UPI):
                  </div>
                  <div style="line-height: 1.5; color: #334155; font-size: 10.5px;">
                    <div>Bank Name: <strong>HDFC Bank Ltd</strong></div>
                    <div>Account Name: <strong>${practiceName}</strong></div>
                    <div>Account Number: <strong style="font-family: monospace;">50200012345678</strong></div>
                    <div>IFSC Code: <strong style="font-family: monospace;">HDFC0001234</strong></div>
                    <div>UPI VPA: <strong style="font-family: monospace;">apextax@hdfcbank</strong></div>
                  </div>
                </div>
              </td>

              <td style="vertical-align: top; width: 48%;">
                <table class="totals-table">
                  <tr>
                    <td style="color: #475569; font-weight: 600;">Taxable Amount:</td>
                    <td style="text-align: right; font-family: monospace; font-weight: 700; color: #0f172a;">${currencyFmt(invoice.subtotal)}</td>
                  </tr>
                  <tr>
                    <td style="color: #475569;">Central GST (CGST @ 9%):</td>
                    <td style="text-align: right; font-family: monospace; color: #334155;">${currencyFmt(Number(invoice.tax || 0) / 2)}</td>
                  </tr>
                  <tr>
                    <td style="color: #475569;">State GST (SGST @ 9%):</td>
                    <td style="text-align: right; font-family: monospace; color: #334155;">${currencyFmt(Number(invoice.tax || 0) / 2)}</td>
                  </tr>
                  <tr style="background-color: #f1f5f9; border-top: 2px solid #0f172a; border-bottom: 2px solid #0f172a;">
                    <td style="font-size: 12px; font-weight: 900; color: #0f172a;">Total Invoice Value:</td>
                    <td style="text-align: right; font-family: monospace; font-size: 13px; font-weight: 900; color: #0f172a;">${currencyFmt(invoice.total)}</td>
                  </tr>
                  <tr>
                    <td style="color: #166534; font-weight: 600;">Amount Received / Paid:</td>
                    <td style="text-align: right; font-family: monospace; font-weight: 700; color: #166534;">${currencyFmt(invoice.paidAmount)}</td>
                  </tr>
                  <tr style="background-color: #fff1f2;">
                    <td style="color: #be123c; font-weight: 800;">Net Balance Due:</td>
                    <td style="text-align: right; font-family: monospace; font-weight: 900; font-size: 12px; color: #be123c;">${currencyFmt(invoice.balanceDue)}</td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>

          ${paymentsHtml}

          <!-- Terms & Authorized Signatory Footer -->
          <table class="footer-section" style="width: 100%;">
            <tr>
              <td style="vertical-align: top; width: 60%; font-size: 9.5px; color: #64748b; line-height: 1.4;">
                <div style="font-weight: 800; text-transform: uppercase; color: #334155; margin-bottom: 2px;">Terms & Conditions:</div>
                <div>1. Payment is due within 15 days of invoice date.</div>
                <div>2. Please quote invoice number in bank transfer remarks.</div>
                <div>3. This is a computer-generated Tax Invoice issued under Rule 46 of CGST Rules, 2017.</div>
              </td>
              <td style="vertical-align: bottom; text-align: right; width: 40%;">
                <div style="font-size: 11px; font-weight: 700; color: #0f172a; margin-bottom: 36px;">
                  For ${practiceName}
                </div>
                <div style="border-top: 1px solid #94a3b8; display: inline-block; width: 180px; text-align: center; font-size: 10px; color: #64748b; padding-top: 4px;">
                  Authorized Signatory / Partner
                </div>
              </td>
            </tr>
          </table>
        </div>
      </body>
    </html>
  `;

  doc.open();
  doc.write(htmlContent);
  doc.close();

  // Trigger print after iframe rendering
  setTimeout(() => {
    iframe.contentWindow?.focus();
    iframe.contentWindow?.print();
  }, 250);
}
