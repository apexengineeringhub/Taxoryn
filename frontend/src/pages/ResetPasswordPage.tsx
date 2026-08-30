import React, { useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { Lock, Eye, EyeOff, CheckCircle2, ShieldAlert, ArrowRight, Check, X, ArrowLeft } from 'lucide-react';
import { TaxorynLogo } from '../components/common/TaxorynLogo';
import { Button } from '../components/common/Button';
import { authApi } from '../api/endpoints';

export const ResetPasswordPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const token = searchParams.get('token') || '';

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Password Complexity Validation Rules
  const hasMinLength = newPassword.length >= 8;
  const hasUppercase = /[A-Z]/.test(newPassword);
  const hasLowercase = /[a-z]/.test(newPassword);
  const hasNumberOrSpecial = /[0-9@#$%^&+=!._-]/.test(newPassword);
  const isMatch = newPassword.length > 0 && newPassword === confirmPassword;

  const isFormValid = hasMinLength && hasUppercase && hasLowercase && hasNumberOrSpecial && isMatch;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) {
      setError('Missing reset token. Please request a new password reset link.');
      return;
    }

    if (!isFormValid) {
      setError('Please satisfy all password security requirements and ensure passwords match.');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      await authApi.resetPassword({
        token: token.trim(),
        newPassword: newPassword.trim(),
      });
      setIsSuccess(true);
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Invalid or expired password reset link. Please request a fresh link.';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-[#082E5B] to-slate-950 flex items-center justify-center p-4 sm:p-6 relative overflow-hidden">
      {/* Background Ambient Glows */}
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-[#00D1A3]/15 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-sky-500/15 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-md bg-white rounded-2xl shadow-2xl p-6 sm:p-8 border border-slate-100 z-10 space-y-6">
        {/* Brand Logo */}
        <div className="flex flex-col items-center text-center space-y-2">
          <TaxorynLogo variant="horizontal" theme="light" size="md" />
          <h2 className="text-xl font-black text-slate-900 tracking-tight mt-3">
            {isSuccess ? 'Password Reset Complete' : 'Set New Password'}
          </h2>
          <p className="text-xs text-slate-500">
            {isSuccess
              ? 'Your password has been updated securely'
              : 'Create a strong, new password for your Taxoryn account'}
          </p>
        </div>

        {/* Missing Token Alert */}
        {!token && !isSuccess && (
          <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-xs text-amber-800 space-y-3">
            <div className="flex items-center gap-2 font-bold text-amber-900">
              <ShieldAlert className="w-4 h-4 text-amber-600 shrink-0" />
              <span>Invalid Reset Link</span>
            </div>
            <p className="text-amber-700">
              No reset token was found in the URL. Please click the link directly from your reset email or request a new one.
            </p>
            <Link to="/forgot-password" className="block w-full">
              <Button variant="outline" size="sm" className="w-full font-bold">
                Request New Reset Link
              </Button>
            </Link>
          </div>
        )}

        {/* Error Alert */}
        {error && (
          <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs font-semibold text-rose-700 flex items-start gap-2">
            <ShieldAlert className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
            <div className="space-y-2">
              <span>{error}</span>
              {(error.includes('expired') || error.includes('Invalid')) && (
                <div>
                  <Link
                    to="/forgot-password"
                    className="text-xs text-rose-800 underline font-bold hover:text-rose-950 block mt-1"
                  >
                    Click here to request a new reset link &rarr;
                  </Link>
                </div>
              )}
            </div>
          </div>
        )}

        {isSuccess ? (
          <div className="space-y-5">
            <div className="p-4 rounded-xl bg-[#E6FBF6] border border-[#00D1A3]/30 text-xs text-slate-700 space-y-2.5">
              <div className="flex items-center gap-2 font-bold text-[#008A68]">
                <CheckCircle2 className="w-5 h-5 text-[#00D1A3] shrink-0" />
                <span className="text-sm">Password Updated Successfully!</span>
              </div>
              <p className="leading-relaxed">
                You can now use your newly configured password to access your Taxoryn practice portal or customer account.
              </p>
            </div>

            <Button
              variant="primary"
              className="w-full font-bold py-2.5"
              onClick={() => navigate('/login')}
              rightIcon={<ArrowRight className="w-4 h-4" />}
            >
              Sign In to Taxoryn
            </Button>
          </div>
        ) : (
          token && (
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* New Password Input */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">
                  New Password
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    autoFocus
                    placeholder="••••••••••••"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    className="w-full text-xs pl-10 pr-10 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-[#00D1A3] focus:border-transparent transition-all"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {/* Confirm Password Input */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">
                  Confirm New Password
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    placeholder="••••••••••••"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    className="w-full text-xs pl-10 pr-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-[#00D1A3] focus:border-transparent transition-all"
                  />
                </div>
              </div>

              {/* Password Strength Requirements Checklist */}
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80 space-y-1.5 text-[11px]">
                <div className="font-bold text-slate-600 text-[10px] uppercase tracking-wider mb-1">
                  Password Requirements
                </div>
                <div className={`flex items-center gap-1.5 ${hasMinLength ? 'text-emerald-700 font-semibold' : 'text-slate-400'}`}>
                  {hasMinLength ? <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0" /> : <X className="w-3.5 h-3.5 shrink-0" />}
                  <span>At least 8 characters long</span>
                </div>
                <div className={`flex items-center gap-1.5 ${hasUppercase ? 'text-emerald-700 font-semibold' : 'text-slate-400'}`}>
                  {hasUppercase ? <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0" /> : <X className="w-3.5 h-3.5 shrink-0" />}
                  <span>At least one uppercase letter (A-Z)</span>
                </div>
                <div className={`flex items-center gap-1.5 ${hasLowercase ? 'text-emerald-700 font-semibold' : 'text-slate-400'}`}>
                  {hasLowercase ? <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0" /> : <X className="w-3.5 h-3.5 shrink-0" />}
                  <span>At least one lowercase letter (a-z)</span>
                </div>
                <div className={`flex items-center gap-1.5 ${hasNumberOrSpecial ? 'text-emerald-700 font-semibold' : 'text-slate-400'}`}>
                  {hasNumberOrSpecial ? <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0" /> : <X className="w-3.5 h-3.5 shrink-0" />}
                  <span>At least one number or special character (@$!%*#?&)</span>
                </div>
                {confirmPassword && (
                  <div className={`flex items-center gap-1.5 ${isMatch ? 'text-emerald-700 font-semibold' : 'text-rose-600 font-semibold'}`}>
                    {isMatch ? <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0" /> : <X className="w-3.5 h-3.5 shrink-0" />}
                    <span>{isMatch ? 'Passwords match' : 'Passwords do not match'}</span>
                  </div>
                )}
              </div>

              <Button
                type="submit"
                variant="primary"
                className="w-full font-bold py-2.5"
                isLoading={isLoading}
                disabled={!isFormValid}
                rightIcon={<ArrowRight className="w-4 h-4" />}
              >
                Reset Password
              </Button>

              <div className="pt-3 border-t border-slate-100 text-center text-xs">
                <Link
                  to="/login"
                  className="inline-flex items-center gap-1.5 text-slate-500 hover:text-slate-800 font-semibold transition-colors"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  <span>Back to Sign In</span>
                </Link>
              </div>
            </form>
          )
        )}
      </div>
    </div>
  );
};