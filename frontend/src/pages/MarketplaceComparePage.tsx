import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  ShieldCheck,
  Star,
  MapPin,
  Check,
  X,
  Calendar,
  MessageSquare,
  Sparkles,
  Award,
  Layers,
  Briefcase,
  CheckCircle2,
  Phone,
  Mail,
  Clock,
  Video,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { TaxorynLogo } from '../components/common/TaxorynLogo';
import { marketplacePublicApi } from '../api/endpoints';
import { MarketplaceProfile } from '../types';
import clsx from 'clsx';

export const MarketplaceComparePage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const [profiles, setProfiles] = useState<MarketplaceProfile[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Conversion Modals
  const [selectedProfileForContact, setSelectedProfileForContact] = useState<MarketplaceProfile | null>(null);
  const [selectedProfileForBooking, setSelectedProfileForBooking] = useState<MarketplaceProfile | null>(null);

  // Contact / Inquiry Form
  const [contactForm, setContactForm] = useState({
    clientName: '',
    clientEmail: '',
    clientPhone: '',
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

  const idsParam = searchParams.get('ids');

  useEffect(() => {
    const loadComparisonProfiles = async () => {
      setIsLoading(true);
      try {
        if (!idsParam) {
          // If no IDs provided, fetch top 3 featured profiles to compare
          const featured = await marketplacePublicApi.getFeatured();
          setProfiles(featured.slice(0, 3));
        } else {
          const ids = idsParam.split(',').filter(Boolean);
          const loaded = await Promise.all(ids.map((id) => marketplacePublicApi.getById(id)));
          setProfiles(loaded);
        }
      } catch (err) {
        console.error('Failed to load comparison profiles', err);
      } finally {
        setIsLoading(false);
      }
    };

    loadComparisonProfiles();
  }, [idsParam]);

  const removeProfile = (id: string) => {
    const updated = profiles.filter((p) => p.id !== id);
    setProfiles(updated);
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
        serviceCategory: contactForm.serviceCategory,
        requirementDescription: `[Entity: ${contactForm.taxpayerEntity}] ${contactForm.requirementDescription}`,
        budgetRange: contactForm.budgetRange,
        urgency: contactForm.urgency,
      });

      const profName = selectedProfileForContact.displayName;
      setSelectedProfileForContact(null);
      setActionSuccess({
        title: 'Requirement Transmitted Successfully!',
        message: `Your requirement has been transmitted to ${profName}. They will review your scope and dispatch a formal engagement proposal.`,
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
        message: `Your 30-minute advisory session with ${profName} is scheduled for ${bookingForm.bookingDate} at ${bookingForm.startTime} IST (${bookingForm.consultationMode}). An Inbound Lead record and calendar invite have been created.`,
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

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex items-center justify-center">
        <div className="text-center space-y-3">
          <div className="animate-spin w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full mx-auto" />
          <p className="text-sm font-medium text-slate-500">Loading comparison matrix...</p>
        </div>
      </div>
    );
  }

  if (profiles.length === 0) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex items-center justify-center p-4">
        <div className="bg-white dark:bg-slate-900 p-8 rounded-3xl border border-slate-200 dark:border-slate-800 text-center space-y-4 max-w-md">
          <h2 className="text-xl font-bold text-slate-900 dark:text-white">No Profiles Selected</h2>
          <p className="text-sm text-slate-500">Please select at least 2 tax professionals from the directory to compare.</p>
          <Button variant="primary" onClick={() => navigate('/marketplace')}>
            Explore Tax Directory
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 pb-24">
      {/* Top Header & Brand Navbar */}
      <nav className="bg-[#07152B] border-b border-white/10 px-4 sm:px-6 lg:px-8 py-3 sticky top-0 z-30 shadow-md">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-3">
            <TaxorynLogo variant="horizontal" theme="dark" size="sm" />
            <span className="hidden sm:inline-block text-[10px] font-bold uppercase tracking-widest text-[#00D1A3] bg-white/5 border border-[#00D1A3]/30 px-2.5 py-0.5 rounded-full">
              Compare
            </span>
          </div>

          <div className="flex items-center gap-4">
            <div className="text-xs font-bold text-slate-400 hidden sm:inline">
              Comparing {profiles.length} Tax Practitioners
            </div>
            <button
              onClick={() => navigate('/marketplace')}
              className="flex items-center gap-1.5 text-xs font-semibold text-slate-300 hover:text-white transition-colors py-1 px-2.5 rounded-lg hover:bg-white/10"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Back to Directory</span>
            </button>
          </div>
        </div>
      </nav>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-8 space-y-8">
        <div className="text-center space-y-2">
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white">
            Side-by-Side Professional Comparison
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 max-w-2xl mx-auto">
            Evaluate qualifications, verified credentials, pricing structures, and specializations to choose the perfect tax partner.
          </p>
        </div>

        {/* Comparison Matrix Table */}
        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-xl overflow-x-auto">
          <table className="w-full border-collapse">
            <thead>
              <tr className="border-b border-slate-200 dark:border-slate-800">
                <th className="p-6 text-left text-xs font-bold text-slate-400 uppercase tracking-wider w-1/4 min-w-[180px] bg-slate-50/50 dark:bg-slate-800/30 sticky left-0 z-10">
                  Feature / Attribute
                </th>
                {profiles.map((p) => (
                  <th key={p.id} className="p-6 text-left w-1/4 min-w-[220px] align-top">
                    <div className="space-y-3">
                      <div className="flex items-start justify-between gap-2">
                        <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-600 text-white font-bold text-lg flex items-center justify-center shadow-md">
                          {p.displayName.charAt(0)}
                        </div>
                        {profiles.length > 1 && (
                          <button
                            onClick={() => removeProfile(p.id)}
                            className="text-slate-400 hover:text-rose-500 text-xs p-1"
                          >
                            <X className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                      <div>
                        <h3 className="text-base font-bold text-slate-900 dark:text-white">{p.displayName}</h3>
                        <p className="text-xs text-slate-500">{p.professionalType?.replace(/_/g, ' ')}</p>
                      </div>

                      {/* Header Conversion CTAs */}
                      <div className="flex items-center gap-2 pt-1">
                        <Button
                          size="sm"
                          variant="secondary"
                          onClick={() => setSelectedProfileForContact(p)}
                          className="flex-1 text-xs rounded-xl"
                        >
                          <MessageSquare className="w-3.5 h-3.5 mr-1" />
                          Contact
                        </Button>
                        <Button
                          size="sm"
                          variant="primary"
                          onClick={() => setSelectedProfileForBooking(p)}
                          className="flex-1 text-xs rounded-xl bg-indigo-600 hover:bg-indigo-700"
                        >
                          <Calendar className="w-3.5 h-3.5 mr-1" />
                          Book
                        </Button>
                      </div>
                    </div>
                  </th>
                ))}
              </tr>
            </thead>

            <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-xs">
              {/* KYC Verification */}
              <tr>
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300 bg-slate-50/50 dark:bg-slate-800/30 sticky left-0 z-10 min-w-[180px]">
                  KYC Verification Badge
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4">
                    {p.verificationStatus === 'VERIFIED' ? (
                      <span className="inline-flex items-center gap-1 font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/60 px-2.5 py-1 rounded-full border border-emerald-200 dark:border-emerald-800">
                        <ShieldCheck className="w-3.5 h-3.5" />
                        ICAI / ICSI Verified
                      </span>
                    ) : (
                      <span className="text-slate-400 font-medium">Pending Verification</span>
                    )}
                  </td>
                ))}
              </tr>

              {/* Experience */}
              <tr>
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300 bg-slate-50/50 dark:bg-slate-800/30 sticky left-0 z-10 min-w-[180px]">
                  Years of Experience
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4 font-semibold text-slate-900 dark:text-white">
                    {p.experienceYears} Years in Practice
                  </td>
                ))}
              </tr>

              {/* Location */}
              <tr>
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300 bg-slate-50/50 dark:bg-slate-800/30 sticky left-0 z-10 min-w-[180px]">
                  Office Location
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4 flex items-center gap-1.5 text-slate-600 dark:text-slate-300">
                    <MapPin className="w-3.5 h-3.5 text-rose-500 shrink-0" />
                    <span>{p.city}, {p.state}</span>
                  </td>
                ))}
              </tr>

              {/* Client Rating */}
              <tr>
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300 bg-slate-50/50 dark:bg-slate-800/30 sticky left-0 z-10 min-w-[180px]">
                  Average Rating & Reviews
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4">
                    <div className="flex items-center gap-1 font-bold text-amber-500">
                      <Star className="w-4 h-4 fill-current" />
                      <span>{p.averageRating?.toFixed(1) || '5.0'}</span>
                      <span className="text-slate-400 font-normal">({p.totalReviews} reviews)</span>
                    </div>
                  </td>
                ))}
              </tr>

              {/* Starting Package Fee */}
              <tr>
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300 bg-slate-50/50 dark:bg-slate-800/30 sticky left-0 z-10 min-w-[180px]">
                  Starting Service Fee
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4 font-extrabold text-sm text-indigo-600 dark:text-indigo-400">
                    ₹{p.startingFee?.toLocaleString('en-IN') || '999'}
                  </td>
                ))}
              </tr>

              {/* Hourly Advisory Rate */}
              <tr>
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300 bg-slate-50/50 dark:bg-slate-800/30 sticky left-0 z-10 min-w-[180px]">
                  Hourly Advisory Rate
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4 font-bold text-slate-900 dark:text-white">
                    ₹{p.hourlyRate?.toLocaleString('en-IN') || '1500'} / hr
                  </td>
                ))}
              </tr>

              {/* Consultation Fee */}
              <tr>
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300 bg-slate-50/50 dark:bg-slate-800/30 sticky left-0 z-10 min-w-[180px]">
                  30-Min Strategy Consultation
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4">
                    {p.consultationEnabled ? (
                      <span className="font-bold text-emerald-600 dark:text-emerald-400">
                        Available (₹{p.consultationFee})
                      </span>
                    ) : (
                      <span className="text-slate-400">By Request Only</span>
                    )}
                  </td>
                ))}
              </tr>

              {/* Specializations */}
              <tr>
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300 bg-slate-50/50 dark:bg-slate-800/30 sticky left-0 z-10 min-w-[180px]">
                  Key Specializations
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4">
                    <div className="flex flex-wrap gap-1">
                      {p.specializations?.map((s, i) => (
                        <span
                          key={i}
                          className="text-[10px] bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 px-2 py-0.5 rounded"
                        >
                          {s.replace(/_/g, ' ')}
                        </span>
                      ))}
                    </div>
                  </td>
                ))}
              </tr>

              {/* Bottom Action Row */}
              <tr className="bg-slate-50/50 dark:bg-slate-800/20">
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300">
                  Take Next Step
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4">
                    <Button
                      size="sm"
                      variant="primary"
                      onClick={() => navigate(`/marketplace/profile/${p.id}`)}
                      className="w-full text-xs rounded-xl"
                    >
                      View Full Profile & Services
                    </Button>
                  </td>
                ))}
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Quick Contact Modal */}
      {selectedProfileForContact && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-lg w-full p-6 space-y-4 shadow-2xl">
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
              <div className="grid grid-cols-2 gap-3">
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

              <div className="grid grid-cols-2 gap-3">
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
                  placeholder="e.g. We require monthly GST filing (GSTR-1, 3B) and quarterly advance tax computation."
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
                  {isSubmitting ? 'Sending...' : 'Transmit Requirement'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Book Consultation Modal */}
      {selectedProfileForBooking && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-lg w-full p-6 space-y-4 shadow-2xl">
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
              <div className="grid grid-cols-2 gap-3">
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

      {/* Action Success Modal */}
      {actionSuccess && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-md w-full p-6 space-y-4 shadow-2xl text-center">
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
              <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Pipeline Reference ID:</div>
              <div className="text-xs font-mono font-bold text-indigo-600 dark:text-indigo-400 break-all">
                {actionSuccess.trackingId}
              </div>
              <div className="text-[10px] text-slate-400">
                Next: {actionSuccess.profileName} will review and dispatch an engagement proposal / onboarding link.
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
