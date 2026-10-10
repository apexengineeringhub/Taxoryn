/**
 * Taxoryn Centralized Currency Formatting Utility
 * Standardizes Indian Rupee (INR) representation using '₹' and Indian locale grouping (en-IN),
 * while preserving existing behavior for non-INR currencies (USD, EUR, GBP, etc.).
 */

export interface CurrencyFormatOptions {
  /** Target currency code, e.g. 'INR', 'USD', 'EUR'. Defaults to 'INR'. */
  currency?: string;
  /** Minimum fractional digits (paise / cents). Default depends on mode or options. */
  minimumFractionDigits?: number;
  /** Maximum fractional digits. Default 2 for exact currency amounts. */
  maximumFractionDigits?: number;
  /**
   * If true, amounts with 0 paise omit decimals (e.g. ₹5,000 instead of ₹5,000.00).
   * Useful for dashboard stats and KPI summaries.
   */
  compactDecimals?: boolean;
  /**
   * Fallback string to display when the amount is null, undefined, or NaN.
   * Default is '₹0.00' (or corresponding zero currency string) or custom placeholder like '—'.
   */
  fallback?: string;
}

/**
 * Formats a numeric amount using appropriate currency symbol and locale grouping.
 * For INR: Uses 'en-IN' locale with '₹' symbol and standard Indian numbering (Lakhs, Crores).
 * For foreign currencies: Uses standard international currency representation.
 *
 * @param amount - The numeric monetary value to format.
 * @param currencyOrOptions - Optional currency code (string) or configuration options object.
 * @returns Formatted currency string (e.g., '₹1,25,000.00', '₹0.00', '$500.00')
 */
export function formatCurrency(
  amount: number | null | undefined,
  currencyOrOptions?: string | CurrencyFormatOptions
): string {
  const options: CurrencyFormatOptions =
    typeof currencyOrOptions === 'string'
      ? { currency: currencyOrOptions }
      : currencyOrOptions || {};

  const currency = (options.currency || 'INR').toUpperCase();
  const isINR = currency === 'INR';
  const locale = isINR ? 'en-IN' : 'en-US';

  if (amount === null || amount === undefined || Number.isNaN(Number(amount))) {
    if (options.fallback !== undefined) {
      return options.fallback;
    }
    // Return standard zero for currency
    const minDec = options.minimumFractionDigits ?? (options.compactDecimals ? 0 : 2);
    const maxDec = options.maximumFractionDigits ?? (options.compactDecimals ? 0 : 2);
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency,
      minimumFractionDigits: minDec,
      maximumFractionDigits: maxDec,
    }).format(0);
  }

  const numericVal = Number(amount);

  let minFractionDigits = options.minimumFractionDigits;
  let maxFractionDigits = options.maximumFractionDigits;

  if (options.compactDecimals) {
    // If integer, display no decimals; if has fractional part, display up to 2
    if (Number.isInteger(numericVal)) {
      minFractionDigits = minFractionDigits ?? 0;
      maxFractionDigits = maxFractionDigits ?? 0;
    } else {
      minFractionDigits = minFractionDigits ?? 2;
      maxFractionDigits = maxFractionDigits ?? 2;
    }
  } else {
    minFractionDigits = minFractionDigits ?? 2;
    maxFractionDigits = maxFractionDigits ?? 2;
  }

  try {
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency,
      minimumFractionDigits: minFractionDigits,
      maximumFractionDigits: maxFractionDigits,
    }).format(numericVal);
  } catch {
    // Fallback if an unknown currency code was passed
    const formattedNum = numericVal.toLocaleString(locale, {
      minimumFractionDigits: minFractionDigits,
      maximumFractionDigits: maxFractionDigits,
    });
    return isINR ? `₹${formattedNum}` : `${currency} ${formattedNum}`;
  }
}

/**
 * Formats an amount with whole rupees for KPIs and dashboard badges.
 * E.g., ₹5,000 or ₹1,25,000 without paise unless there are fractional amounts.
 */
export function formatCurrencyCompact(
  amount: number | null | undefined,
  currency: string = 'INR'
): string {
  return formatCurrency(amount, {
    currency,
    compactDecimals: true,
  });
}
