import React, { useState, useEffect } from 'react';
import {
  Sparkles,
  CheckCircle2,
  AlertCircle,
  Globe,
  MapPin,
  Phone,
  Mail,
  RefreshCw,
  Award,
  Link,
  Check,
  Eye,
  EyeOff,
  Clock,
  Building2,
  ArrowRight,
  Info,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { useBranding } from '../context/BrandingContext';
import { useAuth } from '../context/AuthContext';
import { marketplacePracticeApi } from '../api/endpoints';
import { MarketplaceProfile, ProfileCompleteness, VisibilityStatus, VerificationStatus } from '../types';
import clsx from 'clsx';

export const PracticeMarketplaceProfilePage: React.FC = () => {
  const { currentTheme } = useBranding();
  const { practiceName } = useAuth();
  const isLight = currentTheme.mode === 'light';

  const [profile, setProfile] = useState<MarketplaceProfile | null>(null);
  const [completeness, setCompleteness] = useState<ProfileCompleteness | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isTogglingVisibility, setIsTogglingVisibility] = useState<boolean>(false);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [isGeneratingSlug, setIsGeneratingSlug] = useState<boolean>(false);
  const [copiedSlug, setCopiedSlug] = useState<boolean>(false);

  // Notifications
  const [successBanner, setSuccessBanner] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Edit Profile Modal
  const [isEditModalOpen, setIsEditModalOpen] = useState<boolean>(false);
  const [formData, setFormData] = useState({
    displayName: '',
    slug: '',
    description: '',
    phone: '',
    email: '',
    website: '',
    city: '',
    state: '',
    pincode: '',
    address: '',
    experienceYears: 0,
  });

  const loadData = async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const [profileData, completenessData] = await Promise.all([
        marketplacePracticeApi.getMyProfile(),
        marketplacePracticeApi.getProfileCompleteness().catch(() => null),
      ]);

      setProfile(profileData);
      setCompleteness(
        completenessData ||
        profileData.profileCompleteness ||
        profileData.completeness || {
          percentage: profileData.completenessScore || 60,
          completedItems: ['Display Name', 'Organization Link'],
          missingItems: ['City & State', 'Contact Phone', 'Bio Description'],
        }
      );

      // Populate edit form
      setFormData({
        displayName: profileData.displayName || practiceName || '',
        slug: profileData.publicSlug || profileData.slug || '',
        description: profileData.description || profileData.bio || '',
        phone: profileData.phone || '',
        email: profileData.email || '',
        website: profileData.website || profileData.websiteUrl || '',
        city: profileData.city || '',
        state: profileData.state || '',
        pincode: profileData.pincode || '',
        address: profileData.address || '',
        experienceYears: profileData.experienceYears || 0,
      });
    } catch (err: any) {
      console.error('Failed to load marketplace profile:', err);
      setErrorMessage('Could not retrieve practice marketplace profile. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const isPublic = profile?.visibilityStatus === 'PUBLIC';

  // Toggle Visibility (ON / OFF)
  const handleToggleVisibility = async () => {
    if (!profile) return;
    setIsTogglingVisibility(true);
    setErrorMessage(null);
    setSuccessBanner(null);

    const targetVisibility: VisibilityStatus = isPublic ? 'PRIVATE' : 'PUBLIC';

    try {
      const updated = await marketplacePracticeApi.updateVisibility(targetVisibility);
      setProfile(updated);
      if (updated.profileCompleteness || updated.completeness) {
        setCompleteness(updated.profileCompleteness || updated.completeness || null);
      }
      setSuccessBanner(
        targetVisibility === 'PUBLIC'
          ? 'Marketplace visibility turned ON! Your practice profile is now live for client discovery.'
          : 'Marketplace visibility turned OFF. Your profile is saved as a private draft.'
      );
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to update visibility.';
      setErrorMessage(msg);
    } finally {
      setIsTogglingVisibility(false);
    }
  };

  // Generate Unique Public Slug
  const handleGenerateSlug = async () => {
    setIsGeneratingSlug(true);
    try {
      const suggested = await marketplacePracticeApi.generateSlug({
        baseName: formData.displayName || profile?.displayName,
        city: formData.city || profile?.city,
      });
      setFormData((prev) => ({ ...prev, slug: suggested }));
    } catch (err: any) {
      console.error('Failed to auto-generate slug:', err);
    } finally {
      setIsGeneratingSlug(false);
    }
  };

  // Save Profile Changes
  const handleSaveProfile = async (e?: React.FormEvent, andPublish = false) => {
    if (e) e.preventDefault();
    setIsSaving(true);
    setErrorMessage(null);
    setSuccessBanner(null);

    try {
      const payload: Partial<MarketplaceProfile> = {
        displayName: formData.displayName,
        slug: formData.slug,
        description: formData.description,
        bio: formData.description,
        phone: formData.phone,
        email: formData.email,
        website: formData.website,
        websiteUrl: formData.website,
        city: formData.city,
        state: formData.state,
        pincode: formData.pincode,
        address: formData.address,
        experienceYears: Number(formData.experienceYears) || 0,
        visibilityStatus: andPublish ? 'PUBLIC' : profile?.visibilityStatus || 'PRIVATE',
      };

      const updated = await marketplacePracticeApi.updateMyProfile(payload);
      setProfile(updated);
      if (updated.profileCompleteness || updated.completeness) {
        setCompleteness(updated.profileCompleteness || updated.completeness || null);
      }

      setIsEditModalOpen(false);
      setSuccessBanner('Practice profile updated successfully.');
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to save practice profile.';
      setErrorMessage(msg);
    } finally {
      setIsSaving(false);
    }
  };

  const copyPublicUrl = () => {
    const slug = profile?.publicSlug || profile?.slug || '';
    if (!slug) return;
    const url = `${window.location.origin}/marketplace/${slug}`;
    navigator.clipboard.writeText(url);
    setCopiedSlug(true);
    setTimeout(() => setCopiedSlug(false), 2500);
  };

  const percentage = completeness?.percentage ?? 0;
  const completedList = completeness?.completedItems || [];
  const missingList = completeness?.missingItems || [];

  return (
    <div className="max-w-4xl mx-auto space-y-6 pb-16 animate-fade-in">
      {/* Breadcrumb Navigation */}
      <div className="flex items-center gap-2 text-xs font-semibold text-slate-500">
        <span>Settings</span>
        <span>/</span>
        <span className="text-slate-900 font-bold">Marketplace</span>
      </div>

      {/* Header Banner */}
      <div
        className={clsx(
          'p-6 sm:p-8 rounded-2xl border transition-all duration-300 relative overflow-hidden',
          isLight
            ? 'bg-gradient-to-br from-indigo-900 via-slate-900 to-slate-950 text-white border-slate-800 shadow-xl'
            : 'bg-gradient-to-br from-slate-900 via-slate-900/90 to-black text-white border-slate-800/80 shadow-2xl'
        )}
      >
        <div className="relative z-10 max-w-2xl space-y-3">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-bold bg-white/10 text-indigo-200 border border-white/15 backdrop-blur-xs">
            <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
            <span>Marketplace Practice Foundation</span>
          </div>

          <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-white">
            Grow your practice with Taxoryn.
          </h1>

          <p className="text-sm sm:text-base text-slate-300 font-normal leading-relaxed">
            Let customers discover your practice based on location and services.
          </p>
        </div>

        {/* Decorative background glow */}
        <div
          className="absolute -right-16 -top-16 w-64 h-64 rounded-full opacity-20 blur-3xl pointer-events-none"
          style={{ backgroundColor: currentTheme.primaryColor }}
        />
      </div>

      {/* Notification Banners */}
      {successBanner && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-medium flex items-center justify-between gap-3 animate-fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{successBanner}</span>
          </div>
          <button
            onClick={() => setSuccessBanner(null)}
            className="text-emerald-600 hover:text-emerald-900 font-bold text-xs"
          >
            Dismiss
          </button>
        </div>
      )}

      {errorMessage && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-medium flex items-center justify-between gap-3 animate-fade-in">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button
            onClick={() => setErrorMessage(null)}
            className="text-rose-600 hover:text-rose-900 font-bold text-xs"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Main Settings Section */}
      <div className="space-y-6">
        {/* 1. Marketplace Visibility Switch */}
        <Card className="p-6 sm:p-7 border border-slate-200 shadow-xs">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-slate-900">Marketplace Visibility</h2>
                <span
                  className={clsx(
                    'px-2.5 py-0.5 rounded-full text-xs font-extrabold tracking-wide uppercase',
                    isPublic
                      ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                      : 'bg-slate-100 text-slate-700 border border-slate-200'
                  )}
                >
                  {isPublic ? 'ON' : 'OFF'}
                </span>
              </div>
              <p className="text-xs text-slate-500 max-w-xl">
                {isPublic
                  ? 'Your practice is currently live and discoverable by clients on the Taxoryn Marketplace.'
                  : 'Your practice is currently hidden. Turn visibility ON when your profile is complete to accept inquiries.'}
              </p>
            </div>

            <div className="flex items-center gap-3 shrink-0">
              <button
                type="button"
                onClick={handleToggleVisibility}
                disabled={isTogglingVisibility || isLoading}
                className={clsx(
                  'relative inline-flex h-8 w-16 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-hidden focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2',
                  isPublic ? 'bg-emerald-500' : 'bg-slate-300'
                )}
                aria-pressed={isPublic}
              >
                <span className="sr-only">Toggle Marketplace Visibility</span>
                <span
                  aria-hidden="true"
                  className={clsx(
                    'pointer-events-none inline-block h-7 w-7 transform rounded-full bg-white shadow-md ring-0 transition duration-200 ease-in-out flex items-center justify-center text-[10px] font-black',
                    isPublic ? 'translate-x-8 text-emerald-600' : 'translate-x-0 text-slate-500'
                  )}
                >
                  {isPublic ? 'ON' : 'OFF'}
                </span>
              </button>

              <Button
                variant={isPublic ? 'outline' : 'primary'}
                size="sm"
                onClick={handleToggleVisibility}
                isLoading={isTogglingVisibility}
                disabled={isLoading}
              >
                {isPublic ? (
                  <>
                    <EyeOff className="w-3.5 h-3.5 mr-1.5" />
                    Turn OFF
                  </>
                ) : (
                  <>
                    <Eye className="w-3.5 h-3.5 mr-1.5" />
                    Turn ON
                  </>
                )}
              </Button>
            </div>
          </div>
        </Card>

        {/* 2. Practice Profile Summary Card */}
        <Card className="p-6 sm:p-7 border border-slate-200 shadow-xs space-y-6">
          <div className="flex items-start justify-between gap-4 border-b border-slate-100 pb-5">
            <div>
              <span className="text-[11px] font-bold uppercase tracking-wider text-indigo-600 block mb-1">
                Practice Profile
              </span>
              <h3 className="text-xl font-extrabold text-slate-900">
                {profile?.displayName || practiceName || 'ABC Tax Consultants'}
              </h3>
              <div className="flex flex-wrap items-center gap-3 mt-1.5 text-xs text-slate-600">
                <span className="flex items-center gap-1">
                  <MapPin className="w-3.5 h-3.5 text-slate-400" />
                  {profile?.city ? `${profile.city}${profile.state ? `, ${profile.state}` : ''}` : 'Bangalore'}
                </span>
                <span>•</span>
                <span className="flex items-center gap-1">
                  <Clock className="w-3.5 h-3.5 text-slate-400" />
                  {profile?.experienceYears ? `${profile.experienceYears} Years Experience` : '10+ Years Experience'}
                </span>
                <span>•</span>
                <span
                  className={clsx(
                    'px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider',
                    profile?.verificationStatus === 'VERIFIED'
                      ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                      : profile?.verificationStatus === 'PENDING'
                      ? 'bg-amber-50 text-amber-700 border border-amber-200'
                      : 'bg-slate-100 text-slate-600 border border-slate-200'
                  )}
                >
                  <Award className="w-3 h-3 inline mr-1" />
                  {profile?.verificationStatus || 'NOT_SUBMITTED'}
                </span>
              </div>
            </div>

            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsEditModalOpen(true)}
              disabled={isLoading}
            >
              Edit Profile
            </Button>
          </div>

          {/* Profile Details Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs text-slate-600 bg-slate-50/70 p-4 rounded-xl border border-slate-100">
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <Mail className="w-3.5 h-3.5 text-slate-400" />
                <span className="font-semibold text-slate-700">Email:</span>
                <span className="text-slate-900">{profile?.email || 'Not specified'}</span>
              </div>
              <div className="flex items-center gap-2">
                <Phone className="w-3.5 h-3.5 text-slate-400" />
                <span className="font-semibold text-slate-700">Phone:</span>
                <span className="text-slate-900">{profile?.phone || 'Not specified'}</span>
              </div>
            </div>

            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <Globe className="w-3.5 h-3.5 text-slate-400" />
                <span className="font-semibold text-slate-700">Website:</span>
                <span className="text-slate-900 truncate">{profile?.website || profile?.websiteUrl || 'Not specified'}</span>
              </div>
              <div className="flex items-center gap-2">
                <Link className="w-3.5 h-3.5 text-slate-400" />
                <span className="font-semibold text-slate-700">Public Slug:</span>
                <span className="font-mono text-indigo-600 font-bold truncate">
                  {profile?.publicSlug || profile?.slug || 'Not generated'}
                </span>
                {(profile?.publicSlug || profile?.slug) && (
                  <button
                    onClick={copyPublicUrl}
                    className="ml-auto text-indigo-600 hover:text-indigo-800 text-[10px] font-bold inline-flex items-center gap-1"
                  >
                    {copiedSlug ? <Check className="w-3 h-3 text-emerald-600" /> : 'Copy Link'}
                  </button>
                )}
              </div>
            </div>
          </div>

          {/* 3. Profile Completeness Progress Widget */}
          <div className="space-y-3 pt-2">
            <div className="flex items-center justify-between text-xs">
              <span className="font-bold text-slate-800 flex items-center gap-1.5">
                <CheckCircle2 className="w-4 h-4 text-indigo-600" />
                Profile Completeness
              </span>
              <span className="font-mono font-extrabold text-slate-900 text-sm">{percentage}%</span>
            </div>

            {/* Visual Progress Bar */}
            <div className="w-full bg-slate-100 rounded-full h-3 overflow-hidden border border-slate-200/80 p-0.5">
              <div
                className="bg-indigo-600 h-full rounded-full transition-all duration-500"
                style={{ width: `${Math.min(100, Math.max(0, percentage))}%` }}
              />
            </div>

            {/* Completed and Missing Checklists */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
              {completedList.length > 0 && (
                <div className="space-y-1.5">
                  <span className="text-[11px] font-bold text-emerald-700 uppercase tracking-wider block">
                    Completed Items
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    {completedList.map((item, idx) => (
                      <span
                        key={idx}
                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200/70"
                      >
                        <Check className="w-3 h-3 text-emerald-600" />
                        {item}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {missingList.length > 0 && (
                <div className="space-y-1.5">
                  <span className="text-[11px] font-bold text-amber-700 uppercase tracking-wider block">
                    Missing Information
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    {missingList.map((item, idx) => (
                      <span
                        key={idx}
                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-amber-50 text-amber-700 border border-amber-200/70"
                      >
                        <Info className="w-3 h-3 text-amber-600" />
                        {item}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Action Button */}
            <div className="pt-3">
              <Button
                variant="primary"
                className="w-full sm:w-auto"
                onClick={() => setIsEditModalOpen(true)}
              >
                Complete Profile
                <ArrowRight className="w-4 h-4 ml-2" />
              </Button>
            </div>
          </div>
        </Card>
      </div>

      {/* Edit / Complete Profile Modal */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title="Complete Practice Profile"
        subtitle="Provide key practice details to maximize visibility on the customer marketplace."
        maxWidth="2xl"
        footer={
          <div className="flex items-center justify-end gap-3 w-full">
            <Button
              variant="outline"
              onClick={() => setIsEditModalOpen(false)}
              disabled={isSaving}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={(e) => handleSaveProfile(e, false)}
              isLoading={isSaving}
            >
              Save Profile
            </Button>
          </div>
        }
      >
        <form onSubmit={(e) => handleSaveProfile(e, false)} className="space-y-4 text-xs">
          {/* Practice Name */}
          <div>
            <label className="block font-bold text-slate-700 mb-1">
              Practice / Firm Display Name <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              required
              value={formData.displayName}
              onChange={(e) => setFormData({ ...formData, displayName: e.target.value })}
              placeholder="e.g. ABC Tax Consultants"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
            />
          </div>

          {/* Public Vanity Slug */}
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="block font-bold text-slate-700">
                Public Slug <span className="text-slate-400 font-normal">(URL identifier)</span>
              </label>
              <button
                type="button"
                onClick={handleGenerateSlug}
                disabled={isGeneratingSlug}
                className="text-indigo-600 hover:text-indigo-800 font-bold inline-flex items-center gap-1 text-[11px]"
              >
                <RefreshCw className={clsx('w-3 h-3', isGeneratingSlug && 'animate-spin')} />
                Auto-generate Slug
              </button>
            </div>
            <div className="flex items-center rounded-lg border border-slate-300 bg-slate-50 overflow-hidden">
              <span className="px-3 py-2 text-slate-500 bg-slate-100 border-r border-slate-300 font-mono text-[11px]">
                taxoryn.com/marketplace/
              </span>
              <input
                type="text"
                value={formData.slug}
                onChange={(e) => setFormData({ ...formData, slug: e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, '-') })}
                placeholder="abc-tax-consultants"
                className="w-full px-3 py-2 bg-white focus:outline-hidden font-mono text-xs"
              />
            </div>
          </div>

          {/* Description */}
          <div>
            <label className="block font-bold text-slate-700 mb-1">
              Practice Description / Value Proposition
            </label>
            <textarea
              rows={3}
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="Describe your firm's core focus, expertise in GST, ITR, corporate tax, etc."
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
            />
          </div>

          {/* Contact Details */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block font-bold text-slate-700 mb-1">
                Contact Phone <span className="text-rose-500">*</span>
              </label>
              <input
                type="tel"
                required
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                placeholder="+91 98200 11223"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">
                Contact Email <span className="text-rose-500">*</span>
              </label>
              <input
                type="email"
                required
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                placeholder="contact@abctax.com"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
              />
            </div>
          </div>

          {/* Website and Experience */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block font-bold text-slate-700 mb-1">Website URL</label>
              <input
                type="url"
                value={formData.website}
                onChange={(e) => setFormData({ ...formData, website: e.target.value })}
                placeholder="https://abctax.com"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">Experience (Years)</label>
              <input
                type="number"
                min={0}
                max={100}
                value={formData.experienceYears}
                onChange={(e) => setFormData({ ...formData, experienceYears: parseInt(e.target.value, 10) || 0 })}
                placeholder="10"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
              />
            </div>
          </div>

          {/* Location */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block font-bold text-slate-700 mb-1">
                City <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                required
                value={formData.city}
                onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                placeholder="Bangalore"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">
                State <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                required
                value={formData.state}
                onChange={(e) => setFormData({ ...formData, state: e.target.value })}
                placeholder="Karnataka"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">Pincode</label>
              <input
                type="text"
                value={formData.pincode}
                onChange={(e) => setFormData({ ...formData, pincode: e.target.value })}
                placeholder="560001"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
              />
            </div>
          </div>
        </form>
      </Modal>
    </div>
  );
};
