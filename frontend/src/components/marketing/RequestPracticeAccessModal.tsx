import React, { useState } from 'react';
import {
  X,
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
} from 'lucide-react';
import { Button } from '../common/Button';
import { marketingApi, EarlyAccessPayload } from '../../api/endpoints';

interface RequestPracticeAccessModalProps {
  isOpen: boolean;
  onClose: () => void;
  source?: string;
}

export const RequestPracticeAccessModal: React.FC<RequestPracticeAccessModalProps> = ({
  isOpen,
  onClose,
  source = 'MARKETING_WEBSITE',
}) => {
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

  if (!isOpen) return null;

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
        source: source,
        honeypot: formData.honeypot || undefined,
      };

      const result = await marketingApi.submitEarlyAccess(payload);
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

  const handleResetAndClose = () => {
    setIsSuccess(false);
    setServerError('');
    setFieldErrors({});
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-3 sm:p-4">
      <div
        className="relative w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden animate-in fade-in zoom-in-95 duration-150"
        role="dialog"
        aria-modal="true"
        aria-labelledby="early-access-title"
      >
        {/* Modal Header */}
        <div className="bg-gradient-to-r from-[#082E5B] via-[#07152B] to-[#082E5B] px-6 py-5 text-white flex items-center justify-between border-b border-white/10">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-[#00D1A3]/20 border border-[#00D1A3]/30 text-[#00D1A3] flex items-center justify-center font-bold text-sm">
              ✨
            </div>
            <div>
              <h2 id="early-access-title" className="text-base font-bold text-white leading-tight">
                Request Practice Access
              </h2>
              <p className="text-[11px] text-slate-300">
                Join the modern Indian tax practice operating system
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleResetAndClose}
            className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-white/10 transition-colors"
            aria-label="Close modal"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6">
          {isSuccess ? (
            /* Success State */
            <div className="text-center py-4 space-y-4">
              <div className="w-14 h-14 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center mx-auto ring-8 ring-emerald-50/50">
                <CheckCircle2 className="w-8 h-8" />
              </div>

              <div className="space-y-1.5">
                <h3 className="text-lg font-extrabold text-slate-900">✓ Request Received</h3>
                <p className="text-xs font-semibold text-emerald-700">
                  Thank you for your interest in Taxoryn.
                </p>
              </div>

              <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl text-left text-xs text-slate-600 space-y-2">
                <p className="leading-relaxed">
                  We've received your practice access request. We'll review your request and contact you shortly.
                </p>
                <p className="leading-relaxed text-slate-500">
                  A confirmation has been sent to <strong className="text-slate-700">{submittedEmail}</strong>.
                </p>
              </div>

              <div className="pt-2">
                <Button
                  type="button"
                  variant="primary"
                  onClick={handleResetAndClose}
                  className="w-full py-2.5 font-bold"
                >
                  Done
                </Button>
              </div>
            </div>
          ) : (
            /* Form State */
            <form onSubmit={handleSubmit} className="space-y-4">
              {serverError && (
                <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-start gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                  <div className="flex-1">
                    <p className="font-bold">Unable to submit your request right now.</p>
                    <p className="text-[11px] text-rose-600 mt-0.5">Please try again.</p>
                  </div>
                </div>
              )}

              {/* Anti-Spam Invisible Honeypot */}
              <div style={{ display: 'none' }} aria-hidden="true">
                <label htmlFor="taxoryn_contact_website">Do not fill this field</label>
                <input
                  type="text"
                  id="taxoryn_contact_website"
                  name="taxoryn_contact_website"
                  tabIndex={-1}
                  autoComplete="off"
                  value={formData.honeypot}
                  onChange={(e) => setFormData({ ...formData, honeypot: e.target.value })}
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                {/* Name */}
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Your Name <span className="text-rose-500">*</span>
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

                {/* Email */}
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

              {/* Firm Name */}
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
                {/* Phone */}
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

                {/* City */}
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
                {/* Practice Profile */}
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

                {/* Primary Area of Interest */}
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
              <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-200 text-[11px] text-slate-500 flex items-start gap-2">
                <Shield className="w-3.5 h-3.5 text-slate-400 shrink-0 mt-0.5" />
                <span>
                  <strong>Privacy Note:</strong> Taxoryn never asks for passwords, PAN/Aadhaar credentials, or financial records in access requests.
                </span>
              </div>

              {/* Submit Action */}
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
      </div>
    </div>
  );
};
