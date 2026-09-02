import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  Search,
  MapPin,
  ShieldCheck,
  Star,
  Sparkles,
  ArrowRight,
  ArrowLeft,
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
  Clock,
  Video,
  UserCheck,
  Check,
  X,
  Compass,
  DollarSign,
  Building2,
  Navigation,
  Filter,
  BookOpen,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { TaxorynLogo } from '../components/common/TaxorynLogo';
import { marketplacePublicApi } from '../api/endpoints';
import { MarketplaceProfile, ProfessionalType, PublicTaxServiceCategory } from '../types';
import clsx from 'clsx';

export const MarketplaceExplorePage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // State
  const [profiles, setProfiles] = useState<MarketplaceProfile[]>([]);
  const [featuredProfiles, setFeaturedProfiles] = useState<MarketplaceProfile[]>([]);
  const [masterCategories, setMasterCategories] = useState<PublicTaxServiceCategory[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [selectedForCompare, setSelectedForCompare] = useState<MarketplaceProfile[]>([]);

  // Active Discovery Mode: 'WIZARD' (Guided 3-Step) vs 'DIRECTORY'
  const [activeTab, setActiveTab] = useState<'WIZARD' | 'DIRECTORY'>('WIZARD');
  const [wizardStep, setWizardStep] = useState<1 | 2 | 3>(1);

  // Filters
  const [search, setSearch] = useState<string>(searchParams.get('q') || '');
  const [city, setCity] = useState<string>(searchParams.get('city') || '');
  const [stateFilter, setStateFilter] = useState<string>(searchParams.get('state') || '');
  const [pincode, setPincode] = useState<string>(searchParams.get('pincode') || '');
  const [userCoords, setUserCoords] = useState<{ latitude: number; longitude: number } | null>(null);
  const [radiusKm, setRadiusKm] = useState<number>(10);
  const [isLocating, setIsLocating] = useState<boolean>(false);
  const [locationError, setLocationError] = useState<string | null>(null);
  const [distanceRadius, setDistanceRadius] = useState<string>('ANY'); // '5KM', '15KM', '50KM', 'ANY'
  const [professionalType, setProfessionalType] = useState<string>(searchParams.get('type') || '');
  const [specialization, setSpecialization] = useState<string>(searchParams.get('spec') || '');
  const [taxpayerEntity, setTaxpayerEntity] = useState<string>('INDIVIDUAL');
  const [verifiedOnly, setVerifiedOnly] = useState<boolean>(searchParams.get('verified') === 'true');
  const [sortBy, setSortBy] = useState<string>('averageRating'); // 'averageRating', 'distance', 'startingFee', 'experienceYears'
  const [sortDirection, setSortDirection] = useState<string>('desc');

  // Modals for Quick Contact and Instant Booking
  const [selectedProfileForContact, setSelectedProfileForContact] = useState<MarketplaceProfile | null>(null);
  const [selectedProfileForBooking, setSelectedProfileForBooking] = useState<MarketplaceProfile | null>(null);

  // Contact / Inquiry Form
  const [contactForm, setContactForm] = useState({
    clientName: '',
    clientEmail: '',
    clientPhone: '',
    city: '',
    pan: '',
    gstin: '',
    serviceCategory: 'GST',
    taxpayerEntity: 'INDIVIDUAL',
    requirementDescription: '',
    budgetRange: '₹2,000 - ₹5,000',
    urgency: 'STANDARD',
  });

  // Booking Form
  const [bookingForm, setBookingForm] = useState({
    clientName: '',
    clientEmail: '',
    clientPhone: '',
    topic: 'Statutory Tax Planning & Compliance Strategy',
    consultationMode: 'VIDEO',
    bookingDate: new Date(Date.now() + 86400000).toISOString().split('T')[0],
    startTime: '15:00',
    endTime: '15:30',
    notes: '',
  });

  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [actionSuccess, setActionSuccess] = useState<{
    title: string;
    message: string;
    trackingId: string;
    type: 'INQUIRY' | 'BOOKING';
    profileName: string;
  } | null>(null);

  // Quick Filters Data
  const popularCities = ['Mumbai', 'New Delhi', 'Bengaluru', 'Pune', 'Hyderabad', 'Chennai', 'Kolkata', 'Ahmedabad'];
  const specializationsList = [
    { id: 'GST_FILING', label: 'GST Filings & Returns', desc: 'Monthly GSTR-1, 3B, ITC audit' },
    { id: 'ITR_FILING', label: 'Income Tax (ITR)', desc: 'Salaried, Capital Gains, Business ITR' },
    { id: 'TDS_COMPLIANCE', label: 'TDS & Form 26Q', desc: 'Quarterly TDS, 24Q, 27Q filings' },
    { id: 'COMPANY_INCORPORATION', label: 'Company Formation', desc: 'Pvt Ltd, LLP, Section 8 registration' },
    { id: 'AUDIT_ASSURANCE', label: 'Statutory & Tax Audit', desc: 'Tax Audit u/s 44AB, statutory audit' },
    { id: 'STARTUP_ADVISORY', label: 'Startup & DPIIT', desc: 'DPIIT tax exemption, 80-IAC advisory' },
    { id: 'GST_LITIGATION', label: 'GST Appeals & Notices', desc: 'SCN response, department representation' },
    { id: 'ROC_COMPLIANCE', label: 'RoC & Secretarial', desc: 'Annual filing, director KYC, MGT-7' },
  ];

  const taxpayerEntities = [
    { id: 'INDIVIDUAL', label: 'Individual / Salaried', icon: '👤' },
    { id: 'PROPRIETORSHIP', label: 'Sole Proprietorship', icon: '🏪' },
    { id: 'LLP', label: 'LLP / Partnership', icon: '🤝' },
    { id: 'PRIVATE_LIMITED', label: 'Private Limited / Corp', icon: '🏢' },
    { id: 'NRI', label: 'NRI / Foreign Entity', icon: '✈️' },
  ];

  const profTypes: { id: ProfessionalType; label: string; icon: any }[] = [
    { id: 'CHARTERED_ACCOUNTANT', label: 'Chartered Accountant (CA)', icon: Award },
    { id: 'COMPANY_SECRETARY', label: 'Company Secretary (CS)', icon: Briefcase },
    { id: 'TAX_ADVOCATE', label: 'Tax Advocate', icon: Scale },
    { id: 'COST_ACCOUNTANT', label: 'Cost Accountant (CMA)', icon: Layers },
    { id: 'TAX_CONSULTANT', label: 'Tax Consultant', icon: ShieldCheck },
  ];

  const handleUseMyLocation = () => {
    if (!navigator.geolocation) {
      setLocationError('Geolocation is not supported by your browser.');
      return;
    }
    setIsLocating(true);
    setLocationError(null);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setIsLocating(false);
        setUserCoords({
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
        });
        setSortBy('distance');
        setSortDirection('asc');
      },
      (err) => {
        setIsLocating(false);
        if (err.code === err.PERMISSION_DENIED) {
          setLocationError('Location access was denied. You can search manually by city, state, or pincode.');
        } else {
          setLocationError('Could not obtain your location. Please enter a city or pincode.');
        }
      },
      { timeout: 10000 }
    );
  };

  const handleClearLocation = () => {
    setUserCoords(null);
    setLocationError(null);
    setSortBy('averageRating');
    setSortDirection('desc');
  };

  const fetchProfiles = async () => {
    setIsLoading(true);
    try {
      const [searchRes, featuredRes, catRes] = await Promise.all([
        marketplacePublicApi.search({
          search: search || undefined,
          city: city || undefined,
          state: stateFilter || undefined,
          pincode: pincode || undefined,
          latitude: userCoords ? userCoords.latitude : undefined,
          longitude: userCoords ? userCoords.longitude : undefined,
          radiusKm: userCoords ? radiusKm : undefined,
          professionalType: professionalType || undefined,
          specialization: specialization || undefined,
          verifiedOnly: verifiedOnly ? true : undefined,
          sortBy: sortBy || undefined,
          sortDirection: sortDirection || 'desc',
          size: 24,
        }),
        marketplacePublicApi.getFeatured().catch(() => []),
        marketplacePublicApi.getTaxServiceCategories().catch(() => []),
      ]);

      let results = searchRes.content || [];
      setMasterCategories(catRes || []);

      // Sort locally if starting fee / experience selected
      if (sortBy === 'startingFee') {
        results = [...results].sort((a, b) => (a.startingFee || 0) - (b.startingFee || 0));
      } else if (sortBy === 'experienceYears') {
        results = [...results].sort((a, b) => (b.experienceYears || 0) - (a.experienceYears || 0));
      } else if (sortBy === 'averageRating') {
        results = [...results].sort((a, b) => (b.averageRating || 0) - (a.averageRating || 0));
      }

      setProfiles(results);
      setTotalElements(searchRes.totalElements || results.length);
      setFeaturedProfiles(featuredRes || []);
    } catch (err) {
      console.error('Failed to load marketplace profiles', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchProfiles();
  }, [city, stateFilter, pincode, userCoords, radiusKm, professionalType, specialization, verifiedOnly, sortBy, sortDirection]);

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

  const incomingTaxServiceId = searchParams.get('taxServiceId');
  const incomingTaxServiceName = searchParams.get('taxServiceName');
  const incomingSourceType = searchParams.get('sourceType') || (incomingTaxServiceId ? 'TAXORYN_LEARN' : undefined);
  const incomingSourceContentId = searchParams.get('sourceContentId');
  const incomingContentSlug = searchParams.get('contentSlug');

  const buildPracticeUrl = (profile: MarketplaceProfile) => {
    const params = new URLSearchParams();
    if (incomingTaxServiceId) params.set('taxServiceId', incomingTaxServiceId);
    if (incomingTaxServiceName) params.set('taxServiceName', incomingTaxServiceName);
    if (incomingSourceType) params.set('sourceType', incomingSourceType);
    if (incomingSourceContentId) params.set('sourceContentId', incomingSourceContentId);
    if (incomingContentSlug) params.set('contentSlug', incomingContentSlug);
    const qs = params.toString();
    const target = `/practice/${profile.publicSlug || profile.slug || profile.id}`;
    return qs ? `${target}?${qs}` : target;
  };

  // Direct Conversion: Submit Quick Contact (Inquiry)
  const handleContactSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProfileForContact) return;
    setIsSubmitting(true);
    try {
      const lead = await marketplacePublicApi.submitLead({
        marketplaceProfileId: selectedProfileForContact.id,
        clientName: contactForm.clientName,
        clientEmail: contactForm.clientEmail,
        clientPhone: contactForm.clientPhone,
        city: contactForm.city || city || undefined,
        pan: contactForm.pan || undefined,
        gstin: contactForm.gstin || undefined,
        serviceCategory: contactForm.serviceCategory,
        taxServiceId: searchParams.get('taxServiceId') || undefined,
        sourceType: searchParams.get('sourceType') || (searchParams.get('taxServiceId') ? 'CONTENT' : undefined),
        sourceContentId: searchParams.get('sourceContentId') || undefined,
        requirementDescription: `[Entity: ${contactForm.taxpayerEntity}] ${contactForm.requirementDescription}`,
        budgetRange: contactForm.budgetRange,
        urgency: contactForm.urgency,
      });

      const profName = selectedProfileForContact.displayName;
      setSelectedProfileForContact(null);
      setActionSuccess({
        title: 'Requirement Dispatched to Tax Practitioner!',
        message: `Your requirement has been transmitted directly into ${profName}'s Inbound Leads pipeline. The practitioner will review your scope and dispatch a formal engagement proposal.`,
        trackingId: lead.id,
        type: 'INQUIRY',
        profileName: profName,
      });
    } catch (err) {
      alert('Failed to submit requirement inquiry.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Direct Conversion: Book Consultation Slot
  const handleBookingSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProfileForBooking) return;
    setIsSubmitting(true);
    try {
      const booking = await marketplacePublicApi.bookConsultation({
        marketplaceProfileId: selectedProfileForBooking.id,
        clientName: bookingForm.clientName,
        clientEmail: bookingForm.clientEmail,
        clientPhone: bookingForm.clientPhone,
        topic: bookingForm.topic,
        consultationMode: bookingForm.consultationMode,
        bookingDate: bookingForm.bookingDate,
        startTime: bookingForm.startTime,
        endTime: bookingForm.endTime,
        notes: bookingForm.notes || undefined,
      });

      const profName = selectedProfileForBooking.displayName;
      setSelectedProfileForBooking(null);
      setActionSuccess({
        title: 'Consultation Appointment Confirmed!',
        message: `Your 30-minute advisory session with ${profName} is confirmed for ${bookingForm.bookingDate} at ${bookingForm.startTime} IST (${bookingForm.consultationMode}). A calendar link and Inbound Lead record have been generated.`,
        trackingId: booking.id,
        type: 'BOOKING',
        profileName: profName,
      });
    } catch (err) {
      alert('Failed to book consultation session.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 pb-20">
      {/* Top Brand Navigation Bar */}
      <nav className="bg-[#07152B] border-b border-white/10 px-4 sm:px-6 lg:px-8 py-3 sticky top-0 z-30 shadow-md">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-3">
            <TaxorynLogo variant="horizontal" theme="dark" size="sm" />
            <span className="hidden sm:inline-block text-[10px] font-bold uppercase tracking-widest text-[#00D1A3] bg-white/5 border border-[#00D1A3]/30 px-2.5 py-0.5 rounded-full">
              Marketplace
            </span>
          </div>
          <div className="flex items-center gap-2 sm:gap-4 text-xs font-semibold">
            <button
              type="button"
              onClick={() => navigate('/learn')}
              className="text-slate-300 hover:text-white px-2.5 py-1 rounded-lg hover:bg-white/10 transition-colors"
            >
              Learn
            </button>
            <button
              type="button"
              onClick={() => navigate('/marketplace/customer/requirements/new')}
              className="text-slate-300 hover:text-white px-2.5 py-1 rounded-lg hover:bg-white/10 transition-colors"
            >
              Post Requirement
            </button>
            <button
              type="button"
              onClick={() => navigate('/login')}
              className="bg-[#00D1A3] hover:bg-[#00B388] text-[#07152B] font-bold px-3.5 py-1.5 rounded-lg shadow-sm transition-all"
            >
              Sign In
            </button>
          </div>
        </div>
      </nav>

      {/* ========================================================================= */}
      {/* Top Customer Entry Banner: "Find Tax Professional" */}
      {/* ========================================================================= */}
      <div className="relative bg-gradient-to-br from-[#082E5B] via-[#07152B] to-[#070C1A] text-white pt-10 pb-16 px-4 sm:px-6 lg:px-8 border-b border-indigo-900/50 overflow-hidden">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_30%,rgba(99,102,241,0.18),transparent_50%)] pointer-events-none" />

        <div className="max-w-7xl mx-auto space-y-6 relative z-10">
          <div className="text-center max-w-3xl mx-auto space-y-3">
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-indigo-500/20 border border-indigo-400/30 text-indigo-300 text-xs font-semibold tracking-wide uppercase">
              <Sparkles className="w-3.5 h-3.5" />
              Taxoryn 3-in-1 Customer Journey & Marketplace
            </div>
            <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white">
              Find Your Ideal Tax Professional in 3 Steps
            </h1>
            <p className="text-sm sm:text-base text-slate-300">
              Match with ICAI / ICSI verified Chartered Accountants & Tax Advocates by Location, Requirement, and Transparent Fees.
            </p>
            <div className="flex items-center justify-center gap-3 pt-1">
              <button
                type="button"
                onClick={() => navigate('/learn')}
                className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-white/10 hover:bg-white/20 text-indigo-200 hover:text-white text-xs font-semibold transition-colors"
              >
                <BookOpen className="w-3.5 h-3.5 text-indigo-400" />
                <span>New to taxes? Read simple guides on Taxoryn Learn →</span>
              </button>
            </div>
          </div>

          {/* Mode Switcher */}
          <div className="flex items-center justify-center gap-2">
            <button
              onClick={() => setActiveTab('WIZARD')}
              className={clsx(
                'flex items-center gap-2 px-5 py-2.5 rounded-2xl text-xs font-bold transition-all shadow-md',
                activeTab === 'WIZARD'
                  ? 'bg-indigo-600 text-white shadow-indigo-600/30'
                  : 'bg-white/10 text-slate-300 hover:bg-white/20'
              )}
            >
              <Compass className="w-4 h-4" />
              <span>3-Step Guided Match Wizard</span>
            </button>
            <button
              onClick={() => setActiveTab('DIRECTORY')}
              className={clsx(
                'flex items-center gap-2 px-5 py-2.5 rounded-2xl text-xs font-bold transition-all shadow-md',
                activeTab === 'DIRECTORY'
                  ? 'bg-indigo-600 text-white shadow-indigo-600/30'
                  : 'bg-white/10 text-slate-300 hover:bg-white/20'
              )}
            >
              <Search className="w-4 h-4" />
              <span>Direct Search & Filters</span>
            </button>
          </div>

          {/* Contextual Tax Service Banner (if routed from Learn Article CTA) */}
          {(incomingTaxServiceName || incomingTaxServiceId) && (
            <div className="bg-indigo-900/80 border border-indigo-400/40 rounded-2xl p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 text-left max-w-4xl mx-auto shadow-lg backdrop-blur-md">
              <div className="flex items-start gap-3">
                <div className="p-2.5 bg-indigo-500/20 text-indigo-300 rounded-xl shrink-0 mt-0.5 sm:mt-0">
                  <Sparkles className="w-5 h-5" />
                </div>
                <div className="space-y-0.5">
                  <div className="text-[11px] font-bold text-indigo-300 uppercase tracking-wider">
                    Tax Service Context From Taxoryn Learn
                  </div>
                  <div className="text-base sm:text-lg font-black text-white">
                    {incomingTaxServiceName || 'Selected Tax Service'}
                  </div>
                  <p className="text-xs text-slate-300">
                    Match with certified Chartered Accountants and Tax Experts specializing in this requirement. Select your location below.
                  </p>
                </div>
              </div>
              {incomingContentSlug && (
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => navigate(`/learn/${incomingContentSlug}`)}
                  className="bg-white/10 hover:bg-white/20 text-indigo-200 hover:text-white border-white/20 text-xs rounded-xl shrink-0"
                >
                  <ArrowLeft className="w-3.5 h-3.5 mr-1" />
                  <span>Back to Article</span>
                </Button>
              )}
            </div>
          )}

          {/* ========================================================================= */}
          {/* 3-Step Guided Match Wizard */}
          {/* ========================================================================= */}
          {activeTab === 'WIZARD' && (
            <div className="bg-white/95 dark:bg-slate-900/95 backdrop-blur-md rounded-3xl p-6 shadow-2xl border border-white/20 dark:border-slate-800 max-w-4xl mx-auto text-slate-900 dark:text-white space-y-6">
              {/* Wizard Steps Header Indicator */}
              <div className="grid grid-cols-3 gap-2 border-b border-slate-200 dark:border-slate-800 pb-4">
                <button
                  onClick={() => setWizardStep(1)}
                  className={clsx(
                    'flex items-center gap-2.5 text-left p-2 rounded-xl transition-all',
                    wizardStep === 1 ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-300 font-bold' : 'text-slate-400'
                  )}
                >
                  <div className={clsx(
                    'w-7 h-7 rounded-lg flex items-center justify-center font-bold text-xs shrink-0',
                    wizardStep === 1 ? 'bg-indigo-600 text-white' : 'bg-slate-200 dark:bg-slate-800 text-slate-500'
                  )}>
                    1
                  </div>
                  <div className="min-w-0">
                    <div className="text-xs font-bold truncate">Location</div>
                    <div className="text-[10px] text-slate-400 truncate">{city || 'Select city/distance'}</div>
                  </div>
                </button>

                <button
                  onClick={() => setWizardStep(2)}
                  className={clsx(
                    'flex items-center gap-2.5 text-left p-2 rounded-xl transition-all',
                    wizardStep === 2 ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-300 font-bold' : 'text-slate-400'
                  )}
                >
                  <div className={clsx(
                    'w-7 h-7 rounded-lg flex items-center justify-center font-bold text-xs shrink-0',
                    wizardStep === 2 ? 'bg-indigo-600 text-white' : 'bg-slate-200 dark:bg-slate-800 text-slate-500'
                  )}>
                    2
                  </div>
                  <div className="min-w-0">
                    <div className="text-xs font-bold truncate">Requirement</div>
                    <div className="text-[10px] text-slate-400 truncate">{specialization ? specialization.replace(/_/g, ' ') : 'Select tax service'}</div>
                  </div>
                </button>

                <button
                  onClick={() => setWizardStep(3)}
                  className={clsx(
                    'flex items-center gap-2.5 text-left p-2 rounded-xl transition-all',
                    wizardStep === 3 ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-300 font-bold' : 'text-slate-400'
                  )}
                >
                  <div className={clsx(
                    'w-7 h-7 rounded-lg flex items-center justify-center font-bold text-xs shrink-0',
                    wizardStep === 3 ? 'bg-indigo-600 text-white' : 'bg-slate-200 dark:bg-slate-800 text-slate-500'
                  )}>
                    3
                  </div>
                  <div className="min-w-0">
                    <div className="text-xs font-bold truncate">Sort & Match</div>
                    <div className="text-[10px] text-slate-400 truncate">Rating / Distance / Fees</div>
                  </div>
                </button>
              </div>

              {/* Step 1: Location & Proximity */}
              {wizardStep === 1 && (
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                        <MapPin className="w-4 h-4 text-rose-500" />
                        <span>Step 1: Choose Practice Location or Search Radius</span>
                      </h3>
                      <p className="text-xs text-slate-500">Pick a metropolitan city, enter your pincode, or select pan-India digital services.</p>
                    </div>
                  </div>

                  <div className="flex gap-2">
                    <div className="flex-1 flex items-center gap-2.5 px-3.5 py-2 bg-slate-50 dark:bg-slate-800 rounded-2xl border border-slate-200 dark:border-slate-700">
                      <Search className="w-4 h-4 text-slate-400 shrink-0" />
                      <input
                        type="text"
                        placeholder="Enter City, State, or Area (e.g. Mumbai, BKC, Bengaluru)..."
                        value={city}
                        onChange={(e) => setCity(e.target.value)}
                        className="w-full bg-transparent border-none text-xs text-slate-900 dark:text-white focus:outline-none"
                      />
                    </div>
                    {city && (
                      <Button variant="secondary" size="sm" onClick={() => setCity('')} className="rounded-2xl text-xs">
                        Clear
                      </Button>
                    )}
                  </div>

                  {/* Popular Cities */}
                  <div className="space-y-1.5">
                    <label className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Metropolitan Hubs:</label>
                    <div className="flex flex-wrap gap-2">
                      {popularCities.map((c) => (
                        <button
                          key={c}
                          type="button"
                          onClick={() => setCity(city === c ? '' : c)}
                          className={clsx(
                            'px-3 py-1.5 rounded-xl text-xs font-semibold transition-all border',
                            city === c
                              ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs'
                              : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 border-slate-200 dark:border-slate-700 hover:bg-slate-200'
                          )}
                        >
                          {c}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Distance Radius Preference */}
                  <div className="space-y-1.5 pt-2">
                    <label className="text-[11px] font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                      <Navigation className="w-3.5 h-3.5 text-indigo-500" />
                      <span>Proximity / Service Mode:</span>
                    </label>
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                      {[
                        { id: '5KM', label: 'Hyperlocal (< 5 km)', desc: 'In-person visits' },
                        { id: '15KM', label: 'Metro Area (< 15 km)', desc: 'City-wide firm' },
                        { id: '50KM', label: 'Regional (< 50 km)', desc: 'State representation' },
                        { id: 'ANY', label: 'Pan-India Digital', desc: '100% Remote / Cloud' },
                      ].map((dist) => (
                        <button
                          key={dist.id}
                          type="button"
                          onClick={() => setDistanceRadius(dist.id)}
                          className={clsx(
                            'p-2.5 rounded-xl text-left border transition-all',
                            distanceRadius === dist.id
                              ? 'bg-indigo-50 dark:bg-indigo-950/60 border-indigo-500 text-indigo-700 dark:text-indigo-300 font-bold'
                              : 'bg-slate-50 dark:bg-slate-800/60 border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400'
                          )}
                        >
                          <div className="text-xs font-bold">{dist.label}</div>
                          <div className="text-[10px] text-slate-400">{dist.desc}</div>
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="flex justify-end pt-2">
                    <Button variant="primary" size="sm" onClick={() => setWizardStep(2)} className="rounded-xl">
                      <span>Next: Tax Requirement</span>
                      <ArrowRight className="w-3.5 h-3.5 ml-1.5" />
                    </Button>
                  </div>
                </div>
              )}

              {/* Step 2: Requirement & Taxpayer Constitution */}
              {wizardStep === 2 && (
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                        <Briefcase className="w-4 h-4 text-indigo-600" />
                        <span>Step 2: Select Tax Requirement & Entity Type</span>
                      </h3>
                      <p className="text-xs text-slate-500">Pick your compliance category to match with verified domain specialists.</p>
                    </div>
                  </div>

                  {/* Taxpayer Entity Classification */}
                  <div className="space-y-1.5">
                    <label className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Your Taxpayer Constitution:</label>
                    <div className="grid grid-cols-2 sm:grid-cols-5 gap-2">
                      {taxpayerEntities.map((ent) => (
                        <button
                          key={ent.id}
                          type="button"
                          onClick={() => setTaxpayerEntity(ent.id)}
                          className={clsx(
                            'p-2 rounded-xl text-center border transition-all',
                            taxpayerEntity === ent.id
                              ? 'bg-indigo-600 text-white border-indigo-600 shadow-md font-bold'
                              : 'bg-slate-50 dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-100'
                          )}
                        >
                          <div className="text-base mb-0.5">{ent.icon}</div>
                          <div className="text-[11px] font-bold leading-tight">{ent.label}</div>
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Service Requirement Chips */}
                  <div className="space-y-1.5 pt-2">
                    <label className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Primary Tax Need:</label>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                      {specializationsList.map((spec) => (
                        <button
                          key={spec.id}
                          type="button"
                          onClick={() => setSpecialization(specialization === spec.id ? '' : spec.id)}
                          className={clsx(
                            'p-3 rounded-xl text-left border transition-all flex items-start justify-between',
                            specialization === spec.id
                              ? 'bg-indigo-50 dark:bg-indigo-950/60 border-indigo-600 text-indigo-900 dark:text-indigo-200 font-bold shadow-xs'
                              : 'bg-slate-50 dark:bg-slate-800/60 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-100'
                          )}
                        >
                          <div>
                            <div className="text-xs font-bold">{spec.label}</div>
                            <div className="text-[10px] text-slate-500">{spec.desc}</div>
                          </div>
                          {specialization === spec.id && (
                            <CheckCircle2 className="w-4 h-4 text-indigo-600 shrink-0 mt-0.5" />
                          )}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-2">
                    <Button variant="secondary" size="sm" onClick={() => setWizardStep(1)} className="rounded-xl">
                      <ArrowLeft className="w-3.5 h-3.5 mr-1.5" />
                      <span>Back to Location</span>
                    </Button>
                    <Button variant="primary" size="sm" onClick={() => setWizardStep(3)} className="rounded-xl">
                      <span>Next: Sort & Discover</span>
                      <ArrowRight className="w-3.5 h-3.5 ml-1.5" />
                    </Button>
                  </div>
                </div>
              )}

              {/* Step 3: Match & Sort (Profile, Rating, Distance, Fees) */}
              {wizardStep === 3 && (
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                        <SlidersHorizontal className="w-4 h-4 text-emerald-500" />
                        <span>Step 3: Rank & View Matching Professionals</span>
                      </h3>
                      <p className="text-xs text-slate-500">
                        {totalElements} verified tax professionals matched your location ({city || 'All India'}) & requirement ({specialization ? specialization.replace(/_/g, ' ') : 'General Tax Advisory'}).
                      </p>
                    </div>
                  </div>

                  {/* Sort Options Grid (Profile, Rating, Distance, Fees) */}
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                    {[
                      { id: 'averageRating', label: 'Highest Rating (★)', desc: 'Top rated 4.8 - 5.0' },
                      { id: 'experienceYears', label: 'Most Experienced', desc: '10+ years in practice' },
                      { id: 'startingFee', label: 'Lowest Starting Fee', desc: 'Transparent packages' },
                      { id: 'distance', label: 'Nearest Distance', desc: 'Closest office proximity' },
                    ].map((s) => (
                      <button
                        key={s.id}
                        type="button"
                        onClick={() => setSortBy(s.id)}
                        className={clsx(
                          'p-3 rounded-xl text-left border transition-all',
                          sortBy === s.id
                            ? 'bg-emerald-50 dark:bg-emerald-950/60 border-emerald-500 text-emerald-800 dark:text-emerald-300 font-bold shadow-xs'
                            : 'bg-slate-50 dark:bg-slate-800/60 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300'
                        )}
                      >
                        <div className="text-xs font-bold">{s.label}</div>
                        <div className="text-[10px] text-slate-400">{s.desc}</div>
                      </button>
                    ))}
                  </div>

                  {/* Verification Filter */}
                  <div className="flex items-center justify-between p-3 bg-slate-50 dark:bg-slate-800/50 rounded-xl border border-slate-200 dark:border-slate-700">
                    <div className="flex items-center gap-2">
                      <ShieldCheck className="w-4 h-4 text-emerald-600" />
                      <span className="text-xs font-bold">Only Show ICAI / ICSI COP Verified Practitioners</span>
                    </div>
                    <input
                      type="checkbox"
                      checked={verifiedOnly}
                      onChange={(e) => setVerifiedOnly(e.target.checked)}
                      className="w-4 h-4 text-emerald-600 rounded border-slate-300 focus:ring-emerald-500"
                    />
                  </div>

                  <div className="flex items-center justify-between pt-2">
                    <Button variant="secondary" size="sm" onClick={() => setWizardStep(2)} className="rounded-xl">
                      <ArrowLeft className="w-3.5 h-3.5 mr-1.5" />
                      <span>Back to Requirement</span>
                    </Button>
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => {
                        const el = document.getElementById('results-section');
                        if (el) el.scrollIntoView({ behavior: 'smooth' });
                      }}
                      className="rounded-xl bg-emerald-600 hover:bg-emerald-700"
                    >
                      <Check className="w-3.5 h-3.5 mr-1.5" />
                      <span>Explore {totalElements} Matched Professionals</span>
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Directory Search Mode */}
          {activeTab === 'DIRECTORY' && (
            <div className="space-y-3 max-w-4xl mx-auto">
              <form
                onSubmit={handleSearchSubmit}
                className="bg-white dark:bg-slate-900 p-3 sm:p-4 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 flex flex-col md:flex-row gap-3"
              >
                <div className="flex-1 flex items-center gap-3 px-4 py-2.5 bg-slate-50 dark:bg-slate-800/60 rounded-2xl border border-slate-100 dark:border-slate-700/50">
                  <Search className="w-5 h-5 text-indigo-500 shrink-0" />
                  <input
                    type="text"
                    placeholder="Search by firm name, CA name, or service (e.g. Apex Tax, Audit)..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    className="w-full bg-transparent border-none text-sm text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none"
                  />
                </div>

                <div className="flex-1 flex items-center gap-3 px-4 py-2.5 bg-slate-50 dark:bg-slate-800/60 rounded-2xl border border-slate-100 dark:border-slate-700/50">
                  <MapPin className="w-5 h-5 text-emerald-500 shrink-0" />
                  <input
                    type="text"
                    placeholder="City, State or Pincode (e.g. Bengaluru, 560001)..."
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    className="w-full bg-transparent border-none text-sm text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none"
                  />
                </div>

                <Button
                  type="button"
                  variant={userCoords ? 'primary' : 'outline'}
                  size="md"
                  onClick={userCoords ? handleClearLocation : handleUseMyLocation}
                  disabled={isLocating}
                  className="rounded-2xl px-4 py-3 shrink-0 flex items-center gap-2 text-xs font-bold"
                >
                  <Navigation className={clsx('w-4 h-4 text-rose-500', isLocating && 'animate-spin')} />
                  <span>{isLocating ? 'Locating...' : userCoords ? 'Location Active ✓' : 'Use My Location'}</span>
                </Button>

                <Button type="submit" variant="primary" size="md" className="rounded-2xl px-6 py-3 shrink-0">
                  <Search className="w-4 h-4 mr-2" />
                  Search Directory
                </Button>
              </form>

              {/* Active Geo Location Indicator & Radius Selector */}
              {userCoords && (
                <div className="flex flex-wrap items-center justify-between gap-3 bg-indigo-900/70 backdrop-blur-md px-4 py-2.5 rounded-2xl border border-indigo-500/30 text-white text-xs">
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                    <span>Searching within <strong>{radiusKm} km</strong> of your coordinates ({userCoords.latitude.toFixed(4)}, {userCoords.longitude.toFixed(4)})</span>
                    <button
                      type="button"
                      onClick={handleClearLocation}
                      className="ml-2 text-indigo-300 hover:text-white underline font-semibold"
                    >
                      Clear Location
                    </button>
                  </div>

                  <div className="flex items-center gap-1.5">
                    <span className="text-[11px] text-indigo-200">Radius:</span>
                    {[5, 10, 25, 50, 100].map((r) => (
                      <button
                        key={r}
                        type="button"
                        onClick={() => setRadiusKm(r)}
                        className={clsx(
                          'px-2.5 py-1 rounded-lg text-xs font-bold transition-all',
                          radiusKm === r
                            ? 'bg-white text-indigo-900 shadow-sm'
                            : 'bg-indigo-800/80 text-indigo-200 hover:bg-indigo-700'
                        )}
                      >
                        {r} km
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Location Error Alert */}
              {locationError && (
                <div className="flex items-center justify-between bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/50 p-3 rounded-2xl text-xs text-amber-800 dark:text-amber-300">
                  <span>{locationError}</span>
                  <button type="button" onClick={() => setLocationError(null)} className="text-amber-600 hover:text-amber-900 font-bold ml-2">
                    ✕
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* ========================================================================= */}
      {/* Main Directory & Results Section */}
      {/* ========================================================================= */}
      <div id="results-section" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-8">
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
          {/* Left Filter Column */}
          <div className="lg:col-span-1 space-y-6">
            <div className="bg-white dark:bg-slate-900 p-6 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-6 sticky top-6">
              <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
                <div className="flex items-center gap-2 font-bold text-slate-900 dark:text-white text-base">
                  <Filter className="w-4 h-4 text-indigo-600" />
                  <span>Filters</span>
                </div>
                {(city || professionalType || specialization || verifiedOnly || search) && (
                  <button
                    onClick={() => {
                      setCity('');
                      setProfessionalType('');
                      setSpecialization('');
                      setVerifiedOnly(false);
                      setSearch('');
                    }}
                    className="text-xs text-indigo-600 hover:underline font-semibold"
                  >
                    Reset All
                  </button>
                )}
              </div>

              {/* Verified Filter */}
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

              {/* Sort By Toggle */}
              <div className="space-y-2">
                <label className="text-xs font-bold uppercase tracking-wider text-slate-400">Sort Results By</label>
                <select
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs font-semibold"
                >
                  <option value="averageRating">Highest Rating (★ 5.0)</option>
                  <option value="experienceYears">Years of Experience</option>
                  <option value="startingFee">Starting Price (Lowest)</option>
                  <option value="distance">Proximity / Distance</option>
                </select>
              </div>

              {/* Professional Type */}
              <div className="space-y-2">
                <label className="text-xs font-bold uppercase tracking-wider text-slate-400">Professional Type</label>
                <div className="space-y-1">
                  <button
                    onClick={() => setProfessionalType('')}
                    className={clsx(
                      'w-full text-left px-3 py-1.5 rounded-xl text-xs font-medium transition-all',
                      !professionalType
                        ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-300 font-bold'
                        : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
                    )}
                  >
                    All Types
                  </button>
                  {profTypes.map((t) => (
                    <button
                      key={t.id}
                      onClick={() => setProfessionalType(t.id)}
                      className={clsx(
                        'w-full flex items-center justify-between px-3 py-1.5 rounded-xl text-xs font-medium transition-all',
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

              {/* Demo Data Button */}
              <div className="pt-4 border-t border-slate-100 dark:border-slate-800">
                <Button variant="outline" size="sm" onClick={handleSeedDemo} className="w-full text-xs">
                  <Sparkles className="w-3.5 h-3.5 mr-1.5 text-amber-500" />
                  Seed Demo Tax Firms
                </Button>
              </div>
            </div>
          </div>

          {/* Right Main Column: Results Cards */}
          <div className="lg:col-span-3 space-y-6">
            {/* Header / Compare Tray Bar */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
              <div>
                <h2 className="text-base font-bold text-slate-900 dark:text-white">
                  {totalElements} Verified Practitioners Matching Your Request
                </h2>
                <p className="text-xs text-slate-500">
                  Click <strong>Contact</strong> for an inquiry or <strong>Book</strong> for an instant strategy session.
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
                  <div key={n} className="bg-white dark:bg-slate-900 p-6 rounded-3xl border border-slate-200 dark:border-slate-800 animate-pulse h-80 space-y-4">
                    <div className="flex gap-4">
                      <div className="w-14 h-14 bg-slate-200 dark:bg-slate-800 rounded-2xl" />
                      <div className="space-y-2 flex-1">
                        <div className="h-4 bg-slate-200 dark:bg-slate-800 rounded w-3/4" />
                        <div className="h-3 bg-slate-200 dark:bg-slate-800 rounded w-1/2" />
                      </div>
                    </div>
                    <div className="h-20 bg-slate-100 dark:bg-slate-800/40 rounded-xl" />
                  </div>
                ))}
              </div>
            ) : profiles.length === 0 ? (
              <div className="bg-white dark:bg-slate-900 p-12 rounded-3xl border border-slate-200 dark:border-slate-800 text-center space-y-4">
                <Briefcase className="w-12 h-12 text-slate-400 mx-auto" />
                <h3 className="text-lg font-bold text-slate-900 dark:text-white">No Tax Professionals Found</h3>
                <p className="text-sm text-slate-500 max-w-md mx-auto">
                  Try broadening your location or specialization filters, or click below to seed realistic demo firms.
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
                        'bg-white dark:bg-slate-900 rounded-3xl border transition-all hover:shadow-xl flex flex-col justify-between overflow-hidden',
                        isSelected
                          ? 'border-indigo-600 ring-2 ring-indigo-500/20'
                          : 'border-slate-200 dark:border-slate-800'
                      )}
                    >
                      <div className="p-6 space-y-4">
                        {/* Header: Avatar, Name, Verified Badge */}
                        <div className="flex items-start justify-between gap-4">
                          <div className="flex items-center gap-3.5 min-w-0 flex-1">
                            <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white font-bold text-xl flex items-center justify-center shadow-md shrink-0">
                              {profile.displayName.charAt(0)}
                            </div>
                            <div className="min-w-0 flex-1">
                              <div className="flex items-center gap-1.5 flex-wrap">
                                <h3
                                  onClick={() => navigate(buildPracticeUrl(profile))}
                                  className="font-bold text-slate-900 dark:text-white text-lg sm:text-xl leading-tight hover:text-indigo-600 transition-colors cursor-pointer tracking-tight break-words"
                                >
                                  {profile.displayName}
                                </h3>
                                {profile.verificationStatus === 'VERIFIED' && (
                                  <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700 dark:text-emerald-300 bg-emerald-50 dark:bg-emerald-950/60 px-2 py-0.5 rounded-full border border-emerald-200 dark:border-emerald-800 shrink-0">
                                    <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                                    Verified
                                  </span>
                                )}
                              </div>
                              <div className="text-xs text-slate-500 flex items-center gap-2 mt-1 flex-wrap">
                                <span className="font-semibold text-slate-700 dark:text-slate-300">
                                  {profile.professionalType?.replace(/_/g, ' ')}
                                </span>
                                <span>•</span>
                                <span>{profile.experienceYears} Yrs Exp.</span>
                              </div>
                            </div>
                          </div>

                          {/* Compare Toggle */}
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

                        {/* Location, Distance & Rating Strip */}
                        <div className="flex items-center justify-between text-xs py-2 px-3 bg-slate-50 dark:bg-slate-800/50 rounded-xl border border-slate-100 dark:border-slate-800 flex-wrap gap-2">
                          <div className="flex items-center gap-1.5 text-slate-600 dark:text-slate-400 flex-wrap">
                            <MapPin className="w-3.5 h-3.5 text-rose-500 shrink-0" />
                            <span className="font-medium text-slate-900 dark:text-white">
                              {profile.nearestLocation?.city || profile.city || 'India'}
                            </span>
                            {profile.distanceKm !== undefined && profile.distanceKm !== null && (
                              <span className="font-bold text-indigo-700 dark:text-indigo-300 bg-indigo-100 dark:bg-indigo-950/80 px-2 py-0.5 rounded-md text-[11px] border border-indigo-200 dark:border-indigo-800">
                                📍 {profile.distanceKm} km away
                              </span>
                            )}
                            {profile.nearestLocation && (
                              <span className="text-[10px] text-slate-400 font-normal">
                                ({profile.nearestLocation.locationName})
                              </span>
                            )}
                          </div>
                          <div className="flex items-center gap-1 font-bold text-amber-600 dark:text-amber-400 shrink-0">
                            <Star className="w-3.5 h-3.5 fill-current" />
                            <span>{profile.averageRating?.toFixed(1) || '5.0'}</span>
                            <span className="text-slate-400 font-normal">({profile.totalReviews || 0} reviews)</span>
                          </div>
                        </div>

                        {/* Standardized Services Offered / Specializations */}
                        {profile.offeredServices && profile.offeredServices.length > 0 ? (
                          <div className="flex flex-wrap gap-1.5">
                            {profile.offeredServices.slice(0, 3).map((svc) => (
                              <span
                                key={svc.id}
                                className="text-[11px] font-medium bg-indigo-50/70 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-300 px-2.5 py-0.5 rounded-lg border border-indigo-100 dark:border-indigo-900/30"
                              >
                                {svc.name}
                              </span>
                            ))}
                            {profile.offeredServices.length > 3 && (
                              <span className="text-[11px] text-slate-400 font-medium px-1">
                                +{profile.offeredServices.length - 3} more
                              </span>
                            )}
                          </div>
                        ) : profile.specializations && profile.specializations.length > 0 ? (
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
                        ) : null}
                      </div>

                      {/* Card Footer: Pricing & Action Buttons (Contact / Book) */}
                      <div className="p-4 bg-slate-50/80 dark:bg-slate-800/30 border-t border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                        <div>
                          <div className="text-[10px] uppercase font-bold text-slate-400">Starting Fee</div>
                          <div className="text-sm font-extrabold text-slate-900 dark:text-white">
                            ₹{profile.startingFee?.toLocaleString('en-IN') || '999'}
                            {profile.consultationFee && (
                              <span className="text-[10px] text-slate-400 font-normal ml-1">
                                (Slot: ₹{profile.consultationFee})
                              </span>
                            )}
                          </div>
                        </div>

                        {/* Direct Conversion Actions: Contact vs Book */}
                        <div className="flex items-center gap-2 w-full sm:w-auto">
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => {
                              setSelectedProfileForContact(profile);
                              setContactForm({
                                ...contactForm,
                                city: profile.city || '',
                                serviceCategory: specialization ? specialization.replace(/_FILING|_COMPLIANCE/g, '') : 'GST',
                              });
                            }}
                            className="text-xs rounded-xl flex-1 sm:flex-initial"
                          >
                            <MessageSquare className="w-3.5 h-3.5 mr-1" />
                            Contact
                          </Button>

                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => {
                              setSelectedProfileForBooking(profile);
                            }}
                            className="text-xs rounded-xl flex-1 sm:flex-initial bg-indigo-600 hover:bg-indigo-700"
                          >
                            <Calendar className="w-3.5 h-3.5 mr-1" />
                            Book Slot
                          </Button>

                          <button
                            onClick={() => navigate(buildPracticeUrl(profile))}
                            title="View Full Profile"
                            className="p-2 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 transition-colors"
                          >
                            <ArrowRight className="w-4 h-4" />
                          </button>
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

      {/* ========================================================================= */}
      {/* 1. Quick Contact / Inquiry Modal (Creates Inbound Lead) */}
      {/* ========================================================================= */}
      {selectedProfileForContact && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white dark:bg-slate-900 rounded-t-3xl sm:rounded-3xl border border-slate-200 dark:border-slate-800 max-w-lg w-full p-6 space-y-4 shadow-2xl max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-2xl bg-indigo-50 text-indigo-600">
                  <MessageSquare className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900 dark:text-white">
                    Contact {selectedProfileForContact.displayName}
                  </h3>
                  <p className="text-xs text-slate-500">Send your tax requirement for proposal review.</p>
                </div>
              </div>
              <button onClick={() => setSelectedProfileForContact(null)} className="text-gray-400 hover:text-gray-600 font-bold">&times;</button>
            </div>

            <form onSubmit={handleContactSubmit} className="space-y-3">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Your Full Name *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Vikram Singhania"
                    value={contactForm.clientName}
                    onChange={(e) => setContactForm({ ...contactForm, clientName: e.target.value })}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Phone Number *</label>
                  <input
                    type="tel"
                    required
                    placeholder="+91 98765 43210"
                    value={contactForm.clientPhone}
                    onChange={(e) => setContactForm({ ...contactForm, clientPhone: e.target.value })}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Business / Personal Email *</label>
                <input
                  type="email"
                  required
                  placeholder="vikram@enterprise.in"
                  value={contactForm.clientEmail}
                  onChange={(e) => setContactForm({ ...contactForm, clientEmail: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Tax Service Category</label>
                  <select
                    value={contactForm.serviceCategory}
                    onChange={(e) => setContactForm({ ...contactForm, serviceCategory: e.target.value })}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  >
                    <option value="GST">GST Filings & Advisory</option>
                    <option value="ITR">Income Tax (ITR)</option>
                    <option value="TDS">TDS & Form 26Q</option>
                    <option value="AUDIT">Tax & Statutory Audit</option>
                    <option value="INCORPORATION">Company Incorporation</option>
                    <option value="NOTICES">Tax Notice Defense</option>
                  </select>
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Urgency</label>
                  <select
                    value={contactForm.urgency}
                    onChange={(e) => setContactForm({ ...contactForm, urgency: e.target.value })}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  >
                    <option value="STANDARD">Standard (within 2-3 days)</option>
                    <option value="URGENT">Urgent (within 24 hours)</option>
                    <option value="LOW">Flexible Timeline</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Describe Your Requirement *</label>
                <textarea
                  rows={3}
                  required
                  placeholder="e.g. We require monthly GST filing (GSTR-1, 3B) and quarterly advance tax computation for our private limited company."
                  value={contactForm.requirementDescription}
                  onChange={(e) => setContactForm({ ...contactForm, requirementDescription: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t">
                <Button variant="secondary" size="sm" onClick={() => setSelectedProfileForContact(null)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" disabled={isSubmitting}>
                  {isSubmitting ? 'Sending...' : 'Transmit Requirement to Practitioner'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 2. Book Consultation Modal (Creates Appointment & Lead) */}
      {/* ========================================================================= */}
      {selectedProfileForBooking && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white dark:bg-slate-900 rounded-t-3xl sm:rounded-3xl border border-slate-200 dark:border-slate-800 max-w-lg w-full p-6 space-y-4 shadow-2xl max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-2xl bg-indigo-50 text-indigo-600">
                  <Calendar className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900 dark:text-white">
                    Book 30-Min Strategy Session
                  </h3>
                  <p className="text-xs text-slate-500">With {selectedProfileForBooking.displayName} (Fee: ₹{selectedProfileForBooking.consultationFee || 999})</p>
                </div>
              </div>
              <button onClick={() => setSelectedProfileForBooking(null)} className="text-gray-400 hover:text-gray-600 font-bold">&times;</button>
            </div>

            <form onSubmit={handleBookingSubmit} className="space-y-3">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Your Full Name *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Anita Deshmukh"
                    value={bookingForm.clientName}
                    onChange={(e) => setBookingForm({ ...bookingForm, clientName: e.target.value })}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Phone Number *</label>
                  <input
                    type="tel"
                    required
                    placeholder="+91 98220 12345"
                    value={bookingForm.clientPhone}
                    onChange={(e) => setBookingForm({ ...bookingForm, clientPhone: e.target.value })}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Email Address *</label>
                <input
                  type="email"
                  required
                  placeholder="anita@company.com"
                  value={bookingForm.clientEmail}
                  onChange={(e) => setBookingForm({ ...bookingForm, clientEmail: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Date *</label>
                  <input
                    type="date"
                    required
                    value={bookingForm.bookingDate}
                    onChange={(e) => setBookingForm({ ...bookingForm, bookingDate: e.target.value })}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Time Slot *</label>
                  <select
                    value={bookingForm.startTime}
                    onChange={(e) => setBookingForm({ ...bookingForm, startTime: e.target.value })}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  >
                    <option value="10:00">10:00 AM</option>
                    <option value="11:30">11:30 AM</option>
                    <option value="14:00">02:00 PM</option>
                    <option value="15:00">03:00 PM</option>
                    <option value="16:30">04:30 PM</option>
                    <option value="18:00">06:00 PM</option>
                  </select>
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Mode</label>
                  <select
                    value={bookingForm.consultationMode}
                    onChange={(e) => setBookingForm({ ...bookingForm, consultationMode: e.target.value })}
                    className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                  >
                    <option value="VIDEO">Google Meet</option>
                    <option value="PHONE">Phone Call</option>
                    <option value="IN_PERSON">In-Office</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Consultation Topic *</label>
                <input
                  type="text"
                  required
                  value={bookingForm.topic}
                  onChange={(e) => setBookingForm({ ...bookingForm, topic: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t">
                <Button variant="secondary" size="sm" onClick={() => setSelectedProfileForBooking(null)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" disabled={isSubmitting}>
                  {isSubmitting ? 'Confirming...' : 'Confirm Strategy Appointment'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 3. Action Success & Lead Tracking Confirmation Modal */}
      {/* ========================================================================= */}
      {actionSuccess && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white dark:bg-slate-900 rounded-t-3xl sm:rounded-3xl border border-slate-200 dark:border-slate-800 max-w-md w-full p-6 space-y-4 shadow-2xl text-center max-h-[90dvh] overflow-y-auto">
            <div className="w-14 h-14 rounded-2xl bg-emerald-50 text-emerald-600 mx-auto flex items-center justify-center shadow-md">
              <CheckCircle2 className="w-8 h-8" />
            </div>

            <div className="space-y-1">
              <h3 className="text-base font-bold text-slate-900 dark:text-white">
                {actionSuccess.title}
              </h3>
              <p className="text-xs text-slate-500 leading-relaxed">
                {actionSuccess.message}
              </p>
            </div>

            <div className="p-3 bg-slate-50 dark:bg-slate-800 rounded-2xl border border-slate-200 dark:border-slate-700 text-left space-y-1.5">
              <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Pipeline Tracking Reference:</div>
              <div className="text-xs font-mono font-bold text-indigo-600 dark:text-indigo-400 break-all">
                {actionSuccess.trackingId}
              </div>
              <div className="text-[10px] text-slate-400">
                Next: {actionSuccess.profileName} will review and provide a formal proposal / onboarding link.
              </div>
            </div>

            <div className="pt-2">
              <Button
                variant="primary"
                size="sm"
                onClick={() => setActionSuccess(null)}
                className="w-full rounded-xl"
              >
                Done
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
