/**
 * Tax & Assessment Year Utility functions for Indian Statutory Compliance.
 *
 * In the Indian tax regime:
 * Financial Year (FY) runs from April 1 to March 31.
 * Assessment Year (AY) is the immediately following year.
 * e.g., For April 1, 2024 to March 31, 2025:
 *   FY is 2024-25, AY is 2025-26.
 */

/**
 * Returns the current Assessment Year as a formatted string, e.g. "2026-27"
 */
export function getCurrentAssessmentYear(referenceDate: Date = new Date()): string {
  const month = referenceDate.getMonth(); // 0 = Jan, 3 = Apr, 11 = Dec
  const year = referenceDate.getFullYear();

  // If before April (Jan, Feb, Mar), we are in FY (Y-1)-(Y), so AY is Y-(Y+1)
  // If April or later, we are in FY Y-(Y+1), so AY is (Y+1)-(Y+2)
  const ayStartYear = month >= 3 ? year + 1 : year;
  const ayEndYear = (ayStartYear + 1) % 100;
  return `${ayStartYear}-${String(ayEndYear).padStart(2, '0')}`;
}

/**
 * Returns the current Financial Year as a formatted string, e.g. "2025-26"
 */
export function getCurrentFinancialYear(referenceDate: Date = new Date()): string {
  const month = referenceDate.getMonth();
  const year = referenceDate.getFullYear();

  const fyStartYear = month >= 3 ? year : year - 1;
  const fyEndYear = (fyStartYear + 1) % 100;
  return `${fyStartYear}-${String(fyEndYear).padStart(2, '0')}`;
}

/**
 * Returns a list of Assessment Years centered around the current AY.
 * @param pastYears Number of past AYs to include (default 5)
 * @param futureYears Number of upcoming AYs to include (default 1)
 * @param referenceDate Reference date (default now)
 */
export function getDynamicAssessmentYears(
  pastYears: number = 6,
  futureYears: number = 1,
  referenceDate: Date = new Date()
): string[] {
  const month = referenceDate.getMonth();
  const year = referenceDate.getFullYear();
  const currentAyStart = month >= 3 ? year + 1 : year;

  const years: string[] = [];
  // Future years down to past years
  for (let offset = futureYears; offset >= -pastYears; offset--) {
    const start = currentAyStart + offset;
    const end = (start + 1) % 100;
    years.push(`${start}-${String(end).padStart(2, '0')}`);
  }
  return years;
}

/**
 * Returns a list of Financial Years centered around the current FY.
 * @param pastYears Number of past FYs to include (default 5)
 * @param futureYears Number of upcoming FYs to include (default 1)
 * @param referenceDate Reference date (default now)
 */
export function getDynamicFinancialYears(
  pastYears: number = 6,
  futureYears: number = 1,
  referenceDate: Date = new Date()
): string[] {
  const month = referenceDate.getMonth();
  const year = referenceDate.getFullYear();
  const currentFyStart = month >= 3 ? year : year - 1;

  const years: string[] = [];
  for (let offset = futureYears; offset >= -pastYears; offset--) {
    const start = currentFyStart + offset;
    const end = (start + 1) % 100;
    years.push(`${start}-${String(end).padStart(2, '0')}`);
  }
  return years;
}
