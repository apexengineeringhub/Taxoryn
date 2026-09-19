import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Building2,
  Mail,
  User,
  Phone,
  MapPin,
  Briefcase,
  Layers,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  Shield,
  Sparkles,
  FolderKanban,
  ShieldCheckIcon,
  TrendingUp,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { TaxorynLogo } from '../components/common/TaxorynLogo';
import { SeoHead } from '../components/common/SeoHead';
import { marketingApi, EarlyAccessPayload } from '../api/endpoints';

export const EarlyAccessPage: React.FC = () => {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    practiceName: '',
    phone: '',
    city: '',
    practiceProfile: 'CA Firm',
    primaryArea: 'Complete Practice Management',
    honeypot: '',
  });

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [submittedEmail, setSubmittedEmail] = useState('');

  const validateForm = () => {
    const errors: Record<string, string> = {};
    if (!formData.name.trim()) {
      errors.name = 'Full name is required';
    }
    if (!formData.email.trim()) {
      errors.email = 'Email address is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      errors.email = 'Please enter a valid email address';
    }
    if (!formData.practiceName.trim()) {
      errors.practiceName = 'Firm / Practice name is required';
    }
    return errors;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setServerError('');
    setFieldErrors({});

    const errors = validateForm();
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    setIsLoading(true);
    try {
      const payload: EarlyAccessPayload = {
        name: formData.name.trim(),
        email: formData.email.trim().toLowerCase(),
        practiceName: formData.practiceName.trim(),
        phone: formData.phone.trim() || undefined,
        city: formData.city.trim() || undefined,
        practiceProfile: formData.practiceProfile || undefined,
        primaryArea: formData.primaryArea || undefined,
        source: 'EARLY_ACCESS_PAGE',
        honeypot: formData.honeypot || undefined,
      };

      await marketingApi.submitEarlyAccess(payload);
      setSubmittedEmail(formData.email.trim());
      setIsSuccess(true);
    } catch (err: any) {
      const errorMessage =
        err.response?.data?.message ||
        'Unable to submit your request right now. Please try again.';
      setServerError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#07152B] flex items-center justify-center p-3 sm:p-4 lg:p-8 relative overflow-hidden">
      <SeoHead
        title="Request Practice Access — Taxoryn"
        description="Request early access to the modern Indian tax practice operating system for CA firms and tax professionals."
        canonicalUrl="https://app.taxoryn.com/early-access"
      />

      {/* Dynamic Background Glows */}
      <div className="absolute -top-40 -left-40 w-[500px] h-[500px] bg-[#00D1A3]/15 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-[500px] h-[500px] bg-[#082E5B]/50 rounded-full blur-[120px] pointer-events-none" />

      {/* Main Container */}
      <div className="w-full max-w-5xl bg-white rounded-2xl shadow-2xl overflow-hidden grid grid-cols-1 lg:grid-cols-12 border border-slate-700/30 z-10 my-2">
        {/* LEFT COLUMN: Brand Hero */}
        <div className="hidden lg:flex lg:col-span-5 bg-gradient-to-br from-[#082E5B] via-[#07152B] to-[#070C1A] p-7 xl:p-8 flex-col justify-between text-white relative border-r border-white/10 overflow-hidden">
          <div className="space-y-6 z-10">
            <div className="pt-1">
              <TaxorynLogo variant="full" theme="dark" size="lg" />
            </div>

            <div>
              <span className="px-2 py-0.5 rounded-full bg-[#00D1A3]/20 text-[#00D1A3] text-[10px] font-bold uppercase tracking-wider">
                Early Access Program
              </span>
              <h1 className="text-xl font-black mt-2 text-white leading-snug">
                Transform Your Indian Tax Practice
              </h1>
              <p className="text-xs text-slate-300 mt-2 leading-relaxed">
                Join leading CA firms and practitioners leveraging Taxoryn for automated GST, ITR, TDS compliance, and secure client collaboration.
              </p>
            </div>

            <div className="space-y-3">
              <div className="flex items-start gap-2.5 p-2.5 rounded-xl bg-white/5 border border-white/10">
                <FolderKanban className="w-4 h-4 text-[#00D1A3] shrink-0 mt-0.5" />
                <div>
                  <div className="text-xs font-bold text-white">Automated Compliance</div>
                  <div className="text-[10px] text-slate-400">GST, TDS, ITR return workflows & reminders</div>
                </div>
              </div>

              <div className="flex items-start gap-2.5 p-2.5 rounded-xl bg-white/5 border border-white/10">
                <ShieldCheckIcon className="w-4 h-4 text-[#00D1A3] shrink-0 mt-0.5" />
                <div>
                  <div className="text-xs font-bold text-white">Client Document Vault</div>
                  <div className="text-[10px] text-slate-400">Encrypted portal, structured requests & approvals</div>
                </div>
              </div>

              <div className="flex items-start gap-2.5 p-2.5 rounded-xl bg-white/5 border border-white/10">
                <TrendingUp className="w-4 h-4 text-[#00D1A3] shrink-0 mt-0.5" />
                <div>
                  <div className="text-xs font-bold text-white">Staff Workload Balancer</div>
                  <div className="text-[10px] text-slate-400">Granular practitioner scopes & delegation</div>
                </div>
              </div>
            </div>
          </div>

          <div className="pt-4 border-t border-white/10 text-[10px] text-slate-400">
            Already have an active practice?{' '}
            <Link to="/login" className="text-[#00D1A3] font-bold hover:underline">
              Sign in to Practice →
            </Link>
          </div>
        </div>

        {/* RIGHT COLUMN: Form */}
        <div className="lg:col-span-7 p-6 sm:p-8 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-4 border-b border-slate-100">
              <div>
                <h2 className="text-lg sm:text-xl font-black text-slate-900">
                  Request Practice Access
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Complete the form below to request access for your firm or practice.
                </p>
              </div>
              <Link to="/login" className="text-xs font-bold text-[#082E5B] hover:text-[#00B388] transition-colors">
                Sign In
              </Link>
            </div>

            {isSuccess ? (
              <div className="text-center py-10 space-y-4">
                <div className="w-16 h-16 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center mx-auto ring-8 ring-emerald-50/50">
                  <CheckCircle2 className="w-10 h-10" />
                </div>

                <div className="space-y-1.5">
                  <h3 className="text-xl font-extrabold text-slate-900">✓ Request Received</h3>
                  <p className="text-sm font-semibold text-emerald-700">
                    Thank you for your interest in Taxoryn.
                  </p>
                </div>

                <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl text-left text-xs text-slate-600 space-y-2 max-w-md mx-auto">
                  <p className="leading-relaxed">
                    We've received your practice access request. We'll review your request and contact you shortly.
                  </p>
                  <p className="leading-relaxed text-slate-500">
                    A confirmation has been sent to <strong className="text-slate-700">{submittedEmail}</strong>.
                  </p>
                </div>

                <div className="pt-4 flex items-center justify-center gap-3">
                  <Link
                    to="/login"
                    className="inline-flex items-center gap-1.5 px-4 py-2.5 bg-[#082E5B] text-white text-xs font-bold rounded-lg hover:bg-[#061e3b] transition-all"
                  >
                    Go to Login
                  </Link>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setIsSuccess(false);
                      setFormData({
                        name: '',
                        email: '',
                        practiceName: '',
                        phone: '',
                        city: '',
                        practiceProfile: 'CA Firm',
                        primaryArea: 'Complete Practice Management',
                        honeypot: '',
                      });
                    }}
                    className="text-xs font-bold py-2.5"
                  >
                    Submit Another Request
                  </Button>
                </div>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-4 mt-5">
                {serverError && (
                  <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-start gap-2">
                    <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                    <div className="flex-1">
                      <p className="font-bold">Unable to submit your request right now.</p>
                      <p className="text-[11px] text-rose-600 mt-0.5">Please try again.</p>
                    </div>
                  </div>
                )}

                {/* Anti-Spam Honeypot */}
                <div style={{ display: 'none' }} aria-hidden="true">
                  <label htmlFor="taxoryn_contact_website_page">Do not fill this field</label>
                  <input
                    type="text"
                    id="taxoryn_contact_website_page"
                    name="taxoryn_contact_website_page"
                    tabIndex={-1}
                    autoComplete="off"
                    value={formData.honeypot}
                    onChange={(e) => setFormData({ ...formData, honeypot: e.target.value })}
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      Your Full Name <span className="text-rose-500">*</span>
                    </label>
                    <div className="relative">
                      <User className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                      <input
                        type="text"
                        required
                        placeholder="CA Rajesh Verma"
                        value={formData.name}
                        onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                        className={`w-full text-xs pl-8 pr-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-[#00D1A3] ${
                          fieldErrors.name ? 'border-rose-400 bg-rose-50/20' : 'border-slate-200'
                        }`}
                      />
                    </div>
                    {fieldErrors.name && (
                      <p className="text-[10px] text-rose-600 mt-1 font-medium">{fieldErrors.name}</p>
                    )}
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      Email Address <span className="text-rose-500">*</span>
                    </label>
                    <div className="relative">
                      <Mail className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                      <input
                        type="email"
                        required
                        placeholder="rajesh@apextax.in"
                        value={formData.email}
                        onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                        className={`w-full text-xs pl-8 pr-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-[#00D1A3] ${
                          fieldErrors.email ? 'border-rose-400 bg-rose-50/20' : 'border-slate-200'
                        }`}
                      />
                    </div>
                    {fieldErrors.email && (
                      <p className="text-[10px] text-rose-600 mt-1 font-medium">{fieldErrors.email}</p>
                    )}
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Firm / Practice Name <span className="text-rose-500">*</span>
                  </label>
                  <div className="relative">
                    <Building2 className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                    <input
                      type="text"
                      required
                      placeholder="Apex Tax Advisors LLP"
                      value={formData.practiceName}
                      onChange={(e) => setFormData({ ...formData, practiceName: e.target.value })}
                      className={`w-full text-xs pl-8 pr-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-[#00D1A3] ${
                        fieldErrors.practiceName ? 'border-rose-400 bg-rose-50/20' : 'border-slate-200'
                      }`}
                    />
                  </div>
                  {fieldErrors.practiceName && (
                    <p className="text-[10px] text-rose-600 mt-1 font-medium">{fieldErrors.practiceName}</p>
                  )}
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      Phone Number <span className="text-slate-400 font-normal">(Optional)</span>
                    </label>
                    <div className="relative">
                      <Phone className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                      <input
                        type="tel"
                        placeholder="+91 98765 43210"
                        value={formData.phone}
                        onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                        className="w-full text-xs pl-8 pr-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#00D1A3]"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      City / Location <span className="text-slate-400 font-normal">(Optional)</span>
                    </label>
                    <div className="relative">
                      <MapPin className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                      <input
                        type="text"
                        placeholder="Mumbai, Maharashtra"
                        value={formData.city}
                        onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                        className="w-full text-xs pl-8 pr-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#00D1A3]"
                      />
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">Practice Profile</label>
                    <div className="relative">
                      <Briefcase className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                      <select
                        value={formData.practiceProfile}
                        onChange={(e) => setFormData({ ...formData, practiceProfile: e.target.value })}
                        className="w-full text-xs pl-8 pr-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#00D1A3] bg-white"
                      >
                        <option value="CA Firm">CA Firm (Multi-Partner)</option>
                        <option value="Solo Practitioner">Solo Tax Practitioner</option>
                        <option value="Tax Consultant">Tax Advocate / Consultant</option>
                        <option value="Corporate Tax Team">Corporate Compliance Team</option>
                        <option value="Other">Other Practice Type</option>
                      </select>
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">Primary Area of Interest</label>
                    <div className="relative">
                      <Layers className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                      <select
                        value={formData.primaryArea}
                        onChange={(e) => setFormData({ ...formData, primaryArea: e.target.value })}
                        className="w-full text-xs pl-8 pr-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#00D1A3] bg-white"
                      >
                        <option value="Complete Practice Management">Complete Practice Management</option>
                        <option value="GST Compliance & 2B Reconciliation">GST Compliance & 2B Rec</option>
                        <option value="ITR & Tax Audit Automation">ITR & Tax Audit Automation</option>
                        <option value="TDS Filings & Form 26AS">TDS Filings & 26AS Rec</option>
                        <option value="Client Portal & Secure Invoicing">Client Portal & Billing</option>
                      </select>
                    </div>
                  </div>
                </div>

                {/* Privacy Warning */}
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 text-[11px] text-slate-500 flex items-start gap-2">
                  <Shield className="w-3.5 h-3.5 text-slate-400 shrink-0 mt-0.5" />
                  <span>
                    <strong>Privacy Note:</strong> Taxoryn never asks for passwords, PAN/Aadhaar credentials, or client financial records during access requests.
                  </span>
                </div>

                <div className="pt-2">
                  <Button
                    type="submit"
                    variant="primary"
                    isLoading={isLoading}
                    disabled={isLoading}
                    className="w-full font-bold py-2.5"
                    rightIcon={<ArrowRight className="w-4 h-4" />}
                  >
                    Submit Practice Access Request
                  </Button>
                </div>
              </form>
            )}
          </div>

          <div className="pt-4 mt-6 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400">
            <span>© {new Date().getFullYear()} Taxoryn SaaS Platform</span>
            <Link to="/register" className="font-bold text-[#082E5B] hover:text-[#00B388] transition-colors">
              Self-Serve Registration →
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
