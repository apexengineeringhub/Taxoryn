import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  ShieldCheck,
  Lock,
  Mail,
  ArrowRight,
  FolderKanban,
  ShieldCheckIcon,
  TrendingUp,
  Users2,
  Sparkles,
  CheckCircle2,
  GraduationCap,
  BookOpen,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { TaxorynLogo } from '../components/common/TaxorynLogo';

export const LoginPage: React.FC = () => {
  const isDemoEnvironment = Boolean(import.meta.env.DEV || import.meta.env.VITE_ENABLE_DEMO_LOGIN === 'true');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    // Auto recall last registered or logged in email
    const savedEmail = localStorage.getItem('taxoryn_last_user_email');
    if (savedEmail) {
      setEmail(savedEmail);
      if (isDemoEnvironment) {
        setPassword('Password123!');
      }
    } else if (isDemoEnvironment) {
      setEmail('pawanadv@gmail.com');
      setPassword('Password123!');
    }
  }, [isDemoEnvironment]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);
    try {
      const loggedInUser = await login(email.trim(), password);
      localStorage.setItem('taxoryn_last_user_email', email.trim());
      const roleCodes = (loggedInUser?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
      if (roleCodes.some((r: string) => ['MARKETPLACE_CUSTOMER'].includes(r))) {
        navigate('/marketplace/customer/dashboard');
      } else if (roleCodes.some((r: string) => ['CLIENT_USER', 'CLIENT_ADMIN'].includes(r))) {
        navigate('/portal');
      } else {
        navigate('/dashboard');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid email or password. Please verify credentials.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuickFill = (demoEmail: string) => {
    if (!isDemoEnvironment) return;
    setEmail(demoEmail);
    setPassword('Password123!');
    setError('');
  };

  const [demoTab, setDemoTab] = useState<'mundeshwari' | 'apex'>('mundeshwari');

  return (
    <div className="min-h-screen bg-[#07152B] flex items-center justify-center p-3 sm:p-4 lg:p-8 relative overflow-hidden">
      {/* Dynamic Background Glows */}
      <div className="absolute -top-40 -left-40 w-[500px] h-[500px] bg-[#00D1A3]/15 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-[500px] h-[500px] bg-[#082E5B]/50 rounded-full blur-[120px] pointer-events-none" />

      {/* Main 2-Column Responsive Card */}
      <div className="w-full max-w-5xl bg-white rounded-2xl shadow-2xl overflow-hidden grid grid-cols-1 lg:grid-cols-12 border border-slate-700/30 z-10 my-2">
        
        {/* LEFT COLUMN: Taxoryn Brand Experience & Knowledge Hub (Desktop Only) */}
        <div className="hidden lg:flex lg:col-span-5 bg-gradient-to-br from-[#082E5B] via-[#07152B] to-[#070C1A] p-7 xl:p-8 flex-col justify-between text-white relative border-r border-white/10 overflow-hidden">
          {/* Subtle Ambient Glow */}
          <div className="absolute top-0 right-0 w-36 h-36 bg-[#00D1A3]/10 rounded-full blur-3xl pointer-events-none" />

          <div className="space-y-5 z-10">
            {/* Full Brand Logo */}
            <div className="pt-1">
              <TaxorynLogo variant="full" theme="dark" size="lg" />
            </div>

            {/* Value Proposition */}
            <p className="text-xs text-slate-300 leading-relaxed">
              The modern, cloud-native practice operating system for Chartered Accountants, Tax Practitioners, and Indian Enterprises.
            </p>

            {/* 4 Core Pillars */}
            <div className="grid grid-cols-2 gap-2.5">
              <div className="p-2.5 rounded-xl bg-white/5 border border-white/10 space-y-1 hover:bg-white/10 transition-colors">
                <FolderKanban className="w-3.5 h-3.5 text-[#00D1A3]" />
                <div className="text-[11px] font-bold text-white">Manage</div>
                <div className="text-[9px] text-slate-400">Clients & Workflows</div>
              </div>

              <div className="p-2.5 rounded-xl bg-white/5 border border-white/10 space-y-1 hover:bg-white/10 transition-colors">
                <ShieldCheckIcon className="w-3.5 h-3.5 text-[#00D1A3]" />
                <div className="text-[11px] font-bold text-white">Comply</div>
                <div className="text-[9px] text-slate-400">GST, ITR & TDS</div>
              </div>

              <div className="p-2.5 rounded-xl bg-white/5 border border-white/10 space-y-1 hover:bg-white/10 transition-colors">
                <TrendingUp className="w-3.5 h-3.5 text-[#00D1A3]" />
                <div className="text-[11px] font-bold text-white">Grow</div>
                <div className="text-[9px] text-slate-400">Billing & Growth</div>
              </div>

              <div className="p-2.5 rounded-xl bg-white/5 border border-white/10 space-y-1 hover:bg-white/10 transition-colors">
                <Users2 className="w-3.5 h-3.5 text-[#00D1A3]" />
                <div className="text-[11px] font-bold text-white">Empower</div>
                <div className="text-[9px] text-slate-400">Team & Portals</div>
              </div>
            </div>

            {/* Left Column Featured Knowledge Hub (Fills the space gracefully) */}
            <Link
              to="/learn"
              className="block p-3.5 rounded-xl bg-gradient-to-br from-white/10 to-white/5 border border-[#00D1A3]/30 hover:border-[#00D1A3] transition-all group shadow-md"
            >
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <div className="w-7 h-7 rounded-lg bg-[#00D1A3]/20 text-[#00D1A3] flex items-center justify-center text-xs font-black">
                    <GraduationCap className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-xs font-black text-white group-hover:text-[#00D1A3] transition-colors flex items-center gap-1.5">
                      <span>Taxoryn Knowledge Hub</span>
                      <span className="px-1.5 py-0.2 rounded text-[8px] font-bold bg-[#00D1A3] text-slate-950 uppercase">Learn</span>
                    </div>
                  </div>
                </div>
                <ArrowRight className="w-4 h-4 text-[#00D1A3] group-hover:translate-x-1 transition-transform" />
              </div>

              <p className="text-[11px] text-slate-300 leading-snug mb-2.5">
                Masterclasses, statutory circular breakdowns, and live practical compliance tutorials.
              </p>

              {/* Topic Badges */}
              <div className="flex flex-wrap gap-1 pt-2 border-t border-white/10">
                <span className="px-2 py-0.5 rounded-full text-[9px] font-bold bg-white/10 text-slate-200">#GST</span>
                <span className="px-2 py-0.5 rounded-full text-[9px] font-bold bg-white/10 text-slate-200">#ITR-Filing</span>
                <span className="px-2 py-0.5 rounded-full text-[9px] font-bold bg-white/10 text-slate-200">#TDS</span>
                <span className="px-2 py-0.5 rounded-full text-[9px] font-bold bg-[#00D1A3]/20 text-[#00D1A3]">Free Access</span>
              </div>
            </Link>
          </div>

          {/* Bottom Trust Motto */}
          <div className="pt-4 border-t border-white/10 z-10 flex items-center justify-between">
            <p className="text-[11px] font-medium text-slate-300 italic">
              &ldquo;Focus on Your Expertise. We&apos;ll Manage the Rest.&rdquo;
            </p>
            <span className="text-[10px] text-slate-400 font-mono">v1.0 Pro</span>
          </div>
        </div>

        {/* RIGHT COLUMN: Sign In & Authentication */}
        <div className="lg:col-span-7 p-4 sm:p-6 lg:p-8 flex flex-col justify-between space-y-5 sm:space-y-6">
          <div className="space-y-4 sm:space-y-5">
            {/* Header (Mobile shows logo, Desktop shows Welcome) */}
            <div className="flex flex-col items-center lg:items-start space-y-1 sm:space-y-2 text-center lg:text-left">
              <div className="lg:hidden mb-2">
                <TaxorynLogo variant="horizontal" theme="light" size="md" />
              </div>
              <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">Welcome Back!</h2>
              <p className="text-xs text-slate-500">Sign in to your Taxoryn Practice Dashboard</p>
            </div>

            {/* Quick Demo Fill Practice Selector Tabs (Dev / Demo Builds Only) */}
            {isDemoEnvironment && (
              <div className="bg-slate-50 border border-slate-200/90 rounded-xl p-3 sm:p-3.5 space-y-2 sm:space-y-2.5">
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-bold text-slate-600 uppercase tracking-wider flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5 text-[#00B388]" />
                    <span>Interactive Practice Profiles</span>
                  </span>
                  <span className="text-[9px] sm:text-[10px] font-semibold text-slate-500 bg-white px-2 py-0.5 rounded-full border border-slate-200/80 shadow-2xs">
                    1-Click Fill
                  </span>
                </div>

                {/* Firm Switcher Tabs */}
                <div className="grid grid-cols-2 gap-1 sm:gap-1.5 p-1 bg-slate-200/70 rounded-lg text-xs font-semibold">
                  <button
                    type="button"
                    onClick={() => setDemoTab('mundeshwari')}
                    className={`py-1.5 px-2 rounded-md transition-all truncate text-center text-[11px] sm:text-xs ${
                      demoTab === 'mundeshwari'
                        ? 'bg-white text-[#082E5B] shadow-xs font-bold'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    Maa Mundeshwari Tax
                  </button>
                  <button
                    type="button"
                    onClick={() => setDemoTab('apex')}
                    className={`py-1.5 px-2 rounded-md transition-all truncate text-center text-[11px] sm:text-xs ${
                      demoTab === 'apex'
                        ? 'bg-white text-[#082E5B] shadow-xs font-bold'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    Apex Tax Advisors
                  </button>
                </div>

                {/* Mundeshwari Accounts */}
                {demoTab === 'mundeshwari' && (
                  <div className="grid grid-cols-2 gap-1.5 sm:gap-2">
                    <button
                      type="button"
                      onClick={() => handleQuickFill('pawanadv@gmail.com')}
                      className={`p-2 sm:px-2.5 sm:py-2 rounded-lg border text-left text-xs font-semibold transition-all ${
                        email === 'pawanadv@gmail.com'
                          ? 'border-[#00D1A3] bg-[#E6FBF6] text-slate-900 ring-1 ring-[#00D1A3]'
                          : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-1">
                        <span className="font-bold text-[10px] sm:text-[11px] truncate">Pawan Pathak</span>
                        <span className="px-1 py-0.2 bg-purple-100 text-purple-700 text-[8px] sm:text-[9px] font-bold rounded shrink-0">Admin</span>
                      </div>
                      <span className="block text-[9px] sm:text-[10px] text-slate-500 truncate mt-0.5">pawanadv@...</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => handleQuickFill('pooja.joshi@maamundeshwari.com')}
                      className={`p-2 sm:px-2.5 sm:py-2 rounded-lg border text-left text-xs font-semibold transition-all ${
                        email === 'pooja.joshi@maamundeshwari.com'
                          ? 'border-[#00D1A3] bg-[#E6FBF6] text-slate-900 ring-1 ring-[#00D1A3]'
                          : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-1">
                        <span className="font-bold text-[10px] sm:text-[11px] truncate">Pooja Joshi</span>
                        <span className="px-1 py-0.2 bg-amber-100 text-amber-800 text-[8px] sm:text-[9px] font-bold rounded shrink-0">Staff</span>
                      </div>
                      <span className="block text-[9px] sm:text-[10px] text-slate-500 truncate mt-0.5">pooja.joshi@...</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => handleQuickFill('client.mundeshwari@maamundeshwari.com')}
                      className={`p-2 sm:px-2.5 sm:py-2 rounded-lg border text-left text-xs font-semibold transition-all ${
                        email === 'client.mundeshwari@maamundeshwari.com'
                          ? 'border-[#00D1A3] bg-[#E6FBF6] text-slate-900 ring-1 ring-[#00D1A3]'
                          : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-1">
                        <span className="font-bold text-[10px] sm:text-[11px] truncate">Mundeshwari Ent</span>
                        <span className="px-1 py-0.2 bg-sky-100 text-sky-700 text-[8px] sm:text-[9px] font-bold rounded shrink-0">Client Admin</span>
                      </div>
                      <span className="block text-[9px] sm:text-[10px] text-slate-500 truncate mt-0.5">client.mundeshwari@...</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => handleQuickFill('client.pawanassoc@maamundeshwari.com')}
                      className={`p-2 sm:px-2.5 sm:py-2 rounded-lg border text-left text-xs font-semibold transition-all ${
                        email === 'client.pawanassoc@maamundeshwari.com'
                          ? 'border-[#00D1A3] bg-[#E6FBF6] text-slate-900 ring-1 ring-[#00D1A3]'
                          : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-1">
                        <span className="font-bold text-[10px] sm:text-[11px] truncate">Pawan Associates</span>
                        <span className="px-1 py-0.2 bg-sky-100 text-sky-700 text-[8px] sm:text-[9px] font-bold rounded shrink-0">Client</span>
                      </div>
                      <span className="block text-[9px] sm:text-[10px] text-slate-500 truncate mt-0.5">client.pawanassoc@...</span>
                    </button>
                  </div>
                )}

                {/* Apex Tax Accounts */}
                {demoTab === 'apex' && (
                  <div className="grid grid-cols-2 gap-1.5 sm:gap-2">
                    <button
                      type="button"
                      onClick={() => handleQuickFill('admin@apextax.com')}
                      className={`p-2 sm:px-2.5 sm:py-2 rounded-lg border text-left text-xs font-semibold transition-all ${
                        email === 'admin@apextax.com'
                          ? 'border-[#00D1A3] bg-[#E6FBF6] text-slate-900 ring-1 ring-[#00D1A3]'
                          : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-1">
                        <span className="font-bold text-[10px] sm:text-[11px] truncate">Apex Admin</span>
                        <span className="px-1 py-0.2 bg-purple-100 text-purple-700 text-[8px] sm:text-[9px] font-bold rounded shrink-0">Admin</span>
                      </div>
                      <span className="block text-[9px] sm:text-[10px] text-slate-500 truncate mt-0.5">admin@apextax...</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => handleQuickFill('neha.sharma@apextax.com')}
                      className={`p-2 sm:px-2.5 sm:py-2 rounded-lg border text-left text-xs font-semibold transition-all ${
                        email === 'neha.sharma@apextax.com'
                          ? 'border-[#00D1A3] bg-[#E6FBF6] text-slate-900 ring-1 ring-[#00D1A3]'
                          : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-1">
                        <span className="font-bold text-[10px] sm:text-[11px] truncate">Neha Sharma</span>
                        <span className="px-1 py-0.2 bg-amber-100 text-amber-800 text-[8px] sm:text-[9px] font-bold rounded shrink-0">Staff</span>
                      </div>
                      <span className="block text-[9px] sm:text-[10px] text-slate-500 truncate mt-0.5">neha.sharma@...</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => handleQuickFill('client.sneha@apextax.com')}
                      className={`p-2 sm:px-2.5 sm:py-2 rounded-lg border text-left text-xs font-semibold transition-all ${
                        email === 'client.sneha@apextax.com'
                          ? 'border-[#00D1A3] bg-[#E6FBF6] text-slate-900 ring-1 ring-[#00D1A3]'
                          : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-1">
                        <span className="font-bold text-[10px] sm:text-[11px] truncate">Sneha Kulkarni</span>
                        <span className="px-1 py-0.2 bg-sky-100 text-sky-700 text-[8px] sm:text-[9px] font-bold rounded shrink-0">Client</span>
                      </div>
                      <span className="block text-[9px] sm:text-[10px] text-slate-500 truncate mt-0.5">client.sneha@...</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => handleQuickFill('client.rajesh@apextax.com')}
                      className={`p-2 sm:px-2.5 sm:py-2 rounded-lg border text-left text-xs font-semibold transition-all ${
                        email === 'client.rajesh@apextax.com'
                          ? 'border-[#00D1A3] bg-[#E6FBF6] text-slate-900 ring-1 ring-[#00D1A3]'
                          : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-1">
                        <span className="font-bold text-[10px] sm:text-[11px] truncate">Dr. Rajesh</span>
                        <span className="px-1 py-0.2 bg-sky-100 text-sky-700 text-[8px] sm:text-[9px] font-bold rounded shrink-0">Client Admin</span>
                      </div>
                      <span className="block text-[9px] sm:text-[10px] text-slate-500 truncate mt-0.5">client.rajesh@...</span>
                    </button>
                  </div>
                )}
              </div>
            )}

            {error && (
              <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-xs font-semibold text-rose-700">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-3.5 sm:space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Email Address</label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="email"
                    required
                    placeholder="ca.admin@taxpractice.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full text-xs pl-9 pr-3 py-2 sm:py-2.5 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#00D1A3]"
                  />
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-bold text-slate-700">Password</label>
                  <Link to="/forgot-password" className="text-[11px] font-bold text-[#00B388] hover:text-[#082E5B] hover:underline">
                    Forgot Password?
                  </Link>
                </div>
                <div className="relative">
                  <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="password"
                    required
                    placeholder="••••••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full text-xs pl-9 pr-3 py-2 sm:py-2.5 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#00D1A3]"
                  />
                </div>
              </div>

              <Button type="submit" variant="primary" className="w-full font-bold py-2.5" isLoading={isLoading} rightIcon={<ArrowRight className="w-4 h-4" />}>
                Sign In to Practice
              </Button>
            </form>
          </div>

          <div className="pt-3.5 sm:pt-4 border-t border-slate-150 space-y-2 sm:space-y-2.5">
            {/* 1. Register Practice (Primary CA / Practitioner CTA) */}
            <div className="p-2.5 sm:p-3 bg-gradient-to-r from-emerald-50/80 via-teal-50/30 to-slate-50 rounded-xl border border-emerald-200/80 flex items-center justify-between gap-2 shadow-2xs">
              <div className="flex items-center gap-2 sm:gap-2.5 min-w-0">
                <div className="w-7 h-7 sm:w-8 sm:h-8 rounded-lg bg-emerald-600 text-white flex items-center justify-center text-xs font-black shadow-xs shrink-0">
                  🏢
                </div>
                <div className="min-w-0">
                  <div className="flex items-center gap-1.5">
                    <span className="text-[11px] sm:text-xs font-bold text-slate-900 block truncate">New to Taxoryn?</span>
                    <span className="px-1.5 py-0.2 rounded text-[8px] sm:text-[9px] font-bold bg-emerald-100 text-emerald-800 uppercase shrink-0">CA Firms</span>
                  </div>
                  <span className="text-[9px] sm:text-[10px] text-slate-500 block truncate">Launch your digital practice in 2 minutes</span>
                </div>
              </div>
              <Link
                to="/register"
                className="inline-flex items-center gap-1 px-2.5 sm:px-3 py-1.5 bg-[#082E5B] hover:bg-[#061e3b] text-white text-[10px] sm:text-[11px] font-bold rounded-lg shadow-xs transition-all shrink-0 hover:scale-[1.02] whitespace-nowrap"
              >
                <span>Register Practice</span>
                <ArrowRight className="w-3 h-3 text-[#00D1A3]" />
              </Link>
            </div>

            {/* 2. Dual-Pill Grid for Secondary User Portals */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {/* Individual Taxpayer */}
              <Link
                to="/marketplace/register"
                className="p-2.5 rounded-xl bg-slate-50/90 hover:bg-sky-50/60 border border-slate-200/80 hover:border-sky-200 transition-all flex items-center justify-between group shadow-2xs"
              >
                <div className="flex items-center gap-2 min-w-0">
                  <div className="w-6 h-6 rounded-lg bg-sky-100 text-sky-700 flex items-center justify-center text-xs font-bold shrink-0">
                    👤
                  </div>
                  <div className="min-w-0">
                    <span className="text-[11px] font-bold text-slate-800 group-hover:text-sky-900 block truncate">Individual Taxpayer</span>
                    <span className="text-[9px] text-slate-400 block truncate">Self-filing portal access</span>
                  </div>
                </div>
                <span className="text-[10px] font-bold text-sky-700 group-hover:translate-x-0.5 transition-transform flex items-center gap-0.5 shrink-0 ml-1 whitespace-nowrap">
                  Sign Up <ArrowRight className="w-3 h-3" />
                </span>
              </Link>

              {/* CA Marketplace Directory */}
              <Link
                to="/marketplace"
                className="p-2.5 rounded-xl bg-slate-50/90 hover:bg-emerald-50/60 border border-slate-200/80 hover:border-emerald-200 transition-all flex items-center justify-between group shadow-2xs"
              >
                <div className="flex items-center gap-2 min-w-0">
                  <div className="w-6 h-6 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center text-xs font-bold shrink-0">
                    🔍
                  </div>
                  <div className="min-w-0">
                    <span className="text-[11px] font-bold text-slate-800 group-hover:text-emerald-900 block truncate">Find a Chartered CA</span>
                    <span className="text-[9px] text-slate-400 block truncate">Browse verified directory</span>
                  </div>
                </div>
                <span className="text-[10px] font-bold text-emerald-700 group-hover:translate-x-0.5 transition-transform flex items-center gap-0.5 shrink-0 ml-1 whitespace-nowrap">
                  Explore <ArrowRight className="w-3 h-3" />
                </span>
              </Link>
            </div>

            {/* 3. Mobile-only Knowledge Hub Link (Desktop displays it prominently on left column) */}
            <div className="lg:hidden pt-0.5">
              <Link
                to="/learn"
                className="group p-2 sm:p-2.5 rounded-xl bg-gradient-to-r from-slate-900 via-[#082E5B] to-[#07152B] text-white hover:shadow-md transition-all flex items-center justify-between gap-2 border border-slate-700/60"
              >
                <div className="flex items-center gap-2 sm:gap-2.5 min-w-0">
                  <div className="w-6 h-6 sm:w-7 sm:h-7 rounded-lg bg-[#00D1A3]/20 border border-[#00D1A3]/30 text-[#00D1A3] flex items-center justify-center text-xs font-black shadow-xs shrink-0">
                    <GraduationCap className="w-3.5 h-3.5" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-1.5">
                      <span className="text-[11px] sm:text-xs font-bold text-white block truncate">Taxoryn Knowledge Hub</span>
                      <span className="px-1.5 py-0.2 rounded text-[7px] sm:text-[8px] font-bold bg-[#00D1A3] text-slate-950 uppercase shrink-0">Learn</span>
                    </div>
                    <span className="text-[9px] sm:text-[10px] text-slate-300 block truncate">
                      GST, ITR & TDS guides & statutory updates
                    </span>
                  </div>
                </div>
                <span className="text-[10px] sm:text-[11px] font-bold text-[#00D1A3] group-hover:translate-x-1 transition-transform flex items-center gap-1 shrink-0 ml-1 whitespace-nowrap">
                  Explore <ArrowRight className="w-3 h-3" />
                </span>
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

