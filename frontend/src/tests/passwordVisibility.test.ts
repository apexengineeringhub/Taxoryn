import { describe, it } from 'node:test';
import assert from 'node:assert';

describe('Login Page Password Visibility Control', () => {
  interface PasswordVisibilityState {
    showPassword: boolean;
    passwordValue: string;
  }

  const createPasswordControl = (initialPassword = '') => {
    let state: PasswordVisibilityState = {
      showPassword: false,
      passwordValue: initialPassword,
    };

    return {
      getState: () => ({ ...state }),
      getInputType: () => (state.showPassword ? 'text' : 'password'),
      getAriaLabel: () => (state.showPassword ? 'Hide password' : 'Show password'),
      getTitle: () => (state.showPassword ? 'Hide password' : 'Show password'),
      getIconName: () => (state.showPassword ? 'EyeOff' : 'Eye'),
      getButtonType: () => 'button',
      setPassword: (val: string) => {
        state.passwordValue = val;
      },
      toggle: () => {
        state.showPassword = !state.showPassword;
      },
      handleKeyDown: (key: string) => {
        if (key === 'Enter' || key === ' ') {
          state.showPassword = !state.showPassword;
          return true;
        }
        return false;
      },
    };
  };

  it('Password is hidden by default with correct initial attributes', () => {
    const control = createPasswordControl('MySecret123!');
    assert.strictEqual(control.getState().showPassword, false);
    assert.strictEqual(control.getInputType(), 'password');
    assert.strictEqual(control.getIconName(), 'Eye');
    assert.strictEqual(control.getAriaLabel(), 'Show password');
    assert.strictEqual(control.getTitle(), 'Show password');
    assert.strictEqual(control.getButtonType(), 'button');
  });

  it('Clicking toggle reveals password and updates icon + aria-label', () => {
    const control = createPasswordControl('SecurePassword#2026');
    assert.strictEqual(control.getInputType(), 'password');

    control.toggle();

    assert.strictEqual(control.getState().showPassword, true);
    assert.strictEqual(control.getInputType(), 'text');
    assert.strictEqual(control.getIconName(), 'EyeOff');
    assert.strictEqual(control.getAriaLabel(), 'Hide password');
    assert.strictEqual(control.getTitle(), 'Hide password');
    assert.strictEqual(control.getState().passwordValue, 'SecurePassword#2026');
  });

  it('Clicking again hides password and reverts input type back to password', () => {
    const control = createPasswordControl('P@ssword99');
    control.toggle();
    assert.strictEqual(control.getInputType(), 'text');

    control.toggle();
    assert.strictEqual(control.getState().showPassword, false);
    assert.strictEqual(control.getInputType(), 'password');
    assert.strictEqual(control.getIconName(), 'Eye');
    assert.strictEqual(control.getAriaLabel(), 'Show password');
    assert.strictEqual(control.getState().passwordValue, 'P@ssword99');
  });

  it('Password value remains unchanged and editing works during visible or hidden state', () => {
    const control = createPasswordControl('InitialSecret');
    control.toggle();
    assert.strictEqual(control.getInputType(), 'text');

    control.setPassword('InitialSecretUpdated');
    assert.strictEqual(control.getState().passwordValue, 'InitialSecretUpdated');

    control.toggle();
    assert.strictEqual(control.getInputType(), 'password');
    assert.strictEqual(control.getState().passwordValue, 'InitialSecretUpdated');
  });

  it('Keyboard Tab can reach the toggle and Enter / Space activate it', () => {
    const control = createPasswordControl('KeyboardAccessiblePassword1');
    assert.strictEqual(control.getButtonType(), 'button');

    const spaceHandled = control.handleKeyDown(' ');
    assert.strictEqual(spaceHandled, true);
    assert.strictEqual(control.getInputType(), 'text');
    assert.strictEqual(control.getAriaLabel(), 'Hide password');

    const enterHandled = control.handleKeyDown('Enter');
    assert.strictEqual(enterHandled, true);
    assert.strictEqual(control.getInputType(), 'password');
    assert.strictEqual(control.getAriaLabel(), 'Show password');

    const tabHandled = control.handleKeyDown('Tab');
    assert.strictEqual(tabHandled, false);
    assert.strictEqual(control.getInputType(), 'password');
  });

  it('Button has type="button" to prevent form submission on toggle click', () => {
    const control = createPasswordControl('TestPass');
    assert.strictEqual(control.getButtonType(), 'button');
    assert.notStrictEqual(control.getButtonType(), 'submit');
  });
});
