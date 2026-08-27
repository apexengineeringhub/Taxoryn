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
  Plus,
  Compass,
  Trash2,
  Navigation,
  CheckSquare,
  Square,
  BookmarkCheck,
  Layers,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { useBranding } from '../context/BrandingContext';
import { useAuth } from '../context/AuthContext';
import { marketplacePracticeApi, marketplacePublicApi } from '../api/endpoints';
import {
  MarketplaceProfile,
  ProfileCompleteness,
  VisibilityStatus,
  VerificationStatus,
  PracticeLocation,
  CreatePracticeLocationRequest,
  UpdatePracticeLocationRequest,
  PublicTaxServiceCategory,
  PracticeService,
} from '../types';
import clsx from 'clsx';

export const PracticeMarketplaceProfilePage: React.FC = () => {
  const { currentTheme } = useBranding();
  const { practiceName } = useAuth();
  const isLight = currentTheme.mode === 'light';

  const [profile, setProfile] = useState<MarketplaceProfile | null>(null);
  const [completeness, setCompleteness] = useState<ProfileCompleteness | null>(null);
  const [locations, setLocations] = useState<PracticeLocation[]>([]);
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
    workingHours: '',
    seoTitle: '',
    metaDescription: '',
    canonicalUrl: '',
  });

  // Location Modal State
  const [isLocationModalOpen, setIsLocationModalOpen] = useState<boolean>(false);
  const [isEditingLocation, setIsEditingLocation] = useState<boolean>(false);
  const [editingLocationId, setEditingLocationId] = useState<string | null>(null);
  const [isSavingLocation, setIsSavingLocation] = useState<boolean>(false);
  const [locationForm, setLocationForm] = useState({
    locationName: '',
    addressLine1: '',
    addressLine2: '',
    landmark: '',
    city: '',
    district: '',
    state: '',
    stateCode: '',
    country: 'India',
    countryCode: 'IN',
    pincode: '',
    latitude: '' as string | number,
    longitude: '' as string | number,
    isPrimary: false,
  });

  // Controlled Tax Services State
  const [masterCategories, setMasterCategories] = useState<PublicTaxServiceCategory[]>([]);
  const [selectedTaxServiceIds, setSelectedTaxServiceIds] = useState<Set<string>>(new Set());
  const [isSavingServices, setIsSavingServices] = useState<boolean>(false);
  const [servicesSuccessBanner, setServicesSuccessBanner] = useState<string | null>(null);
  const [servicesErrorBanner, setServicesErrorBanner] = useState<string | null>(null);

  const loadData = async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const [profileData, completenessData, locationsData, categoriesData, myServicesData] = await Promise.all([
        marketplacePracticeApi.getMyProfile(),
        marketplacePracticeApi.getProfileCompleteness().catch(() => null),
        marketplacePracticeApi.getLocations().catch(() => []),
        marketplacePublicApi.getTaxServiceCategories().catch(() => []),
        marketplacePracticeApi.getControlledTaxServices().catch(() => []),
      ]);

      setProfile(profileData);
      setLocations(locationsData || []);
      setMasterCategories(categoriesData || []);
      setSelectedTaxServiceIds(new Set((myServicesData || []).map((s: PracticeService) => s.taxServiceId)));
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
        workingHours: profileData.workingHours || '',
        seoTitle: profileData.seoTitle || '',
        metaDescription: profileData.metaDescription || '',
        canonicalUrl: profileData.canonicalUrl || '',
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
      console.error('Failed to generate slug:', err);
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
        workingHours: formData.workingHours,
        seoTitle: formData.seoTitle,
        metaDescription: formData.metaDescription,
        canonicalUrl: formData.canonicalUrl,
        visibilityStatus: andPublish ? 'PUBLIC' : profile?.visibilityStatus || 'PRIVATE',
      };

      const updated = await marketplacePracticeApi.updateMyProfile(payload);
      setProfile(updated);
      if (updated.profileCompleteness || updated.completeness) {
        setCompleteness(updated.profileCompleteness || updated.completeness || null);
      }
      setIsEditModalOpen(false);
      setSuccessBanner('Marketplace profile saved successfully!');
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to save profile changes.';
      setErrorMessage(msg);
    } finally {
      setIsSaving(false);
    }
  };

  // Open Add Location Modal
  const handleOpenAddLocation = () => {
    setIsEditingLocation(false);
    setEditingLocationId(null);
    setLocationForm({
      locationName: locations.length === 0 ? 'Main Office' : '',
      addressLine1: '',
      addressLine2: '',
      landmark: '',
      city: profile?.city || '',
      district: '',
      state: profile?.state || '',
      stateCode: '',
      country: 'India',
      countryCode: 'IN',
      pincode: '',
      latitude: '',
      longitude: '',
      isPrimary: locations.length === 0,
    });
    setIsLocationModalOpen(true);
  };

  // Open Edit Location Modal
  const handleOpenEditLocation = (loc: PracticeLocation) => {
    setIsEditingLocation(true);
    setEditingLocationId(loc.id);
    setLocationForm({
      locationName: loc.locationName,
      addressLine1: loc.addressLine1,
      addressLine2: loc.addressLine2 || '',
      landmark: loc.landmark || '',
      city: loc.city,
      district: loc.district || '',
      state: loc.state,
      stateCode: loc.stateCode || '',
      country: loc.country || 'India',
      countryCode: loc.countryCode || 'IN',
      pincode: loc.pincode,
      latitude: loc.latitude !== undefined && loc.latitude !== null ? loc.latitude : '',
      longitude: loc.longitude !== undefined && loc.longitude !== null ? loc.longitude : '',
      isPrimary: loc.isPrimary,
    });
    setIsLocationModalOpen(true);
  };

  // Save Location (Create or Update)
  const handleSaveLocation = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSavingLocation(true);
    setErrorMessage(null);
    setSuccessBanner(null);

    try {
      const latNum = locationForm.latitude !== '' ? Number(locationForm.latitude) : undefined;
      const lngNum = locationForm.longitude !== '' ? Number(locationForm.longitude) : undefined;

      if (isEditingLocation && editingLocationId) {
        const payload: UpdatePracticeLocationRequest = {
          locationName: locationForm.locationName,
          addressLine1: locationForm.addressLine1,
          addressLine2: locationForm.addressLine2 || undefined,
          landmark: locationForm.landmark || undefined,
          city: locationForm.city,
          district: locationForm.district || undefined,
          state: locationForm.state,
          stateCode: locationForm.stateCode || undefined,
          country: locationForm.country,
          countryCode: locationForm.countryCode,
          pincode: locationForm.pincode,
          latitude: latNum,
          longitude: lngNum,
          isPrimary: locationForm.isPrimary,
        };
        await marketplacePracticeApi.updateLocation(editingLocationId, payload);
        setSuccessBanner(`Location "${locationForm.locationName}" updated successfully!`);
      } else {
        const payload: CreatePracticeLocationRequest = {
          locationName: locationForm.locationName,
          addressLine1: locationForm.addressLine1,
          addressLine2: locationForm.addressLine2 || undefined,
          landmark: locationForm.landmark || undefined,
          city: locationForm.city,
          district: locationForm.district || undefined,
          state: locationForm.state,
          stateCode: locationForm.stateCode || undefined,
          country: locationForm.country,
          countryCode: locationForm.countryCode,
          pincode: locationForm.pincode,
          latitude: latNum,
          longitude: lngNum,
          isPrimary: locationForm.isPrimary,
        };
        await marketplacePracticeApi.createLocation(payload);
        setSuccessBanner(`New branch location "${locationForm.locationName}" added!`);
      }

      setIsLocationModalOpen(false);
      // Reload locations and completeness
      const [updatedLocations, comp] = await Promise.all([
        marketplacePracticeApi.getLocations(),
        marketplacePracticeApi.getProfileCompleteness().catch(() => null),
      ]);
      setLocations(updatedLocations);
      if (comp) setCompleteness(comp);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to save location.';
      setErrorMessage(msg);
    } finally {
      setIsSavingLocation(false);
    }
  };

  // Set Primary Location
  const handleSetPrimary = async (locationId: string) => {
    try {
      setErrorMessage(null);
      await marketplacePracticeApi.setPrimaryLocation(locationId);
      const updatedLocations = await marketplacePracticeApi.getLocations();
      setLocations(updatedLocations);
      setSuccessBanner('Primary practice location updated successfully!');
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || 'Failed to set primary location.');
    }
  };

  // Toggle Location Active / Deactive
  const handleToggleLocationActive = async (loc: PracticeLocation) => {
    try {
      setErrorMessage(null);
      if (loc.isActive) {
        await marketplacePracticeApi.deactivateLocation(loc.id);
        setSuccessBanner(`Branch location "${loc.locationName}" deactivated.`);
      } else {
        await marketplacePracticeApi.activateLocation(loc.id);
        setSuccessBanner(`Branch location "${loc.locationName}" activated.`);
      }
      const [updatedLocations, comp] = await Promise.all([
        marketplacePracticeApi.getLocations(),
        marketplacePracticeApi.getProfileCompleteness().catch(() => null),
      ]);
      setLocations(updatedLocations);
      if (comp) setCompleteness(comp);
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || 'Failed to update location status.');
    }
  };

  // Copy public slug link
  const copyPublicUrl = () => {
    const slugVal = profile?.publicSlug || profile?.slug;
    if (!slugVal) return;
    const url = `${window.location.origin}/marketplace/practice/${slugVal}`;
    navigator.clipboard.writeText(url);
    setCopiedSlug(true);
    setTimeout(() => setCopiedSlug(false), 2500);
  };

  const handleToggleTaxService = (serviceId: string) => {
    setSelectedTaxServiceIds((prev) => {
      const next = new Set(prev);
      if (next.has(serviceId)) {
        next.delete(serviceId);
      } else {
        next.add(serviceId);
      }
      return next;
    });
  };

  const handleSelectAllCategory = (cat: PublicTaxServiceCategory) => {
    setSelectedTaxServiceIds((prev) => {
      const next = new Set(prev);
      cat.services.forEach((s) => next.add(s.id));
      return next;
    });
  };

  const handleDeselectAllCategory = (cat: PublicTaxServiceCategory) => {
    setSelectedTaxServiceIds((prev) => {
      const next = new Set(prev);
      cat.services.forEach((s) => next.delete(s.id));
      return next;
    });
  };

  const handleSaveControlledServices = async () => {
    setIsSavingServices(true);
    setServicesSuccessBanner(null);
    setServicesErrorBanner(null);
    try {
      const updated = await marketplacePracticeApi.updateControlledTaxServices(Array.from(selectedTaxServiceIds));
      setSelectedTaxServiceIds(new Set(updated.map((s: PracticeService) => s.taxServiceId)));
      setServicesSuccessBanner(`Successfully updated practice offerings to ${updated.length} active tax services!`);
      setTimeout(() => setServicesSuccessBanner(null), 4000);
    } catch (err: any) {
      setServicesErrorBanner(err.response?.data?.message || 'Failed to save practice services.');
    } finally {
      setIsSavingServices(false);
    }
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
            Let customers discover your practice based on multi-branch locations and verified tax services.
          </p>

          <div className="pt-2">
            <a
              href={`/practice/${profile?.publicSlug || profile?.slug || 'my-practice'}`}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-white text-slate-900 hover:bg-slate-100 font-bold text-xs shadow-md transition-all"
            >
              <Eye className="w-3.5 h-3.5 text-indigo-600" />
              Preview Public Profile
              <ArrowRight className="w-3 h-3 text-slate-400" />
            </a>
          </div>
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
                  ? 'Your practice is currently live and discoverable by clients across all configured branch locations.'
                  : 'Your practice is currently hidden. Turn visibility ON when your profile has at least one active location.'}
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
                  {locations.length > 0 ? (
                    <span>{locations.length} {locations.length === 1 ? 'Location' : 'Locations'}</span>
                  ) : (
                    <span>{profile?.city ? `${profile.city}${profile.state ? `, ${profile.state}` : ''}` : 'No location added'}</span>
                  )}
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

          {/* Profile Completeness Progress Widget */}
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
          </div>
        </Card>

        {/* 3. Practice Locations Management Card */}
        <Card className="p-6 sm:p-7 border border-slate-200 shadow-xs space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-4">
            <div>
              <div className="flex items-center gap-2">
                <Building2 className="w-5 h-5 text-indigo-600" />
                <h3 className="text-base font-bold text-slate-900">Practice Locations</h3>
                <span className="px-2 py-0.5 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-100">
                  {locations.length} {locations.length === 1 ? 'Office' : 'Offices'}
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-1">
                Manage all physical offices, branches, and consultation hubs where your practice serves clients.
              </p>
            </div>

            <Button
              variant="primary"
              size="sm"
              onClick={handleOpenAddLocation}
              disabled={isLoading}
            >
              <Plus className="w-4 h-4 mr-1.5" />
              Add Location
            </Button>
          </div>

          {/* Locations List */}
          {locations.length === 0 ? (
            <div className="p-8 text-center border-2 border-dashed border-slate-200 rounded-xl bg-slate-50/50 space-y-3">
              <MapPin className="w-8 h-8 text-slate-400 mx-auto" />
              <div className="space-y-1">
                <h4 className="text-sm font-bold text-slate-700">No practice locations added yet</h4>
                <p className="text-xs text-slate-500 max-w-md mx-auto">
                  Add your primary office location to satisfy marketplace publishing requirements and make your practice discoverable.
                </p>
              </div>
              <Button size="sm" variant="outline" onClick={handleOpenAddLocation}>
                <Plus className="w-3.5 h-3.5 mr-1" />
                Add Head Office Location
              </Button>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-3.5">
              {locations.map((loc) => (
                <div
                  key={loc.id}
                  className={clsx(
                    'p-4 rounded-xl border transition-all duration-200 flex flex-col md:flex-row md:items-center justify-between gap-4',
                    loc.isPrimary
                      ? 'bg-indigo-50/30 border-indigo-200'
                      : loc.isActive
                      ? 'bg-white border-slate-200 hover:border-slate-300'
                      : 'bg-slate-50/60 border-slate-200 opacity-60'
                  )}
                >
                  <div className="space-y-1.5 flex-1 min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-bold text-sm text-slate-900 truncate">
                        {loc.locationName}
                      </span>

                      {loc.isPrimary && (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-indigo-100 text-indigo-800 border border-indigo-200">
                          Primary Headquarter
                        </span>
                      )}

                      <span
                        className={clsx(
                          'px-2 py-0.5 rounded-full text-[10px] font-semibold',
                          loc.isActive
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-100'
                            : 'bg-slate-100 text-slate-500 border border-slate-200'
                        )}
                      >
                        {loc.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </div>

                    <div className="text-xs text-slate-600 space-y-0.5">
                      <p>
                        {loc.addressLine1}
                        {loc.addressLine2 && `, ${loc.addressLine2}`}
                        {loc.landmark && ` (Near ${loc.landmark})`}
                      </p>
                      <p className="font-medium text-slate-700">
                        {loc.city}, {loc.state} - {loc.pincode}, {loc.country}
                      </p>
                    </div>

                    {loc.latitude && loc.longitude && (
                      <div className="inline-flex items-center gap-1 text-[11px] text-slate-500 font-mono">
                        <Compass className="w-3 h-3 text-indigo-500" />
                        <span>Lat: {loc.latitude}, Long: {loc.longitude}</span>
                      </div>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2 shrink-0 pt-2 md:pt-0 border-t md:border-t-0 border-slate-100">
                    {!loc.isPrimary && loc.isActive && (
                      <Button
                        variant="outline"
                        size="sm"
                        className="text-xs font-semibold"
                        onClick={() => handleSetPrimary(loc.id)}
                      >
                        Make Primary
                      </Button>
                    )}

                    <Button
                      variant="outline"
                      size="sm"
                      className="text-xs"
                      onClick={() => handleOpenEditLocation(loc)}
                    >
                      Edit
                    </Button>

                    <Button
                      variant={loc.isActive ? 'ghost' : 'outline'}
                      size="sm"
                      className={clsx('text-xs', loc.isActive ? 'text-rose-600 hover:text-rose-700' : 'text-slate-700')}
                      onClick={() => handleToggleLocationActive(loc)}
                    >
                      {loc.isActive ? 'Deactivate' : 'Activate'}
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>

        {/* 4. Controlled Tax Services Offered Management Card */}
        <Card className="p-6 sm:p-7 border border-slate-200 shadow-xs space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-4">
            <div>
              <div className="flex items-center gap-2">
                <BookmarkCheck className="w-5 h-5 text-indigo-600" />
                <h3 className="text-base font-bold text-slate-900">Services Offered</h3>
                <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-100">
                  {selectedTaxServiceIds.size} Selected
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-1">
                Select from standardized Indian tax & compliance services. These controlled services power marketplace discovery, alias resolution, and geo-search matching.
              </p>
            </div>

            <Button
              variant="primary"
              size="sm"
              onClick={handleSaveControlledServices}
              isLoading={isSavingServices}
              disabled={isLoading}
            >
              Save Services
            </Button>
          </div>

          {/* Feedback Messages */}
          {servicesSuccessBanner && (
            <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-800 flex items-center gap-2 font-medium">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>{servicesSuccessBanner}</span>
            </div>
          )}

          {servicesErrorBanner && (
            <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800 flex items-center gap-2 font-medium">
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
              <span>{servicesErrorBanner}</span>
            </div>
          )}

          {/* Categories & Services Grid */}
          <div className="space-y-6">
            {masterCategories.map((cat) => {
              const categorySelectedCount = cat.services.filter((s) => selectedTaxServiceIds.has(s.id)).length;
              const allSelected = cat.services.length > 0 && categorySelectedCount === cat.services.length;

              return (
                <div key={cat.id} className="border border-slate-200/80 rounded-xl p-4 sm:p-5 bg-white space-y-4 shadow-2xs">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 pb-3">
                    <div className="space-y-0.5">
                      <div className="flex items-center gap-2">
                        <Layers className="w-4 h-4 text-indigo-600" />
                        <h4 className="text-sm font-bold text-slate-900">{cat.name}</h4>
                        <span className="text-[11px] font-semibold text-slate-500">
                          ({categorySelectedCount}/{cat.services.length} offered)
                        </span>
                      </div>
                      {cat.description && (
                        <p className="text-[11px] text-slate-500">{cat.description}</p>
                      )}
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => handleSelectAllCategory(cat)}
                        className="text-[11px] font-bold text-indigo-600 hover:text-indigo-800 transition"
                      >
                        Select All
                      </button>
                      <span className="text-slate-300">•</span>
                      <button
                        type="button"
                        onClick={() => handleDeselectAllCategory(cat)}
                        className="text-[11px] font-semibold text-slate-500 hover:text-slate-700 transition"
                      >
                        Clear
                      </button>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {cat.services.map((svc) => {
                      const isChecked = selectedTaxServiceIds.has(svc.id);
                      return (
                        <div
                          key={svc.id}
                          onClick={() => handleToggleTaxService(svc.id)}
                          className={clsx(
                            'p-3.5 rounded-xl border transition-all cursor-pointer select-none flex items-start gap-3',
                            isChecked
                              ? 'bg-indigo-50/40 border-indigo-300 ring-1 ring-indigo-500/20'
                              : 'bg-slate-50/60 border-slate-200 hover:border-slate-300 hover:bg-slate-50'
                          )}
                        >
                          <div className="mt-0.5 text-indigo-600">
                            {isChecked ? (
                              <CheckSquare className="w-4 h-4 fill-indigo-600 text-white" />
                            ) : (
                              <Square className="w-4 h-4 text-slate-400" />
                            )}
                          </div>
                          <div className="space-y-1 flex-1 min-w-0">
                            <div className="flex items-center justify-between">
                              <span className={clsx('text-xs font-bold', isChecked ? 'text-indigo-950' : 'text-slate-800')}>
                                {svc.name}
                              </span>
                              <span className="text-[9px] font-mono font-semibold text-slate-400 uppercase">
                                {svc.code}
                              </span>
                            </div>
                            {svc.description && (
                              <p className="text-[11px] text-slate-500 line-clamp-2 leading-relaxed">
                                {svc.description}
                              </p>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>

          <div className="flex items-center justify-between pt-2 border-t border-slate-100">
            <span className="text-xs text-slate-500">
              Selected <strong>{selectedTaxServiceIds.size}</strong> services across {masterCategories.length} categories
            </span>
            <Button
              variant="primary"
              size="sm"
              onClick={handleSaveControlledServices}
              isLoading={isSavingServices}
              disabled={isLoading}
            >
              Save Services
            </Button>
          </div>
        </Card>
      </div>

      {/* Edit Profile Modal */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title="Edit Practice Profile"
        subtitle="Update fundamental practice metadata for the customer marketplace."
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
          {/* Working Hours */}
          <div>
            <label className="block font-bold text-slate-700 mb-1">
              Operating / Working Hours
            </label>
            <input
              type="text"
              value={formData.workingHours}
              onChange={(e) => setFormData({ ...formData, workingHours: e.target.value })}
              placeholder="e.g. Mon - Fri: 9:30 AM - 6:30 PM, Sat: 10:00 AM - 2:00 PM"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
            />
          </div>

          {/* Search Engine Optimization (SEO) Settings */}
          <div className="pt-3 border-t border-slate-200 space-y-3">
            <h4 className="font-bold text-slate-800 text-xs flex items-center gap-1.5">
              <Globe className="w-3.5 h-3.5 text-indigo-600" />
              Google Discovery & SEO Settings
            </h4>

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block font-medium text-slate-700">Custom SEO Title Tag</label>
                <span className="text-[10px] text-slate-400 font-mono">{formData.seoTitle?.length || 0} / 60</span>
              </div>
              <input
                type="text"
                value={formData.seoTitle}
                onChange={(e) => setFormData({ ...formData, seoTitle: e.target.value })}
                placeholder="e.g. ABC Tax Consultants - CA Firm in Bangalore"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
              />
            </div>

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block font-medium text-slate-700">Custom Meta Description</label>
                <span className="text-[10px] text-slate-400 font-mono">{formData.metaDescription?.length || 0} / 160</span>
              </div>
              <textarea
                rows={2}
                value={formData.metaDescription}
                onChange={(e) => setFormData({ ...formData, metaDescription: e.target.value })}
                placeholder="e.g. Leading Chartered Accountants providing verified GST returns, ITR filings, and corporate tax compliance in Bangalore."
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
              />
            </div>

            <div>
              <label className="block font-medium text-slate-700 mb-1">Canonical URL Override (Optional)</label>
              <input
                type="url"
                value={formData.canonicalUrl}
                onChange={(e) => setFormData({ ...formData, canonicalUrl: e.target.value })}
                placeholder="https://taxoryn.com/practice/abc-tax-consultants"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
              />
            </div>
          </div>
        </form>
      </Modal>

      {/* Add / Edit Location Modal */}
      <Modal
        isOpen={isLocationModalOpen}
        onClose={() => setIsLocationModalOpen(false)}
        title={isEditingLocation ? 'Edit Practice Location' : 'Add Practice Location'}
        subtitle="Enter structured address details for your branch or consultation office."
        maxWidth="2xl"
        footer={
          <div className="flex items-center justify-end gap-3 w-full">
            <Button
              variant="outline"
              onClick={() => setIsLocationModalOpen(false)}
              disabled={isSavingLocation}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleSaveLocation}
              isLoading={isSavingLocation}
            >
              {isEditingLocation ? 'Save Changes' : 'Add Location'}
            </Button>
          </div>
        }
      >
        <form onSubmit={handleSaveLocation} className="space-y-4 text-xs">
          {/* Location / Branch Name */}
          <div>
            <label className="block font-bold text-slate-700 mb-1">
              Branch / Location Name <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              required
              value={locationForm.locationName}
              onChange={(e) => setLocationForm({ ...locationForm, locationName: e.target.value })}
              placeholder="e.g. Head Office, Whitefield Branch, Connaught Place Office"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
            />
          </div>

          {/* Address Line 1 */}
          <div>
            <label className="block font-bold text-slate-700 mb-1">
              Address Line 1 <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              required
              value={locationForm.addressLine1}
              onChange={(e) => setLocationForm({ ...locationForm, addressLine1: e.target.value })}
              placeholder="e.g. Suite 402, Prestige Meridian II, MG Road"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
            />
          </div>

          {/* Address Line 2 & Landmark */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block font-bold text-slate-700 mb-1">Address Line 2</label>
              <input
                type="text"
                value={locationForm.addressLine2}
                onChange={(e) => setLocationForm({ ...locationForm, addressLine2: e.target.value })}
                placeholder="Floor, Building, Wing"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">Landmark</label>
              <input
                type="text"
                value={locationForm.landmark}
                onChange={(e) => setLocationForm({ ...locationForm, landmark: e.target.value })}
                placeholder="e.g. Near Trinity Metro Station"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
              />
            </div>
          </div>

          {/* City, District, State, Pincode */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block font-bold text-slate-700 mb-1">
                City <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                required
                value={locationForm.city}
                onChange={(e) => setLocationForm({ ...locationForm, city: e.target.value })}
                placeholder="Bengaluru"
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
                value={locationForm.state}
                onChange={(e) => setLocationForm({ ...locationForm, state: e.target.value })}
                placeholder="Karnataka"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">
                PIN Code <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                required
                maxLength={6}
                value={locationForm.pincode}
                onChange={(e) => setLocationForm({ ...locationForm, pincode: e.target.value.replace(/\D/g, '') })}
                placeholder="560001"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs font-mono"
              />
            </div>
          </div>

          {/* District and Country */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block font-bold text-slate-700 mb-1">District</label>
              <input
                type="text"
                value={locationForm.district}
                onChange={(e) => setLocationForm({ ...locationForm, district: e.target.value })}
                placeholder="e.g. Bengaluru Urban"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">Country</label>
              <input
                type="text"
                value={locationForm.country}
                onChange={(e) => setLocationForm({ ...locationForm, country: e.target.value })}
                placeholder="India"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-hidden text-xs"
              />
            </div>
          </div>

          {/* Geographic Coordinates */}
          <div className="p-3 rounded-lg bg-slate-50 border border-slate-200 space-y-2">
            <div className="flex items-center gap-1.5 text-[11px] font-bold text-slate-700">
              <Compass className="w-3.5 h-3.5 text-indigo-600" />
              <span>Geographic Coordinates (Optional for Map & Distance Search)</span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block font-medium text-slate-600 mb-0.5 text-[11px]">Latitude (-90 to 90)</label>
                <input
                  type="number"
                  step="any"
                  min={-90}
                  max={90}
                  value={locationForm.latitude}
                  onChange={(e) => setLocationForm({ ...locationForm, latitude: e.target.value })}
                  placeholder="e.g. 12.971600"
                  className="w-full px-2.5 py-1.5 border border-slate-300 rounded bg-white focus:outline-hidden text-xs font-mono"
                />
              </div>

              <div>
                <label className="block font-medium text-slate-600 mb-0.5 text-[11px]">Longitude (-180 to 180)</label>
                <input
                  type="number"
                  step="any"
                  min={-180}
                  max={180}
                  value={locationForm.longitude}
                  onChange={(e) => setLocationForm({ ...locationForm, longitude: e.target.value })}
                  placeholder="e.g. 77.594600"
                  className="w-full px-2.5 py-1.5 border border-slate-300 rounded bg-white focus:outline-hidden text-xs font-mono"
                />
              </div>
            </div>
          </div>

          {/* Primary Location Checkbox */}
          <div className="flex items-center gap-2 pt-1">
            <input
              type="checkbox"
              id="isPrimaryLoc"
              checked={locationForm.isPrimary}
              onChange={(e) => setLocationForm({ ...locationForm, isPrimary: e.target.checked })}
              className="rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 h-4 w-4"
            />
            <label htmlFor="isPrimaryLoc" className="text-xs font-bold text-slate-800 cursor-pointer">
              Set as primary headquarter / main location
            </label>
          </div>
        </form>
      </Modal>
    </div>
  );
};
