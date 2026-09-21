import { describe, it } from 'node:test';
import assert from 'node:assert';

type OrganizationType =
  | 'UNKNOWN'
  | 'SOLO_PRACTITIONER'
  | 'SMALL_TAX_FIRM'
  | 'GROWING_PRACTICE'
  | 'BUSINESS';

interface RegisterOrganizationRequest {
  organizationName: string;
  organizationEmail: string;
  organizationPhone?: string;
  pan?: string;
  gstin?: string;
  organizationType: OrganizationType;
  adminFirstName: string;
  adminLastName?: string;
  adminEmail: string;
  adminPassword: string;
  adminPhone?: string;
}

describe('Register Organization - OrganizationType Selection & Onboarding Flow', () => {
  const supportedOrganizationTypes: { type: OrganizationType; title: string; description: string }[] = [
    {
      type: 'SOLO_PRACTITIONER',
      title: 'Solo Practitioner',
      description: 'Independent CA or Tax Consultant',
    },
    {
      type: 'SMALL_TAX_FIRM',
      title: 'Small Tax Firm',
      description: 'Boutique tax practice (2–10 members)',
    },
    {
      type: 'GROWING_PRACTICE',
      title: 'Growing Practice',
      description: 'Multi-partner firm with expanding clients',
    },
    {
      type: 'BUSINESS',
      title: 'Business',
      description: 'Corporate in-house tax & finance team',
    },
  ];

  interface RegistrationFormState {
    organizationName: string;
    organizationEmail: string;
    organizationType: OrganizationType | '';
    pan: string;
    gstin: string;
    adminFirstName: string;
    adminLastName: string;
    adminEmail: string;
    adminPhone: string;
    adminPassword: string;
    confirmPassword: string;
  }

  const createRegistrationFormState = (initialType: OrganizationType | '' = ''): RegistrationFormState => ({
    organizationName: 'Apex Tax Advisors LLP',
    organizationEmail: 'contact@apextax.com',
    organizationType: initialType,
    pan: 'AABFA1234K',
    gstin: '27AABFA1234K1Z5',
    adminFirstName: 'Rajesh',
    adminLastName: 'Verma',
    adminEmail: 'admin@apextax.com',
    adminPhone: '+919876543210',
    adminPassword: 'Password123!',
    confirmPassword: 'Password123!',
  });

  const validateForm = (state: RegistrationFormState): { isValid: boolean; errors: Record<string, string> } => {
    const errors: Record<string, string> = {};

    if (!state.organizationType) {
      errors.organizationType = 'Please select how you will use Taxoryn';
    }
    if (!state.organizationName) {
      errors.organizationName = 'Organization name is required';
    }
    if (!state.adminFirstName) {
      errors.adminFirstName = 'Admin first name is required';
    }
    if (!state.adminEmail) {
      errors.adminEmail = 'Admin email is required';
    }
    if (!state.adminPassword) {
      errors.adminPassword = 'Admin password is required';
    } else if (state.adminPassword.length < 8) {
      errors.adminPassword = 'Password must be at least 8 characters long';
    }
    if (!state.confirmPassword) {
      errors.confirmPassword = 'Confirm password is required';
    } else if (state.adminPassword !== state.confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
    }

    return {
      isValid: Object.keys(errors).length === 0,
      errors,
    };
  };

  const buildPayload = (state: RegistrationFormState): RegisterOrganizationRequest => {
    return {
      organizationName: state.organizationName.trim(),
      organizationEmail: (state.organizationEmail || state.adminEmail).trim(),
      organizationType: state.organizationType as OrganizationType,
      pan: state.pan ? state.pan.trim().toUpperCase() : undefined,
      gstin: state.gstin ? state.gstin.trim().toUpperCase() : undefined,
      adminFirstName: state.adminFirstName.trim(),
      adminLastName: state.adminLastName ? state.adminLastName.trim() : undefined,
      adminEmail: state.adminEmail.trim(),
      adminPhone: state.adminPhone ? state.adminPhone.trim() : undefined,
      adminPassword: state.adminPassword,
    };
  };

  it('Displays exactly four supported customer segment options without UNKNOWN', () => {
    assert.strictEqual(supportedOrganizationTypes.length, 4);
    const types = supportedOrganizationTypes.map((o) => o.type);
    assert.deepStrictEqual(types, [
      'SOLO_PRACTITIONER',
      'SMALL_TAX_FIRM',
      'GROWING_PRACTICE',
      'BUSINESS',
    ]);
    assert.strictEqual(types.includes('UNKNOWN' as OrganizationType), false);
  });

  it('Form validation fails when no organization type is selected', () => {
    const state = createRegistrationFormState('');
    const { isValid, errors } = validateForm(state);

    assert.strictEqual(isValid, false);
    assert.strictEqual(errors.organizationType, 'Please select how you will use Taxoryn');
  });

  it('Selecting SOLO_PRACTITIONER clears validation error and builds valid payload', () => {
    const state = createRegistrationFormState('SOLO_PRACTITIONER');
    const { isValid, errors } = validateForm(state);

    assert.strictEqual(isValid, true);
    assert.strictEqual(errors.organizationType, undefined);

    const payload = buildPayload(state);
    assert.strictEqual(payload.organizationType, 'SOLO_PRACTITIONER');
    assert.strictEqual(payload.organizationName, 'Apex Tax Advisors LLP');
  });

  it('Selecting SMALL_TAX_FIRM correctly maps to SMALL_TAX_FIRM payload', () => {
    const state = createRegistrationFormState('SMALL_TAX_FIRM');
    const { isValid } = validateForm(state);
    assert.strictEqual(isValid, true);

    const payload = buildPayload(state);
    assert.strictEqual(payload.organizationType, 'SMALL_TAX_FIRM');
  });

  it('Selecting GROWING_PRACTICE correctly maps to GROWING_PRACTICE payload', () => {
    const state = createRegistrationFormState('GROWING_PRACTICE');
    const { isValid } = validateForm(state);
    assert.strictEqual(isValid, true);

    const payload = buildPayload(state);
    assert.strictEqual(payload.organizationType, 'GROWING_PRACTICE');
  });

  it('Selecting BUSINESS correctly maps to BUSINESS payload', () => {
    const state = createRegistrationFormState('BUSINESS');
    const { isValid } = validateForm(state);
    assert.strictEqual(isValid, true);

    const payload = buildPayload(state);
    assert.strictEqual(payload.organizationType, 'BUSINESS');
  });

  it('Switching option updates state to the new single selected option', () => {
    const state = createRegistrationFormState('SOLO_PRACTITIONER');
    assert.strictEqual(state.organizationType, 'SOLO_PRACTITIONER');

    // User switches to GROWING_PRACTICE
    state.organizationType = 'GROWING_PRACTICE';
    assert.strictEqual(state.organizationType, 'GROWING_PRACTICE');

    const payload = buildPayload(state);
    assert.strictEqual(payload.organizationType, 'GROWING_PRACTICE');
  });
});
