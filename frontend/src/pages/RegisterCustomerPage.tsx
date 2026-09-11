import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { marketplaceCustomerApi } from '../api/endpoints';
import { RegisterCustomerRequest, User as UserType } from '../types';
import {
  User,
  Mail,
  Lock,
  Phone,
  Building,
  ArrowRight,
  Eye,
  EyeOff,
  Check,
  X,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { TaxorynLogo } from '../components/common/TaxorynLogo';
import { useAuth } from '../context/AuthContext';
import { getBookingIntent } from '../utils/bookingIntent';
import { Calendar } from 'lucide-react';

export const RegisterCustomerPage: React.FC = () => {
  const { setAuthSession } = useAuth();
  const navigate = useNavigate();
  const bookingIntent = getBookingIntent();

  const [formData, setFormData] = useState<RegisterCustomerRequest>({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    password: '',
    customerType: 'INDIVIDUAL',
    businessName: '',
    city: '',
    state: '',
    pincode: '',
    preferredLanguage: 'English',
  });

  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  // Password validation rules
  const hasMinLength = formData.password.length >= 8;
  const hasUppercase = /[A-Z]/.test(formData.password);
  const hasLowercase = /[a-z]/.test(formData.password);
  const hasNumber = /[0-9]/.test(formData.password);
  const hasSpecial = /[@#$%^&+=!._-]/.test(formData.password);
  const passwordsMatch = formData.password.length > 0 && formData.password === confirmPassword;
  const isPasswordValid =
    hasMinLength && hasUppercase && hasLowercase && hasNumber && hasSpecial && passwordsMatch;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!formData.firstName.trim() || !formData.email.trim() || !formData.password) {
      setError('Please fill in all required fields (First Name, Email, and Password).');
      return;
    }

    if (!isPasswordValid) {
      if (!passwordsMatch) {
        setError('Passwords do not match.');
      } else {
        setError('Please ensure your password meets all complexity requirements.');
      }
      return;
    }

    setIsLoading(true);
    try {
      const authData = await marketplaceCustomerApi.register(formData);
      if (authData?.accessToken && authData?.customer) {
        const customerUser: UserType = {
          id: authData.customer.userId || authData.customer.id,
          organizationId: '',
          email: authData.customer.email,
          firstName: authData.customer.firstName,
          lastName: authData.customer.lastName,
          phone: authData.customer.phone,
          status: 'ACTIVE',
          roles: ['MARKETPLACE_CUSTOMER'],
          permissions: [],
        };
        setAuthSession(authData.accessToken, customerUser, null);
        
        const pendingBooking = getBookingIntent();
        if (pendingBooking) {
          navigate(`${pendingBooking.returnUrl || '/marketplace'}?restoreBooking=true`, { replace: true });
        } else {
          navigate('/marketplace');
        }
      } else {
        navigate('/login');
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Registration failed. Please try again.';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  // -------------------------------------------------------------
  // Registration Form
  // -------------------------------------------------------------
  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4 relative overflow-hidden">
      {/* Glow effects */}
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-brand-500/20 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-purple-500/20 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-xl bg-white rounded-2xl shadow-2xl p-8 border border-slate-100 z-10 space-y-6">
        <div className="flex flex-col items-center text-center space-y-2">
          <TaxorynLogo variant="horizontal" theme="light" size="md" />
          <h2 className="text-2xl font-black text-slate-900 tracking-tight mt-2">Create Customer Account</h2>
          <p className="text-xs text-slate-500">Discover trusted Tax Professionals, book consultations, and track filings</p>
        </div>

        {bookingIntent && (
          <div className="p-3.5 rounded-xl bg-indigo-50 border border-indigo-200 text-xs text-indigo-900 flex items-start gap-2.5 shadow-xs animate-fade-in">
            <Calendar className="w-4 h-4 text-indigo-600 mt-0.5 shrink-0" />
            <div className="space-y-0.5">
              <div className="font-bold text-indigo-950">Pending Consultation Booking</div>
              <p className="text-[11px] text-indigo-700 leading-relaxed">
                Create your account to confirm your appointment with <strong>{bookingIntent.professionalName || 'Tax Professional'}</strong> on <strong>{bookingIntent.bookingDate} at {bookingIntent.startTime}</strong>.
              </p>
            </div>
          </div>
        )}

        {/* Customer Type Selector */}
        <div className="grid grid-cols-2 gap-2 p-1 bg-slate-100 rounded-xl">
          <button
            type="button"
            onClick={() => setFormData((prev) => ({ ...prev, customerType: 'INDIVIDUAL' }))}
            className={`py-2 text-xs font-bold rounded-lg transition-all flex items-center justify-center gap-1.5 ${
              formData.customerType === 'INDIVIDUAL'
                ? 'bg-white text-brand-700 shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <User className="w-4 h-4" />
            Individual Taxpayer
          </button>
          <button
            type="button"
            onClick={() => setFormData((prev) => ({ ...prev, customerType: 'BUSINESS' }))}
            className={`py-2 text-xs font-bold rounded-lg transition-all flex items-center justify-center gap-1.5 ${
              formData.customerType === 'BUSINESS'
                ? 'bg-white text-brand-700 shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <Building className="w-4 h-4" />
            Business / Company
          </button>
        </div>

        {error && (
          <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-700 font-medium flex items-center gap-2">
            <span className="w-1.5 h-1.5 rounded-full bg-rose-500 flex-shrink-0" />
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">First Name *</label>
              <div className="relative">
                <User className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                <input
                  type="text"
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleChange}
                  placeholder="e.g. Rahul"
                  className="w-full pl-9 pr-3 py-2 text-xs border border-slate-200 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
                  required
                />
              </div>
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Last Name</label>
              <input
                type="text"
                name="lastName"
                value={formData.lastName}
                onChange={handleChange}
                placeholder="e.g. Sharma"
                className="w-full px-3 py-2 text-xs border border-slate-200 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
              />
            </div>
          </div>

          {formData.customerType === 'BUSINESS' && (
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Business / Firm Name</label>
              <div className="relative">
                <Building className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                <input
                  type="text"
                  name="businessName"
                  value={formData.businessName || ''}
                  onChange={handleChange}
                  placeholder="e.g. Sharma Enterprises LLP"
                  className="w-full pl-9 pr-3 py-2 text-xs border border-slate-200 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
                />
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Email Address *</label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  placeholder="rahul@example.com"
                  className="w-full pl-9 pr-3 py-2 text-xs border border-slate-200 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
                  required
                />
              </div>
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Phone Number</label>
              <div className="relative">
                <Phone className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                <input
                  type="tel"
                  name="phone"
                  value={formData.phone}
                  onChange={handleChange}
                  placeholder="9876543210"
                  className="w-full pl-9 pr-3 py-2 text-xs border border-slate-200 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
                />
              </div>
            </div>
          </div>

          {/* Password & Confirm Password */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Password *</label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="Minimum 8 characters"
                  className="w-full pl-9 pr-9 py-2 text-xs border border-slate-200 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-600"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Confirm Password *</label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  name="confirmPassword"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Re-enter password"
                  className="w-full pl-9 pr-9 py-2 text-xs border border-slate-200 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-600"
                >
                  {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>
          </div>

          {/* Password Complexity Policy Indicator */}
          {formData.password.length > 0 && (
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
          )}

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
            <div>
              <label className="block text-[11px] font-bold text-slate-700 mb-1">City</label>
              <input
                type="text"
                name="city"
                value={formData.city}
                onChange={handleChange}
                placeholder="Bangalore"
                className="w-full px-2.5 py-2 text-xs border border-slate-200 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-[11px] font-bold text-slate-700 mb-1">State</label>
              <input
                type="text"
                name="state"
                value={formData.state}
                onChange={handleChange}
                placeholder="Karnataka"
                className="w-full px-2.5 py-2 text-xs border border-slate-200 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-[11px] font-bold text-slate-700 mb-1">Pincode</label>
              <input
                type="text"
                name="pincode"
                value={formData.pincode}
                onChange={handleChange}
                placeholder="560001"
                className="w-full px-2.5 py-2 text-xs border border-slate-200 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
              />
            </div>
          </div>

          <Button
            type="submit"
            disabled={isLoading || (formData.password.length > 0 && !isPasswordValid)}
            className="w-full py-2.5 bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-lg shadow-brand-500/20 disabled:opacity-50"
          >
            {isLoading ? 'Creating Account...' : 'Create Account & Explore Marketplace'}
            <ArrowRight className="w-4 h-4 ml-1.5" />
          </Button>
        </form>

        <div className="border-t border-slate-100 pt-4 text-center space-y-2">
          <p className="text-xs text-slate-500">
            Already have an account?{' '}
            <Link to="/login" className="text-brand-600 font-bold hover:underline">
              Sign In
            </Link>
          </p>
          <p className="text-[11px] text-slate-400">
            Are you a Chartered Accountant or Tax Practitioner?{' '}
            <Link to="/register" className="text-indigo-600 font-semibold hover:underline">
              Register Practice
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};
