import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import {
  CheckCircle2,
  ShieldAlert,
  ArrowRight,
  Mail,
  RefreshCw,
  Lock,
  Eye,
  EyeOff,
  Building2,
  User as UserIcon,
  Check,
  X
} from 'lucide-react';
import { TaxorynLogo } from '../components/common/TaxorynLogo';
import { Button } from '../components/common/Button';
import { authApi } from '../api/endpoints';
import { ValidateActivationTokenResponse } from '../types';

export const ActivateOrgPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';

  // Validation phase state
  const [isValidating, setIsValidating] = useState(false);
  const [tokenInfo, setTokenInfo] = useState<ValidateActivationTokenResponse | null>(null);

  // Activation submission state
  const [isActivating, setIsActivating] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Password setup state (for invited employees)
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  // Resend activation state
  const [resendEmail, setResendEmail] = useState('');
  const [isResending, setIsResending] = useState(false);
  const [resendSuccess, setResendSuccess] = useState(false);
  const [resendError, setResendError] = useState<string | null>(null);

  useEffect(() => {
    if (token) {
      validateToken(token);
    }
  }, [token]);

  const validateToken = async (rawToken: string) => {
    setIsValidating(true);
    setError(null);

    try {
      const info = await authApi.validateActivationToken(rawToken.trim());
      setTokenInfo(info);

      // If token is valid and does NOT require password setup (e.g. Org Admin), auto-activate
      if (info.valid && !info.requiresPasswordSetup) {
        await handleDirectActivation(rawToken.trim());
      }
    } catch (err: any) {
      const msg =
        err?.response?.data?.message ||
        'Invalid or expired activation link. Please request a fresh activation link below.';
      setError(msg);
    } finally {
      setIsValidating(false);
    }
  };

  const handleDirectActivation = async (rawToken: string) => {
    setIsActivating(true);
    setError(null);

    try {
      await authApi.activateOrg(rawToken);
      setIsSuccess(true);
    } catch (err: any) {
      const msg =
        err?.response?.data?.message ||
        'Failed to activate account. The activation token may have expired or already been used.';
      setError(msg);
    } finally {
      setIsActivating(false);
    }
  };

  // Password rules validation
  const hasMinLength = password.length >= 8;
  const hasUppercase = /[A-Z]/.test(password);
  const hasLowercase = /[a-z]/.test(password);
  const hasNumber = /[0-9]/.test(password);
  const hasSpecial = /[@#$%^&+=!._-]/.test(password);
  const passwordsMatch = password.length > 0 && password === confirmPassword;
  const isPasswordValid =
    hasMinLength && hasUppercase && hasLowercase && hasNumber && hasSpecial && passwordsMatch;

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!isPasswordValid) {
      if (!passwordsMatch) {
        setFormError('Passwords do not match.');
      } else {
        setFormError('Please ensure your password meets all complexity requirements.');
      }
      return;
    }

    setIsActivating(true);
    setError(null);

    try {
      await authApi.activateOrg(token.trim(), password);
      setIsSuccess(true);
    } catch (err: any) {
      const msg =
        err?.response?.data?.message ||
        'Failed to complete activation. The link may have expired. Please try requesting a new invite.';
      setError(msg);
    } finally {
      setIsActivating(false);
    }
  };

  const handleResend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resendEmail.trim()) {
      setResendError('Please enter your email address.');
      return;
    }

    setIsResending(true);
    setResendError(null);
    setResendSuccess(false);

    try {
      await authApi.resendActivation(resendEmail.trim());
      setResendSuccess(true);
    } catch (err: any) {
      setResendError(
        err?.response?.data?.message || 'Failed to resend activation email. Please try again.'
      );
    } finally {
      setIsResending(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-[#082E5B] to-slate-950 flex items-center justify-center p-4 sm:p-6 relative overflow-hidden">
      {/* Background Ambient Glows */}
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-[#00D1A3]/15 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-sky-500/15 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-lg bg-white rounded-2xl shadow-2xl p-6 sm:p-8 border border-slate-100 z-10 space-y-6">
        {/* Brand Logo */}
        <div className="flex flex-col items-center text-center space-y-2">
          <TaxorynLogo variant="horizontal" theme="light" size="md" />
          <h2 className="text-xl font-black text-slate-900 tracking-tight mt-3">
            {isSuccess
              ? 'Account Activated!'
              : isValidating
              ? 'Verifying Activation Token...'
              : tokenInfo?.requiresPasswordSetup
              ? 'Complete Your Account Setup'
              : 'Account Activation'}
          </h2>
          <p className="text-xs text-slate-500">
            {isSuccess
              ? 'Your Taxoryn account is now fully active and ready to use'
              : isValidating
              ? 'Please wait while we verify your invitation details'
              : tokenInfo?.requiresPasswordSetup
              ? 'Set your account password to activate your Taxoryn access'
              : 'Activate your workspace to get started'}
          </p>
        </div>

        {/* Loading Spinner */}
        {(isValidating || (isActivating && !tokenInfo?.requiresPasswordSetup)) && (
          <div className="py-8 flex flex-col items-center justify-center space-y-4 text-center">
            <div className="w-12 h-12 rounded-full border-4 border-[#00D1A3] border-t-transparent animate-spin" />
            <p className="text-xs font-semibold text-slate-600">
              {isValidating ? 'Validating activation token...' : 'Activating your workspace...'}
            </p>
          </div>
        )}

        {/* Success State */}
        {isSuccess && !isActivating && !isValidating && (
          <div className="space-y-6 text-center">
            <div className="w-16 h-16 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center mx-auto ring-8 ring-emerald-50/50">
              <CheckCircle2 className="w-8 h-8" />
            </div>

            <div className="space-y-2">
              <p className="text-base font-bold text-slate-900">
                Welcome to Taxoryn{tokenInfo?.organizationName ? `, ${tokenInfo.organizationName}` : ''}!
              </p>
              <p className="text-xs text-slate-600 leading-relaxed">
                {tokenInfo?.requiresPasswordSetup
                  ? 'Your password has been set securely and your account is now active.'
                  : 'Your account credentials and email have been successfully verified.'}{' '}
                You can now log in with your email and password.
              </p>
            </div>

            <Link to="/login" className="block w-full">
              <Button
                variant="primary"
                size="lg"
                className="w-full bg-[#00D1A3] hover:bg-[#00b88f] text-slate-950 font-bold py-3 text-sm shadow-md"
              >
                <span>Proceed to Login</span>
                <ArrowRight className="w-4 h-4 ml-2" />
              </Button>
            </Link>
          </div>
        )}

        {/* Password Setup Form (For Invited Employees) */}
        {!isSuccess && !isValidating && tokenInfo?.valid && tokenInfo?.requiresPasswordSetup && (
          <form onSubmit={handlePasswordSubmit} className="space-y-5">
            {/* Account Details Box */}
            <div className="p-3.5 bg-slate-50 border border-slate-200/80 rounded-xl space-y-2 text-xs">
              {tokenInfo.organizationName && (
                <div className="flex items-center gap-2 text-slate-700">
                  <Building2 className="w-4 h-4 text-slate-400 shrink-0" />
                  <span className="font-semibold text-slate-500">Practice:</span>
                  <span className="font-bold text-slate-900">{tokenInfo.organizationName}</span>
                </div>
              )}
              {tokenInfo.userFullName && (
                <div className="flex items-center gap-2 text-slate-700">
                  <UserIcon className="w-4 h-4 text-slate-400 shrink-0" />
                  <span className="font-semibold text-slate-500">Name:</span>
                  <span className="font-medium text-slate-800">{tokenInfo.userFullName}</span>
                </div>
              )}
              {tokenInfo.email && (
                <div className="flex items-center gap-2 text-slate-700">
                  <Mail className="w-4 h-4 text-slate-400 shrink-0" />
                  <span className="font-semibold text-slate-500">Email:</span>
                  <span className="font-medium text-slate-800">{tokenInfo.email}</span>
                </div>
              )}
            </div>

            {formError && (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-xs font-semibold text-rose-700 flex items-start gap-2">
                <ShieldAlert className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span>{formError}</span>
              </div>
            )}

            {error && (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-xs font-semibold text-rose-700 flex items-start gap-2">
                <ShieldAlert className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span>{error}</span>
              </div>
            )}

            {/* Password Input */}
            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-700">
                New Password <span className="text-rose-500">*</span>
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Create a strong password"
                  className="w-full pl-9 pr-10 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-[#00D1A3] focus:border-transparent"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-0.5"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {/* Confirm Password Input */}
            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-700">
                Confirm Password <span className="text-rose-500">*</span>
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Re-enter your password"
                  className="w-full pl-9 pr-10 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-[#00D1A3] focus:border-transparent"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-0.5"
                >
                  {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {/* Password Policy Checklist */}
            <div className="p-3 bg-slate-50 border border-slate-200/60 rounded-xl space-y-1.5 text-[11px]">
              <span className="font-semibold text-slate-600 block mb-1">Password Requirements:</span>
              <div className="grid grid-cols-2 gap-1.5">
                <div className={`flex items-center gap-1.5 ${hasMinLength ? 'text-emerald-600' : 'text-slate-500'}`}>
                  {hasMinLength ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <X className="w-3.5 h-3.5 text-slate-400" />}
                  <span>8+ characters</span>
                </div>
                <div className={`flex items-center gap-1.5 ${hasUppercase ? 'text-emerald-600' : 'text-slate-500'}`}>
                  {hasUppercase ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <X className="w-3.5 h-3.5 text-slate-400" />}
                  <span>Uppercase letter</span>
                </div>
                <div className={`flex items-center gap-1.5 ${hasLowercase ? 'text-emerald-600' : 'text-slate-500'}`}>
                  {hasLowercase ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <X className="w-3.5 h-3.5 text-slate-400" />}
                  <span>Lowercase letter</span>
                </div>
                <div className={`flex items-center gap-1.5 ${hasNumber ? 'text-emerald-600' : 'text-slate-500'}`}>
                  {hasNumber ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <X className="w-3.5 h-3.5 text-slate-400" />}
                  <span>Number (0-9)</span>
                </div>
                <div className={`flex items-center gap-1.5 ${hasSpecial ? 'text-emerald-600' : 'text-slate-500'}`}>
                  {hasSpecial ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <X className="w-3.5 h-3.5 text-slate-400" />}
                  <span>Special character</span>
                </div>
                <div className={`flex items-center gap-1.5 ${passwordsMatch ? 'text-emerald-600' : 'text-slate-500'}`}>
                  {passwordsMatch ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <X className="w-3.5 h-3.5 text-slate-400" />}
                  <span>Passwords match</span>
                </div>
              </div>
            </div>

            {/* Submit Button */}
            <Button
              type="submit"
              variant="primary"
              size="lg"
              disabled={isActivating || !isPasswordValid}
              className="w-full bg-[#00D1A3] hover:bg-[#00b88f] text-slate-950 font-bold py-3 text-xs shadow-md disabled:opacity-50"
            >
              {isActivating ? 'Setting Password & Activating...' : 'Set Password & Complete Activation'}
            </Button>
          </form>
        )}

        {/* Missing Token Alert */}
        {!token && !isSuccess && !isValidating && (
          <div className="space-y-4">
            <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-xs text-amber-800 space-y-2">
              <div className="flex items-center gap-2 font-bold text-amber-900">
                <ShieldAlert className="w-4 h-4 text-amber-600 shrink-0" />
                <span>Activation Token Missing</span>
              </div>
              <p className="text-amber-700">
                No activation token was detected in the URL. Please click the activation link directly from the email sent to your address.
              </p>
            </div>
          </div>
        )}

        {/* Error Alert (when token invalid or activation failed) */}
        {error && !isValidating && !tokenInfo?.valid && (
          <div className="space-y-4">
            <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs font-semibold text-rose-700 flex items-start gap-2">
              <ShieldAlert className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          </div>
        )}

        {/* Resend Activation Form */}
        {(!token || error || !tokenInfo?.valid) && !isSuccess && !isValidating && (
          <div className="pt-4 border-t border-slate-100 space-y-4">
            <div className="space-y-1">
              <h3 className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                <RefreshCw className="w-3.5 h-3.5 text-slate-500" />
                <span>Resend Activation Email</span>
              </h3>
              <p className="text-[11px] text-slate-500">
                Enter your account email to receive a fresh activation link.
              </p>
            </div>

            {resendSuccess ? (
              <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-xs font-medium text-emerald-800">
                If an inactive or invited account exists for that email, a fresh activation link has been sent. Please check your inbox.
              </div>
            ) : (
              <form onSubmit={handleResend} className="space-y-3">
                {resendError && (
                  <p className="text-[11px] text-rose-600 font-medium">{resendError}</p>
                )}
                <div>
                  <div className="relative">
                    <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                      type="email"
                      required
                      value={resendEmail}
                      onChange={(e) => setResendEmail(e.target.value)}
                      placeholder="user@yourfirm.com"
                      className="w-full pl-9 pr-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-[#00D1A3] focus:border-transparent"
                    />
                  </div>
                </div>

                <Button
                  type="submit"
                  variant="outline"
                  size="sm"
                  disabled={isResending}
                  className="w-full font-bold text-xs"
                >
                  {isResending ? 'Sending...' : 'Send Fresh Activation Link'}
                </Button>
              </form>
            )}

            <div className="text-center pt-2">
              <Link to="/login" className="text-xs text-slate-500 hover:text-slate-800 font-medium transition-colors">
                Already activated? <span className="text-[#082E5B] font-bold">Sign In</span>
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
