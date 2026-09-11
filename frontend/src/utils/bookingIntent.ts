/**
 * Booking Intent Storage & Lifecycle Manager
 * Preserves marketplace consultation booking context across authentication (Login / Register)
 * Enforces TTL expiry and tab-scoped privacy using sessionStorage.
 */

export interface BookingIntent {
  marketplaceProfileId: string;
  professionalName?: string;
  consultationFee?: number;
  topic: string;
  consultationMode: 'VIDEO' | 'PHONE' | 'IN_PERSON';
  bookingDate: string; // YYYY-MM-DD
  startTime: string;   // HH:mm (e.g. "11:30")
  endTime: string;     // HH:mm (e.g. "12:00")
  notes?: string;
  returnUrl?: string;  // e.g. "/marketplace" or "/practice/apex-tax"
  createdAt: number;   // Timestamp in ms
}

const BOOKING_INTENT_KEY = 'taxoryn_booking_intent';
const INTENT_TTL_MS = 30 * 60 * 1000; // 30 minutes expiration

/**
 * Saves unconfirmed booking selection prior to authentication.
 */
export const saveBookingIntent = (intent: Omit<BookingIntent, 'createdAt'>): void => {
  try {
    const fullIntent: BookingIntent = {
      ...intent,
      createdAt: Date.now(),
    };
    sessionStorage.setItem(BOOKING_INTENT_KEY, JSON.stringify(fullIntent));
  } catch (err) {
    console.warn('Could not persist booking intent to sessionStorage', err);
  }
};

/**
 * Retrieves valid unexpired booking intent from storage.
 */
export const getBookingIntent = (): BookingIntent | null => {
  try {
    const raw = sessionStorage.getItem(BOOKING_INTENT_KEY);
    if (!raw) return null;
    const parsed: BookingIntent = JSON.parse(raw);
    if (!parsed.marketplaceProfileId || !parsed.bookingDate || !parsed.startTime) {
      clearBookingIntent();
      return null;
    }
    // Check expiration
    if (Date.now() - parsed.createdAt > INTENT_TTL_MS) {
      clearBookingIntent();
      return null;
    }
    return parsed;
  } catch {
    clearBookingIntent();
    return null;
  }
};

/**
 * Clears booking intent upon completion or cancellation.
 */
export const clearBookingIntent = (): void => {
  try {
    sessionStorage.removeItem(BOOKING_INTENT_KEY);
  } catch {
    // Ignore
  }
};

/**
 * Returns true if a valid, unexpired booking intent is waiting.
 */
export const hasBookingIntent = (): boolean => {
  return getBookingIntent() !== null;
};
