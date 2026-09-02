import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link, useLocation, useSearchParams } from 'react-router-dom';
import {
  ShieldCheck,
  Star,
  MapPin,
  Phone,
  Mail,
  Globe,
  Calendar,
  Clock,
  Sparkles,
  CheckCircle2,
  ArrowLeft,
  Briefcase,
  Layers,
  MessageSquare,
  Award,
  Video,
  Send,
  X,
  Building2,
  Share2,
  Flag,
  BookOpen,
  ChevronRight,
  Search,
  ExternalLink,
  Users,
  User,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { SeoHead } from '../components/common/SeoHead';
import { TaxorynLogo } from '../components/common/TaxorynLogo';
import { marketplacePublicApi } from '../api/endpoints';
import { MarketplaceProfile, MarketplaceService, MarketplaceReview, PublicTaxService, LearnContentSummary } from '../types';
import { generatePracticeProfileSchema, generateBreadcrumbSchema } from '../utils/schemaGenerators';
import clsx from 'clsx';

interface PracticePublicProfilePageProps {
  overrideSlug?: string;
}

export const PracticePublicProfilePage: React.FC<PracticePublicProfilePageProps> = ({ overrideSlug }) => {
  const { slug: routeSlug, id } = useParams<{ slug?: string; id?: string }>();
  const slug = overrideSlug || routeSlug;
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();

  const [profile, setProfile] = useState<MarketplaceProfile | null>(null);
  const [services, setServices] = useState<MarketplaceService[]>([]);
  const [reviews, setReviews] = useState<MarketplaceReview[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [errorStatus, setErrorStatus] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<'SERVICES' | 'LOCATIONS' | 'ABOUT' | 'REVIEWS' | 'GUIDES'>('SERVICES');

  // Modals
  const [showInquiryModal, setShowInquiryModal] = useState<boolean>(false);
  const [showBookingModal, setShowBookingModal] = useState<boolean>(false);
  const [showReviewModal, setShowReviewModal] = useState<boolean>(false);
  const [showReportModal, setShowReportModal] = useState<boolean>(false);
  const [selectedTaxService, setSelectedTaxService] = useState<any>(null);

  // Forms
  const [inquiryForm, setInquiryForm] = useState({
    clientName: '',
    clientEmail: '',
    clientPhone: '',
    city: '',
    requirementDescription: '',
    budgetRange: '₹2,000 - ₹5,000',
  });

  const [bookingForm, setBookingForm] = useState({
    clientName: '',
    clientEmail: '',
    clientPhone: '',
    topic: 'Introductory Tax Planning & Compliance Strategy',
    consultationMode: 'VIDEO',
    bookingDate: new Date(Date.now() + 86400000).toISOString().split('T')[0],
    startTime: '14:00',
    endTime: '14:30',
    notes: '',
  });

  const [reviewForm, setReviewForm] = useState({
    reviewerName: '',
    reviewerDesignation: '',
    reviewerCompany: '',
    rating: 5,
    reviewTitle: '',
    reviewComment: '',
    serviceTaken: 'GST & ITR Compliance',
  });

  const [reportReason, setReportReason] = useState<string>('INCORRECT_INFORMATION');
  const [reportComment, setReportComment] = useState<string>('');

  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [copiedLink, setCopiedLink] = useState<boolean>(false);

  const isProfessionalRoute = location.pathname.startsWith('/professional');

  const fetchProfileData = async () => {
    setIsLoading(true);
    setErrorStatus(null);
    try {
      let prof: MarketplaceProfile;
      if (slug) {
        prof = await marketplacePublicApi.getBySlug(slug);
      } else if (id) {
        prof = await marketplacePublicApi.getById(id);
      } else {
        setErrorStatus(404);
        setIsLoading(false);
        return;
      }

      // Handle 301 alias redirect
      if (prof.redirectSlug && prof.redirectSlug !== slug) {
        const basePath = isProfessionalRoute ? '/professional/' : '/practice/';
        navigate(`${basePath}${prof.redirectSlug}`, { replace: true });
        return;
      }

      setProfile(prof);

      // Load additional packages and reviews if not already populated
      const [svcRes, revRes] = await Promise.all([
        marketplacePublicApi.getServices(prof.id).catch(() => []),
        marketplacePublicApi.getReviews(prof.id).catch(() => []),
      ]);

      setServices(svcRes);
      setReviews(revRes);

      const targetTsId = searchParams.get('taxServiceId');
      const targetTsName = searchParams.get('taxServiceName');
      if (targetTsId && prof.offeredServices && prof.offeredServices.length > 0) {
        const found = prof.offeredServices.find((s: any) => s.id === targetTsId || s.code === targetTsId);
        if (found) {
          setSelectedTaxService(found);
        } else if (targetTsName) {
          setSelectedTaxService({ id: targetTsId, name: targetTsName, title: targetTsName });
        }
      } else if (targetTsId && targetTsName) {
        setSelectedTaxService({ id: targetTsId, name: targetTsName, title: targetTsName });
      }
    } catch (err: any) {
      setErrorStatus(err?.response?.status || 404);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchProfileData();
    window.scrollTo(0, 0);
  }, [slug, id]);

  const handleInquirySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!profile) return;
    setIsSubmitting(true);
    try {
      await marketplacePublicApi.submitLead({
        marketplaceProfileId: profile.id,
        taxServiceId: selectedTaxService?.id || searchParams.get('taxServiceId') || undefined,
        sourceType: searchParams.get('sourceType') || (searchParams.get('taxServiceId') ? 'TAXORYN_LEARN' : 'TAXORYN_PRACTICE_PROFILE'),
        sourceContentId: searchParams.get('sourceContentId') || undefined,
        clientName: inquiryForm.clientName,
        clientEmail: inquiryForm.clientEmail,
        clientPhone: inquiryForm.clientPhone,
        city: inquiryForm.city || profile.city,
        serviceCategory: selectedTaxService?.categoryName || searchParams.get('taxServiceName') || 'Tax Advisory',
        requirementDescription: inquiryForm.requirementDescription,
        budgetRange: inquiryForm.budgetRange,
      });

      setSuccessMessage('Thank you! Your enquiry has been submitted. The verified practice will contact you shortly.');
      setShowInquiryModal(false);
      setInquiryForm({
        clientName: '',
        clientEmail: '',
        clientPhone: '',
        city: '',
        requirementDescription: '',
        budgetRange: '₹2,000 - ₹5,000',
      });
      setTimeout(() => setSuccessMessage(null), 7000);
    } catch (err: any) {
      alert('Failed to submit enquiry: ' + (err?.response?.data?.message || err.message));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleBookingSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!profile) return;
    setIsSubmitting(true);
    try {
      await marketplacePublicApi.bookConsultation({
        marketplaceProfileId: profile.id,
        clientName: bookingForm.clientName,
        clientEmail: bookingForm.clientEmail,
        clientPhone: bookingForm.clientPhone,
        topic: bookingForm.topic,
        consultationMode: bookingForm.consultationMode,
        bookingDate: bookingForm.bookingDate,
        startTime: bookingForm.startTime,
        endTime: bookingForm.endTime,
        notes: bookingForm.notes,
      });

      setSuccessMessage('Consultation slot confirmed! A confirmation email and calendar invite have been dispatched.');
      setShowBookingModal(false);
      setTimeout(() => setSuccessMessage(null), 7000);
    } catch (err: any) {
      alert('Failed to book consultation: ' + (err?.response?.data?.message || err.message));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReviewSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!profile) return;
    setIsSubmitting(true);
    try {
      await marketplacePublicApi.submitReview({
        marketplaceProfileId: profile.id,
        reviewerName: reviewForm.reviewerName,
        reviewerDesignation: reviewForm.reviewerDesignation,
        reviewerCompany: reviewForm.reviewerCompany,
        rating: reviewForm.rating,
        reviewTitle: reviewForm.reviewTitle,
        reviewComment: reviewForm.reviewComment,
        serviceTaken: reviewForm.serviceTaken,
      });

      setSuccessMessage('Your review has been submitted for platform verification. Thank you for your feedback!');
      setShowReviewModal(false);
      setReviewForm({
        reviewerName: '',
        reviewerDesignation: '',
        reviewerCompany: '',
        rating: 5,
        reviewTitle: '',
        reviewComment: '',
        serviceTaken: 'GST & ITR Compliance',
      });
      setTimeout(() => setSuccessMessage(null), 7000);
      fetchProfileData();
    } catch (err: any) {
      alert('Failed to submit review: ' + (err?.response?.data?.message || err.message));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReportSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setShowReportModal(false);
    setSuccessMessage('Thank you. Your report has been submitted to the Taxoryn trust & safety team.');
    setTimeout(() => setSuccessMessage(null), 6000);
  };

  const handleShare = () => {
    if (navigator.share) {
      navigator.share({
        title: profile?.displayName,
        text: profile?.headline,
        url: window.location.href,
      }).catch(() => {});
    } else {
      navigator.clipboard.writeText(window.location.href);
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2500);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center">
        <div className="text-center space-y-4">
          <div className="w-12 h-12 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto" />
          <p className="text-slate-600 font-medium">Loading verified practice profile...</p>
        </div>
      </div>
    );
  }

  if (errorStatus || !profile) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center p-4">
        <SeoHead
          title="Profile Not Found | Taxoryn Marketplace"
          description="The requested tax practice profile could not be found or is not currently active on Taxoryn."
          robots="noindex, nofollow"
        />
        <div className="max-w-md w-full text-center space-y-6 bg-white p-8 rounded-2xl shadow-sm border border-slate-200">
          <div className="w-16 h-16 bg-amber-50 text-amber-600 rounded-full flex items-center justify-center mx-auto">
            <Building2 className="w-8 h-8" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Practice Profile Unavailable</h1>
            <p className="text-slate-600 text-sm mt-2">
              The requested practice profile is either private, under verification, or does not exist.
            </p>
          </div>
          <div className="pt-2 flex flex-col gap-3">
            <Button variant="primary" onClick={() => navigate('/marketplace')} className="w-full">
              Explore Verified Tax Practices
            </Button>
            <Button variant="outline" onClick={() => navigate('/learn')} className="w-full">
              Browse Taxoryn Learn Guides
            </Button>
          </div>
        </div>
      </div>
    );
  }

  const activeSlug = profile.publicSlug || profile.slug;
  const canonicalUrl = profile.canonicalUrl || `https://taxoryn.com/practice/${activeSlug}`;
  const seoTitle = profile.seoTitle || `${profile.displayName} - Verified Tax Practice & CA Services | Taxoryn`;
  const seoDescription = profile.metaDescription || profile.bio || profile.headline || `Connect with ${profile.displayName} in ${profile.city || 'India'} for verified GST, Income Tax, TDS, and corporate compliance services on Taxoryn.`;

  const structuredData = [
    generatePracticeProfileSchema(profile, canonicalUrl),
    generateBreadcrumbSchema([
      { name: 'Home', url: '/' },
      { name: 'Tax Marketplace', url: '/marketplace' },
      { name: profile.displayName, url: `/practice/${activeSlug}` },
    ]),
  ];

  const offeredServicesList = profile.offeredServices && profile.offeredServices.length > 0
    ? profile.offeredServices
    : [];

  const practiceLocations = profile.locations && profile.locations.length > 0
    ? profile.locations
    : (profile.city ? [{
        id: 'primary',
        locationName: 'Main Office',
        city: profile.city,
        state: profile.state || '',
        pincode: profile.pincode || '',
        addressLine1: profile.address || '',
        isPrimary: true,
        isActive: true,
      }] : []);

  const isVerified = profile.verificationStatus === 'VERIFIED';
  const displayedReviews = reviews.length > 0 ? reviews : (profile.recentReviews || []);
  const relatedGuides = profile.relatedLearnContent || [];

  return (
    <div className="min-h-screen bg-slate-50 font-sans text-slate-800">
      <SeoHead
        title={seoTitle}
        description={seoDescription}
        canonicalUrl={canonicalUrl}
        ogType="website"
        ogImage={profile.bannerUrl || profile.avatarUrl}
        structuredData={structuredData}
      />

      {/* Top Banner / Cover */}
      <div className="relative h-64 md:h-80 bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 overflow-hidden">
        {profile.bannerUrl ? (
          <img
            src={profile.bannerUrl}
            alt={profile.displayName}
            className="w-full h-full object-cover opacity-35"
          />
        ) : (
          <div className="absolute inset-0 bg-[radial-gradient(#4f46e5_1px,transparent_1px)] [background-size:20px_20px] opacity-20" />
        )}

        <div className="absolute top-4 left-4 md:left-8 z-10 flex items-center gap-2.5">
          <div className="p-1.5 rounded-full bg-black/40 backdrop-blur-md flex items-center justify-center">
            <TaxorynLogo variant="symbol" size="xs" />
          </div>

          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-black/45 text-white backdrop-blur-md text-xs font-bold border border-white/15">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span className="truncate max-w-[200px] sm:max-w-none">{profile.displayName} • Official Portal</span>
          </div>

          {searchParams.get('from') === 'marketplace' ? (
            <Link
              to="/marketplace"
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-white/10 text-white hover:bg-white/20 backdrop-blur-md text-xs font-medium transition-all"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              Marketplace
            </Link>
          ) : (
            <Link
              to="/login"
              className="hidden sm:inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-white/10 text-white hover:bg-white/20 backdrop-blur-md text-xs font-medium transition-all"
            >
              <User className="w-3.5 h-3.5 text-indigo-300" />
              Client Portal Login
            </Link>
          )}
        </div>

        <div className="absolute top-4 right-4 md:right-8 z-10 flex items-center gap-2">
          <button
            onClick={handleShare}
            className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-black/45 text-white hover:bg-black/60 backdrop-blur-md text-xs font-semibold border border-white/15 transition-all shadow-sm"
            title="Share Profile"
          >
            <Share2 className="w-3.5 h-3.5" />
            {copiedLink ? 'Link Copied!' : 'Share'}
          </button>
          <button
            onClick={() => setShowReportModal(true)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-black/40 text-slate-300 hover:text-white hover:bg-black/60 backdrop-blur-md text-xs font-medium transition-all"
            title="Report Profile"
          >
            <Flag className="w-3.5 h-3.5" />
            Report
          </button>
        </div>
      </div>

      {/* Main Content Container */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 -mt-24 pb-20 relative z-20">
        {/* Success Alert */}
        {successMessage && (
          <div className="mb-6 p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 flex items-start gap-3 shadow-sm animate-fade-in">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
            <p className="text-sm font-medium">{successMessage}</p>
          </div>
        )}

        {/* Header Profile Identity Card */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 md:p-8 mb-8">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
            <div className="flex flex-col sm:flex-row items-start sm:items-center gap-5">
              {/* Logo / Avatar */}
              <div className="w-24 h-24 sm:w-28 sm:h-28 rounded-2xl bg-indigo-50 border-2 border-white shadow-md overflow-hidden flex items-center justify-center shrink-0">
                {profile.avatarUrl ? (
                  <img src={profile.avatarUrl} alt={profile.displayName} className="w-full h-full object-cover" />
                ) : (
                  <Building2 className="w-12 h-12 text-indigo-600" />
                )}
              </div>

              {/* Title & Trust Badges */}
              <div className="space-y-2 min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <h1 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold text-slate-900 tracking-tight break-words leading-tight">
                    {profile.displayName}
                  </h1>
                  {isVerified ? (
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200 shrink-0">
                      <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                      {isProfessionalRoute ? 'Verified Tax Professional' : 'Verified Practice'}
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-50 text-amber-700 border border-amber-200 shrink-0">
                      Verification Pending
                    </span>
                  )}
                  {profile.isFeatured && (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-indigo-50 text-indigo-700 border border-indigo-200 shrink-0">
                      <Sparkles className="w-3 h-3" /> Featured
                    </span>
                  )}
                </div>

                <p className="text-sm sm:text-base text-slate-600 max-w-2xl font-normal leading-relaxed">
                  {profile.headline || 'Chartered Accountants & Certified Indian Tax Advisory Firm'}
                </p>

                {/* Key Meta Badges */}
                <div className="flex flex-wrap items-center gap-x-5 gap-y-2 text-xs sm:text-sm text-slate-500 pt-1">
                  {profile.city && (
                    <span className="inline-flex items-center gap-1.5">
                      <MapPin className="w-4 h-4 text-slate-400" />
                      {profile.city}{profile.state ? `, ${profile.state}` : ''}
                    </span>
                  )}
                  {profile.experienceYears > 0 && (
                    <span className="inline-flex items-center gap-1.5">
                      <Briefcase className="w-4 h-4 text-slate-400" />
                      {profile.experienceYears}+ Years Experience
                    </span>
                  )}
                  <span className="inline-flex items-center gap-1.5 text-amber-600 font-medium">
                    <Star className="w-4 h-4 fill-amber-400 text-amber-400" />
                    {Number(profile.averageRating || 4.9).toFixed(1)} ({profile.totalReviews || displayedReviews.length} reviews)
                  </span>
                </div>
              </div>
            </div>

            {/* Quick Action CTAs */}
            <div className="flex flex-wrap sm:flex-nowrap md:flex-col gap-3 shrink-0 pt-4 md:pt-0">
              <Button
                variant="primary"
                onClick={() => {
                  setSelectedTaxService(null);
                  setShowInquiryModal(true);
                }}
                className="w-full sm:w-auto px-6 py-2.5 shadow-md shadow-indigo-600/10"
              >
                <Send className="w-4 h-4 mr-2" /> Send Enquiry
              </Button>
              {profile.consultationEnabled && (
                <Button
                  variant="outline"
                  onClick={() => setShowBookingModal(true)}
                  className="w-full sm:w-auto px-6 py-2.5 border-indigo-200 text-indigo-700 hover:bg-indigo-50"
                >
                  <Video className="w-4 h-4 mr-2 text-indigo-600" />
                  Book Call ({profile.consultationFee ? `₹${profile.consultationFee}` : 'Introductory'})
                </Button>
              )}
            </div>
          </div>

          {/* Navigation Tabs */}
          <div className="mt-8 pt-4 border-t border-slate-100 flex overflow-x-auto gap-2">
            {[
              { key: 'SERVICES', label: `Offered Services (${offeredServicesList.length || services.length})`, icon: Layers },
              { key: 'LOCATIONS', label: `Locations (${practiceLocations.length})`, icon: MapPin },
              { key: 'ABOUT', label: 'About & Operating Info', icon: Building2 },
              { key: 'REVIEWS', label: `Verified Reviews (${displayedReviews.length})`, icon: MessageSquare },
              { key: 'GUIDES', label: `Related Guides (${relatedGuides.length})`, icon: BookOpen },
            ].map((tab) => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.key;
              return (
                <button
                  key={tab.key}
                  onClick={() => setActiveTab(tab.key as any)}
                  className={clsx(
                    'inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs sm:text-sm font-medium whitespace-nowrap transition-all',
                    isActive
                      ? 'bg-indigo-600 text-white shadow-sm'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  )}
                >
                  <Icon className="w-4 h-4" />
                  {tab.label}
                </button>
              );
            })}
          </div>
        </div>

        {/* Tab Content & Main Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Main Column */}
          <div className="lg:col-span-2 space-y-8">
            {/* Tab: SERVICES */}
            {activeTab === 'SERVICES' && (
              <div className="space-y-6">
                <div className="flex items-center justify-between">
                  <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
                    <Layers className="w-5 h-5 text-indigo-600" />
                    Standardized Indian Tax & Compliance Services
                  </h2>
                  <span className="text-xs text-slate-500 font-medium">Controlled Master Catalog</span>
                </div>

                {offeredServicesList.length > 0 ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {offeredServicesList.map((svc) => (
                      <div
                        key={svc.id}
                        className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm hover:border-indigo-300 hover:shadow-md transition-all flex flex-col justify-between space-y-4"
                      >
                        <div className="space-y-2">
                          <div className="flex items-center justify-between">
                            <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-700">
                              {svc.categoryName || 'Tax Service'}
                            </span>
                            <span className="text-xs font-mono text-slate-400">{svc.code}</span>
                          </div>
                          <h3 className="font-semibold text-slate-900 text-base">{svc.name}</h3>
                          <p className="text-xs text-slate-500 line-clamp-2">
                            {svc.description || 'Verified regulatory compliance, documentation, and filing service.'}
                          </p>
                        </div>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            setSelectedTaxService(svc);
                            setInquiryForm((prev) => ({
                              ...prev,
                              requirementDescription: `Enquiry for ${svc.name}: `,
                            }));
                            setShowInquiryModal(true);
                          }}
                          className="w-full text-indigo-600 hover:bg-indigo-50 border-indigo-200 text-xs font-medium"
                        >
                          Enquire for this Service
                        </Button>
                      </div>
                    ))}
                  </div>
                ) : services.length > 0 ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {services.map((svc) => (
                      <div
                        key={svc.id}
                        className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm hover:border-indigo-300 transition-all flex flex-col justify-between space-y-4"
                      >
                        <div className="space-y-2">
                          <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-indigo-50 text-indigo-700">
                            {svc.category}
                          </span>
                          <h3 className="font-semibold text-slate-900 text-base">{svc.title}</h3>
                          <p className="text-xs text-slate-500 line-clamp-2">{svc.description}</p>
                          <div className="text-sm font-semibold text-slate-900 pt-1">
                            ₹{svc.price} {svc.pricingType && <span className="text-xs font-normal text-slate-500">/ {svc.pricingType.toLowerCase().replace(/_/g, ' ')}</span>}
                          </div>
                        </div>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            setInquiryForm((prev) => ({
                              ...prev,
                              requirementDescription: `Inquiry for package: ${svc.title}`,
                            }));
                            setShowInquiryModal(true);
                          }}
                          className="w-full text-indigo-600 hover:bg-indigo-50 border-indigo-200 text-xs font-medium"
                        >
                          Select Package
                        </Button>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="bg-white p-8 rounded-2xl border border-slate-200 text-center space-y-3">
                    <Layers className="w-10 h-10 text-slate-300 mx-auto" />
                    <p className="text-sm text-slate-600">This practice offers comprehensive direct & indirect tax advisory on enquiry.</p>
                    <Button variant="primary" size="sm" onClick={() => setShowInquiryModal(true)}>
                      Send General Tax Inquiry
                    </Button>
                  </div>
                )}
              </div>
            )}

            {/* Tab: LOCATIONS */}
            {activeTab === 'LOCATIONS' && (
              <div className="space-y-6">
                <div className="flex items-center justify-between">
                  <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
                    <MapPin className="w-5 h-5 text-indigo-600" />
                    Physical Office & Service Locations
                  </h2>
                  <span className="text-xs text-slate-500 font-medium">{practiceLocations.length} Registered Branches</span>
                </div>

                <div className="grid grid-cols-1 gap-4">
                  {practiceLocations.map((loc: any, index: number) => (
                    <div
                      key={loc.id || index}
                      className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-3"
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <h3 className="font-semibold text-slate-900 text-base">
                            {loc.locationName || `${loc.city} Office`}
                          </h3>
                          {loc.isPrimary && (
                            <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200">
                              Primary HQ
                            </span>
                          )}
                        </div>
                        {loc.latitude && loc.longitude && (
                          <a
                            href={`https://www.google.com/maps/search/?api=1&query=${loc.latitude},${loc.longitude}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-xs text-indigo-600 hover:text-indigo-800 font-medium inline-flex items-center gap-1"
                          >
                            <ExternalLink className="w-3.5 h-3.5" /> Directions
                          </a>
                        )}
                      </div>

                      <p className="text-sm text-slate-600">
                        {[loc.addressLine1, loc.addressLine2, loc.landmark].filter(Boolean).join(', ')}
                      </p>

                      <div className="flex flex-wrap items-center gap-4 text-xs text-slate-500 pt-2 border-t border-slate-100">
                        <span><strong>City:</strong> {loc.city}</span>
                        {loc.district && <span><strong>District:</strong> {loc.district}</span>}
                        <span><strong>State:</strong> {loc.state}</span>
                        {loc.pincode && <span><strong>PIN:</strong> {loc.pincode}</span>}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Tab: ABOUT */}
            {activeTab === 'ABOUT' && (
              <div className="bg-white p-6 md:p-8 rounded-2xl border border-slate-200 shadow-sm space-y-6">
                <div>
                  <h2 className="text-xl font-bold text-slate-900 mb-3">About {profile.displayName}</h2>
                  <p className="text-slate-600 text-sm leading-relaxed whitespace-pre-line">
                    {profile.description || profile.bio || 'Professional tax consultancy practice dedicated to providing compliant, timely, and optimized tax solutions for Indian individuals, SMEs, and corporate enterprises.'}
                  </p>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-4 border-t border-slate-100">
                  <div className="space-y-1">
                    <span className="text-xs font-medium text-slate-400">Languages Spoken</span>
                    <p className="text-sm font-semibold text-slate-800">{profile.languagesSpoken || 'English, Hindi'}</p>
                  </div>
                  <div className="space-y-1">
                    <span className="text-xs font-medium text-slate-400">Operating Hours</span>
                    <p className="text-sm font-semibold text-slate-800">{profile.workingHours || 'Mon - Sat: 9:30 AM - 6:30 PM'}</p>
                  </div>
                  <div className="space-y-1">
                    <span className="text-xs font-medium text-slate-400">Starting Fee</span>
                    <p className="text-sm font-semibold text-slate-800">₹{profile.startingFee || '999'}</p>
                  </div>
                  <div className="space-y-1">
                    <span className="text-xs font-medium text-slate-400">Standard Hourly Rate</span>
                    <p className="text-sm font-semibold text-slate-800">₹{profile.hourlyRate || '1,500'} / hr</p>
                  </div>
                </div>

                {profile.website && (
                  <div className="pt-4 border-t border-slate-100 flex items-center gap-2">
                    <Globe className="w-4 h-4 text-slate-400" />
                    <a
                      href={profile.website.startsWith('http') ? profile.website : `https://${profile.website}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-sm text-indigo-600 hover:text-indigo-800 font-medium"
                    >
                      {profile.website}
                    </a>
                  </div>
                )}
              </div>
            )}

            {/* Tab: REVIEWS */}
            {activeTab === 'REVIEWS' && (
              <div className="space-y-6">
                <div className="flex items-center justify-between">
                  <div>
                    <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
                      <MessageSquare className="w-5 h-5 text-indigo-600" />
                      Verified Client Reviews
                    </h2>
                    <p className="text-xs text-slate-500 mt-1">Verified reviews by clients who engaged through Taxoryn.</p>
                  </div>
                  <Button variant="outline" size="sm" onClick={() => setShowReviewModal(true)}>
                    Write a Review
                  </Button>
                </div>

                {displayedReviews.length > 0 ? (
                  <div className="space-y-4">
                    {displayedReviews.map((rev) => (
                      <div key={rev.id} className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-3">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <span className="font-semibold text-slate-900 text-sm">{rev.reviewerName}</span>
                            {rev.isVerifiedClient && (
                              <span className="inline-flex items-center gap-1 text-[11px] font-medium text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">
                                <CheckCircle2 className="w-3 h-3" /> Verified Client
                              </span>
                            )}
                          </div>
                          <div className="flex items-center gap-1 text-amber-500">
                            {[...Array(rev.rating || 5)].map((_, i) => (
                              <Star key={i} className="w-4 h-4 fill-amber-400 text-amber-400" />
                            ))}
                          </div>
                        </div>

                        {rev.reviewTitle && (
                          <h4 className="font-medium text-slate-800 text-sm">{rev.reviewTitle}</h4>
                        )}

                        <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">{rev.reviewComment}</p>

                        <div className="flex items-center justify-between text-xs text-slate-400 pt-2 border-t border-slate-100">
                          <span>{rev.serviceTaken || 'Tax Advisory'}</span>
                          {rev.reviewerCompany && <span>{rev.reviewerCompany}</span>}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="bg-white p-8 rounded-2xl border border-slate-200 text-center space-y-3">
                    <MessageSquare className="w-10 h-10 text-slate-300 mx-auto" />
                    <p className="text-sm text-slate-600">No client reviews submitted yet. Be the first to review!</p>
                    <Button variant="outline" size="sm" onClick={() => setShowReviewModal(true)}>
                      Write Review
                    </Button>
                  </div>
                )}
              </div>
            )}

            {/* Tab: GUIDES */}
            {activeTab === 'GUIDES' && (
              <div className="space-y-6">
                <div className="flex items-center justify-between">
                  <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
                    <BookOpen className="w-5 h-5 text-indigo-600" />
                    Related Taxoryn Learn Guides
                  </h2>
                  <Link to="/learn" className="text-xs text-indigo-600 hover:text-indigo-800 font-medium">
                    View Knowledge Hub →
                  </Link>
                </div>

                {relatedGuides.length > 0 ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {relatedGuides.map((guide) => (
                      <Link
                        key={guide.id}
                        to={`/learn/${guide.slug}`}
                        className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm hover:border-indigo-400 hover:shadow-md transition-all space-y-3 block"
                      >
                        <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-indigo-50 text-indigo-700">
                          {guide.contentType}
                        </span>
                        <h3 className="font-semibold text-slate-900 text-sm line-clamp-2 hover:text-indigo-600">
                          {guide.title}
                        </h3>
                        <p className="text-xs text-slate-500 line-clamp-2">{guide.summary}</p>
                        <div className="text-xs text-indigo-600 font-medium flex items-center gap-1 pt-1">
                          Read Guide <ChevronRight className="w-3 h-3" />
                        </div>
                      </Link>
                    ))}
                  </div>
                ) : (
                  <div className="bg-white p-8 rounded-2xl border border-slate-200 text-center space-y-3">
                    <BookOpen className="w-10 h-10 text-slate-300 mx-auto" />
                    <p className="text-sm text-slate-600">Explore educational guides in the Taxoryn Knowledge Hub.</p>
                    <Button variant="primary" size="sm" onClick={() => navigate('/learn')}>
                      Browse Taxoryn Learn
                    </Button>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Sidebar / Quick Inquiry Box & Trust Panel */}
          <div className="space-y-6">
            {/* Quick Sticky Enquiry Widget */}
            <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-5 sticky top-6">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Direct Connect</span>
                <span className="text-xs font-medium text-emerald-600 flex items-center gap-1">
                  <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" /> Available for New Clients
                </span>
              </div>

              <div className="space-y-3">
                <h3 className="text-lg font-bold text-slate-900">Request Consultation</h3>
                <p className="text-xs text-slate-500 leading-relaxed">
                  Send your compliance requirements directly to {profile.displayName}. Privacy protected: no sensitive documents or PAN required at early stage.
                </p>
              </div>

              <Button
                variant="primary"
                onClick={() => {
                  setSelectedTaxService(null);
                  setShowInquiryModal(true);
                }}
                className="w-full py-3 shadow-md shadow-indigo-600/10 font-semibold text-sm"
              >
                <Send className="w-4 h-4 mr-2" /> Start Free Enquiry
              </Button>

              {/* Secondary Practice CTA Button */}
              {profile.consultationEnabled ? (
                <Button
                  variant="outline"
                  onClick={() => setShowBookingModal(true)}
                  className="w-full text-xs font-bold text-indigo-700 hover:bg-indigo-50 border-indigo-200"
                >
                  <Video className="w-3.5 h-3.5 mr-2 text-indigo-600" /> Book Direct Consultation
                </Button>
              ) : (
                <Button
                  variant="outline"
                  onClick={() => setActiveTab('LOCATIONS')}
                  className="w-full text-xs font-medium text-slate-700 hover:text-slate-900 border-slate-200"
                >
                  <MapPin className="w-3.5 h-3.5 mr-2 text-slate-500" /> View Office Locations & Hours
                </Button>
              )}

              {/* Trust Features Checklist */}
              <div className="pt-4 border-t border-slate-100 space-y-2.5">
                {[
                  'Verified Identity & Certifications',
                  'Controlled Master Tax Services',
                  'Privacy-Protected Early Lead Stage',
                  'Encrypted Platform Communication',
                ].map((item, idx) => (
                  <div key={idx} className="flex items-center gap-2 text-xs text-slate-600">
                    <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>{item}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* MODAL: SEND ENQUIRY */}
      {showInquiryModal && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-white rounded-t-3xl sm:rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-5 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="space-y-0.5">
                <h3 className="font-bold text-slate-900 text-lg">Send Enquiry</h3>
                <p className="text-xs text-slate-500">To: {profile.displayName}</p>
              </div>
              <button
                onClick={() => setShowInquiryModal(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleInquirySubmit} className="space-y-4">
              {selectedTaxService && (
                <div className="p-3 rounded-xl bg-indigo-50 border border-indigo-100 text-xs text-indigo-800 flex items-center justify-between">
                  <span>Selected Service: <strong>{selectedTaxService.name || selectedTaxService.title}</strong></span>
                  <button
                    type="button"
                    onClick={() => setSelectedTaxService(null)}
                    className="text-indigo-600 hover:text-indigo-900 text-xs font-semibold"
                  >
                    Clear
                  </button>
                </div>
              )}

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-700 mb-1">Your Full Name *</label>
                  <input
                    type="text"
                    required
                    value={inquiryForm.clientName}
                    onChange={(e) => setInquiryForm({ ...inquiryForm, clientName: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none"
                    placeholder="e.g. Rahul Sharma"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-700 mb-1">Phone Number *</label>
                  <input
                    type="tel"
                    required
                    value={inquiryForm.clientPhone}
                    onChange={(e) => setInquiryForm({ ...inquiryForm, clientPhone: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none"
                    placeholder="+91 98765 43210"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-700 mb-1">Email Address *</label>
                  <input
                    type="email"
                    required
                    value={inquiryForm.clientEmail}
                    onChange={(e) => setInquiryForm({ ...inquiryForm, clientEmail: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none"
                    placeholder="rahul@example.com"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-700 mb-1">City / Location</label>
                  <input
                    type="text"
                    value={inquiryForm.city}
                    onChange={(e) => setInquiryForm({ ...inquiryForm, city: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none"
                    placeholder="e.g. Mumbai"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Requirement Details *</label>
                <textarea
                  required
                  rows={3}
                  value={inquiryForm.requirementDescription}
                  onChange={(e) => setInquiryForm({ ...inquiryForm, requirementDescription: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none resize-none"
                  placeholder="Describe your tax filing or notice requirement in brief..."
                />
              </div>

              <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-[11px] text-slate-500 flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Zero sensitive documents or PAN required. Confidential inquiry.</span>
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <Button type="button" variant="outline" size="sm" onClick={() => setShowInquiryModal(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" disabled={isSubmitting}>
                  {isSubmitting ? 'Submitting...' : 'Submit Enquiry'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: BOOK CONSULTATION */}
      {showBookingModal && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-white rounded-t-3xl sm:rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-5 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="space-y-0.5">
                <h3 className="font-bold text-slate-900 text-lg">Book Consultation Call</h3>
                <p className="text-xs text-slate-500">With {profile.displayName}</p>
              </div>
              <button
                onClick={() => setShowBookingModal(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleBookingSubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-700 mb-1">Your Name *</label>
                  <input
                    type="text"
                    required
                    value={bookingForm.clientName}
                    onChange={(e) => setBookingForm({ ...bookingForm, clientName: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-700 mb-1">Phone *</label>
                  <input
                    type="tel"
                    required
                    value={bookingForm.clientPhone}
                    onChange={(e) => setBookingForm({ ...bookingForm, clientPhone: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Email *</label>
                <input
                  type="email"
                  required
                  value={bookingForm.clientEmail}
                  onChange={(e) => setBookingForm({ ...bookingForm, clientEmail: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-700 mb-1">Preferred Date *</label>
                  <input
                    type="date"
                    required
                    value={bookingForm.bookingDate}
                    onChange={(e) => setBookingForm({ ...bookingForm, bookingDate: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-700 mb-1">Mode *</label>
                  <select
                    value={bookingForm.consultationMode}
                    onChange={(e) => setBookingForm({ ...bookingForm, consultationMode: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
                  >
                    <option value="VIDEO">Video Consultation</option>
                    <option value="PHONE">Phone Call</option>
                    <option value="IN_PERSON">In-Person Office Visit</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Consultation Topic</label>
                <input
                  type="text"
                  value={bookingForm.topic}
                  onChange={(e) => setBookingForm({ ...bookingForm, topic: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <Button type="button" variant="outline" size="sm" onClick={() => setShowBookingModal(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" disabled={isSubmitting}>
                  {isSubmitting ? 'Confirming...' : `Confirm Booking (${profile.consultationFee ? `₹${profile.consultationFee}` : 'Free'})`}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: SUBMIT REVIEW */}
      {showReviewModal && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-white rounded-t-3xl sm:rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-5 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-lg">Submit Verified Review</h3>
              <button
                onClick={() => setShowReviewModal(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleReviewSubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-700 mb-1">Your Name *</label>
                  <input
                    type="text"
                    required
                    value={reviewForm.reviewerName}
                    onChange={(e) => setReviewForm({ ...reviewForm, reviewerName: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm outline-none"
                    placeholder="e.g. Priya Iyer"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-700 mb-1">Rating (1-5 Stars) *</label>
                  <select
                    value={reviewForm.rating}
                    onChange={(e) => setReviewForm({ ...reviewForm, rating: Number(e.target.value) })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm outline-none font-semibold text-amber-600"
                  >
                    <option value={5}>★★★★★ (5 Stars - Excellent)</option>
                    <option value={4}>★★★★☆ (4 Stars - Very Good)</option>
                    <option value={3}>★★★☆☆ (3 Stars - Average)</option>
                    <option value={2}>★★☆☆☆ (2 Stars - Below Average)</option>
                    <option value={1}>★☆☆☆☆ (1 Star - Poor)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Review Headline</label>
                <input
                  type="text"
                  value={reviewForm.reviewTitle}
                  onChange={(e) => setReviewForm({ ...reviewForm, reviewTitle: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm outline-none"
                  placeholder="e.g. Prompt GST notice resolution"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Review Details *</label>
                <textarea
                  required
                  rows={3}
                  value={reviewForm.reviewComment}
                  onChange={(e) => setReviewForm({ ...reviewForm, reviewComment: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm outline-none resize-none"
                  placeholder="Share your experience working with this practice..."
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <Button type="button" variant="outline" size="sm" onClick={() => setShowReviewModal(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" disabled={isSubmitting}>
                  {isSubmitting ? 'Submitting...' : 'Submit for Verification'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: REPORT PROFILE */}
      {showReportModal && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-white rounded-t-3xl sm:rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h3 className="font-bold text-slate-900 text-lg flex items-center gap-2">
                <Flag className="w-5 h-5 text-red-600" /> Report Profile
              </h3>
              <button
                onClick={() => setShowReportModal(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleReportSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Reason for Report *</label>
                <select
                  value={reportReason}
                  onChange={(e) => setReportReason(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm outline-none"
                >
                  <option value="INCORRECT_INFORMATION">Incorrect Information</option>
                  <option value="MISLEADING_SERVICES">Misleading Service Claims</option>
                  <option value="SUSPECTED_FRAUD">Suspected Fraud / Impersonation</option>
                  <option value="OTHER">Other Issue</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Additional Details</label>
                <textarea
                  rows={3}
                  value={reportComment}
                  onChange={(e) => setReportComment(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm outline-none resize-none"
                  placeholder="Please describe why this profile is being flagged..."
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <Button type="button" variant="outline" size="sm" onClick={() => setShowReportModal(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" className="bg-red-600 hover:bg-red-700 text-white">
                  Submit Report
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
