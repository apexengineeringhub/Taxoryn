import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  Search,
  MapPin,
  ShieldCheck,
  Star,
  Sparkles,
  ArrowRight,
  SlidersHorizontal,
  Briefcase,
  CheckCircle2,
  Phone,
  Mail,
  Calendar,
  MessageSquare,
  Scale,
  Award,
  Layers,
  HelpCircle,
  ExternalLink,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { marketplacePublicApi } from '../api/endpoints';
import { MarketplaceProfile, ProfessionalType } from '../types';
import clsx from 'clsx';

export const MarketplaceExplorePage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // State
  const [profiles, setProfiles] = useState<MarketplaceProfile[]>([]);
  const [featuredProfiles, setFeaturedProfiles] = useState<MarketplaceProfile[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [selectedForCompare, setSelectedForCompare] = useState<MarketplaceProfile[]>([]);

  // Filters
  const [search, setSearch] = useState<string>(searchParams.get('q') || '');
  const [city, setCity] = useState<string>(searchParams.get('city') || '');
  const [professionalType, setProfessionalType] = useState<string>(searchParams.get('type') || '');
  const [specialization, setSpecialization] = useState<string>(searchParams.get('spec') || '');
  const [verifiedOnly, setVerifiedOnly] = useState<boolean>(searchParams.get('verified') === 'true');
  const [priceRange, setPriceRange] = useState<number>(10000);

  // Quick Filters Data
  const popularCities = ['Mumbai', 'New Delhi', 'Bengaluru', 'Pune', 'Hyderabad', 'Chennai', 'Kolkata', 'Ahmedabad'];
  const specializationsList = [
    { id: 'GST_FILING', label: 'GST Filings & Returns' },
    { id: 'ITR_FILING', label: 'Income Tax (ITR)' },
    { id: 'TDS_COMPLIANCE', label: 'TDS & Form 26Q' },
    { id: 'COMPANY_INCORPORATION', label: 'Company Formation' },
    { id: 'AUDIT_ASSURANCE', label: 'Statutory & Tax Audit' },
    { id: 'STARTUP_ADVISORY', label: 'Startup & DPIIT Advisory' },
    { id: 'GST_LITIGATION', label: 'GST Appeals & Notices' },
    { id: 'ROC_COMPLIANCE', label: 'RoC & Secretarial' },
  ];

  const profTypes: { id: ProfessionalType; label: string; icon: any }[] = [
    { id: 'CHARTERED_ACCOUNTANT', label: 'Chartered Accountant (CA)', icon: Award },
    { id: 'COMPANY_SECRETARY', label: 'Company Secretary (CS)', icon: Briefcase },
    { id: 'TAX_ADVOCATE', label: 'Tax Advocate', icon: Scale },
    { id: 'COST_ACCOUNTANT', label: 'Cost Accountant (CMA)', icon: Layers },
    { id: 'TAX_CONSULTANT', label: 'Tax Consultant', icon: ShieldCheck },
  ];

  const fetchProfiles = async () => {
    setIsLoading(true);
    try {
      const [searchRes, featuredRes] = await Promise.all([
        marketplacePublicApi.search({
          search: search || undefined,
          city: city || undefined,
          professionalType: professionalType || undefined,
          specialization: specialization || undefined,
          verifiedOnly: verifiedOnly ? true : undefined,
          size: 24,
        }),
        marketplacePublicApi.getFeatured().catch(() => []),
      ]);

      setProfiles(searchRes.content || []);
      setTotalElements(searchRes.totalElements || 0);
      setFeaturedProfiles(featuredRes || []);
    } catch (err) {
      console.error('Failed to load marketplace profiles', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchProfiles();
  }, [city, professionalType, specialization, verifiedOnly]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchProfiles();
  };

  const handleSeedDemo = async () => {
    setIsLoading(true);
    try {
      await marketplacePublicApi.seedDemo();
      await fetchProfiles();
    } catch (err) {
      console.error('Failed to seed demo marketplace data', err);
    } finally {
      setIsLoading(false);
    }
  };

  const toggleCompare = (profile: MarketplaceProfile) => {
    if (selectedForCompare.some((p) => p.id === profile.id)) {
      setSelectedForCompare(selectedForCompare.filter((p) => p.id !== profile.id));
    } else {
      if (selectedForCompare.length >= 3) {
        alert('You can compare up to 3 tax professionals at a time.');
        return;
      }
      setSelectedForCompare([...selectedForCompare, profile]);
    }
  };

  const handleGoToCompare = () => {
    const ids = selectedForCompare.map((p) => p.id).join(',');
    navigate(`/marketplace/compare?ids=${ids}`);
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 pb-20">
      {/* Top Marketplace Header / Hero */}
      <div className="relative bg-gradient-to-br from-indigo-900 via-slate-900 to-slate-950 text-white pt-12 pb-20 px-4 sm:px-6 lg:px-8 border-b border-indigo-900/50 overflow-hidden">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_30%,rgba(99,102,241,0.15),transparent_50%)] pointer-events-none" />

        <div className="max-w-7xl mx-auto space-y-8 relative z-10">
          <div className="text-center max-w-3xl mx-auto space-y-4">
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-indigo-500/20 border border-indigo-400/30 text-indigo-300 text-xs font-semibold tracking-wide uppercase">
              <Sparkles className="w-3.5 h-3.5" />
              Taxoryn Verified Tax Professional Directory
            </div>
            <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white">
              Find Verified Chartered Accountants, CS & Tax Advocates Across India
            </h1>
            <p className="text-base sm:text-lg text-slate-300">
              Discover, compare, and book consultations with top-rated tax practitioners. Direct KYC verified, transparent fees, and automated practice onboarding.
            </p>
          </div>

          {/* Search Bar Floating Card */}
          <form
            onSubmit={handleSearchSubmit}
            className="bg-white dark:bg-slate-900 p-3 sm:p-4 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 max-w-4xl mx-auto flex flex-col md:flex-row gap-3"
          >
            <div className="flex-1 flex items-center gap-3 px-4 py-2.5 bg-slate-50 dark:bg-slate-800/60 rounded-2xl border border-slate-100 dark:border-slate-700/50">
              <Search className="w-5 h-5 text-indigo-500 shrink-0" />
              <input
                type="text"
                placeholder="Search firm, service (e.g. GST, ITR, Audit)..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full bg-transparent border-none text-sm text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none"
              />
            </div>

            <div className="flex-1 flex items-center gap-3 px-4 py-2.5 bg-slate-50 dark:bg-slate-800/60 rounded-2xl border border-slate-100 dark:border-slate-700/50">
              <MapPin className="w-5 h-5 text-emerald-500 shrink-0" />
              <input
                type="text"
                placeholder="City, State or Pincode (e.g. Mumbai, 400001)..."
                value={city}
                onChange={(e) => setCity(e.target.value)}
                className="w-full bg-transparent border-none text-sm text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none"
              />
            </div>

            <Button type="submit" variant="primary" size="md" className="rounded-2xl px-6 py-3 shrink-0">
              <Search className="w-4 h-4 mr-2" />
              Find Experts
            </Button>
          </form>

          {/* Quick City Pills */}
          <div className="flex items-center justify-center gap-2 flex-wrap text-xs text-slate-400">
            <span className="font-semibold text-slate-300">Popular Cities:</span>
            {popularCities.map((c) => (
              <button
                key={c}
                type="button"
                onClick={() => setCity(city === c ? '' : c)}
                className={clsx(
                  'px-3 py-1 rounded-full transition-all border',
                  city === c
                    ? 'bg-indigo-600 text-white border-indigo-500'
                    : 'bg-white/10 text-slate-300 border-white/10 hover:bg-white/20'
                )}
              >
                {c}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Main Content Area */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 -mt-6">
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
          {/* Left Sidebar Filter Column */}
          <div className="lg:col-span-1 space-y-6">
            <div className="bg-white dark:bg-slate-900 p-6 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-6 sticky top-6">
              <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
                <div className="flex items-center gap-2 font-bold text-slate-900 dark:text-white text-base">
                  <SlidersHorizontal className="w-4 h-4 text-indigo-600" />
                  <span>Filter Directory</span>
                </div>
                {(city || professionalType || specialization || verifiedOnly) && (
                  <button
                    onClick={() => {
                      setCity('');
                      setProfessionalType('');
                      setSpecialization('');
                      setVerifiedOnly(false);
                    }}
                    className="text-xs text-indigo-600 hover:underline font-semibold"
                  >
                    Reset
                  </button>
                )}
              </div>

              {/* Verified KYC Toggle */}
              <div className="flex items-center justify-between p-3.5 bg-emerald-50/50 dark:bg-emerald-950/20 rounded-2xl border border-emerald-100 dark:border-emerald-900/40">
                <div className="flex items-center gap-2.5">
                  <ShieldCheck className="w-5 h-5 text-emerald-600" />
                  <div>
                    <div className="text-xs font-bold text-slate-900 dark:text-white">ICAI / ICSI Verified</div>
                    <div className="text-[11px] text-slate-500">Government credentials verified</div>
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={verifiedOnly}
                  onChange={(e) => setVerifiedOnly(e.target.checked)}
                  className="w-4 h-4 text-emerald-600 rounded border-slate-300 focus:ring-emerald-500"
                />
              </div>

              {/* Professional Type */}
              <div className="space-y-2.5">
                <label className="text-xs font-bold uppercase tracking-wider text-slate-400">Professional Type</label>
                <div className="space-y-1.5">
                  <button
                    onClick={() => setProfessionalType('')}
                    className={clsx(
                      'w-full text-left px-3 py-2 rounded-xl text-xs font-medium transition-all',
                      !professionalType
                        ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-300 font-bold'
                        : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
                    )}
                  >
                    All Professional Types
                  </button>
                  {profTypes.map((t) => (
                    <button
                      key={t.id}
                      onClick={() => setProfessionalType(t.id)}
                      className={clsx(
                        'w-full flex items-center justify-between px-3 py-2 rounded-xl text-xs font-medium transition-all',
                        professionalType === t.id
                          ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-300 font-bold'
                          : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
                      )}
                    >
                      <span>{t.label}</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Specialization List */}
              <div className="space-y-2.5">
                <label className="text-xs font-bold uppercase tracking-wider text-slate-400">Specialization</label>
                <div className="space-y-1.5 max-h-56 overflow-y-auto pr-1">
                  <button
                    onClick={() => setSpecialization('')}
                    className={clsx(
                      'w-full text-left px-3 py-2 rounded-xl text-xs font-medium transition-all',
                      !specialization
                        ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-300 font-bold'
                        : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
                    )}
                  >
                    All Specializations
                  </button>
                  {specializationsList.map((s) => (
                    <button
                      key={s.id}
                      onClick={() => setSpecialization(s.id)}
                      className={clsx(
                        'w-full text-left px-3 py-2 rounded-xl text-xs font-medium transition-all',
                        specialization === s.id
                          ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-300 font-bold'
                          : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
                      )}
                    >
                      {s.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Demo Data Button */}
              <div className="pt-4 border-t border-slate-100 dark:border-slate-800">
                <Button variant="outline" size="sm" onClick={handleSeedDemo} className="w-full text-xs">
                  <Sparkles className="w-3.5 h-3.5 mr-1.5 text-amber-500" />
                  Seed Demo Tax Firms
                </Button>
              </div>
            </div>
          </div>

          {/* Right Main Column: Directory Results */}
          <div className="lg:col-span-3 space-y-6">
            {/* Action / Results Bar */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
              <div>
                <h2 className="text-lg font-bold text-slate-900 dark:text-white">
                  {totalElements} Tax Professionals Found
                </h2>
                <p className="text-xs text-slate-500">
                  Showing top verified practitioners matching your location and service requirements.
                </p>
              </div>

              {selectedForCompare.length > 0 && (
                <div className="flex items-center gap-3 bg-indigo-50 dark:bg-indigo-950/40 px-4 py-2 rounded-xl border border-indigo-200 dark:border-indigo-800">
                  <span className="text-xs font-semibold text-indigo-700 dark:text-indigo-300">
                    {selectedForCompare.length} Selected to Compare
                  </span>
                  <Button size="sm" variant="primary" onClick={handleGoToCompare}>
                    Compare Side-by-Side
                  </Button>
                </div>
              )}
            </div>

            {/* Profile Cards Grid */}
            {isLoading ? (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {[1, 2, 3, 4].map((n) => (
                  <div key={n} className="bg-white dark:bg-slate-900 p-6 rounded-3xl border border-slate-200 dark:border-slate-800 animate-pulse h-72 space-y-4">
                    <div className="flex gap-4">
                      <div className="w-16 h-16 bg-slate-200 dark:bg-slate-800 rounded-2xl" />
                      <div className="space-y-2 flex-1">
                        <div className="h-4 bg-slate-200 dark:bg-slate-800 rounded w-3/4" />
                        <div className="h-3 bg-slate-200 dark:bg-slate-800 rounded w-1/2" />
                      </div>
                    </div>
                    <div className="h-16 bg-slate-100 dark:bg-slate-800/40 rounded-xl" />
                  </div>
                ))}
              </div>
            ) : profiles.length === 0 ? (
              <div className="bg-white dark:bg-slate-900 p-12 rounded-3xl border border-slate-200 dark:border-slate-800 text-center space-y-4">
                <Briefcase className="w-12 h-12 text-slate-400 mx-auto" />
                <h3 className="text-lg font-bold text-slate-900 dark:text-white">No Tax Professionals Found</h3>
                <p className="text-sm text-slate-500 max-w-md mx-auto">
                  Try broadening your search criteria, clearing filters, or seeding realistic demo Chartered Accountancy firms.
                </p>
                <Button variant="primary" onClick={handleSeedDemo}>
                  <Sparkles className="w-4 h-4 mr-2" />
                  Load Sample Verified Tax Practitioners
                </Button>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {profiles.map((profile) => {
                  const isSelected = selectedForCompare.some((p) => p.id === profile.id);
                  return (
                    <div
                      key={profile.id}
                      className={clsx(
                        'bg-white dark:bg-slate-900 rounded-3xl border transition-all hover:shadow-xl hover:-translate-y-0.5 flex flex-col justify-between overflow-hidden',
                        isSelected
                          ? 'border-indigo-600 ring-2 ring-indigo-500/20'
                          : 'border-slate-200 dark:border-slate-800'
                      )}
                    >
                      <div className="p-6 space-y-4">
                        {/* Header: Avatar, Name, Verified Badge */}
                        <div className="flex items-start justify-between gap-4">
                          <div className="flex items-center gap-3.5">
                            <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white font-bold text-xl flex items-center justify-center shadow-md shrink-0">
                              {profile.displayName.charAt(0)}
                            </div>
                            <div>
                              <div className="flex items-center gap-1.5 flex-wrap">
                                <h3 className="font-bold text-slate-900 dark:text-white text-base hover:text-indigo-600 transition-colors">
                                  {profile.displayName}
                                </h3>
                                {profile.verificationStatus === 'VERIFIED' && (
                                  <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700 dark:text-emerald-300 bg-emerald-50 dark:bg-emerald-950/60 px-2 py-0.5 rounded-full border border-emerald-200 dark:border-emerald-800">
                                    <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                                    Verified
                                  </span>
                                )}
                              </div>
                              <div className="text-xs text-slate-500 flex items-center gap-2 mt-0.5">
                                <span className="font-medium text-slate-700 dark:text-slate-300">
                                  {profile.professionalType?.replace(/_/g, ' ')}
                                </span>
                                <span>•</span>
                                <span>{profile.experienceYears} Years Exp.</span>
                              </div>
                            </div>
                          </div>

                          {/* Compare Checkbox */}
                          <button
                            type="button"
                            onClick={() => toggleCompare(profile)}
                            className={clsx(
                              'text-xs font-semibold px-2.5 py-1 rounded-lg border transition-all shrink-0',
                              isSelected
                                ? 'bg-indigo-600 text-white border-indigo-600'
                                : 'bg-slate-50 dark:bg-slate-800 text-slate-600 dark:text-slate-300 border-slate-200 dark:border-slate-700 hover:bg-slate-100'
                            )}
                          >
                            {isSelected ? 'Comparing' : '+ Compare'}
                          </button>
                        </div>

                        {/* Headline */}
                        {profile.headline && (
                          <p className="text-xs text-slate-600 dark:text-slate-300 line-clamp-2 leading-relaxed">
                            {profile.headline}
                          </p>
                        )}

                        {/* Location & Rating Strip */}
                        <div className="flex items-center justify-between text-xs py-2 px-3 bg-slate-50 dark:bg-slate-800/50 rounded-xl border border-slate-100 dark:border-slate-800">
                          <div className="flex items-center gap-1.5 text-slate-600 dark:text-slate-400">
                            <MapPin className="w-3.5 h-3.5 text-rose-500" />
                            <span>{profile.city || 'India'}</span>
                          </div>
                          <div className="flex items-center gap-1 font-bold text-amber-600 dark:text-amber-400">
                            <Star className="w-3.5 h-3.5 fill-current" />
                            <span>{profile.averageRating?.toFixed(1) || '5.0'}</span>
                            <span className="text-slate-400 font-normal">({profile.totalReviews || 0} reviews)</span>
                          </div>
                        </div>

                        {/* Specialization Tags */}
                        {profile.specializations && profile.specializations.length > 0 && (
                          <div className="flex flex-wrap gap-1.5">
                            {profile.specializations.slice(0, 3).map((spec, i) => (
                              <span
                                key={i}
                                className="text-[11px] font-medium bg-indigo-50/70 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-300 px-2.5 py-0.5 rounded-lg border border-indigo-100 dark:border-indigo-900/30"
                              >
                                {spec.replace(/_/g, ' ')}
                              </span>
                            ))}
                            {profile.specializations.length > 3 && (
                              <span className="text-[11px] text-slate-400 font-medium px-1">
                                +{profile.specializations.length - 3} more
                              </span>
                            )}
                          </div>
                        )}
                      </div>

                      {/* Card Footer: Pricing & Actions */}
                      <div className="p-4 bg-slate-50/80 dark:bg-slate-800/30 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
                        <div>
                          <div className="text-[10px] uppercase font-bold text-slate-400">Starting From</div>
                          <div className="text-sm font-extrabold text-slate-900 dark:text-white">
                            ₹{profile.startingFee?.toLocaleString('en-IN') || '999'}
                          </div>
                        </div>

                        <div className="flex items-center gap-2">
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => navigate(`/marketplace/profile/${profile.id}`)}
                            className="rounded-xl px-3.5"
                          >
                            <span>View Profile & Book</span>
                            <ArrowRight className="w-3.5 h-3.5 ml-1" />
                          </Button>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
