import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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
  UserPlus,
  Send,
  X,
  CreditCard,
  Building2,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { TaxorynLogo } from '../components/common/TaxorynLogo';
import { marketplacePublicApi } from '../api/endpoints';
import { MarketplaceProfile, MarketplaceService, MarketplaceReview } from '../types';
import clsx from 'clsx';

export const MarketplaceProfileDetailPage: React.FC = () => {
  const { id, slug } = useParams<{ id?: string; slug?: string }>();
  const navigate = useNavigate();

  const [profile, setProfile] = useState<MarketplaceProfile | null>(null);
  const [services, setServices] = useState<MarketplaceService[]>([]);
  const [reviews, setReviews] = useState<MarketplaceReview[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [activeTab, setActiveTab] = useState<'SERVICES' | 'ABOUT' | 'REVIEWS'>('SERVICES');

  // Modals
  const [showInquiryModal, setShowInquiryModal] = useState<boolean>(false);
  const [showBookingModal, setShowBookingModal] = useState<boolean>(false);
  const [showOnboardModal, setShowOnboardModal] = useState<boolean>(false);
  const [showReviewModal, setShowReviewModal] = useState<boolean>(false);
  const [selectedService, setSelectedService] = useState<MarketplaceService | null>(null);

  // Form States
  const [inquiryForm, setInquiryForm] = useState({
    clientName: '',
    clientEmail: '',
    clientPhone: '',
    city: '',
    pan: '',
    gstin: '',
    serviceCategory: 'GST',
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

  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const fetchProfileData = async () => {
    setIsLoading(true);
    try {
      let prof: MarketplaceProfile;
      if (slug) {
        prof = await marketplacePublicApi.getBySlug(slug);
      } else if (id) {
        prof = await marketplacePublicApi.getById(id);
      } else {
        return;
      }

      setProfile(prof);
      const [svcRes, revRes] = await Promise.all([
        marketplacePublicApi.getServices(prof.id).catch(() => []),
        marketplacePublicApi.getReviews(prof.id).catch(() => []),
      ]);
      setServices(svcRes || []);
      setReviews(revRes || []);
    } catch (err) {
      console.error('Failed to load profile details', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchProfileData();
  }, [id, slug]);

  const handleInquirySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!profile) return;
    setIsSubmitting(true);
    try {
      await marketplacePublicApi.submitLead({
        marketplaceProfileId: profile.id,
        serviceId: selectedService?.id,
        ...inquiryForm,
      });
      setSuccessMessage('Your requirement has been sent to the practitioner! They will contact you shortly.');
      setShowInquiryModal(false);
      setShowOnboardModal(false);
    } catch (err) {
      alert('Failed to submit inquiry. Please check your information and try again.');
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
        ...bookingForm,
      });
      setSuccessMessage(`Consultation confirmed for ${bookingForm.bookingDate} at ${bookingForm.startTime}! Meeting link has been sent to ${bookingForm.clientEmail}.`);
      setShowBookingModal(false);
    } catch (err) {
      alert('Failed to book consultation.');
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
        ...reviewForm,
      });
      setShowReviewModal(false);
      await fetchProfileData();
      setSuccessMessage('Thank you! Your verified rating & review has been published.');
    } catch (err) {
      alert('Failed to submit review.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex items-center justify-center">
        <div className="text-center space-y-3">
          <div className="animate-spin w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full mx-auto" />
          <p className="text-sm font-medium text-slate-500">Loading professional profile...</p>
        </div>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex items-center justify-center p-4">
        <div className="bg-white dark:bg-slate-900 p-8 rounded-3xl border border-slate-200 dark:border-slate-800 text-center space-y-4 max-w-md">
          <h2 className="text-xl font-bold text-slate-900 dark:text-white">Profile Not Found</h2>
          <p className="text-sm text-slate-500">The requested tax professional profile does not exist or has been unpublished.</p>
          <Button variant="primary" onClick={() => navigate('/marketplace')}>
            Back to Directory
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 pb-24">
      {/* Top Brand & Directory Navigation */}
      <nav className="bg-[#07152B] border-b border-white/10 px-4 sm:px-6 lg:px-8 py-3 sticky top-0 z-30 shadow-md">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-3">
            <TaxorynLogo variant="horizontal" theme="dark" size="sm" />
            <span className="hidden sm:inline-block text-[10px] font-bold uppercase tracking-widest text-[#00D1A3] bg-white/5 border border-[#00D1A3]/30 px-2.5 py-0.5 rounded-full">
              Marketplace
            </span>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/marketplace')}
              className="flex items-center gap-1.5 text-xs font-semibold text-slate-300 hover:text-white transition-colors py-1 px-2.5 rounded-lg hover:bg-white/10"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Back to Directory</span>
            </button>
            <Button size="sm" variant="outline" onClick={() => setShowReviewModal(true)} className="text-xs text-slate-200 border-slate-700 bg-white/5 hover:bg-white/10 hover:text-white">
              <Star className="w-3.5 h-3.5 mr-1 text-amber-400 fill-current" />
              Write a Review
            </Button>
          </div>
        </div>
      </nav>

      {/* Success Notification Banner */}
      {successMessage && (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-4">
          <div className="bg-emerald-50 dark:bg-emerald-950/50 p-4 rounded-2xl border border-emerald-200 dark:border-emerald-800 flex items-center justify-between text-sm text-emerald-800 dark:text-emerald-200">
            <div className="flex items-center gap-2 font-medium">
              <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
              <span>{successMessage}</span>
            </div>
            <button onClick={() => setSuccessMessage(null)} className="text-emerald-600 hover:text-emerald-800">
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Profile Hero Header */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-6">
        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 sm:p-8 shadow-sm space-y-6">
          <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
            <div className="flex items-center gap-5">
              <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-3xl bg-gradient-to-tr from-indigo-600 to-violet-600 text-white font-extrabold text-3xl sm:text-4xl flex items-center justify-center shadow-xl shrink-0">
                {profile.displayName.charAt(0)}
              </div>
              <div className="space-y-1.5 min-w-0 flex-1">
                <div className="flex items-center gap-2.5 flex-wrap">
                  <h1 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold text-slate-900 dark:text-white tracking-tight break-words leading-tight">
                    {profile.displayName}
                  </h1>
                  {profile.verificationStatus === 'VERIFIED' && (
                    <span className="inline-flex items-center gap-1.5 text-xs font-bold text-emerald-700 dark:text-emerald-300 bg-emerald-50 dark:bg-emerald-950/60 px-3 py-1 rounded-full border border-emerald-200 dark:border-emerald-800 shadow-sm shrink-0">
                      <ShieldCheck className="w-4 h-4 text-emerald-600" />
                      ICAI / ICSI Verified Practice
                    </span>
                  )}
                </div>

                <div className="text-sm font-semibold text-indigo-600 dark:text-indigo-400 flex items-center gap-2">
                  <span>{profile.professionalType?.replace(/_/g, ' ')}</span>
                  <span>•</span>
                  <span>{profile.experienceYears} Years Established</span>
                  <span>•</span>
                  <span className="text-slate-500">{profile.totalClientsServed}+ Clients Served</span>
                </div>

                <div className="flex items-center gap-4 text-xs text-slate-500 pt-1 flex-wrap">
                  <span className="flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5 text-rose-500" />
                    {profile.city}, {profile.state}
                  </span>
                  <span className="flex items-center gap-1 font-bold text-amber-600 dark:text-amber-400">
                    <Star className="w-3.5 h-3.5 fill-current" />
                    {profile.averageRating?.toFixed(1) || '5.0'} ({reviews.length} Verified Reviews)
                  </span>
                </div>
              </div>
            </div>

            {/* Quick Action Buttons */}
            <div className="flex flex-col sm:flex-row gap-3 w-full md:w-auto shrink-0">
              <Button
                variant="outline"
                size="md"
                onClick={() => {
                  setSelectedService(null);
                  setShowInquiryModal(true);
                }}
                className="rounded-2xl font-bold"
              >
                <MessageSquare className="w-4 h-4 mr-2" />
                Direct Inquiry
              </Button>

              <Button
                variant="primary"
                size="md"
                onClick={() => setShowBookingModal(true)}
                className="rounded-2xl font-bold bg-indigo-600 hover:bg-indigo-700 text-white shadow-lg shadow-indigo-600/20"
              >
                <Calendar className="w-4 h-4 mr-2" />
                Book Consultation (₹{profile.consultationFee})
              </Button>

              <Button
                variant="secondary"
                size="md"
                onClick={() => setShowOnboardModal(true)}
                className="rounded-2xl font-bold bg-emerald-600 hover:bg-emerald-700 text-white shadow-lg shadow-emerald-600/20"
              >
                <UserPlus className="w-4 h-4 mr-2" />
                Become a Client
              </Button>
            </div>
          </div>

          {/* Headline Quote */}
          {profile.headline && (
            <div className="p-4 bg-slate-50 dark:bg-slate-800/60 rounded-2xl border border-slate-100 dark:border-slate-800 text-sm text-slate-700 dark:text-slate-200 font-medium italic">
              "{profile.headline}"
            </div>
          )}

          {/* Controlled Services Offered / Specializations */}
          {profile.offeredServices && profile.offeredServices.length > 0 ? (
            <div className="flex flex-wrap items-center gap-2 pt-2">
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider mr-1">Services Offered:</span>
              {profile.offeredServices.map((s) => (
                <span
                  key={s.id}
                  className="text-xs font-semibold bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 px-3 py-1 rounded-xl border border-indigo-100 dark:border-indigo-900/40 flex items-center gap-1.5"
                >
                  <CheckCircle2 className="w-3.5 h-3.5 text-indigo-600 dark:text-indigo-400" />
                  <span>{s.name}</span>
                </span>
              ))}
            </div>
          ) : profile.specializations && profile.specializations.length > 0 ? (
            <div className="flex flex-wrap items-center gap-2 pt-2">
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider mr-1">Expertise:</span>
              {profile.specializations.map((s, i) => (
                <span
                  key={i}
                  className="text-xs font-semibold bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 px-3 py-1 rounded-xl border border-indigo-100 dark:border-indigo-900/40"
                >
                  {s.replace(/_/g, ' ')}
                </span>
              ))}
            </div>
          ) : null}
        </div>
      </div>

      {/* Profile Nav Tabs */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-8">
        <div className="flex items-center gap-2 border-b border-slate-200 dark:border-slate-800 pb-3">
          <button
            onClick={() => setActiveTab('SERVICES')}
            className={clsx(
              'px-5 py-2.5 rounded-2xl text-sm font-bold transition-all flex items-center gap-2',
              activeTab === 'SERVICES'
                ? 'bg-indigo-600 text-white shadow-md'
                : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
            )}
          >
            <Layers className="w-4 h-4" />
            <span>Service Packages & Pricing ({services.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('ABOUT')}
            className={clsx(
              'px-5 py-2.5 rounded-2xl text-sm font-bold transition-all flex items-center gap-2',
              activeTab === 'ABOUT'
                ? 'bg-indigo-600 text-white shadow-md'
                : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
            )}
          >
            <Briefcase className="w-4 h-4" />
            <span>About Practice & Office</span>
          </button>

          <button
            onClick={() => setActiveTab('REVIEWS')}
            className={clsx(
              'px-5 py-2.5 rounded-2xl text-sm font-bold transition-all flex items-center gap-2',
              activeTab === 'REVIEWS'
                ? 'bg-indigo-600 text-white shadow-md'
                : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
            )}
          >
            <Star className="w-4 h-4 fill-current text-amber-500" />
            <span>Client Reviews ({reviews.length})</span>
          </button>
        </div>

        {/* Tab 1: Service Packages Grid */}
        {activeTab === 'SERVICES' && (
          <div className="mt-6 space-y-6">
            {services.length === 0 ? (
              <div className="bg-white dark:bg-slate-900 p-12 rounded-3xl border border-slate-200 dark:border-slate-800 text-center space-y-3">
                <Layers className="w-10 h-10 text-slate-400 mx-auto" />
                <h3 className="text-base font-bold text-slate-900 dark:text-white">Custom Retainer Offerings</h3>
                <p className="text-xs text-slate-500">Contact this practitioner directly for customized corporate tax planning quotes.</p>
                <Button variant="primary" size="sm" onClick={() => setShowInquiryModal(true)}>
                  Request Quote
                </Button>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {services.map((svc) => (
                  <div
                    key={svc.id}
                    className="bg-white dark:bg-slate-900 p-6 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm flex flex-col justify-between hover:shadow-lg transition-all space-y-4"
                  >
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-[11px] font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/60 px-2.5 py-1 rounded-lg">
                          {svc.category}
                        </span>
                        <span className="text-xs text-slate-500 font-medium flex items-center gap-1">
                          <Clock className="w-3.5 h-3.5" />
                          {svc.deliveryDays} Days TAT
                        </span>
                      </div>

                      <h3 className="text-base font-bold text-slate-900 dark:text-white">{svc.title}</h3>
                      {svc.description && (
                        <p className="text-xs text-slate-600 dark:text-slate-300 line-clamp-3 leading-relaxed">
                          {svc.description}
                        </p>
                      )}

                      {svc.deliverables && (
                        <div className="pt-2 text-xs text-slate-500 space-y-1">
                          <span className="font-semibold text-slate-700 dark:text-slate-300">Deliverables:</span>
                          <p className="text-[11px] text-slate-600 dark:text-slate-400 bg-slate-50 dark:bg-slate-800/40 p-2.5 rounded-xl border border-slate-100 dark:border-slate-800">
                            {svc.deliverables}
                          </p>
                        </div>
                      )}
                    </div>

                    <div className="pt-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
                      <div>
                        <div className="text-[10px] uppercase font-bold text-slate-400">
                          {svc.pricingType === 'MONTHLY_RETAINER' ? 'Monthly Fee' : 'Fixed Fee'}
                        </div>
                        <div className="text-xl font-extrabold text-slate-900 dark:text-white">
                          ₹{svc.price?.toLocaleString('en-IN')}
                        </div>
                      </div>

                      <Button
                        size="sm"
                        variant="primary"
                        onClick={() => {
                          setSelectedService(svc);
                          setInquiryForm({ ...inquiryForm, serviceCategory: svc.category, requirementDescription: `Interested in: ${svc.title}` });
                          setShowInquiryModal(true);
                        }}
                      >
                        Order / Inquire
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Tab 2: About & Practice Bio */}
        {activeTab === 'ABOUT' && (
          <div className="mt-6 grid grid-cols-1 lg:grid-cols-3 gap-8">
            <div className="lg:col-span-2 space-y-6">
              <div className="bg-white dark:bg-slate-900 p-8 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
                <h3 className="text-lg font-bold text-slate-900 dark:text-white">Practice Biography</h3>
                <div className="text-sm text-slate-600 dark:text-slate-300 leading-relaxed space-y-3 whitespace-pre-line">
                  {profile.bio || 'Comprehensive accounting and taxation firm catering to Indian corporations, SMEs, and high-net-worth individuals.'}
                </div>
              </div>
            </div>

            {/* Office & Locations Info Card */}
            <div className="space-y-6">
              {/* Practice Locations */}
              <div className="bg-white dark:bg-slate-900 p-6 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
                    <Building2 className="w-4 h-4 text-indigo-600" />
                    <span>Office & Branch Locations</span>
                  </h3>
                  {profile.locations && profile.locations.length > 0 && (
                    <span className="text-[11px] font-extrabold bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-full">
                      {profile.locations.length} {profile.locations.length === 1 ? 'Office' : 'Offices'}
                    </span>
                  )}
                </div>

                {profile.locations && profile.locations.length > 0 ? (
                  <div className="space-y-3">
                    {profile.locations.map((loc) => (
                      <div
                        key={loc.id}
                        className={clsx(
                          'p-3.5 rounded-2xl border text-xs space-y-1',
                          loc.isPrimary
                            ? 'bg-indigo-50/40 dark:bg-indigo-950/30 border-indigo-200 dark:border-indigo-900/50'
                            : 'bg-slate-50/80 dark:bg-slate-800/40 border-slate-200 dark:border-slate-800'
                        )}
                      >
                        <div className="flex items-center justify-between gap-2">
                          <span className="font-bold text-slate-900 dark:text-white">
                            {loc.locationName}
                          </span>
                          {loc.isPrimary && (
                            <span className="text-[10px] font-extrabold text-indigo-700 bg-indigo-100 dark:bg-indigo-900/60 px-2 py-0.5 rounded-full">
                              Head Office
                            </span>
                          )}
                        </div>

                        <p className="text-slate-600 dark:text-slate-300">
                          {loc.addressLine1}
                          {loc.addressLine2 && `, ${loc.addressLine2}`}
                          {loc.landmark && ` (Near ${loc.landmark})`}
                        </p>

                        <p className="font-medium text-slate-700 dark:text-slate-200">
                          {loc.city}, {loc.state} - {loc.pincode}
                        </p>

                        {loc.latitude && loc.longitude && (
                          <p className="text-[10px] font-mono text-slate-400">
                            Coordinates: {loc.latitude}, {loc.longitude}
                          </p>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="space-y-3 text-xs text-slate-600 dark:text-slate-300">
                    {profile.address && (
                      <div className="flex items-start gap-2.5">
                        <MapPin className="w-4 h-4 text-rose-500 shrink-0 mt-0.5" />
                        <span>{profile.address}, {profile.city}, {profile.state} - {profile.pincode}</span>
                      </div>
                    )}
                  </div>
                )}
              </div>

              {/* Direct Contacts Card */}
              <div className="bg-white dark:bg-slate-900 p-6 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
                <h3 className="text-base font-bold text-slate-900 dark:text-white">Contact & Communication</h3>
                <div className="space-y-3 text-xs text-slate-600 dark:text-slate-300">
                  {profile.phone && (
                    <div className="flex items-center gap-2.5">
                      <Phone className="w-4 h-4 text-emerald-500 shrink-0" />
                      <span>{profile.phone}</span>
                    </div>
                  )}
                  {profile.email && (
                    <div className="flex items-center gap-2.5">
                      <Mail className="w-4 h-4 text-indigo-500 shrink-0" />
                      <span>{profile.email}</span>
                    </div>
                  )}
                  {profile.languagesSpoken && (
                    <div className="flex items-center gap-2.5 pt-2 border-t border-slate-100 dark:border-slate-800">
                      <span className="font-semibold text-slate-700 dark:text-slate-300">Languages:</span>
                      <span>{profile.languagesSpoken}</span>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tab 3: Client Reviews */}
        {activeTab === 'REVIEWS' && (
          <div className="mt-6 space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-lg font-bold text-slate-900 dark:text-white">
                  Verified Client Reviews ({reviews.length})
                </h3>
                <p className="text-xs text-slate-500">Reviews submitted by verified clients and businesses.</p>
              </div>
              <Button size="sm" variant="outline" onClick={() => setShowReviewModal(true)}>
                <Star className="w-3.5 h-3.5 mr-1.5 text-amber-500 fill-current" />
                Write a Review
              </Button>
            </div>

            {reviews.length === 0 ? (
              <div className="bg-white dark:bg-slate-900 p-12 rounded-3xl border border-slate-200 dark:border-slate-800 text-center space-y-3">
                <Star className="w-10 h-10 text-slate-400 mx-auto" />
                <h4 className="text-base font-bold text-slate-900 dark:text-white">No Reviews Yet</h4>
                <p className="text-xs text-slate-500">Be the first client to review this practitioner.</p>
                <Button size="sm" variant="primary" onClick={() => setShowReviewModal(true)}>
                  Add Review
                </Button>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {reviews.map((rev) => (
                  <div
                    key={rev.id}
                    className="bg-white dark:bg-slate-900 p-6 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-3"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-1 text-amber-500">
                        {[...Array(5)].map((_, i) => (
                          <Star
                            key={i}
                            className={clsx('w-4 h-4', i < rev.rating ? 'fill-current' : 'text-slate-200 dark:text-slate-700')}
                          />
                        ))}
                      </div>
                      <span className="text-[11px] text-slate-400 font-medium">
                        {new Date(rev.createdAt).toLocaleDateString('en-IN', { month: 'short', day: 'numeric', year: 'numeric' })}
                      </span>
                    </div>

                    {rev.reviewTitle && (
                      <h4 className="text-sm font-bold text-slate-900 dark:text-white">{rev.reviewTitle}</h4>
                    )}

                    <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
                      "{rev.reviewComment}"
                    </p>

                    <div className="pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs">
                      <div>
                        <div className="font-bold text-slate-900 dark:text-white">{rev.reviewerName}</div>
                        {(rev.reviewerDesignation || rev.reviewerCompany) && (
                          <div className="text-[11px] text-slate-400">
                            {rev.reviewerDesignation} {rev.reviewerCompany ? `• ${rev.reviewerCompany}` : ''}
                          </div>
                        )}
                      </div>
                      {rev.serviceTaken && (
                        <span className="text-[10px] font-semibold bg-slate-100 dark:bg-slate-800 px-2 py-0.5 rounded-md text-slate-600 dark:text-slate-400">
                          {rev.serviceTaken}
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Modal 1: Direct Inquiry Modal */}
      {showInquiryModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white dark:bg-slate-900 rounded-t-3xl sm:rounded-3xl border border-slate-200 dark:border-slate-800 max-w-lg w-full p-6 sm:p-8 space-y-6 shadow-2xl animate-in fade-in zoom-in-95 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
              <div>
                <h3 className="text-lg font-bold text-slate-900 dark:text-white">Direct Inquiry to {profile.displayName}</h3>
                <p className="text-xs text-slate-500">Provide your requirements for callback and custom quote.</p>
              </div>
              <button onClick={() => setShowInquiryModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Privacy Assurance Notice */}
            <div className="p-3 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800/60 rounded-2xl flex items-start gap-2.5 text-xs text-emerald-800 dark:text-emerald-300">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
              <span>
                <strong className="text-emerald-950 dark:text-emerald-200">Privacy Guaranteed:</strong> Early-stage inquiries disclose only your service category and sanitized requirement summary. Your PAN, salary, and documents remain strictly confidential.
              </span>
            </div>

            <form onSubmit={handleInquirySubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Your Full Name *</label>
                  <input
                    type="text"
                    required
                    value={inquiryForm.clientName}
                    onChange={(e) => setInquiryForm({ ...inquiryForm, clientName: e.target.value })}
                    className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    placeholder="Ananya Roy"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Phone Number *</label>
                  <input
                    type="tel"
                    required
                    value={inquiryForm.clientPhone}
                    onChange={(e) => setInquiryForm({ ...inquiryForm, clientPhone: e.target.value })}
                    className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    placeholder="+91 98765 43210"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Email Address *</label>
                <input
                  type="email"
                  required
                  value={inquiryForm.clientEmail}
                  onChange={(e) => setInquiryForm({ ...inquiryForm, clientEmail: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="ananya@company.com"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">City / Location</label>
                  <input
                    type="text"
                    value={inquiryForm.city}
                    onChange={(e) => setInquiryForm({ ...inquiryForm, city: e.target.value })}
                    className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    placeholder="Mumbai"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Service Category</label>
                  <select
                    value={inquiryForm.serviceCategory}
                    onChange={(e) => setInquiryForm({ ...inquiryForm, serviceCategory: e.target.value })}
                    className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  >
                    <option value="GST">GST Return Filing & Notice</option>
                    <option value="ITR">Income Tax (ITR) Filing</option>
                    <option value="TDS">TDS & Form 26Q/24Q</option>
                    <option value="COMPANY_FORMATION">Company Incorporation</option>
                    <option value="AUDIT">Statutory / Tax Audit</option>
                    <option value="STARTUP">Startup & Advisory</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Describe Your Requirements *</label>
                <textarea
                  required
                  rows={3}
                  value={inquiryForm.requirementDescription}
                  onChange={(e) => setInquiryForm({ ...inquiryForm, requirementDescription: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="e.g. Need monthly GST filing for our e-commerce business and annual ITR-3 submission..."
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-800">
                <Button type="button" variant="outline" size="sm" onClick={() => setShowInquiryModal(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" disabled={isSubmitting}>
                  <Send className="w-3.5 h-3.5 mr-1.5" />
                  {isSubmitting ? 'Sending...' : 'Send Inquiry'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal 2: Book Consultation Modal */}
      {showBookingModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white dark:bg-slate-900 rounded-t-3xl sm:rounded-3xl border border-slate-200 dark:border-slate-800 max-w-lg w-full p-6 sm:p-8 space-y-6 shadow-2xl animate-in fade-in zoom-in-95 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
              <div>
                <h3 className="text-lg font-bold text-slate-900 dark:text-white">Book Dedicated Consultation</h3>
                <p className="text-xs text-slate-500">30-min strategy session with {profile.displayName}.</p>
              </div>
              <button onClick={() => setShowBookingModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleBookingSubmit} className="space-y-4">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Your Full Name *</label>
                <input
                  type="text"
                  required
                  value={bookingForm.clientName}
                  onChange={(e) => setBookingForm({ ...bookingForm, clientName: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Email Address *</label>
                  <input
                    type="email"
                    required
                    value={bookingForm.clientEmail}
                    onChange={(e) => setBookingForm({ ...bookingForm, clientEmail: e.target.value })}
                    className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Phone Number *</label>
                  <input
                    type="tel"
                    required
                    value={bookingForm.clientPhone}
                    onChange={(e) => setBookingForm({ ...bookingForm, clientPhone: e.target.value })}
                    className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Date *</label>
                  <input
                    type="date"
                    required
                    value={bookingForm.bookingDate}
                    onChange={(e) => setBookingForm({ ...bookingForm, bookingDate: e.target.value })}
                    className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Mode *</label>
                  <select
                    value={bookingForm.consultationMode}
                    onChange={(e) => setBookingForm({ ...bookingForm, consultationMode: e.target.value })}
                    className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  >
                    <option value="VIDEO">Google Meet / Video Call</option>
                    <option value="PHONE">Phone Call</option>
                    <option value="IN_PERSON">In-Person Office Visit</option>
                  </select>
                </div>
              </div>

              <div className="p-4 bg-indigo-50 dark:bg-indigo-950/40 rounded-2xl border border-indigo-200 dark:border-indigo-800 flex items-center justify-between text-xs font-bold text-indigo-900 dark:text-indigo-200">
                <span>Session Fee (30 Mins):</span>
                <span className="text-base font-extrabold">₹{profile.consultationFee}</span>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-800">
                <Button type="button" variant="outline" size="sm" onClick={() => setShowBookingModal(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" disabled={isSubmitting}>
                  <CreditCard className="w-3.5 h-3.5 mr-1.5" />
                  {isSubmitting ? 'Confirming...' : `Confirm Booking (₹${profile.consultationFee})`}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal 3: 1-Click "Become Client" Onboarding */}
      {showOnboardModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white dark:bg-slate-900 rounded-t-3xl sm:rounded-3xl border border-slate-200 dark:border-slate-800 max-w-lg w-full p-6 sm:p-8 space-y-6 shadow-2xl animate-in fade-in zoom-in-95 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
              <div className="flex items-center gap-2.5">
                <div className="p-2 rounded-xl bg-emerald-50 text-emerald-600">
                  <UserPlus className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-slate-900 dark:text-white">Direct Client Onboarding</h3>
                  <p className="text-xs text-slate-500">Auto-provisions your account in {profile.displayName}'s CRM.</p>
                </div>
              </div>
              <button onClick={() => setShowOnboardModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleInquirySubmit} className="space-y-4">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Entity / Individual Name *</label>
                <input
                  type="text"
                  required
                  value={inquiryForm.clientName}
                  onChange={(e) => setInquiryForm({ ...inquiryForm, clientName: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  placeholder="e.g. Zenith Technologies Pvt Ltd"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">PAN (10 Characters)</label>
                  <input
                    type="text"
                    maxLength={10}
                    value={inquiryForm.pan}
                    onChange={(e) => setInquiryForm({ ...inquiryForm, pan: e.target.value.toUpperCase() })}
                    className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs uppercase font-mono focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    placeholder="AAACZ1234D"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">GSTIN (Optional)</label>
                  <input
                    type="text"
                    maxLength={15}
                    value={inquiryForm.gstin}
                    onChange={(e) => setInquiryForm({ ...inquiryForm, gstin: e.target.value.toUpperCase() })}
                    className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs uppercase font-mono focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    placeholder="27AAACZ1234D1Z8"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Contact Email *</label>
                  <input
                    type="email"
                    required
                    value={inquiryForm.clientEmail}
                    onChange={(e) => setInquiryForm({ ...inquiryForm, clientEmail: e.target.value })}
                    className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Phone Number *</label>
                  <input
                    type="tel"
                    required
                    value={inquiryForm.clientPhone}
                    onChange={(e) => setInquiryForm({ ...inquiryForm, clientPhone: e.target.value })}
                    className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Initial Tax & Compliance Scope *</label>
                <textarea
                  required
                  rows={2}
                  value={inquiryForm.requirementDescription}
                  onChange={(e) => setInquiryForm({ ...inquiryForm, requirementDescription: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  placeholder="Need monthly GST filing, quarterly TDS 26Q, and annual audit..."
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-800">
                <Button type="button" variant="outline" size="sm" onClick={() => setShowOnboardModal(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" disabled={isSubmitting} className="bg-emerald-600 hover:bg-emerald-700 text-white">
                  <UserPlus className="w-3.5 h-3.5 mr-1.5" />
                  {isSubmitting ? 'Registering...' : 'Register as Client'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal 4: Write Review Modal */}
      {showReviewModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white dark:bg-slate-900 rounded-t-3xl sm:rounded-3xl border border-slate-200 dark:border-slate-800 max-w-lg w-full p-6 sm:p-8 space-y-6 shadow-2xl animate-in fade-in zoom-in-95 max-h-[90dvh] overflow-y-auto">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
              <div>
                <h3 className="text-lg font-bold text-slate-900 dark:text-white">Write a Verified Review</h3>
                <p className="text-xs text-slate-500">Share your experience with {profile.displayName}.</p>
              </div>
              <button onClick={() => setShowReviewModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleReviewSubmit} className="space-y-4">
              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Rating *</label>
                <div className="flex items-center gap-2 mt-1.5">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      type="button"
                      onClick={() => setReviewForm({ ...reviewForm, rating: star })}
                      className="p-1"
                    >
                      <Star
                        className={clsx('w-6 h-6', star <= reviewForm.rating ? 'fill-current text-amber-500' : 'text-slate-300 dark:text-slate-700')}
                      />
                    </button>
                  ))}
                  <span className="text-xs font-bold text-slate-700 dark:text-slate-300 ml-2">
                    {reviewForm.rating} of 5 Stars
                  </span>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Your Name *</label>
                  <input
                    type="text"
                    required
                    value={reviewForm.reviewerName}
                    onChange={(e) => setReviewForm({ ...reviewForm, reviewerName: e.target.value })}
                    className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Company (Optional)</label>
                  <input
                    type="text"
                    value={reviewForm.reviewerCompany}
                    onChange={(e) => setReviewForm({ ...reviewForm, reviewerCompany: e.target.value })}
                    className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Review Headline</label>
                <input
                  type="text"
                  value={reviewForm.reviewTitle}
                  onChange={(e) => setReviewForm({ ...reviewForm, reviewTitle: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="e.g. Excellent GST support and prompt filing"
                />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300">Your Detailed Feedback *</label>
                <textarea
                  required
                  rows={3}
                  value={reviewForm.reviewComment}
                  onChange={(e) => setReviewForm({ ...reviewForm, reviewComment: e.target.value })}
                  className="w-full mt-1.5 px-3.5 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-800">
                <Button type="button" variant="outline" size="sm" onClick={() => setShowReviewModal(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" disabled={isSubmitting}>
                  {isSubmitting ? 'Submitting...' : 'Post Review'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
