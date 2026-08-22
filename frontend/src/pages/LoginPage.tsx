import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, Lock, Mail, ArrowRight, Sparkles, Building2, User } from 'lucide-react';
import { Button } from '../components/common/Button';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    // Auto recall last registered or logged in email
    const savedEmail = localStorage.getItem('taxoryn_last_user_email') || 'pawanadv@gmail.com';
    if (savedEmail) {
      setEmail(savedEmail);
      setPassword('Password123!');
    }
  }, []);

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
    setEmail(demoEmail);
    setPassword('Password123!');
    setError('');
  };

  const [demoTab, setDemoTab] = useState<'mundeshwari' | 'apex'>('mundeshwari');

  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background Glows */}
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-brand-500/20 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-purple-500/20 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-lg bg-white rounded-2xl shadow-2xl p-8 border border-slate-100 z-10 space-y-6">
        <div className="text-center space-y-2">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-600 flex items-center justify-center text-white font-black text-xl mx-auto shadow-lg shadow-brand-500/30">
            TX
          </div>
          <h2 className="text-2xl font-black text-slate-900 tracking-tight">Sign in to Taxoryn</h2>
          <p className="text-xs text-slate-500">Enterprise Multi-Tenant Tax Practice Management</p>
        </div>

        {/* Quick Demo Fill Practice Selector Tabs */}
        <div className="bg-slate-50 border border-slate-200 rounded-xl p-3.5 space-y-2.5">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">
              ⚡ 1-Click Quick Fill Demo Logins
            </span>
            <span className="text-[10px] text-slate-400 font-mono">Pwd: Password123!</span>
          </div>

          {/* Firm Switcher Tabs */}
          <div className="grid grid-cols-2 gap-1.5 p-1 bg-slate-200/70 rounded-lg text-xs font-semibold">
            <button
              type="button"
              onClick={() => setDemoTab('mundeshwari')}
              className={`py-1.5 px-2 rounded-md transition-all truncate text-center ${
                demoTab === 'mundeshwari'
                  ? 'bg-white text-brand-900 shadow-xs font-bold'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Maa Mundeshwari Tax
            </button>
            <button
              type="button"
              onClick={() => setDemoTab('apex')}
              className={`py-1.5 px-2 rounded-md transition-all truncate text-center ${
                demoTab === 'apex'
                  ? 'bg-white text-brand-900 shadow-xs font-bold'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Apex Tax Advisors
            </button>
          </div>

          {/* Mundeshwari Accounts */}
          {demoTab === 'mundeshwari' && (
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => handleQuickFill('pawanadv@gmail.com')}
                className={`px-2.5 py-2 rounded-lg border text-left text-xs font-semibold transition-all ${
                  email === 'pawanadv@gmail.com'
                    ? 'border-brand-600 bg-brand-50 text-brand-900 ring-1 ring-brand-500'
                    : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-[11px] truncate">Pawan Pathak</span>
                  <span className="px-1 py-0.2 bg-purple-100 text-purple-700 text-[9px] font-bold rounded">Admin</span>
                </div>
                <span className="block text-[10px] text-slate-500 truncate mt-0.5">pawanadv@gmail.com</span>
              </button>

              <button
                type="button"
                onClick={() => handleQuickFill('pooja.joshi@maamundeshwari.com')}
                className={`px-2.5 py-2 rounded-lg border text-left text-xs font-semibold transition-all ${
                  email === 'pooja.joshi@maamundeshwari.com'
                    ? 'border-brand-600 bg-brand-50 text-brand-900 ring-1 ring-brand-500'
                    : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-[11px] truncate">Pooja Joshi</span>
                  <span className="px-1 py-0.2 bg-amber-100 text-amber-800 text-[9px] font-bold rounded">Staff</span>
                </div>
                <span className="block text-[10px] text-slate-500 truncate mt-0.5">pooja.joshi@...</span>
              </button>

              <button
                type="button"
                onClick={() => handleQuickFill('client.mundeshwari@maamundeshwari.com')}
                className={`px-2.5 py-2 rounded-lg border text-left text-xs font-semibold transition-all ${
                  email === 'client.mundeshwari@maamundeshwari.com'
                    ? 'border-brand-600 bg-brand-50 text-brand-900 ring-1 ring-brand-500'
                    : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-[11px] truncate">Mundeshwari Ent</span>
                  <span className="px-1 py-0.2 bg-sky-100 text-sky-700 text-[9px] font-bold rounded">Client Admin</span>
                </div>
                <span className="block text-[10px] text-slate-500 truncate mt-0.5">client.mundeshwari@...</span>
              </button>

              <button
                type="button"
                onClick={() => handleQuickFill('client.pawanassoc@maamundeshwari.com')}
                className={`px-2.5 py-2 rounded-lg border text-left text-xs font-semibold transition-all ${
                  email === 'client.pawanassoc@maamundeshwari.com'
                    ? 'border-brand-600 bg-brand-50 text-brand-900 ring-1 ring-brand-500'
                    : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-[11px] truncate">Pawan Associates</span>
                  <span className="px-1 py-0.2 bg-sky-100 text-sky-700 text-[9px] font-bold rounded">Client</span>
                </div>
                <span className="block text-[10px] text-slate-500 truncate mt-0.5">client.pawanassoc@...</span>
              </button>
            </div>
          )}

          {/* Apex Tax Accounts */}
          {demoTab === 'apex' && (
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => handleQuickFill('admin@apextax.com')}
                className={`px-2.5 py-2 rounded-lg border text-left text-xs font-semibold transition-all ${
                  email === 'admin@apextax.com'
                    ? 'border-brand-600 bg-brand-50 text-brand-900 ring-1 ring-brand-500'
                    : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-[11px] truncate">Apex Admin</span>
                  <span className="px-1 py-0.2 bg-purple-100 text-purple-700 text-[9px] font-bold rounded">Admin</span>
                </div>
                <span className="block text-[10px] text-slate-500 truncate mt-0.5">admin@apextax.com</span>
              </button>

              <button
                type="button"
                onClick={() => handleQuickFill('neha.sharma@apextax.com')}
                className={`px-2.5 py-2 rounded-lg border text-left text-xs font-semibold transition-all ${
                  email === 'neha.sharma@apextax.com'
                    ? 'border-brand-600 bg-brand-50 text-brand-900 ring-1 ring-brand-500'
                    : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-[11px] truncate">Neha Sharma</span>
                  <span className="px-1 py-0.2 bg-amber-100 text-amber-800 text-[9px] font-bold rounded">Staff</span>
                </div>
                <span className="block text-[10px] text-slate-500 truncate mt-0.5">neha.sharma@...</span>
              </button>

              <button
                type="button"
                onClick={() => handleQuickFill('client.sneha@apextax.com')}
                className={`px-2.5 py-2 rounded-lg border text-left text-xs font-semibold transition-all ${
                  email === 'client.sneha@apextax.com'
                    ? 'border-brand-600 bg-brand-50 text-brand-900 ring-1 ring-brand-500'
                    : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-[11px] truncate">Sneha Kulkarni</span>
                  <span className="px-1 py-0.2 bg-sky-100 text-sky-700 text-[9px] font-bold rounded">Client</span>
                </div>
                <span className="block text-[10px] text-slate-500 truncate mt-0.5">client.sneha@apextax...</span>
              </button>

              <button
                type="button"
                onClick={() => handleQuickFill('client.rajesh@apextax.com')}
                className={`px-2.5 py-2 rounded-lg border text-left text-xs font-semibold transition-all ${
                  email === 'client.rajesh@apextax.com'
                    ? 'border-brand-600 bg-brand-50 text-brand-900 ring-1 ring-brand-500'
                    : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-[11px] truncate">Dr. Rajesh Sharma</span>
                  <span className="px-1 py-0.2 bg-sky-100 text-sky-700 text-[9px] font-bold rounded">Client Admin</span>
                </div>
                <span className="block text-[10px] text-slate-500 truncate mt-0.5">client.rajesh@apextax...</span>
              </button>
            </div>
          )}
        </div>

        {error && (
          <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-xs font-semibold text-rose-700">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Email Address</label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="email"
                required
                placeholder="ca.admin@taxpractice.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full text-xs pl-9 pr-3 py-2.5 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Password</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="password"
                required
                placeholder="••••••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full text-xs pl-9 pr-3 py-2.5 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>
          </div>

          <Button type="submit" className="w-full" isLoading={isLoading} rightIcon={<ArrowRight className="w-4 h-4" />}>
            Sign In to Practice
          </Button>
        </form>

        <div className="pt-2 border-t border-slate-100 flex flex-col gap-2 text-xs text-slate-500">
          <div className="flex items-center justify-between">
            <span className="text-[11px]">Default Password: <code className="font-mono font-bold text-slate-700">Password123!</code></span>
            <Link to="/register" className="hover:text-slate-800 font-semibold text-brand-600">
              Register Practice →
            </Link>
          </div>
          <div className="flex items-center justify-between pt-1 border-t border-slate-50 text-[11px]">
            <Link to="/marketplace/register" className="text-purple-600 font-bold hover:underline">
              ✦ Customer? Sign up here
            </Link>
            <Link to="/marketplace" className="text-slate-500 hover:text-slate-800">
              Explore Marketplace →
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
