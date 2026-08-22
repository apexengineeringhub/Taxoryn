import React, { useState, useEffect } from 'react';
import {
  Store,
  ShieldCheck,
  Package,
  Sparkles,
  Save,
  Plus,
  Trash2,
  Edit2,
  ExternalLink,
  CheckCircle2,
  AlertCircle,
  FileText,
  Clock,
  DollarSign,
  Users,
  Eye,
  TrendingUp,
  Globe,
  MapPin,
  Phone,
  Mail,
  RefreshCw,
  Award,
  Check,
  X,
  Lock,
  EyeOff,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { marketplacePracticeApi } from '../api/endpoints';
import { MarketplaceProfile, MarketplaceService, MarketplaceVerification, MarketplaceStats } from '../types';
import clsx from 'clsx';

export const PracticeMarketplaceProfilePage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'PROFILE' | 'SERVICES' | 'VERIFICATION' | 'ANALYTICS'>('PROFILE');
  const [profile, setProfile] = useState<MarketplaceProfile | null>(null);
  const [services, setServices] = useState<MarketplaceService[]>([]);
  const [verification, setVerification] = useState<MarketplaceVerification | null>(null);
  const [stats, setStats] = useState<MarketplaceStats | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [isGeneratingSlug, setIsGeneratingSlug] = useState<boolean>(false);
  const [successBanner, setSuccessBanner] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Service Modal
  const [showServiceModal, setShowServiceModal] = useState<boolean>(false);
  const [editingService, setEditingService] = useState<MarketplaceService | null>(null);
  const [serviceForm, setServiceForm] = useState({
    title: '',
    category: 'GST',
    description: '',
    price: 1999,
    pricingType: 'FIXED' as const,
    deliveryDays: 3,
    deliverables: '',
    isActive: true,
  });

  // Verification Form
  const [kycForm, setKycForm] = useState({
    professionalBody: 'ICAI',
    membershipNumber: '',
    copNumber: '',
    firmRegistrationNumber: '',
    documentUrl: '',
  });

  const loadPracticeData = async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const [profRes, svcRes, kycRes, statsRes] = await Promise.all([
        marketplacePracticeApi.getMyProfile(),
        marketplacePracticeApi.getMyServices().catch(() => []),
        marketplacePracticeApi.getVerificationStatus().catch(() => null),
        marketplacePracticeApi.getStats().catch(() => null),
      ]);

      setProfile(profRes);
      setServices(svcRes || []);
      setVerification(kycRes);
      setStats(statsRes);
    } catch (err: any) {
      console.error('Failed to load practice marketplace profile', err);
      setErrorMessage('Could not load profile. Please verify your practice organization settings.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadPracticeData();
  }, []);

  // 1. Save Profile (Update)
  const handleProfileSave = async (e?: React.FormEvent, publishOverride?: boolean) => {
    if (e) e.preventDefault();
    if (!profile) return;
    setIsSaving(true);
    setErrorMessage(null);
    try {
      const payload: Partial<MarketplaceProfile> = {
        ...profile,
        ...(publishOverride !== undefined ? { isPublished: publishOverride } : {}),
      };

      const updated = await marketplacePracticeApi.updateMyProfile(payload);
      setProfile(updated);
      setSuccessBanner(
        publishOverride === true
          ? 'Profile published to Marketplace directory!'
          : publishOverride === false
          ? 'Profile saved in Draft mode (hidden from customer search).'
          : 'Marketplace profile settings updated successfully!'
      );
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to save profile changes.';
      setErrorMessage(msg);
    } finally {
      setIsSaving(false);
    }
  };

  // 2. Generate Unique Public Slug
  const handleGenerateSlug = async () => {
    if (!profile) return;
    setIsGeneratingSlug(true);
    try {
      const newSlug = await marketplacePracticeApi.generateSlug({
        baseName: profile.displayName,
        city: profile.city,
      });
      setProfile({ ...profile, slug: newSlug });
      setSuccessBanner(`Generated new unique URL slug: ${newSlug}`);
    } catch (err) {
      alert('Could not auto-generate slug. You can enter a custom slug manually.');
    } finally {
      setIsGeneratingSlug(false);
    }
  };

  const handleSaveService = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    try {
      if (editingService?.id) {
        await marketplacePracticeApi.updateService(editingService.id, serviceForm);
      } else {
        await marketplacePracticeApi.createService(serviceForm);
      }
      setShowServiceModal(false);
      setEditingService(null);
      const svc = await marketplacePracticeApi.getMyServices();
      setServices(svc);
      setSuccessBanner('Service package saved successfully!');
      // Reload profile to refresh completeness score
      const prof = await marketplacePracticeApi.getMyProfile();
      setProfile(prof);
    } catch (err) {
      alert('Failed to save service package.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteService = async (serviceId: string) => {
    if (!confirm('Are you sure you want to delete this service package?')) return;
    try {
      await marketplacePracticeApi.deleteService(serviceId);
      setServices(services.filter((s) => s.id !== serviceId));
      setSuccessBanner('Service package deleted.');
      const prof = await marketplacePracticeApi.getMyProfile();
      setProfile(prof);
    } catch (err) {
      alert('Failed to delete service.');
    }
  };

  const handleKycSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    try {
      const res = await marketplacePracticeApi.submitVerification(kycForm);
      setVerification(res);
      setSuccessBanner('KYC verification credentials submitted! Platform Admin will review shortly.');
      const prof = await marketplacePracticeApi.getMyProfile();
      setProfile(prof);
    } catch (err) {
      alert('Failed to submit KYC credentials.');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="p-8 flex items-center justify-center min-h-[400px]">
        <div className="text-center space-y-3">
          <div className="animate-spin w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full mx-auto" />
          <p className="text-sm font-medium text-slate-500">Loading marketplace profile...</p>
        </div>
      </div>
    );
  }

  // Calculate completeness (Requirement 6)
  const completeness = profile?.completenessScore || 0;
  const missingFields = profile?.missingCompletenessFields || [];

  return (
    <div className="max-w-6xl mx-auto space-y-6 pb-20">
      {/* Top Banner & Public Preview */}
      <div className="bg-gradient-to-r from-indigo-900 via-slate-900 to-indigo-950 p-6 sm:p-8 rounded-3xl text-white shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div className="space-y-2">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/20 border border-indigo-400/30 text-indigo-300 text-xs font-semibold">
            <Store className="w-3.5 h-3.5" />
            Marketplace Practice Profile Foundation
          </div>
          <div className="flex items-center gap-3 flex-wrap">
            <h1 className="text-2xl sm:text-3xl font-extrabold">{profile?.displayName || 'My Practice'}</h1>
            {profile?.isPublished ? (
              <span className="px-3 py-1 rounded-full bg-emerald-500/20 border border-emerald-400/40 text-emerald-300 text-xs font-bold flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5" />
                Live on Directory
              </span>
            ) : (
              <span className="px-3 py-1 rounded-full bg-amber-500/20 border border-amber-400/40 text-amber-300 text-xs font-bold flex items-center gap-1.5">
                <Lock className="w-3.5 h-3.5" />
                Draft Mode (Hidden from Customer Search)
              </span>
            )}
          </div>
          <p className="text-xs sm:text-sm text-slate-300 max-w-2xl">
            Configure your firm's public listing, hourly pricing, service packages, public URL slug, and KYC credentials.
          </p>
        </div>

        <div className="flex items-center gap-3 shrink-0 flex-wrap">
          {profile?.slug && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => window.open(`/marketplace/profile/${profile.id}`, '_blank')}
              className="rounded-2xl text-xs bg-white/10 text-white border-white/20 hover:bg-white/20"
            >
              <Eye className="w-3.5 h-3.5 mr-1.5" />
              Preview Profile
            </Button>
          )}

          {profile?.isPublished ? (
            <Button
              variant="outline"
              size="sm"
              disabled={isSaving}
              onClick={() => handleProfileSave(undefined, false)}
              className="rounded-2xl text-xs bg-amber-500/20 text-amber-200 border-amber-400/40 hover:bg-amber-500/30"
            >
              <EyeOff className="w-3.5 h-3.5 mr-1.5" />
              Take Offline
            </Button>
          ) : (
            <Button
              variant="primary"
              size="sm"
              disabled={isSaving}
              onClick={() => handleProfileSave(undefined, true)}
              className="rounded-2xl text-xs bg-emerald-600 hover:bg-emerald-500 text-white"
            >
              <Globe className="w-3.5 h-3.5 mr-1.5" />
              Publish to Directory
            </Button>
          )}
        </div>
      </div>

      {successBanner && (
        <div className="bg-emerald-50 dark:bg-emerald-950/50 p-4 rounded-2xl border border-emerald-200 dark:border-emerald-800 flex items-center justify-between text-sm text-emerald-800 dark:text-emerald-200 animate-fadeIn">
          <div className="flex items-center gap-2 font-medium">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span>{successBanner}</span>
          </div>
          <button onClick={() => setSuccessBanner(null)} className="text-emerald-600 font-bold hover:opacity-75">×</button>
        </div>
      )}

      {errorMessage && (
        <div className="bg-rose-50 dark:bg-rose-950/50 p-4 rounded-2xl border border-rose-200 dark:border-rose-800 flex items-center justify-between text-sm text-rose-800 dark:text-rose-200 animate-fadeIn">
          <div className="flex items-center gap-2 font-medium">
            <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button onClick={() => setErrorMessage(null)} className="text-rose-600 font-bold hover:opacity-75">×</button>
        </div>
      )}

      {/* Profile Completeness Card (Requirement 6) */}
      <div className="bg-white dark:bg-slate-900 p-6 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <Award className="w-5 h-5 text-indigo-600" />
              <h3 className="text-sm sm:text-base font-bold text-slate-900 dark:text-white">Profile Completeness</h3>
              <span
                className={clsx(
                  'px-2.5 py-0.5 rounded-full text-xs font-extrabold',
                  completeness >= 80
                    ? 'bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300'
                    : completeness >= 50
                    ? 'bg-amber-100 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300'
                    : 'bg-rose-100 dark:bg-rose-950/60 text-rose-700 dark:text-rose-300'
                )}
              >
                {completeness}% {completeness >= 80 ? 'Excellent' : completeness >= 50 ? 'Good' : 'Incomplete'}
              </span>
            </div>
            <p className="text-xs text-slate-500">
              Complete profiles rank higher and convert 3.5x more prospective clients on the marketplace.
            </p>
          </div>

          <div className="w-full sm:w-64 space-y-1.5">
            <div className="flex justify-between text-xs font-semibold text-slate-600 dark:text-slate-400">
              <span>Overall Score</span>
              <span>{completeness} / 100</span>
            </div>
            <div className="w-full h-2.5 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
              <div
                className={clsx(
                  'h-full transition-all duration-500 rounded-full',
                  completeness >= 80 ? 'bg-emerald-500' : completeness >= 50 ? 'bg-amber-500' : 'bg-rose-500'
                )}
                style={{ width: `${completeness}%` }}
              />
            </div>
          </div>
        </div>

        {missingFields.length > 0 && (
          <div className="p-4 bg-slate-50 dark:bg-slate-800/60 rounded-2xl border border-slate-200/60 dark:border-slate-700/60">
            <div className="text-xs font-bold text-slate-700 dark:text-slate-300 mb-2">Recommended steps to reach 100%:</div>
            <div className="flex flex-wrap gap-2">
              {missingFields.map((field, idx) => (
                <span
                  key={idx}
                  className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-[11px] font-medium text-slate-600 dark:text-slate-300"
                >
                  <span className="w-1.5 h-1.5 rounded-full bg-amber-500" />
                  {field}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Navigation Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-200 dark:border-slate-800 pb-3 flex-wrap">
        <button
          onClick={() => setActiveTab('PROFILE')}
          className={clsx(
            'px-5 py-2.5 rounded-2xl text-xs sm:text-sm font-bold transition-all flex items-center gap-2',
            activeTab === 'PROFILE'
              ? 'bg-indigo-600 text-white shadow-md'
              : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
          )}
        >
          <Store className="w-4 h-4" />
          <span>Listing Profile & Pricing</span>
        </button>

        <button
          onClick={() => setActiveTab('SERVICES')}
          className={clsx(
            'px-5 py-2.5 rounded-2xl text-xs sm:text-sm font-bold transition-all flex items-center gap-2',
            activeTab === 'SERVICES'
              ? 'bg-indigo-600 text-white shadow-md'
              : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
          )}
        >
          <Package className="w-4 h-4" />
          <span>Service Packages ({services.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('VERIFICATION')}
          className={clsx(
            'px-5 py-2.5 rounded-2xl text-xs sm:text-sm font-bold transition-all flex items-center gap-2',
            activeTab === 'VERIFICATION'
              ? 'bg-indigo-600 text-white shadow-md'
              : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
          )}
        >
          <ShieldCheck className="w-4 h-4" />
          <span>KYC & Verified Badge</span>
        </button>

        <button
          onClick={() => setActiveTab('ANALYTICS')}
          className={clsx(
            'px-5 py-2.5 rounded-2xl text-xs sm:text-sm font-bold transition-all flex items-center gap-2',
            activeTab === 'ANALYTICS'
              ? 'bg-indigo-600 text-white shadow-md'
              : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
          )}
        >
          <TrendingUp className="w-4 h-4" />
          <span>Discovery Analytics</span>
        </button>
      </div>

      {/* Tab 1: Profile & Settings */}
      {activeTab === 'PROFILE' && profile && (
        <form onSubmit={(e) => handleProfileSave(e)} className="space-y-6">
          <div className="bg-white dark:bg-slate-900 p-6 sm:p-8 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-6">
            
            {/* Visibility & Draft Status (Requirements 4 & 5) */}
            <div className="p-5 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-700/60 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  <Globe className="w-4 h-4 text-indigo-600" />
                  Marketplace Search Visibility
                </h3>
                <p className="text-xs text-slate-500">
                  Control whether prospective clients can find your profile in customer marketplace search.
                </p>
              </div>
              <div className="flex items-center gap-4">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={profile.isPublished}
                    onChange={(e) => setProfile({ ...profile, isPublished: e.target.checked })}
                    className="w-5 h-5 text-indigo-600 rounded border-slate-300 focus:ring-indigo-500"
                  />
                  <span className="text-xs font-bold text-slate-800 dark:text-slate-200">
                    {profile.isPublished ? 'Public Listing Enabled' : 'Draft Mode (Hidden)'}
                  </span>
                </label>
              </div>
            </div>

            {/* Public Slug Manager (Requirement 7) */}
            <div className="p-5 rounded-2xl bg-indigo-50/50 dark:bg-indigo-950/20 border border-indigo-100 dark:border-indigo-900/40 space-y-3">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <h4 className="text-xs font-bold text-indigo-900 dark:text-indigo-200 flex items-center gap-1.5">
                    <ExternalLink className="w-3.5 h-3.5 text-indigo-600" />
                    Public Directory URL Slug
                  </h4>
                  <p className="text-[11px] text-slate-500">
                    Your unique URL identifier for sharing with prospective clients.
                  </p>
                </div>
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  disabled={isGeneratingSlug}
                  onClick={handleGenerateSlug}
                  className="rounded-xl text-xs shrink-0"
                >
                  <RefreshCw className={clsx('w-3.5 h-3.5 mr-1.5', isGeneratingSlug && 'animate-spin')} />
                  {isGeneratingSlug ? 'Generating...' : 'Auto-Generate Slug'}
                </Button>
              </div>

              <div className="flex items-center gap-2">
                <span className="text-xs font-medium text-slate-500 dark:text-slate-400 bg-white dark:bg-slate-900 px-3 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 select-none">
                  taxoryn.com/marketplace/profile/
                </span>
                <input
                  type="text"
                  required
                  value={profile.slug || ''}
                  onChange={(e) =>
                    setProfile({
                      ...profile,
                      slug: e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, '-'),
                    })
                  }
                  className="flex-1 px-3.5 py-2.5 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-xs font-mono text-indigo-600 dark:text-indigo-400 font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="firm-unique-slug"
                />
              </div>
            </div>

            {/* Basic Information (Requirements 1 & 3) */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Firm / Display Name *</label>
                <input
                  type="text"
                  required
                  value={profile.displayName || ''}
                  onChange={(e) => setProfile({ ...profile, displayName: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="e.g. Apex Corporate & Tax Advisors"
                />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Professional Designation *</label>
                <select
                  value={profile.professionalType}
                  onChange={(e) => setProfile({ ...profile, professionalType: e.target.value as any })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                >
                  <option value="CHARTERED_ACCOUNTANT">Chartered Accountant (CA)</option>
                  <option value="COMPANY_SECRETARY">Company Secretary (CS)</option>
                  <option value="TAX_ADVOCATE">Tax Advocate</option>
                  <option value="COST_ACCOUNTANT">Cost Accountant (CMA)</option>
                  <option value="TAX_CONSULTANT">Tax Consultant</option>
                </select>
              </div>
            </div>

            <div>
              <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Listing Headline / Tagline</label>
              <input
                type="text"
                value={profile.headline || ''}
                onChange={(e) => setProfile({ ...profile, headline: e.target.value })}
                className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                placeholder="e.g. Ex-Big4 Senior CAs specializing in Corporate Direct & Indirect Tax Optimization"
              />
            </div>

            <div>
              <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Detailed Firm Bio</label>
              <textarea
                rows={4}
                value={profile.bio || ''}
                onChange={(e) => setProfile({ ...profile, bio: e.target.value })}
                className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500 leading-relaxed"
                placeholder="Describe your practice history, partners' qualifications, core areas of expertise..."
              />
            </div>

            {/* Location & Contact Information */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">City *</label>
                <input
                  type="text"
                  value={profile.city || ''}
                  onChange={(e) => setProfile({ ...profile, city: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="e.g. Mumbai"
                />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">State *</label>
                <input
                  type="text"
                  value={profile.state || ''}
                  onChange={(e) => setProfile({ ...profile, state: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="e.g. Maharashtra"
                />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Pincode</label>
                <input
                  type="text"
                  value={profile.pincode || ''}
                  onChange={(e) => setProfile({ ...profile, pincode: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="400001"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Office Phone</label>
                <input
                  type="text"
                  value={profile.phone || ''}
                  onChange={(e) => setProfile({ ...profile, phone: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="+91 98201 12233"
                />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Public Email</label>
                <input
                  type="email"
                  value={profile.email || ''}
                  onChange={(e) => setProfile({ ...profile, email: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="contact@apextax.in"
                />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Website URL</label>
                <input
                  type="url"
                  value={profile.websiteUrl || ''}
                  onChange={(e) => setProfile({ ...profile, websiteUrl: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="https://apextax.in"
                />
              </div>
            </div>

            <div>
              <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Office Address</label>
              <input
                type="text"
                value={profile.address || ''}
                onChange={(e) => setProfile({ ...profile, address: e.target.value })}
                className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                placeholder="Suite 402, Trade Center, BKC, Bandra East"
              />
            </div>

            {/* Experience, Languages & Specializations */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Years in Practice</label>
                <input
                  type="number"
                  value={profile.experienceYears}
                  onChange={(e) => setProfile({ ...profile, experienceYears: parseInt(e.target.value) || 0 })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Languages Spoken</label>
                <input
                  type="text"
                  value={profile.languagesSpoken || ''}
                  onChange={(e) => setProfile({ ...profile, languagesSpoken: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="e.g. English, Hindi, Marathi, Gujarati"
                />
              </div>
            </div>

            {/* Pricing Details */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Starting Service Package Fee (₹)</label>
                <input
                  type="number"
                  value={profile.startingFee}
                  onChange={(e) => setProfile({ ...profile, startingFee: parseFloat(e.target.value) || 0 })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Standard Hourly Advisory Rate (₹)</label>
                <input
                  type="number"
                  value={profile.hourlyRate}
                  onChange={(e) => setProfile({ ...profile, hourlyRate: parseFloat(e.target.value) || 0 })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            </div>

            {/* Paid Consultation Settings */}
            <div className="p-5 bg-indigo-50/50 dark:bg-indigo-950/20 rounded-2xl border border-indigo-100 dark:border-indigo-900/40 space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h4 className="text-xs font-bold text-indigo-900 dark:text-indigo-200">Paid Direct Consultation Booking</h4>
                  <p className="text-[11px] text-slate-500">Allow customers to book a scheduled 30-min strategy session.</p>
                </div>
                <input
                  type="checkbox"
                  checked={profile.consultationEnabled}
                  onChange={(e) => setProfile({ ...profile, consultationEnabled: e.target.checked })}
                  className="w-4 h-4 text-indigo-600 rounded"
                />
              </div>

              {profile.consultationEnabled && (
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Session Fee (INR)</label>
                    <input
                      type="number"
                      value={profile.consultationFee}
                      onChange={(e) => setProfile({ ...profile, consultationFee: parseFloat(e.target.value) || 0 })}
                      className="w-full mt-1 px-3 py-2 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-xs"
                    />
                  </div>
                  <div>
                    <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Duration (Minutes)</label>
                    <input
                      type="number"
                      value={profile.consultationDurationMinutes}
                      onChange={(e) => setProfile({ ...profile, consultationDurationMinutes: parseInt(e.target.value) || 30 })}
                      className="w-full mt-1 px-3 py-2 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-xs"
                    />
                  </div>
                </div>
              )}
            </div>

            {/* Action Bar (Requirements 3, 4, 5) */}
            <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-6 border-t border-slate-100 dark:border-slate-800">
              <Button
                type="button"
                variant="outline"
                size="md"
                disabled={isSaving}
                onClick={() => handleProfileSave(undefined, false)}
                className="w-full sm:w-auto rounded-2xl"
              >
                <Save className="w-4 h-4 mr-2 text-amber-500" />
                Save as Draft (Offline)
              </Button>

              <div className="flex items-center gap-3 w-full sm:w-auto">
                <Button
                  type="submit"
                  variant="primary"
                  size="md"
                  disabled={isSaving}
                  className="w-full sm:w-auto rounded-2xl px-6"
                >
                  <Save className="w-4 h-4 mr-2" />
                  {isSaving ? 'Saving Changes...' : 'Save Profile Changes'}
                </Button>
              </div>
            </div>
          </div>
        </form>
      )}

      {/* Tab 2: Services Manager */}
      {activeTab === 'SERVICES' && (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-white">Service Packages & Pricing</h3>
              <p className="text-xs text-slate-500">Offer transparent fixed-fee or monthly retainer packages to prospective clients.</p>
            </div>
            <Button
              size="sm"
              variant="primary"
              onClick={() => {
                setEditingService(null);
                setServiceForm({
                  title: '',
                  category: 'GST',
                  description: '',
                  price: 1999,
                  pricingType: 'FIXED',
                  deliveryDays: 3,
                  deliverables: '',
                  isActive: true,
                });
                setShowServiceModal(true);
              }}
              className="rounded-2xl"
            >
              <Plus className="w-4 h-4 mr-1.5" />
              Add Service Package
            </Button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {services.map((svc) => (
              <div
                key={svc.id}
                className="bg-white dark:bg-slate-900 p-6 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm flex flex-col justify-between space-y-4"
              >
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-600 bg-indigo-50 dark:bg-indigo-950/60 px-2 py-0.5 rounded">
                      {svc.category}
                    </span>
                    <span className="text-xs text-slate-400">{svc.deliveryDays}d TAT</span>
                  </div>
                  <h4 className="text-base font-bold text-slate-900 dark:text-white">{svc.title}</h4>
                  <p className="text-xs text-slate-500 line-clamp-2">{svc.description}</p>
                </div>

                <div className="pt-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
                  <div className="text-lg font-extrabold text-slate-900 dark:text-white">
                    ₹{svc.price?.toLocaleString('en-IN')}
                    <span className="text-[10px] text-slate-400 font-normal ml-1">
                      {svc.pricingType === 'MONTHLY_RETAINER' ? '/mo' : ''}
                    </span>
                  </div>

                  <div className="flex items-center gap-1.5">
                    <button
                      onClick={() => {
                        setEditingService(svc);
                        setServiceForm({
                          title: svc.title,
                          category: svc.category,
                          description: svc.description || '',
                          price: svc.price,
                          pricingType: svc.pricingType as any,
                          deliveryDays: svc.deliveryDays,
                          deliverables: svc.deliverables || '',
                          isActive: svc.isActive,
                        });
                        setShowServiceModal(true);
                      }}
                      className="p-2 text-slate-500 hover:text-indigo-600"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                    {svc.id && (
                      <button onClick={() => handleDeleteService(svc.id!)} className="p-2 text-slate-500 hover:text-rose-600">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Tab 3: Verification & KYC */}
      {activeTab === 'VERIFICATION' && (
        <div className="bg-white dark:bg-slate-900 p-8 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-6">
          <div className="flex items-start justify-between">
            <div className="space-y-1">
              <h3 className="text-lg font-bold text-slate-900 dark:text-white">ICAI / ICSI Verified Practice Badge</h3>
              <p className="text-xs text-slate-500">
                Verified badges build trust and rank your practice at the top of customer search results.
              </p>
            </div>
            {verification ? (
              <span
                className={clsx(
                  'px-3.5 py-1.5 rounded-full text-xs font-bold flex items-center gap-1.5 border',
                  verification.verificationStatus === 'VERIFIED'
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : verification.verificationStatus === 'PENDING'
                    ? 'bg-amber-50 text-amber-700 border-amber-200'
                    : 'bg-rose-50 text-rose-700 border-rose-200'
                )}
              >
                <ShieldCheck className="w-4 h-4" />
                Status: {verification.verificationStatus}
              </span>
            ) : null}
          </div>

          <form onSubmit={handleKycSubmit} className="space-y-4 max-w-2xl">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Statutory Governing Body *</label>
                <select
                  value={kycForm.professionalBody}
                  onChange={(e) => setKycForm({ ...kycForm, professionalBody: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                >
                  <option value="ICAI">ICAI (Chartered Accountants)</option>
                  <option value="ICSI">ICSI (Company Secretaries)</option>
                  <option value="ICMAI">ICMAI (Cost Accountants)</option>
                  <option value="BAR_COUNCIL">State Bar Council (Advocates)</option>
                </select>
              </div>
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Membership Number (MRN) *</label>
                <input
                  type="text"
                  required
                  value={kycForm.membershipNumber}
                  onChange={(e) => setKycForm({ ...kycForm, membershipNumber: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  placeholder="e.g. FCA-504932"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Certificate of Practice (COP) No.</label>
                <input
                  type="text"
                  value={kycForm.copNumber}
                  onChange={(e) => setKycForm({ ...kycForm, copNumber: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  placeholder="COP-2019/8492"
                />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Firm Registration Number (FRN)</label>
                <input
                  type="text"
                  value={kycForm.firmRegistrationNumber}
                  onChange={(e) => setKycForm({ ...kycForm, firmRegistrationNumber: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  placeholder="104928W"
                />
              </div>
            </div>

            <div>
              <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Certificate Document URL / Cloud Drive</label>
              <input
                type="text"
                value={kycForm.documentUrl}
                onChange={(e) => setKycForm({ ...kycForm, documentUrl: e.target.value })}
                className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                placeholder="https://drive.google.com/... or uploaded document link"
              />
            </div>

            <Button type="submit" variant="primary" size="md" disabled={isSaving} className="mt-4">
              <ShieldCheck className="w-4 h-4 mr-2" />
              {isSaving ? 'Submitting...' : 'Submit Credentials for Verification'}
            </Button>
          </form>
        </div>
      )}

      {/* Tab 4: Analytics */}
      {activeTab === 'ANALYTICS' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white dark:bg-slate-900 p-6 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
            <div className="text-xs font-bold uppercase text-slate-400">Total Inbound Inquiries</div>
            <div className="text-3xl font-extrabold text-slate-900 dark:text-white">{stats?.totalInboundLeads || 0}</div>
            <div className="text-[11px] text-slate-500">Prospective clients requesting callback</div>
          </div>

          <div className="bg-white dark:bg-slate-900 p-6 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
            <div className="text-xs font-bold uppercase text-slate-400">Converted to Practice Clients</div>
            <div className="text-3xl font-extrabold text-emerald-600">{stats?.totalConvertedClients || 0}</div>
            <div className="text-[11px] text-emerald-600 font-semibold">{stats?.leadConversionRate || 0}% Conversion Rate</div>
          </div>

          <div className="bg-white dark:bg-slate-900 p-6 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
            <div className="text-xs font-bold uppercase text-slate-400">Estimated Pipeline Value</div>
            <div className="text-3xl font-extrabold text-indigo-600">
              ₹{(stats?.estimatedMarketplacePipelineValue || 0).toLocaleString('en-IN')}
            </div>
            <div className="text-[11px] text-slate-500">Calculated across incoming compliance requirements</div>
          </div>
        </div>
      )}

      {/* Service Modal */}
      {showServiceModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-md w-full p-6 space-y-4 shadow-2xl">
            <h3 className="text-base font-bold text-slate-900 dark:text-white">
              {editingService ? 'Edit Service Package' : 'Create Service Package'}
            </h3>

            <form onSubmit={handleSaveService} className="space-y-3">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Package Title *</label>
                <input
                  type="text"
                  required
                  value={serviceForm.title}
                  onChange={(e) => setServiceForm({ ...serviceForm, title: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  placeholder="e.g. Monthly GST Filing & ITC Reconciliation"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Category *</label>
                  <select
                    value={serviceForm.category}
                    onChange={(e) => setServiceForm({ ...serviceForm, category: e.target.value })}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  >
                    <option value="GST">GST</option>
                    <option value="ITR">Income Tax (ITR)</option>
                    <option value="TDS">TDS</option>
                    <option value="COMPANY_FORMATION">Company Formation</option>
                    <option value="AUDIT">Audit</option>
                  </select>
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Price (INR) *</label>
                  <input
                    type="number"
                    required
                    value={serviceForm.price}
                    onChange={(e) => setServiceForm({ ...serviceForm, price: parseFloat(e.target.value) || 0 })}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Deliverables</label>
                <textarea
                  rows={2}
                  value={serviceForm.deliverables}
                  onChange={(e) => setServiceForm({ ...serviceForm, deliverables: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  placeholder="e.g. GSTR-3B ARN, GSTR-1 ARN, ITC Reconciliation Sheet"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3">
                <Button type="button" variant="outline" size="sm" onClick={() => setShowServiceModal(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" disabled={isSaving}>
                  Save Package
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
