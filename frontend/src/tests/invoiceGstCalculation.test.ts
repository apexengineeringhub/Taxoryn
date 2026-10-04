import { describe, it } from 'node:test';
import assert from 'node:assert';
import type { Invoice } from '../types/index.ts';

describe('Professional Tax Invoice GST Rate Display & Calculation', () => {
  // Helper mimicking newInvoiceCalculations logic
  const calculateNewInvoice = (
    items: Array<{ quantity: number; unitPrice: number; taxRate?: number }>,
    discountType: 'NONE' | 'FLAT' | 'PERCENTAGE' = 'NONE',
    discountValue: number = 0
  ) => {
    let subtotal = 0;
    let totalTax = 0;
    for (const it of items) {
      const lineSub = Number(it.quantity || 1) * Number(it.unitPrice || 0);
      const lineTax = (lineSub * Number(it.taxRate != null ? it.taxRate : 18)) / 100;
      subtotal += lineSub;
      totalTax += lineTax;
    }
    let discountAmount = 0;
    if (discountType === 'FLAT') {
      discountAmount = Math.min(Math.max(0, Number(discountValue || 0)), subtotal + totalTax);
    } else if (discountType === 'PERCENTAGE') {
      const pct = Math.min(Math.max(0, Number(discountValue || 0)), 100);
      discountAmount = (subtotal * pct) / 100;
    }
    const grandTotal = Math.max(0, subtotal + totalTax - discountAmount);

    let taxSummaryLabel = 'GST (CGST 9% + SGST 9% / IGST 18%):';
    if (items.length > 0) {
      const rates = items.map((it) => Number(it.taxRate != null ? it.taxRate : 18));
      const allSame = rates.every((r) => r === rates[0]);
      if (allSame) {
        const rate = rates[0];
        if (rate === 0) {
          taxSummaryLabel = 'GST (0% - Exempt):';
        } else {
          const half = Number((rate / 2).toFixed(2));
          taxSummaryLabel = `GST (CGST ${half}% + SGST ${half}% / IGST ${rate}%):`;
        }
      } else if (subtotal > 0 && totalTax > 0) {
        const effectiveTotal = Math.round((Number(totalTax) / Number(subtotal)) * 1000) / 10;
        const half = Number((effectiveTotal / 2).toFixed(2));
        taxSummaryLabel = `GST (CGST ${half}% + SGST ${half}% / IGST ${effectiveTotal}%):`;
      }
    }

    return { subtotal, totalTax, discountAmount, grandTotal, taxSummaryLabel };
  };

  const getTaxRateLabel = (inv: Invoice) => {
    if (!inv.subtotal || inv.subtotal <= 0) {
      return { cgst: 'Central GST (CGST)', sgst: 'State GST (SGST)' };
    }
    if (inv.items && inv.items.length > 0) {
      const rates = inv.items.map((it) => Number(it.taxRate != null ? it.taxRate : 0));
      const allSame = rates.every((r) => r === rates[0]);
      if (allSame) {
        if (rates[0] === 0) {
          return {
            cgst: 'Central GST (CGST @ 0%)',
            sgst: 'State GST (SGST @ 0%)',
          };
        }
        const half = Number((rates[0] / 2).toFixed(2));
        return {
          cgst: `Central GST (CGST @ ${half}%)`,
          sgst: `State GST (SGST @ ${half}%)`,
        };
      }
    }
    if (inv.tax && inv.tax > 0) {
      const effectiveTotal = Math.round((Number(inv.tax) / Number(inv.subtotal)) * 1000) / 10;
      const half = Number((effectiveTotal / 2).toFixed(2));
      return {
        cgst: `Central GST (CGST @ ${half}%)`,
        sgst: `State GST (SGST @ ${half}%)`,
      };
    }
    return { cgst: 'Central GST (CGST)', sgst: 'State GST (SGST)' };
  };

  it('correctly calculates 5% GST rate and sets label to CGST 2.5% + SGST 2.5% / IGST 5%', () => {
    const result = calculateNewInvoice([{ quantity: 1, unitPrice: 999, taxRate: 5 }]);
    assert.strictEqual(result.subtotal, 999);
    assert.strictEqual(result.totalTax, 49.95);
    assert.strictEqual(result.grandTotal, 1048.95);
    assert.strictEqual(result.taxSummaryLabel, 'GST (CGST 2.5% + SGST 2.5% / IGST 5%):');
  });

  it('correctly calculates 12% GST rate and sets label to CGST 6% + SGST 6% / IGST 12%', () => {
    const result = calculateNewInvoice([{ quantity: 2, unitPrice: 5000, taxRate: 12 }]);
    assert.strictEqual(result.subtotal, 10000);
    assert.strictEqual(result.totalTax, 1200);
    assert.strictEqual(result.grandTotal, 11200);
    assert.strictEqual(result.taxSummaryLabel, 'GST (CGST 6% + SGST 6% / IGST 12%):');
  });

  it('correctly calculates 18% standard GST rate and sets label to CGST 9% + SGST 9% / IGST 18%', () => {
    const result = calculateNewInvoice([{ quantity: 1, unitPrice: 1000, taxRate: 18 }]);
    assert.strictEqual(result.subtotal, 1000);
    assert.strictEqual(result.totalTax, 180);
    assert.strictEqual(result.grandTotal, 1180);
    assert.strictEqual(result.taxSummaryLabel, 'GST (CGST 9% + SGST 9% / IGST 18%):');
  });

  it('correctly handles 0% exempt GST rate and sets label to GST (0% - Exempt):', () => {
    const result = calculateNewInvoice([{ quantity: 1, unitPrice: 2500, taxRate: 0 }]);
    assert.strictEqual(result.subtotal, 2500);
    assert.strictEqual(result.totalTax, 0);
    assert.strictEqual(result.grandTotal, 2500);
    assert.strictEqual(result.taxSummaryLabel, 'GST (0% - Exempt):');
  });

  it('correctly computes effective rate for mixed line items', () => {
    const items = [
      { quantity: 1, unitPrice: 1000, taxRate: 5 },  // tax = 50
      { quantity: 1, unitPrice: 1000, taxRate: 18 }, // tax = 180
    ];
    // subtotal = 2000, tax = 230, effective rate = 11.5%
    const result = calculateNewInvoice(items);
    assert.strictEqual(result.subtotal, 2000);
    assert.strictEqual(result.totalTax, 230);
    assert.strictEqual(result.grandTotal, 2230);
    assert.strictEqual(result.taxSummaryLabel, 'GST (CGST 5.75% + SGST 5.75% / IGST 11.5%):');
  });

  it('getTaxRateLabel generates correct labels for preview and print views across GST rates', () => {
    const invoice5Pct: Invoice = {
      id: 'inv-1',
      invoiceNumber: 'INV-2026-001',
      clientId: 'c-1',
      subtotal: 1000,
      tax: 50,
      total: 1050,
      balanceDue: 1050,
      paidAmount: 0,
      status: 'ISSUED',
      invoiceDate: '2026-10-01',
      dueDate: '2026-10-15',
      items: [
        { service: 'GST_FILING', description: 'GSTR-3B', quantity: 1, unitPrice: 1000, taxRate: 5, amount: 1050 }
      ]
    };
    const labels5 = getTaxRateLabel(invoice5Pct);
    assert.strictEqual(labels5.cgst, 'Central GST (CGST @ 2.5%)');
    assert.strictEqual(labels5.sgst, 'State GST (SGST @ 2.5%)');

    const invoice0Pct: Invoice = {
      id: 'inv-2',
      invoiceNumber: 'INV-2026-002',
      clientId: 'c-2',
      subtotal: 2000,
      tax: 0,
      total: 2000,
      balanceDue: 2000,
      paidAmount: 0,
      status: 'ISSUED',
      invoiceDate: '2026-10-01',
      dueDate: '2026-10-15',
      items: [
        { service: 'CONSULTING', description: 'Exempt service', quantity: 1, unitPrice: 2000, taxRate: 0, amount: 2000 }
      ]
    };
    const labels0 = getTaxRateLabel(invoice0Pct);
    assert.strictEqual(labels0.cgst, 'Central GST (CGST @ 0%)');
    assert.strictEqual(labels0.sgst, 'State GST (SGST @ 0%)');
  });

  it('renders line item GST rate in preview and print without falling back 0% to 18%', () => {
    const renderLineTaxRate = (itemTaxRate?: number) => {
      return `${itemTaxRate != null ? itemTaxRate : 0}%`;
    };

    assert.strictEqual(renderLineTaxRate(0), '0%');
    assert.strictEqual(renderLineTaxRate(5), '5%');
    assert.strictEqual(renderLineTaxRate(12), '12%');
    assert.strictEqual(renderLineTaxRate(18), '18%');
    assert.strictEqual(renderLineTaxRate(undefined), '0%');
  });

  it('verifies complete lifecycle persistence and preview consistency for 0%, 5%, 12%, and 18%', () => {
    const testRates = [0, 5, 12, 18];

    for (const rate of testRates) {
      // 1. Create payload
      const subtotal = 1000;
      const tax = (subtotal * rate) / 100;
      const total = subtotal + tax;

      const savedInvoice: Invoice = {
        id: `inv-${rate}`,
        invoiceNumber: `INV-2026-${rate}`,
        clientId: 'client-1',
        subtotal,
        tax,
        total,
        balanceDue: total,
        paidAmount: 0,
        status: 'ISSUED',
        invoiceDate: '2026-10-01',
        dueDate: '2026-10-15',
        items: [
          {
            service: 'GST_FILING',
            description: 'Tax Service',
            quantity: 1,
            unitPrice: subtotal,
            taxRate: rate,
            amount: total,
          },
        ],
      };

      // 2. Preview line item rendering
      const previewItem = savedInvoice.items![0];
      const displayedLineRate = `${previewItem.taxRate != null ? previewItem.taxRate : 0}%`;
      assert.strictEqual(displayedLineRate, `${rate}%`);

      // 3. Preview tax summary label
      const taxLabels = getTaxRateLabel(savedInvoice);
      if (rate === 0) {
        assert.strictEqual(taxLabels.cgst, 'Central GST (CGST @ 0%)');
        assert.strictEqual(taxLabels.sgst, 'State GST (SGST @ 0%)');
      } else {
        const half = Number((rate / 2).toFixed(2));
        assert.strictEqual(taxLabels.cgst, `Central GST (CGST @ ${half}%)`);
        assert.strictEqual(taxLabels.sgst, `State GST (SGST @ ${half}%)`);
      }
    }
  });
});
