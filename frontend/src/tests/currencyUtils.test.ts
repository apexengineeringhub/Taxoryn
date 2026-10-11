import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { formatCurrency, formatCurrencyCompact } from '../utils/currencyUtils.ts';

describe('formatCurrency utility', () => {
  it('formats zero correctly with 2 decimal places for INR', () => {
    const res = formatCurrency(0);
    assert.match(res, /₹/);
    assert.match(res, /0\.00/);
  });

  it('formats null and undefined safely with ₹0.00 default', () => {
    assert.match(formatCurrency(null), /₹.*0\.00/);
    assert.match(formatCurrency(undefined), /₹.*0\.00/);
  });

  it('supports custom fallback string', () => {
    assert.equal(formatCurrency(null, { fallback: '—' }), '—');
    assert.equal(formatCurrency(undefined, { fallback: 'N/A' }), 'N/A');
  });

  it('formats small amounts and paise correctly in INR', () => {
    const res = formatCurrency(1250.5);
    assert.match(res, /₹/);
    assert.match(res, /1,250\.50/);
  });

  it('formats thousands, lakhs and crores according to Indian grouping (en-IN)', () => {
    // 1 Lakh = 1,00,000.00
    const oneLakh = formatCurrency(100000);
    assert.match(oneLakh, /1,00,000\.00/);

    // 1 Crore 23 Lakh 45 Thousand = 1,23,45,678.90
    const crore = formatCurrency(12345678.9);
    assert.match(crore, /1,23,45,678\.90/);
  });

  it('formats negative amounts correctly', () => {
    const neg = formatCurrency(-5000);
    assert.match(neg, /5,000\.00/);
    assert.ok(neg.includes('-') || neg.includes('('));
  });

  it('preserves non-INR currencies such as USD', () => {
    const usd = formatCurrency(1250.5, 'USD');
    assert.match(usd, /\$|USD/);
    assert.match(usd, /1,250\.50/);
  });

  it('preserves non-INR currencies with options object', () => {
    const eur = formatCurrency(2500, { currency: 'EUR' });
    assert.match(eur, /€|EUR/);
    assert.match(eur, /2,500\.00/);
  });

  it('handles formatCurrencyCompact omitting zero paise', () => {
    const compactInt = formatCurrencyCompact(5000);
    assert.match(compactInt, /₹/);
    assert.match(compactInt, /5,000/);
    assert.ok(!compactInt.includes('.00'));

    // But preserves fractions when present
    const compactFrac = formatCurrencyCompact(5000.75);
    assert.match(compactFrac, /5,000\.75/);
  });
});
