import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail, ArrowLeft, ArrowRight, CheckCircle2, ShieldAlert } from 'lucide-react';
import { TaxorynLogo } from '../components/common/TaxorynLogo';
import { Button } from '../components/common/Button';
import { authApi } from '../api/endpoints';

export const ForgotPasswordPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) return;

    setIsLoading(true);
    setError(null);

    try {
      await authApi.forgotPassword(email.trim());
      setIsSubmitted(true);
    } catch (err: any) {
      // Even in error scenarios (unless strict network failure), stay resilient
      const msg = err?.response?.data?.message || 'Unable to process request right now. Please try again.';
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
        {/* Brand Logo Lockup */}
        <div className="flex flex-col items-center text-center space-y-2">
          <TaxorynLogo variant="horizontal" theme="light" size="md" />
          <h2 className="text-xl font-black text-slate-900 tracking-tight mt-3">
            {isSubmitted ? 'Check Your Email' : 'Forgot Password'}
          </h2>
          <p className="text-xs text-slate-500">
            {isSubmitted
              ? 'Password recovery instructions have been dispatched'
              : 'Enter your email address to receive password recovery instructions'}
          </p>
        </div>

        {error && (
          <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs font-semibold text-rose-700 flex items-start gap-2">
            <ShieldAlert className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        {isSubmitted ? (
          <div className="space-y-5">
            <div className="p-4 rounded-xl bg-[#E6FBF6] border border-[#00D1A3]/30 text-xs text-slate-700 space-y-2.5">
              <div className="flex items-center gap-2 font-bold text-[#008A68]">
                <CheckCircle2 className="w-4 h-4 text-[#00D1A3] shrink-0" />
                <span>Instructions Sent</span>
              </div>
              <p className="leading-relaxed">
                If an account exists for <strong className="text-slate-900">{email}</strong>, you will receive password reset instructions shortly.
              </p>
              <p className="text-[11px] text-slate-500">
                Please check your inbox (and junk/spam folder). The secure reset link expires in <strong>30 minutes</strong>.
              </p>
            </div>

            <div className="pt-2 flex flex-col gap-3">
              <Link to="/login" className="w-full">
                <Button variant="primary" className="w-full font-bold py-2.5">
                  Return to Sign In
                </Button>
              </Link>
              <button
                type="button"
                onClick={() => {
                  setIsSubmitted(false);
                  setEmail('');
                }}
                className="text-xs font-semibold text-slate-500 hover:text-slate-800 transition-colors text-center"
              >
                Try a different email address
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1.5">
                Registered Email Address
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  required
                  autoFocus
                  placeholder="name@taxpractice.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full text-xs pl-10 pr-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-[#00D1A3] focus:border-transparent transition-all"
                />
              </div>
              <p className="text-[11px] text-slate-500 mt-1.5">
                Enter the email address associated with your Taxoryn account.
              </p>
            </div>

            <Button
              type="submit"
              variant="primary"
              className="w-full font-bold py-2.5 mt-2"
              isLoading={isLoading}
              rightIcon={<ArrowRight className="w-4 h-4" />}
            >
              Send Reset Link
            </Button>

            <div className="pt-4 border-t border-slate-100 flex items-center justify-between text-xs">
              <Link
                to="/login"
                className="flex items-center gap-1.5 text-slate-600 hover:text-[#082E5B] font-semibold transition-colors"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>Back to Sign In</span>
              </Link>
              <Link
                to="/register"
                className="text-[#00B388] hover:text-[#082E5B] font-bold transition-colors"
              >
                New Practice? Register &rarr;
              </Link>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};