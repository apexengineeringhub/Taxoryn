import { describe, it } from 'node:test';
import assert from 'node:assert';
import {
  getCurrentAssessmentYear,
  getCurrentFinancialYear,
  getDynamicAssessmentYears,
  getDynamicFinancialYears,
} from '../utils/taxYearUtils.ts';

describe('Tax Year & Dynamic Assessment Year (AY) Calculation Tests', () => {

  // Test 1: Indian tax year boundary: 31 March vs 1 April
  describe('Year Boundary Precision (31 March / 1 April)', () => {
    it('resolves 31 March 2024 to FY 2023-24 and AY 2024-25', () => {
      // 31 March 2024 (Month index 2 in JavaScript Date)
      const d = new Date(2024, 2, 31, 23, 59, 59);
      assert.strictEqual(getCurrentFinancialYear(d), '2023-24');
      assert.strictEqual(getCurrentAssessmentYear(d), '2024-25');
    });

    it('resolves 1 April 2024 to FY 2024-25 and AY 2025-26', () => {
      // 1 April 2024 (Month index 3 in JavaScript Date)
      const d = new Date(2024, 3, 1, 0, 0, 0);
      assert.strictEqual(getCurrentFinancialYear(d), '2024-25');
      assert.strictEqual(getCurrentAssessmentYear(d), '2025-26');
    });

    it('resolves 31 March 2025 to FY 2024-25 and AY 2025-26', () => {
      const d = new Date(2025, 2, 31, 12, 0, 0);
      assert.strictEqual(getCurrentFinancialYear(d), '2024-25');
      assert.strictEqual(getCurrentAssessmentYear(d), '2025-26');
    });

    it('resolves 1 April 2025 to FY 2025-26 and AY 2026-27', () => {
      const d = new Date(2025, 3, 1, 12, 0, 0);
      assert.strictEqual(getCurrentFinancialYear(d), '2025-26');
      assert.strictEqual(getCurrentAssessmentYear(d), '2026-27');
    });

    it('resolves 31 March 2026 to FY 2025-26 and AY 2026-27', () => {
      const d = new Date(2026, 2, 31, 18, 0, 0);
      assert.strictEqual(getCurrentFinancialYear(d), '2025-26');
      assert.strictEqual(getCurrentAssessmentYear(d), '2026-27');
    });

    it('resolves 1 April 2026 to FY 2026-27 and AY 2027-28', () => {
      const d = new Date(2026, 3, 1, 0, 1, 0);
      assert.strictEqual(getCurrentFinancialYear(d), '2026-27');
      assert.strictEqual(getCurrentAssessmentYear(d), '2027-28');
    });

    it('resolves mid-year date 15 October 2026 to FY 2026-27 and AY 2027-28', () => {
      const d = new Date(2026, 9, 15, 10, 30, 0);
      assert.strictEqual(getCurrentFinancialYear(d), '2026-27');
      assert.strictEqual(getCurrentAssessmentYear(d), '2027-28');
    });

    it('resolves early calendar year 15 January 2027 to FY 2026-27 and AY 2027-28', () => {
      const d = new Date(2027, 0, 15, 10, 30, 0);
      assert.strictEqual(getCurrentFinancialYear(d), '2026-27');
      assert.strictEqual(getCurrentAssessmentYear(d), '2027-28');
    });
  });

  // Test 2: Rolling window of Assessment Years
  describe('Rolling Assessment Years Generation', () => {
    it('generates correct sequence of past and future AYs centered on reference date', () => {
      // Reference date: 15 October 2026 -> Current AY is 2027-28
      const ref = new Date(2026, 9, 15);
      const ays = getDynamicAssessmentYears(5, 1, ref);

      // Future 1: 2028-29, Current: 2027-28, Past 5: 2026-27, 2025-26, 2024-25, 2023-24, 2022-23
      assert.strictEqual(ays.length, 7);
      assert.strictEqual(ays[0], '2028-29'); // future +1
      assert.strictEqual(ays[1], '2027-28'); // current AY
      assert.strictEqual(ays[2], '2026-27'); // past -1
      assert.strictEqual(ays[3], '2025-26'); // past -2
      assert.strictEqual(ays[4], '2024-25'); // past -3
      assert.strictEqual(ays[5], '2023-24'); // past -4
      assert.strictEqual(ays[6], '2022-23'); // past -5
    });

    it('generates correct sequence of past and future FYs', () => {
      const ref = new Date(2026, 9, 15);
      const fys = getDynamicFinancialYears(5, 1, ref);

      assert.strictEqual(fys.length, 7);
      assert.strictEqual(fys[0], '2027-28'); // future +1 FY
      assert.strictEqual(fys[1], '2026-27'); // current FY
      assert.strictEqual(fys[2], '2025-26');
      assert.strictEqual(fys[3], '2024-25');
      assert.strictEqual(fys[4], '2023-24');
      assert.strictEqual(fys[5], '2022-23');
      assert.strictEqual(fys[6], '2021-22');
    });

    it('preserves historical selection range without dropping legacy years like 2024-25 or 2023-24', () => {
      const ref = new Date(2026, 9, 15);
      const ays = getDynamicAssessmentYears(6, 1, ref);
      assert.strictEqual(ays.includes('2024-25'), true);
      assert.strictEqual(ays.includes('2025-26'), true);
      assert.strictEqual(ays.includes('2026-27'), true);
      assert.strictEqual(ays.includes('2027-28'), true);
    });
  });

  // Test 3: Century boundary wrapping safety
  describe('Century Year Formatting Safety', () => {
    it('formats single digit century wrap cleanly (e.g. 2099-00)', () => {
      const centuryRef = new Date(2099, 5, 1);
      const ay = getCurrentAssessmentYear(centuryRef);
      // Start year 2100 -> End year 2101 % 100 = 01
      assert.strictEqual(ay, '2100-01');
    });
  });
});
