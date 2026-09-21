import React, { useState, useRef, useEffect } from 'react';
import {
  Palette,
  Image,
  User,
  Upload,
  Check,
  Trash2,
  Sparkles,
  Camera,
  Sun,
  Moon,
  Globe,
  Link as LinkIcon,
  Building2,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Users,
  TrendingUp,
  Briefcase,
} from 'lucide-react';
import { formatPracticeDisplayUrl, buildPracticePathUrl } from '../utils/tenantUrl';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { TaxorynLogo } from '../components/common/TaxorynLogo';
import { useBranding, THEME_TEMPLATES } from '../context/BrandingContext';
import { useAuth } from '../context/AuthContext';
import { organizationApi } from '../api/endpoints';
import { OrganizationType } from '../types';
import { hasPermission } from '../utils/permissionUtils';
import clsx from 'clsx';

export const ORGANIZATION_TYPE_LABELS: Record<OrganizationType, string> = {
  UNKNOWN: 'Not Configured',
  SOLO_PRACTITIONER: 'Solo Practitioner',
  SMALL_TAX_FIRM: 'Small Tax Firm',
  GROWING_PRACTICE: 'Growing Practice',
  BUSINESS: 'Business',
};

export const PracticeBrandingPage: React.FC = () => {
  const {
    currentTheme,
    setTheme,
    practiceLogo,
    setPracticeLogo,
    setEmployeeAvatar,
    getEmployeeAvatar,
    activeTabFilter,
    setActiveTabFilter,
  } = useBranding();

  const { user, organization, practiceName, practiceInitials, refreshOrganization } = useAuth();
  const logoInputRef = useRef<HTMLInputElement>(null);
  const avatarInputRef = useRef<HTMLInputElement>(null);

  const currentUserAvatar = getEmployeeAvatar(user?.email || user?.id);

  // Organization Type Management State
  const canManageOrg = hasPermission(
    user,
    ['ORGANIZATION_UPDATE', 'ORG_WRITE'],
    ['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER']
  );

  const [selectedOrgType, setSelectedOrgType] = useState<OrganizationType>(
    organization?.organizationType || 'UNKNOWN'
  );
  const [isSavingOrgType, setIsSavingOrgType] = useState(false);
  const [orgTypeSuccessMessage, setOrgTypeSuccessMessage] = useState<string | null>(null);
  const [orgTypeError, setOrgTypeError] = useState<string | null>(null);

  useEffect(() => {
    if (organization?.organizationType) {
      setSelectedOrgType(organization.organizationType);
    }
  }, [organization?.organizationType]);

  const handleSaveOrgType = async () => {
    if (!canManageOrg) return;
    setIsSavingOrgType(true);
    setOrgTypeError(null);
    setOrgTypeSuccessMessage(null);

    try {
      await organizationApi.updateCurrent({
        name: organization?.name || practiceName,
        organizationType: selectedOrgType,
      });
      await refreshOrganization();
      setOrgTypeSuccessMessage(`Organization type successfully updated to ${ORGANIZATION_TYPE_LABELS[selectedOrgType]}.`);
      setTimeout(() => setOrgTypeSuccessMessage(null), 4000);
    } catch (err: any) {
      const resp = err?.response?.data;
      setOrgTypeError(resp?.message || 'Failed to update organization type. Please try again.');
    } finally {
      setIsSavingOrgType(false);
    }
  };

  const handleCancelOrgType = () => {
    setSelectedOrgType(organization?.organizationType || 'UNKNOWN');
    setOrgTypeError(null);
  };

  const handleLogoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 2 * 1024 * 1024) {
        alert('Logo file size must be under 2MB');
        return;
      }
      const reader = new FileReader();
      reader.onloadend = () => {
        setPracticeLogo(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleAvatarUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 2 * 1024 * 1024) {
        alert('Photo size must be under 2MB');
        return;
      }
      const reader = new FileReader();
      reader.onloadend = () => {
        if (user?.email) {
          setEmployeeAvatar(user.email, reader.result as string);
        }
        if (user?.id) {
          setEmployeeAvatar(user.id, reader.result as string);
        }
      };
      reader.readAsDataURL(file);
    }
  };

  const filteredThemes = THEME_TEMPLATES.filter((tpl) => {
    if (activeTabFilter === 'LIGHT') return tpl.mode === 'light';
    if (activeTabFilter === 'DARK') return tpl.mode === 'dark';
    return true;
  });

  return (
    <div className="space-y-8 max-w-6xl mx-auto animate-fade-in">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">Practice Branding & Theme Studio</h1>
          <p className="text-xs text-slate-500 mt-1">
            Choose clean light or dark executive themes, upload official practice logos, and configure team member profile pictures.
          </p>
        </div>
        <div className="inline-flex items-center gap-2 bg-white border border-slate-200 rounded-lg px-3 py-1.5 shadow-2xs text-xs font-semibold text-slate-700">
          <Sparkles className="w-3.5 h-3.5" style={{ color: currentTheme.primaryColor }} />
          <span>Active: {currentTheme.name}</span>
        </div>
      </div>

      {/* Top 3 Columns: Logo, Profile Picture, Status */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 1. Practice Logo Studio */}
        <Card
          title="Practice Official Logo"
          subtitle="Displayed on Sidebar, Client Invoices, and Reports"
          className="lg:col-span-1"
        >
          <div className="space-y-4">
            <div className="p-6 rounded-2xl bg-slate-900 flex flex-col items-center justify-center min-h-[160px] border border-slate-800 relative group overflow-hidden">
              {practiceLogo ? (
                <div className="flex flex-col items-center gap-3">
                  <img
                    src={practiceLogo}
                    alt="Practice Logo"
                    className="max-h-16 max-w-[180px] object-contain drop-shadow-md rounded"
                  />
                  <span className="text-[10px] font-semibold text-slate-400">Current Sidebar Logo</span>
                </div>
              ) : (
                <div className="flex flex-col items-center gap-2 text-center">
                  <div
                    className="w-12 h-12 rounded-xl flex items-center justify-center text-white font-black text-lg shadow-lg"
                    style={{ backgroundColor: currentTheme.primaryColor }}
                  >
                    {practiceInitials}
                  </div>
                  <span className="text-xs font-bold text-slate-200 mt-1">{practiceName}</span>
                  <span className="text-[10px] text-slate-500">Using dynamic monogram fallback</span>
                </div>
              )}
            </div>

            <div className="flex items-center gap-2">
              <input
                type="file"
                ref={logoInputRef}
                accept="image/png, image/jpeg, image/svg+xml, image/webp"
                onChange={handleLogoUpload}
                className="hidden"
              />
              <Button
                variant="primary"
                size="sm"
                className="flex-1"
                leftIcon={<Upload className="w-3.5 h-3.5" />}
                onClick={() => logoInputRef.current?.click()}
              >
                {practiceLogo ? 'Replace Logo' : 'Upload Firm Logo'}
              </Button>
              {practiceLogo && (
                <Button
                  variant="outline"
                  size="sm"
                  title="Remove Logo"
                  onClick={() => setPracticeLogo(null)}
                  leftIcon={<Trash2 className="w-3.5 h-3.5 text-rose-600" />}
                >
                  Reset
                </Button>
              )}
            </div>
            <p className="text-[11px] text-slate-400 text-center">
              Recommended: PNG or SVG with transparent background (Max 2MB)
            </p>
          </div>
        </Card>

        {/* 2. Employee Profile Picture */}
        <Card
          title="My Account Profile Picture"
          subtitle="Shown in user avatar pill and CA signature stamps"
          className="lg:col-span-1"
        >
          <div className="space-y-4">
            <div className="p-6 rounded-2xl bg-slate-50 border border-slate-200/80 flex flex-col items-center justify-center min-h-[160px]">
              <div className="relative group">
                {currentUserAvatar ? (
                  <img
                    src={currentUserAvatar}
                    alt={user?.firstName}
                    className="w-20 h-20 rounded-full object-cover border-2 shadow-md"
                    style={{ borderColor: currentTheme.primaryColor }}
                  />
                ) : (
                  <div
                    className="w-20 h-20 rounded-full text-white font-black text-2xl flex items-center justify-center shadow-md"
                    style={{ backgroundColor: currentTheme.primaryColor }}
                  >
                    {user?.firstName ? user.firstName.charAt(0).toUpperCase() : 'U'}
                  </div>
                )}
                <button
                  onClick={() => avatarInputRef.current?.click()}
                  className="absolute bottom-0 right-0 p-1.5 bg-slate-900 hover:bg-brand-600 text-white rounded-full shadow-md transition-colors"
                  title="Change Photo"
                >
                  <Camera className="w-3.5 h-3.5" />
                </button>
              </div>
              <p className="text-xs font-bold text-slate-800 mt-3">{user?.firstName} {user?.lastName || ''}</p>
              <span className="text-[10px] text-slate-400">{user?.email}</span>
            </div>

            <input
              type="file"
              ref={avatarInputRef}
              accept="image/png, image/jpeg, image/webp"
              onChange={handleAvatarUpload}
              className="hidden"
            />
            <Button
              variant="outline"
              size="sm"
              className="w-full"
              leftIcon={<Upload className="w-3.5 h-3.5" />}
              onClick={() => avatarInputRef.current?.click()}
            >
              Upload Profile Photo
            </Button>
          </div>
        </Card>

        {/* 3. Live Practice Branding Preview */}
        <Card
          title="Practice Branding Status"
          subtitle="Workspace theme & visual properties"
          className="lg:col-span-1"
        >
          <div className="space-y-3 text-xs">
            <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 flex items-center justify-between">
              <span className="text-slate-500">Practice Entity:</span>
              <span className="font-bold text-slate-900 truncate max-w-[160px]">{practiceName}</span>
            </div>
            <div className="p-3 rounded-xl bg-indigo-50/60 border border-indigo-100 flex items-center justify-between">
              <span className="text-indigo-700 font-medium">Public Profile:</span>
              <a
                href={buildPracticePathUrl(user?.organizationId ? 'apex' : 'practice')}
                target="_blank"
                rel="noopener noreferrer"
                className="font-mono font-bold text-indigo-700 hover:text-indigo-900 truncate flex items-center gap-1"
              >
                <span>{formatPracticeDisplayUrl(user?.organizationId ? 'apex' : 'practice')}</span>
              </a>
            </div>
            <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 flex items-center justify-between">
              <span className="text-slate-500">Theme Mode:</span>
              <span className="font-bold text-slate-900 inline-flex items-center gap-1 uppercase">
                {currentTheme.mode === 'light' ? <Sun className="w-3.5 h-3.5 text-amber-500" /> : <Moon className="w-3.5 h-3.5 text-indigo-500" />}
                {currentTheme.mode} Mode
              </span>
            </div>
            <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 flex items-center justify-between">
              <span className="text-slate-500">Primary Tone:</span>
              <div className="flex items-center gap-1.5 font-bold text-slate-900">
                <span
                  className="w-3.5 h-3.5 rounded-full"
                  style={{ backgroundColor: currentTheme.primaryColor }}
                />
                <span>{currentTheme.name.split('&')[0]}</span>
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* Organization Classification & Customer Segment Card */}
      <Card
        title="Practice Classification & Organization Type"
        subtitle="Manage customer segment classification for tailored workflows and settings"
      >
        <div className="space-y-4 text-xs">
          {orgTypeSuccessMessage && (
            <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-xs font-semibold text-emerald-800 flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>{orgTypeSuccessMessage}</span>
            </div>
          )}

          {orgTypeError && (
            <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs font-semibold text-rose-800 flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
              <span>{orgTypeError}</span>
            </div>
          )}

          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-4 rounded-xl bg-slate-50 border border-slate-200/80">
            <div>
              <span className="text-slate-500 font-medium block">Current Organization Classification:</span>
              <div className="flex items-center gap-2 mt-1">
                <Building2 className="w-4 h-4 text-slate-700" />
                <span className="text-sm font-black text-slate-900">
                  {ORGANIZATION_TYPE_LABELS[organization?.organizationType || 'UNKNOWN']}
                </span>
                {(organization?.organizationType === 'UNKNOWN' || !organization?.organizationType) && (
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-200">
                    Legacy / Unclassified
                  </span>
                )}
              </div>
            </div>

            {canManageOrg ? (
              <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
                <select
                  value={selectedOrgType}
                  onChange={(e) => setSelectedOrgType(e.target.value as OrganizationType)}
                  aria-label="Select Organization Type"
                  className="px-3 py-2 bg-white border border-slate-300 rounded-lg font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-brand-500 text-xs shadow-2xs"
                >
                  <option value="SOLO_PRACTITIONER">Solo Practitioner</option>
                  <option value="SMALL_TAX_FIRM">Small Tax Firm</option>
                  <option value="GROWING_PRACTICE">Growing Practice</option>
                  <option value="BUSINESS">Business</option>
                  {selectedOrgType === 'UNKNOWN' && (
                    <option value="UNKNOWN" disabled>Not Configured</option>
                  )}
                </select>

                <div className="flex items-center gap-2">
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={handleSaveOrgType}
                    isLoading={isSavingOrgType}
                    disabled={selectedOrgType === 'UNKNOWN' || selectedOrgType === organization?.organizationType}
                    className="font-bold shadow-2xs"
                  >
                    Save Changes
                  </Button>
                  {selectedOrgType !== organization?.organizationType && (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleCancelOrgType}
                      disabled={isSavingOrgType}
                    >
                      Cancel
                    </Button>
                  )}
                </div>
              </div>
            ) : (
              <span className="text-slate-400 italic text-[11px]">
                Read-only (Admin privileges required to reclassify)
              </span>
            )}
          </div>
        </div>
      </Card>

      {/* Curated Color Themes & Template Palette */}
      <div className="space-y-4">
        {/* Filter Switcher */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-200 pb-3">
          <div>
            <h3 className="text-base font-bold text-slate-900">Curated Practice Color Schemes & Visual Templates</h3>
            <p className="text-xs text-slate-500 mt-0.5">1-click activation — updates sidebar backgrounds, buttons, and navigation accents</p>
          </div>

          <div className="inline-flex items-center gap-1 p-1 bg-slate-100 border border-slate-200 rounded-xl text-xs font-semibold">
            <button
              onClick={() => setActiveTabFilter('ALL')}
              className={clsx(
                'px-3 py-1 rounded-lg transition-all',
                activeTabFilter === 'ALL' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-500 hover:text-slate-700'
              )}
            >
              All Themes ({THEME_TEMPLATES.length})
            </button>
            <button
              onClick={() => setActiveTabFilter('LIGHT')}
              className={clsx(
                'px-3 py-1 rounded-lg transition-all flex items-center gap-1',
                activeTabFilter === 'LIGHT' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-500 hover:text-slate-700'
              )}
            >
              <Sun className="w-3 h-3 text-amber-500" />
              <span>Clean Light ({THEME_TEMPLATES.filter((t) => t.mode === 'light').length})</span>
            </button>
            <button
              onClick={() => setActiveTabFilter('DARK')}
              className={clsx(
                'px-3 py-1 rounded-lg transition-all flex items-center gap-1',
                activeTabFilter === 'DARK' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-500 hover:text-slate-700'
              )}
            >
              <Moon className="w-3 h-3 text-indigo-500" />
              <span>Executive Dark ({THEME_TEMPLATES.filter((t) => t.mode === 'dark').length})</span>
            </button>
          </div>
        </div>

        {/* Theme Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredThemes.map((tpl) => {
            const isSelected = currentTheme.id === tpl.id;

            return (
              <div
                key={tpl.id}
                onClick={() => setTheme(tpl.id)}
                className={clsx(
                  'p-5 rounded-2xl border cursor-pointer transition-all relative overflow-hidden group',
                  isSelected
                    ? 'border-brand-600 ring-2 ring-brand-500/20 bg-slate-50/80 shadow-card'
                    : 'border-slate-200 hover:border-slate-300 hover:shadow-2xs bg-white'
                )}
              >
                {isSelected && (
                  <span
                    className="absolute top-3.5 right-3.5 w-6 h-6 rounded-full text-white flex items-center justify-center shadow-xs"
                    style={{ backgroundColor: tpl.primaryColor }}
                  >
                    <Check className="w-3.5 h-3.5 stroke-[3]" />
                  </span>
                )}

                <div className="flex items-center gap-3 mb-4">
                  <div
                    className="w-9 h-9 rounded-xl shadow-2xs border border-white/20 flex items-center justify-center text-xs font-black p-1"
                    style={{ backgroundColor: tpl.primaryColor }}
                  >
                    <TaxorynLogo variant="symbol" size="xs" theme="dark" />
                  </div>
                  <div>
                    <div className="flex items-center gap-1.5">
                      <h4 className="text-xs font-black text-slate-900">{tpl.name}</h4>
                    </div>
                    <span className="text-[10px] text-slate-400 font-medium block">{tpl.category}</span>
                  </div>
                </div>

                {/* Color Swatch Bars */}
                <div className="flex items-center gap-1.5 h-3.5 rounded-full overflow-hidden p-0.5 bg-slate-100 mb-4 border border-slate-200/60">
                  <div className="flex-1 h-full rounded-full" style={{ backgroundColor: tpl.primaryColor }} />
                  <div className="w-10 h-full rounded-full border border-slate-300/40" style={{ backgroundColor: tpl.sidebarBg }} />
                  <div className="w-8 h-full rounded-full" style={{ backgroundColor: tpl.accentColor }} />
                </div>

                <div className="flex items-center justify-between text-[10px] text-slate-500 pt-3 border-t border-slate-100">
                  <span className="font-mono font-semibold">{tpl.primaryColor}</span>
                  <span
                    className={clsx(
                      'font-bold',
                      isSelected ? 'text-brand-600' : 'text-slate-400 group-hover:text-slate-700'
                    )}
                  >
                    {isSelected ? 'Active Theme' : 'Click to Apply'}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
