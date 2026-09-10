export type PasswordStrengthLevel = 'Weak' | 'Fair' | 'Good' | 'Strong';

export interface PasswordStrengthResult {
  score: number; // 0 to 4
  label: PasswordStrengthLevel;
  colorClass: string;
  bgColorClass: string;
  barPercent: number;
}

export const KNOWN_WEAK_PASSWORDS = new Set([
  'password123!',
  'password123',
  'password',
  'admin123!',
  'admin123',
  'admin',
  'taxoryn123!',
  'taxoryn123',
  'taxoryn',
  '12345678',
  '123456789',
  '1234567890',
  'qwertyuiop',
  'superadmin123!',
  'changeme',
]);

/**
 * Evaluates password strength based on SaaS security standards:
 * - Length (>= 8, bonus for >= 12)
 * - Lowercase & uppercase letters
 * - Digits
 * - Special characters
 * - Known weak password dictionary check
 */
export const evaluatePasswordStrength = (password: string): PasswordStrengthResult => {
  if (!password || password.trim().length === 0) {
    return {
      score: 0,
      label: 'Weak',
      colorClass: 'text-slate-400',
      bgColorClass: 'bg-slate-200',
      barPercent: 0,
    };
  }

  const trimmed = password.trim();
  const normalized = trimmed.toLowerCase();

  if (KNOWN_WEAK_PASSWORDS.has(normalized) || trimmed.length < 8) {
    return {
      score: 1,
      label: 'Weak',
      colorClass: 'text-rose-600',
      bgColorClass: 'bg-rose-500',
      barPercent: 25,
    };
  }

  let points = 0;
  if (trimmed.length >= 8) points += 1;
  if (trimmed.length >= 12) points += 1;
  if (/[a-z]/.test(trimmed)) points += 1;
  if (/[A-Z]/.test(trimmed)) points += 1;
  if (/[0-9]/.test(trimmed)) points += 1;
  if (/[!@#$%^&*()_+\-=[\]{}|;:,.<>?]/.test(trimmed)) points += 1;

  if (points <= 2) {
    return {
      score: 1,
      label: 'Weak',
      colorClass: 'text-rose-600',
      bgColorClass: 'bg-rose-500',
      barPercent: 25,
    };
  }

  if (points <= 4) {
    return {
      score: 2,
      label: 'Fair',
      colorClass: 'text-amber-600',
      bgColorClass: 'bg-amber-500',
      barPercent: 50,
    };
  }

  if (points === 5) {
    return {
      score: 3,
      label: 'Good',
      colorClass: 'text-blue-600',
      bgColorClass: 'bg-blue-500',
      barPercent: 75,
    };
  }

  return {
    score: 4,
    label: 'Strong',
    colorClass: 'text-emerald-600',
    bgColorClass: 'bg-emerald-500',
    barPercent: 100,
  };
};
