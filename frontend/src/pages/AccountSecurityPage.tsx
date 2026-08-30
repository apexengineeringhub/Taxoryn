import React, { useState } from 'react';
import { Shield, Lock, Eye, EyeOff, Check, X, CheckCircle2, ShieldAlert, KeyRound, UserCheck } from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../api/endpoints';

export const AccountSecurityPage: React.FC = () => {
  const { user } = useAuth();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [isLoading, setIsLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Validation rules
  const hasMinLength = newPassword.length >= 8;
  const hasUppercase = /[A-Z]/.test(newPassword);
  const hasLowercase = /[a-z]/.test(newPassword);
  const hasNumberOrSpecial = /[0-9@#$%^&+=!._-]/.test(newPassword);
  const isDifferent = newPassword.length > 0 && currentPassword.length > 0 && newPassword !== currentPassword;
  const isMatch = newPassword.length > 0 && newPassword === confirmPassword;

  const isFormValid =
    currentPassword.length > 0 &&
    hasMinLength &&
    hasUppercase &&
    hasLowercase &&
    hasNumberOrSpecial &&
    isDifferent &&
    isMatch;

  const handleResetForm = () => {
    setCurrentPassword('');
    setNewPassword('');
    setConfirmPassword('');
    setErrorMessage(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    if (!currentPassword) {
      setErrorMessage('Current password is required.');
      return;
    }

    if (!isDifferent) {
      setErrorMessage('New password must be different from your current password.');
      return;
    }

    if (!isMatch) {
      setErrorMessage('New password and confirm password do not match.');
      return;
    }

    if (!hasMinLength || !hasUppercase || !hasLowercase || !hasNumberOrSpecial) {
      setErrorMessage('Please ensure your new password satisfies all security requirements.');
      return;
    }

    setIsLoading(true);

    try {
      await authApi.changePassword({
        currentPassword: currentPassword.trim(),
        newPassword: newPassword.trim(),
        confirmPassword: confirmPassword.trim(),
      });
      setSuccessMessage('Your password has been changed successfully.');
      handleResetForm();
    } catch (err: any) {
      const msg =
        err?.response?.data?.message ||
        'Failed to change password. Please ensure your current password is correct and try again.';
      setErrorMessage(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto pb-12">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-xl font-bold text-slate-900 flex items-center gap-2.5">
            <Shield className="w-5 h-5 text-[#00D1A3]" />
            <span>Account Security & Password</span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Update your authentication credentials and secure your Taxoryn practice account.
          </p>
        </div>
      </div>

      {/* User Information Card */}
      <Card className="p-5 border border-slate-200/80 bg-white">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-[#082E5B] to-slate-900 text-white flex items-center justify-center font-bold text-base shadow-sm">
            {user?.firstName ? user.firstName.charAt(0).toUpperCase() : 'U'}
          </div>
          <div className="flex-1 min-w-0">
            <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <span>{user?.firstName} {user?.lastName || ''}</span>
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                <UserCheck className="w-3 h-3 text-emerald-600" />
                Active Account
              </span>
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">{user?.email}</p>
          </div>
        </div>
      </Card>

      {/* Change Password Form Card */}
      <Card className="p-6 border border-slate-200 bg-white space-y-6">
        <div className="border-b border-slate-100 pb-4">
          <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
            <KeyRound className="w-4 h-4 text-[#00B388]" />
            <span>Change Password</span>
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            Enter your current password followed by your new desired password.
          </p>
        </div>

        {/* Success Banner */}
        {successMessage && (
          <div className="p-4 rounded-xl bg-[#E6FBF6] border border-[#00D1A3]/30 text-xs text-slate-800 flex items-start gap-3 animate-in fade-in">
            <CheckCircle2 className="w-4 h-4 text-[#00D1A3] shrink-0 mt-0.5" />
            <div className="space-y-1">
              <span className="font-bold text-[#008A68]">Success</span>
              <p>{successMessage}</p>
            </div>
          </div>
        )}

        {/* Error Alert */}
        {errorMessage && (
          <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-700 flex items-start gap-3 animate-in fade-in">
            <ShieldAlert className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
            <div className="space-y-1">
              <span className="font-bold text-rose-800">Security Alert</span>
              <p>{errorMessage}</p>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Current Password */}
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1.5">
              Current Password
            </label>
            <div className="relative max-w-lg">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type={showCurrentPassword ? 'text' : 'password'}
                required
                placeholder="••••••••••••"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                className="w-full text-xs pl-10 pr-10 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-[#00D1A3] focus:border-transparent transition-all"
              />
              <button
                type="button"
                onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                {showCurrentPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          {/* New Password */}
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1.5">
              New Password
            </label>
            <div className="relative max-w-lg">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type={showNewPassword ? 'text' : 'password'}
                required
                placeholder="••••••••••••"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="w-full text-xs pl-10 pr-10 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-[#00D1A3] focus:border-transparent transition-all"
              />
              <button
                type="button"
                onClick={() => setShowNewPassword(!showNewPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                {showNewPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          {/* Confirm New Password */}
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1.5">
              Confirm New Password
            </label>
            <div className="relative max-w-lg">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type={showConfirmPassword ? 'text' : 'password'}
                required
                placeholder="••••••••••••"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="w-full text-xs pl-10 pr-10 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-[#00D1A3] focus:border-transparent transition-all"
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          {/* Password Security Policy Checklist */}
          <div className="max-w-lg p-3.5 bg-slate-50 rounded-xl border border-slate-200/80 space-y-2 text-[11px]">
            <div className="font-bold text-slate-600 text-[10px] uppercase tracking-wider mb-1">
              Password Security Rules
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
            {currentPassword && newPassword && (
              <div className={`flex items-center gap-1.5 ${isDifferent ? 'text-emerald-700 font-semibold' : 'text-rose-600 font-semibold'}`}>
                {isDifferent ? <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0" /> : <X className="w-3.5 h-3.5 shrink-0" />}
                <span>{isDifferent ? 'New password differs from current password' : 'New password cannot be the same as current password'}</span>
              </div>
            )}
            {confirmPassword && (
              <div className={`flex items-center gap-1.5 ${isMatch ? 'text-emerald-700 font-semibold' : 'text-rose-600 font-semibold'}`}>
                {isMatch ? <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0" /> : <X className="w-3.5 h-3.5 shrink-0" />}
                <span>{isMatch ? 'Passwords match' : 'Passwords do not match'}</span>
              </div>
            )}
          </div>

          {/* Form Actions */}
          <div className="flex items-center gap-3 pt-2">
            <Button
              type="submit"
              variant="primary"
              isLoading={isLoading}
              disabled={!isFormValid}
              className="font-bold px-6 py-2.5"
            >
              Change Password
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={handleResetForm}
              disabled={isLoading}
              className="font-semibold px-5 py-2.5"
            >
              Cancel
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
};