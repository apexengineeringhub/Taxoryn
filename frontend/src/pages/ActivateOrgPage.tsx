import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { CheckCircle2, ShieldAlert, ArrowRight, Mail, RefreshCw } from 'lucide-react';
import { TaxorynLogo } from '../components/common/TaxorynLogo';
import { Button } from '../components/common/Button';
import { authApi } from '../api/endpoints';

export const ActivateOrgPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';

  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Resend activation state
  const [resendEmail, setResendEmail] = useState('');
  const [isResending, setIsResending] = useState(false);
  const [resendSuccess, setResendSuccess] = useState(false);
  const [resendError, setResendError] = useState<string | null>(null);

  useEffect(() => {
    if (token) {
      handleActivation(token);
    }
  }, [token]);

  const handleActivation = async (rawToken: string) => {
    setIsLoading(true);
    setError(null);

    try {
      await authApi.activateOrg(rawToken.trim());
      setIsSuccess(true);
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Invalid or expired activation link. Please request a new activation email below.';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  const handleResend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resendEmail.trim()) {
      setResendError('Please enter your administrator email address.');
      return;
    }

    setIsResending(true);
    setResendError(null);
    setResendSuccess(false);

    try {
      await authApi.resendActivation(resendEmail.trim());
      setResendSuccess(true);
    } catch (err: any) {
      setResendError(err?.response?.data?.message || 'Failed to resend activation email. Please try again.');
    } finally {
      setIsResending(false);
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
            {isSuccess
              ? 'Organization Activated!'
              : isLoading
              ? 'Activating Your Organization...'
              : 'Organization Activation'}
          </h2>
          <p className="text-xs text-slate-500">
            {isSuccess
              ? 'Your Taxoryn organization account is now active and ready to use'
              : isLoading
              ? 'Please wait while we verify your activation token'
              : 'Activate your multi-tenant workspace to get started'}
          </p>
        </div>

        {/* Loading Spinner */}
        {isLoading && (
          <div className="py-8 flex flex-col items-center justify-center space-y-4 text-center">
            <div className="w-12 h-12 rounded-full border-4 border-[#00D1A3] border-t-transparent animate-spin" />
            <p className="text-xs font-semibold text-slate-600">Verifying activation token...</p>
          </div>
        )}

        {/* Success State */}
        {isSuccess && !isLoading && (
          <div className="space-y-6 text-center">
            <div className="w-16 h-16 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center mx-auto ring-8 ring-emerald-50/50">
              <CheckCircle2 className="w-8 h-8" />
            </div>

            <div className="space-y-2">
              <p className="text-sm font-bold text-slate-900">
                Welcome to Taxoryn!
              </p>
              <p className="text-xs text-slate-600 leading-relaxed">
                Your organization and administrator credentials have been verified. You can now log in with your email and password.
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

        {/* Missing Token Alert (if no token in URL and not successful) */}
        {!token && !isSuccess && !isLoading && (
          <div className="space-y-4">
            <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-xs text-amber-800 space-y-2">
              <div className="flex items-center gap-2 font-bold text-amber-900">
                <ShieldAlert className="w-4 h-4 text-amber-600 shrink-0" />
                <span>Activation Token Missing</span>
              </div>
              <p className="text-amber-700">
                No activation token was detected in the URL. Please click the activation link directly from the email sent to your administrator address.
              </p>
            </div>
          </div>
        )}

        {/* Error Alert */}
        {error && !isLoading && (
          <div className="space-y-4">
            <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs font-semibold text-rose-700 flex items-start gap-2">
              <ShieldAlert className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          </div>
        )}

        {/* Resend Activation Form (shown if error or missing token and not success) */}
        {(!token || error) && !isSuccess && !isLoading && (
          <div className="pt-4 border-t border-slate-100 space-y-4">
            <div className="space-y-1">
              <h3 className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                <RefreshCw className="w-3.5 h-3.5 text-slate-500" />
                <span>Resend Activation Email</span>
              </h3>
              <p className="text-[11px] text-slate-500">
                Enter your administrator email to receive a fresh activation link.
              </p>
            </div>

            {resendSuccess ? (
              <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-xs font-medium text-emerald-800">
                If an inactive account exists for that email, a fresh activation link has been sent. Please check your inbox.
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
                      placeholder="admin@yourfirm.com"
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
