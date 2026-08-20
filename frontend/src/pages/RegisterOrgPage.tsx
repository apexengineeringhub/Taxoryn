import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Building2, ArrowRight, AlertCircle, Sparkles } from 'lucide-react';
import { Button } from '../components/common/Button';
import { authApi } from '../api/endpoints';

export const RegisterOrgPage: React.FC = () => {
  const [formData, setFormData] = useState({
    organizationName: '',
    organizationEmail: '',
    pan: '',
    gstin: '',
    adminFirstName: '',
    adminLastName: '',
    adminEmail: '',
    adminPassword: '',
  });

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleQuickFillSample = () => {
    setFormData({
      organizationName: 'Apex Tax Advisors LLP',
      organizationEmail: 'contact@apextax.com',
      pan: 'AABFA1234K',
      gstin: '27AABFA1234K1Z5',
      adminFirstName: 'Rajesh',
      adminLastName: 'Verma',
      adminEmail: `admin.${Date.now()}@apextax.com`,
      adminPassword: 'Password123!',
    });
    setFieldErrors({});
    setGeneralError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFieldErrors({});
    setGeneralError('');
    setIsLoading(true);

    // Client-side quick check
    const errors: Record<string, string> = {};
    if (formData.pan && !/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/.test(formData.pan.trim().toUpperCase())) {
      errors.pan = 'Invalid PAN format. Must be 5 letters, 4 digits, 1 letter (e.g. AABFA1234K)';
    }
    if (formData.gstin && !/^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/.test(formData.gstin.trim().toUpperCase())) {
      errors.gstin = 'Invalid GSTIN format (e.g. 27AABFA1234K1Z5)';
    }
    if (formData.adminPassword && formData.adminPassword.length < 8) {
      errors.adminPassword = 'Password must be at least 8 characters long';
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      setIsLoading(false);
      return;
    }

    try {
      const payload = {
        organizationName: formData.organizationName.trim(),
        organizationEmail: (formData.organizationEmail || formData.adminEmail).trim(),
        pan: formData.pan ? formData.pan.trim().toUpperCase() : undefined,
        gstin: formData.gstin ? formData.gstin.trim().toUpperCase() : undefined,
        adminFirstName: formData.adminFirstName.trim(),
        adminLastName: formData.adminLastName ? formData.adminLastName.trim() : undefined,
        adminEmail: formData.adminEmail.trim(),
        adminPassword: formData.adminPassword,
      };

      await authApi.registerOrg(payload);
      await login(payload.adminEmail, payload.adminPassword);
      navigate('/dashboard');
    } catch (err: any) {
      const resp = err.response?.data;
      if (resp?.validationErrors && Array.isArray(resp.validationErrors)) {
        const backendFieldErrors: Record<string, string> = {};
        resp.validationErrors.forEach((vErr: { field: string; message: string }) => {
          backendFieldErrors[vErr.field] = vErr.message;
        });
        setFieldErrors(backendFieldErrors);
        setGeneralError('Please fix the highlighted field errors below.');
      } else {
        setGeneralError(resp?.message || 'Organization registration failed. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4 py-12 relative overflow-hidden">
      {/* Background Glows */}
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-brand-500/20 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-purple-500/20 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-xl bg-white rounded-2xl shadow-2xl p-8 border border-slate-100 z-10 space-y-6">
        <div className="text-center space-y-2">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-600 flex items-center justify-center text-white font-black text-xl mx-auto shadow-lg shadow-brand-500/30">
            TX
          </div>
          <h2 className="text-2xl font-black text-slate-900 tracking-tight">Onboard Your Tax Practice</h2>
          <p className="text-xs text-slate-500">Create a dedicated multi-tenant practice environment on Taxoryn</p>
        </div>

        {/* General Error Alert */}
        {generalError && (
          <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs font-semibold text-rose-700 flex items-start gap-2">
            <AlertCircle className="w-4 h-4 shrink-0 text-rose-600 mt-0.5" />
            <div>
              <p>{generalError}</p>
              {Object.keys(fieldErrors).length > 0 && (
                <ul className="list-disc list-inside mt-1 font-normal text-rose-600">
                  {Object.entries(fieldErrors).map(([field, msg]) => (
                    <li key={field}><strong className="capitalize">{field}</strong>: {msg}</li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4 text-xs">
          {/* Organization Name */}
          <div>
            <label className="block font-semibold text-slate-700 mb-1">Firm / Practice Name *</label>
            <input
              type="text"
              required
              placeholder="e.g. Apex Tax Advisors LLP"
              value={formData.organizationName}
              onChange={(e) => setFormData({ ...formData, organizationName: e.target.value })}
              className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 ${
                fieldErrors.organizationName ? 'border-rose-400 focus:ring-rose-500/20 bg-rose-50/20' : 'border-slate-200 focus:ring-brand-500'
              }`}
            />
            {fieldErrors.organizationName && (
              <p className="text-rose-600 text-[11px] font-medium mt-1">{fieldErrors.organizationName}</p>
            )}
          </div>

          {/* Firm PAN & GSTIN */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                Firm PAN * <span className="text-[10px] text-slate-400 font-normal">(e.g. AABFA1234K)</span>
              </label>
              <input
                type="text"
                required
                maxLength={10}
                placeholder="AABFA1234K"
                value={formData.pan}
                onChange={(e) => setFormData({ ...formData, pan: e.target.value.toUpperCase() })}
                className={`w-full font-mono uppercase px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 ${
                  fieldErrors.pan ? 'border-rose-400 focus:ring-rose-500/20 bg-rose-50/20' : 'border-slate-200 focus:ring-brand-500'
                }`}
              />
              {fieldErrors.pan && (
                <p className="text-rose-600 text-[11px] font-medium mt-1">{fieldErrors.pan}</p>
              )}
            </div>

            <div>
              <label className="block font-semibold text-slate-700 mb-1">
                Firm GSTIN <span className="text-[10px] text-slate-400 font-normal">(Optional, 15 chars)</span>
              </label>
              <input
                type="text"
                maxLength={15}
                placeholder="27AABFA1234K1Z5"
                value={formData.gstin}
                onChange={(e) => setFormData({ ...formData, gstin: e.target.value.toUpperCase() })}
                className={`w-full font-mono uppercase px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 ${
                  fieldErrors.gstin ? 'border-rose-400 focus:ring-rose-500/20 bg-rose-50/20' : 'border-slate-200 focus:ring-brand-500'
                }`}
              />
              {fieldErrors.gstin && (
                <p className="text-rose-600 text-[11px] font-medium mt-1">{fieldErrors.gstin}</p>
              )}
            </div>
          </div>

          {/* Partner / Admin First & Last Name */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Admin First Name *</label>
              <input
                type="text"
                required
                placeholder="Rajesh"
                value={formData.adminFirstName}
                onChange={(e) => setFormData({ ...formData, adminFirstName: e.target.value })}
                className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 ${
                  fieldErrors.adminFirstName ? 'border-rose-400 focus:ring-rose-500/20 bg-rose-50/20' : 'border-slate-200 focus:ring-brand-500'
                }`}
              />
              {fieldErrors.adminFirstName && (
                <p className="text-rose-600 text-[11px] font-medium mt-1">{fieldErrors.adminFirstName}</p>
              )}
            </div>
            <div>
              <label className="block font-semibold text-slate-700 mb-1">Last Name</label>
              <input
                type="text"
                placeholder="Verma"
                value={formData.adminLastName}
                onChange={(e) => setFormData({ ...formData, adminLastName: e.target.value })}
                className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>
          </div>

          {/* Admin Email */}
          <div>
            <label className="block font-semibold text-slate-700 mb-1">Admin Login Email *</label>
            <input
              type="email"
              required
              placeholder="admin@apextax.com"
              value={formData.adminEmail}
              onChange={(e) => setFormData({ ...formData, adminEmail: e.target.value })}
              className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 ${
                fieldErrors.adminEmail ? 'border-rose-400 focus:ring-rose-500/20 bg-rose-50/20' : 'border-slate-200 focus:ring-brand-500'
              }`}
            />
            {fieldErrors.adminEmail && (
              <p className="text-rose-600 text-[11px] font-medium mt-1">{fieldErrors.adminEmail}</p>
            )}
          </div>

          {/* Password */}
          <div>
            <label className="block font-semibold text-slate-700 mb-1">
              Admin Password * <span className="text-[10px] text-slate-400 font-normal">(Min 8 chars, e.g. Password123!)</span>
            </label>
            <input
              type="password"
              required
              placeholder="••••••••••••"
              value={formData.adminPassword}
              onChange={(e) => setFormData({ ...formData, adminPassword: e.target.value })}
              className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 ${
                fieldErrors.adminPassword ? 'border-rose-400 focus:ring-rose-500/20 bg-rose-50/20' : 'border-slate-200 focus:ring-brand-500'
              }`}
            />
            {fieldErrors.adminPassword && (
              <p className="text-rose-600 text-[11px] font-medium mt-1">{fieldErrors.adminPassword}</p>
            )}
          </div>

          <Button type="submit" className="w-full mt-4" isLoading={isLoading} rightIcon={<ArrowRight className="w-4 h-4" />}>
            Create Practice Account
          </Button>
        </form>

        <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
          <button
            type="button"
            onClick={handleQuickFillSample}
            className="text-brand-600 hover:text-brand-700 font-semibold inline-flex items-center gap-1"
          >
            <Sparkles className="w-3.5 h-3.5" /> Quick Fill Valid Sample
          </button>
          <Link to="/login" className="hover:text-slate-800 font-semibold">
            Sign In →
          </Link>
        </div>
      </div>
    </div>
  );
};
