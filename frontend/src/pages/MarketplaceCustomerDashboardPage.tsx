import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { marketplaceCustomerApi } from '../api/endpoints';
import {
  CustomerDashboard,
  EnquiryDetail,
  EnquiryStatus,
  SubmitEnquiryReviewRequest,
  CancelEnquiryRequest,
  EnquiryMessage,
  EnquiryMessageThread,
} from '../types';
import {
  Compass,
  FileText,
  Calendar,
  Award,
  Star,
  CheckCircle2,
  Clock,
  ArrowRight,
  User,
  ShieldCheck,
  ChevronRight,
  ExternalLink,
  Plus,
  Layers,
  MapPin,
  MessageSquarePlus,
  History,
  XCircle,
  X,
  MessageSquare,
  CheckCircle,
  Send,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Badge } from '../components/common/Badge';
import clsx from 'clsx';

export const MarketplaceCustomerDashboardPage: React.FC = () => {
  const [dashboard, setDashboard] = useState<CustomerDashboard | null>(null);
  const [enquiries, setEnquiries] = useState<EnquiryDetail[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'requirements' | 'requests' | 'consultations' | 'proposals' | 'reviews'>('requests');

  // Cancel Enquiry Modal
  const [cancelTarget, setCancelTarget] = useState<EnquiryDetail | null>(null);
  const [cancelReason, setCancelReason] = useState<string>('');
  const [isSubmittingAction, setIsSubmittingAction] = useState<boolean>(false);

  // Review Modal
  const [reviewTarget, setReviewTarget] = useState<EnquiryDetail | null>(null);
  const [reviewForm, setReviewForm] = useState<SubmitEnquiryReviewRequest>({
    rating: 5,
    reviewTitle: '',
    reviewComment: '',
  });

  // Timeline Drawer Modal
  const [timelineTarget, setTimelineTarget] = useState<EnquiryDetail | null>(null);

  // Secure Messages Drawer Modal
  const [selectedEnquiryForMessages, setSelectedEnquiryForMessages] = useState<EnquiryDetail | null>(null);
  const [messageThread, setMessageThread] = useState<EnquiryMessageThread | null>(null);
  const [messageText, setMessageText] = useState<string>('');
  const [isLoadingMessages, setIsLoadingMessages] = useState<boolean>(false);
  const [isSendingMessage, setIsSendingMessage] = useState<boolean>(false);

  const [notificationBanner, setNotificationBanner] = useState<string | null>(null);

  const fetchDashboardAndEnquiries = async () => {
    try {
      setIsLoading(true);
      const [dashData, enqData] = await Promise.all([
        marketplaceCustomerApi.getDashboard().catch(() => null),
        marketplaceCustomerApi.getEnquiries({ size: 50 }).then((r) => r.content || []).catch(() => []),
      ]);
      setDashboard(dashData);
      setEnquiries(enqData);
    } catch (err) {
      console.error('Failed to load customer dashboard', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardAndEnquiries();
  }, []);

  const handleCancelEnquiry = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!cancelTarget) return;
    try {
      setIsSubmittingAction(true);
      await marketplaceCustomerApi.cancelEnquiry(cancelTarget.id, {
        cancellationReason: cancelReason || undefined,
      });
      setCancelTarget(null);
      setCancelReason('');
      await fetchDashboardAndEnquiries();
      setNotificationBanner(`Enquiry ${cancelTarget.referenceNumber} has been successfully cancelled.`);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to cancel enquiry.');
    } finally {
      setIsSubmittingAction(false);
    }
  };

  const handleSubmitReview = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!reviewTarget) return;
    try {
      setIsSubmittingAction(true);
      await marketplaceCustomerApi.submitVerifiedReview(reviewTarget.id, reviewForm);
      setReviewTarget(null);
      setReviewForm({ rating: 5, reviewTitle: '', reviewComment: '' });
      await fetchDashboardAndEnquiries();
      setNotificationBanner(`Thank you! Your verified review for ${reviewTarget.practiceName || 'the practice'} has been published.`);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to submit review.');
    } finally {
      setIsSubmittingAction(false);
    }
  };

  const openMessages = async (enquiry: EnquiryDetail) => {
    setSelectedEnquiryForMessages(enquiry);
    setMessageText('');
    setIsLoadingMessages(true);
    try {
      const thread = await marketplaceCustomerApi.getEnquiryMessages(enquiry.id);
      setMessageThread(thread);
      await marketplaceCustomerApi.markMessagesRead(enquiry.id);
    } catch (err: any) {
      console.error('Failed to load customer enquiry messages:', err);
    } finally {
      setIsLoadingMessages(false);
    }
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedEnquiryForMessages || !messageText.trim() || isSendingMessage) return;
    setIsSendingMessage(true);
    try {
      const newMsg = await marketplaceCustomerApi.sendEnquiryMessage(selectedEnquiryForMessages.id, {
        messageBody: messageText.trim(),
      });
      setMessageText('');
      if (messageThread) {
        setMessageThread({
          ...messageThread,
          messages: [...messageThread.messages, newMsg],
        });
      }
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to send message.');
    } finally {
      setIsSendingMessage(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6 text-xs text-slate-500">
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 border-2 border-brand-600 border-t-transparent rounded-full animate-spin" />
          <span>Loading your marketplace dashboard...</span>
        </div>
      </div>
    );
  }

  const profile = dashboard?.profile;
  const completeness = profile?.profileCompleteness;

  const getStatusBadge = (status: EnquiryStatus) => {
    switch (status) {
      case 'NEW':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-blue-100 text-blue-800 border border-blue-200">Submitted</span>;
      case 'RECEIVED':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-sky-100 text-sky-800 border border-sky-200">Received by Practice</span>;
      case 'ACCEPTED':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-800 border border-indigo-200">Accepted</span>;
      case 'IN_PROGRESS':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-200 animate-pulse">In Progress</span>;
      case 'COMPLETED':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">Completed</span>;
      case 'REJECTED':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-800 border border-rose-200">Declined</span>;
      case 'CANCELLED':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-700 border border-slate-200">Cancelled</span>;
      default:
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-700">{status}</span>;
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 pb-16">
      {/* Customer Header */}
      <div className="bg-slate-900 text-white border-b border-slate-800">
        <div className="max-w-7xl mx-auto px-4 py-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-brand-600 to-indigo-600 flex items-center justify-center text-white font-black text-xl shadow-lg shadow-brand-500/20">
                {profile?.displayName ? profile.displayName.slice(0, 2).toUpperCase() : 'CU'}
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h1 className="text-lg font-bold tracking-tight">Welcome, {profile?.displayName || 'Customer'}</h1>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-brand-500/20 text-brand-300 border border-brand-500/30">
                    {profile?.customerType === 'BUSINESS' ? 'Business Client' : 'Individual Taxpayer'}
                  </span>
                </div>
                <p className="text-xs text-slate-400 mt-0.5">
                  Track your service enquiries, active compliance work, and verified reviews
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <Link to="/marketplace/customer/feedback">
                <Button variant="secondary" size="sm" className="text-xs bg-slate-800 text-slate-200 border-slate-700 hover:bg-slate-700">
                  <MessageSquarePlus className="w-3.5 h-3.5 mr-1.5" />
                  Feedback
                </Button>
              </Link>
              <Link to="/marketplace/customer/requirements/new">
                <Button size="sm" className="text-xs bg-indigo-600 hover:bg-indigo-700 text-white shadow-md shadow-indigo-600/20 font-bold">
                  <Plus className="w-3.5 h-3.5 mr-1.5" />
                  Post Tax Need
                </Button>
              </Link>
              <Link to="/marketplace/customer/profile">
                <Button variant="secondary" size="sm" className="text-xs bg-slate-800 text-slate-200 border-slate-700 hover:bg-slate-700">
                  <User className="w-3.5 h-3.5 mr-1.5" />
                  Profile
                </Button>
              </Link>
              <Link to="/marketplace">
                <Button size="sm" className="text-xs bg-brand-600 hover:bg-brand-700 text-white shadow-md shadow-brand-500/20">
                  <Compass className="w-3.5 h-3.5 mr-1.5" />
                  Find Practitioners
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 py-6 space-y-6">
        {/* Banner notification */}
        {notificationBanner && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center justify-between text-xs text-emerald-800 font-semibold">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              <span>{notificationBanner}</span>
            </div>
            <button onClick={() => setNotificationBanner(null)} className="text-emerald-600 font-bold">&times;</button>
          </div>
        )}

        {/* Profile Completeness Alert if < 100% */}
        {completeness && completeness.percentage < 100 && (
          <div className="p-4 bg-gradient-to-r from-amber-500/10 via-brand-500/10 to-indigo-500/10 border border-amber-200 rounded-2xl flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-amber-600" />
                <span className="text-xs font-bold text-slate-900">Your profile is {completeness.percentage}% complete</span>
              </div>
              <p className="text-[11px] text-slate-600">
                Complete your tax profile to get matched with certified CAs faster.
              </p>
            </div>
            <Link to="/marketplace/customer/profile">
              <Button size="sm" variant="secondary" className="text-xs whitespace-nowrap bg-white text-slate-800 border-slate-300">
                Complete Profile
              </Button>
            </Link>
          </div>
        )}

        {/* Quick Stats Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-sm space-y-1">
            <div className="text-[10px] uppercase font-bold text-slate-400">My Enquiries</div>
            <div className="text-xl font-extrabold text-slate-900">{enquiries.length}</div>
            <div className="text-[10px] text-slate-500">Active & completed</div>
          </div>
          <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-sm space-y-1">
            <div className="text-[10px] uppercase font-bold text-slate-400">In Progress</div>
            <div className="text-xl font-extrabold text-amber-600">
              {enquiries.filter((e) => e.enquiryStatus === 'IN_PROGRESS' || e.enquiryStatus === 'ACCEPTED').length}
            </div>
            <div className="text-[10px] text-slate-500">Being handled by CA</div>
          </div>
          <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-sm space-y-1">
            <div className="text-[10px] uppercase font-bold text-slate-400">Consultations</div>
            <div className="text-xl font-extrabold text-purple-600">{dashboard?.totalConsultations || 0}</div>
            <div className="text-[10px] text-slate-500">Scheduled sessions</div>
          </div>
          <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-sm space-y-1">
            <div className="text-[10px] uppercase font-bold text-slate-400">My Verified Reviews</div>
            <div className="text-xl font-extrabold text-emerald-600">{dashboard?.totalReviews || 0}</div>
            <div className="text-[10px] text-slate-500">Shared feedback</div>
          </div>
        </div>

        {/* Main Tabs Card */}
        <Card className="p-6 bg-white border-slate-200 shadow-sm rounded-3xl">
          <div className="flex items-center justify-between border-b border-slate-200 pb-4 mb-6 overflow-x-auto">
            <div className="flex items-center gap-2">
              <button
                onClick={() => setActiveTab('requests')}
                className={clsx(
                  'px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all',
                  activeTab === 'requests'
                    ? 'bg-brand-600 text-white shadow-sm'
                    : 'text-slate-600 hover:bg-slate-100'
                )}
              >
                My Enquiries ({enquiries.length})
              </button>
              <button
                onClick={() => setActiveTab('requirements')}
                className={clsx(
                  'px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all',
                  activeTab === 'requirements'
                    ? 'bg-brand-600 text-white shadow-sm'
                    : 'text-slate-600 hover:bg-slate-100'
                )}
              >
                Posted Needs ({dashboard?.recentTaxRequirements?.length || 0})
              </button>
              <button
                onClick={() => setActiveTab('consultations')}
                className={clsx(
                  'px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all',
                  activeTab === 'consultations'
                    ? 'bg-brand-600 text-white shadow-sm'
                    : 'text-slate-600 hover:bg-slate-100'
                )}
              >
                Consultations ({dashboard?.recentConsultations?.length || 0})
              </button>
              <button
                onClick={() => setActiveTab('proposals')}
                className={clsx(
                  'px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all',
                  activeTab === 'proposals'
                    ? 'bg-brand-600 text-white shadow-sm'
                    : 'text-slate-600 hover:bg-slate-100'
                )}
              >
                Engagement Proposals ({dashboard?.recentProposals?.length || 0})
              </button>
            </div>

            <Link to="/marketplace" className="text-xs font-semibold text-brand-600 hover:underline flex items-center gap-1 shrink-0 ml-4">
              Browse More Practices <ExternalLink className="w-3.5 h-3.5" />
            </Link>
          </div>

          {/* Tab 1: Enquiries with Lifecycle State Machine */}
          {activeTab === 'requests' && (
            <div className="space-y-4">
              {enquiries.length > 0 ? (
                enquiries.map((enquiry) => (
                  <div
                    key={enquiry.id}
                    className="p-5 rounded-2xl border border-slate-200 hover:border-slate-300 transition-all bg-slate-50/50 space-y-4"
                  >
                    <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 border-b border-slate-200/80 pb-3">
                      <div>
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="font-mono text-xs font-bold text-indigo-700 bg-indigo-100/80 px-2.5 py-0.5 rounded-md">
                            {enquiry.referenceNumber}
                          </span>
                          <span className="text-xs font-bold text-slate-900">
                            {enquiry.taxServiceName || enquiry.serviceCategory || 'Tax Advisory'}
                          </span>
                          {getStatusBadge(enquiry.enquiryStatus)}
                        </div>
                        <div className="text-[11px] text-slate-500 flex items-center gap-2 mt-1">
                          <span>Practice: <strong>{enquiry.practiceName || 'Tax Practice'}</strong></span>
                          {enquiry.practiceCity && <span>• {enquiry.practiceCity}</span>}
                          <span>• Submitted {new Date(enquiry.createdAt).toLocaleDateString()}</span>
                        </div>
                      </div>

                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => openMessages(enquiry)}
                          className="px-2.5 py-1 rounded-lg text-xs font-semibold bg-indigo-50 border border-indigo-200 text-indigo-700 hover:bg-indigo-100 flex items-center gap-1"
                        >
                          <MessageSquare className="w-3.5 h-3.5 text-indigo-600" />
                          Messages
                        </button>

                        <button
                          onClick={() => setTimelineTarget(enquiry)}
                          className="px-2.5 py-1 rounded-lg text-xs font-semibold bg-white border border-slate-200 text-slate-700 hover:bg-slate-100 flex items-center gap-1"
                        >
                          <History className="w-3.5 h-3.5 text-indigo-600" />
                          Timeline
                        </button>

                        {enquiry.canCancel && (
                          <button
                            onClick={() => {
                              setCancelTarget(enquiry);
                              setCancelReason('');
                            }}
                            className="px-2.5 py-1 rounded-lg text-xs font-semibold text-rose-700 hover:bg-rose-50 border border-rose-200"
                          >
                            Cancel Enquiry
                          </button>
                        )}

                        {enquiry.canReview && (
                          <button
                            onClick={() => {
                              setReviewTarget(enquiry);
                              setReviewForm({ rating: 5, reviewTitle: '', reviewComment: '' });
                            }}
                            className="px-3 py-1 rounded-lg text-xs font-bold bg-amber-500 hover:bg-amber-600 text-white shadow-sm flex items-center gap-1"
                          >
                            <Star className="w-3.5 h-3.5 fill-white" />
                            Leave Verified Review
                          </button>
                        )}
                      </div>
                    </div>

                    {/* Visual Progress Stepper */}
                    {enquiry.enquiryStatus !== 'REJECTED' && enquiry.enquiryStatus !== 'CANCELLED' && (
                      <div className="grid grid-cols-5 gap-2 pt-1">
                        {[
                          { label: 'Submitted', key: 'NEW' },
                          { label: 'Received', key: 'RECEIVED' },
                          { label: 'Accepted', key: 'ACCEPTED' },
                          { label: 'In Progress', key: 'IN_PROGRESS' },
                          { label: 'Completed', key: 'COMPLETED' },
                        ].map((step, idx) => {
                          const order = ['NEW', 'RECEIVED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED'];
                          const currentIdx = order.indexOf(enquiry.enquiryStatus);
                          const isDone = currentIdx >= idx;
                          const isCurrent = currentIdx === idx;
                          return (
                            <div key={step.key} className="text-center space-y-1">
                              <div
                                className={clsx(
                                  'h-1.5 rounded-full transition-all',
                                  isDone ? 'bg-emerald-500' : 'bg-slate-200'
                                )}
                              />
                              <div
                                className={clsx(
                                  'text-[10px] font-bold',
                                  isCurrent ? 'text-indigo-600' : isDone ? 'text-slate-700' : 'text-slate-400'
                                )}
                              >
                                {step.label}
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    )}

                    {enquiry.rejectionReason && (
                      <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800">
                        <strong>Practice Note:</strong> This enquiry was declined due to:{' '}
                        {enquiry.rejectionReason.replace(/_/g, ' ')}. {enquiry.rejectionNote}
                      </div>
                    )}

                    {enquiry.cancellationReason && (
                      <div className="p-3 bg-slate-100 rounded-xl text-xs text-slate-600">
                        <strong>Cancelled Reason:</strong> {enquiry.cancellationReason}
                      </div>
                    )}
                  </div>
                ))
              ) : (
                <div className="text-center py-12 space-y-3">
                  <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
                    <FileText className="w-6 h-6" />
                  </div>
                  <p className="text-xs text-slate-500 font-medium">You haven't submitted any service enquiries yet.</p>
                  <Link to="/marketplace">
                    <Button size="sm" className="text-xs bg-brand-600 text-white">
                      Explore Practices
                    </Button>
                  </Link>
                </div>
              )}
            </div>
          )}

          {/* Tab 0: Tax Needs & Requirements */}
          {activeTab === 'requirements' && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Your Posted Tax Needs</h3>
                  <p className="text-xs text-slate-500">Requirements you've created for return filings, advisory, and registrations.</p>
                </div>
                <div className="flex items-center gap-2">
                  <Link to="/marketplace/customer/requirements">
                    <Button variant="outline" size="sm" className="text-xs">
                      View All
                    </Button>
                  </Link>
                  <Link to="/marketplace/customer/requirements/new">
                    <Button size="sm" className="text-xs bg-indigo-600 hover:bg-indigo-700 text-white font-bold">
                      <Plus className="w-3.5 h-3.5 mr-1" />
                      Post New Need
                    </Button>
                  </Link>
                </div>
              </div>

              {dashboard?.recentTaxRequirements && dashboard.recentTaxRequirements.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  {dashboard.recentTaxRequirements.map((req) => (
                    <div
                      key={req.id}
                      className="p-4 rounded-2xl border border-slate-200 bg-slate-50/50 hover:bg-white hover:shadow-sm transition-all flex flex-col justify-between space-y-3"
                    >
                      <div className="space-y-1.5">
                        <div className="flex items-start justify-between gap-2">
                          <div>
                            <span className="font-mono text-[9px] font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-700 uppercase">
                              {req.categoryName || 'Tax Service'}
                            </span>
                            <h4 className="text-xs font-bold text-slate-900 line-clamp-1 mt-0.5">{req.taxServiceName}</h4>
                          </div>
                          <span
                            className={clsx(
                              'px-2 py-0.5 rounded-full text-[9px] font-bold',
                              req.status === 'SUBMITTED' && 'bg-emerald-100 text-emerald-800',
                              req.status === 'DRAFT' && 'bg-amber-100 text-amber-800',
                              req.status === 'CANCELLED' && 'bg-slate-100 text-slate-500',
                              req.status === 'CLOSED' && 'bg-indigo-100 text-indigo-800'
                            )}
                          >
                            {req.status}
                          </span>
                        </div>

                        <div className="flex items-center gap-3 text-[11px] text-slate-500">
                          <span>FY: <strong>{req.financialYearDisplay || 'N/A'}</strong></span>
                          <span>•</span>
                          <span>{req.customerTypeDisplayName || req.customerType?.replace(/_/g, ' ') || 'General'}</span>
                        </div>
                      </div>

                      <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
                        <span className="text-[10px] text-slate-400">
                          {new Date(req.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}
                        </span>
                        <Link
                          to="/marketplace/customer/requirements"
                          className="text-xs text-indigo-600 font-bold hover:underline flex items-center gap-0.5"
                        >
                          Manage <ChevronRight className="w-3.5 h-3.5" />
                        </Link>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="p-8 bg-slate-50 rounded-2xl border border-slate-100 text-center space-y-2">
                  <Layers className="w-8 h-8 text-indigo-400 mx-auto" />
                  <div className="text-xs font-bold text-slate-700">No Tax Requirements Yet</div>
                  <p className="text-[11px] text-slate-400 max-w-sm mx-auto">
                    Tell us what tax help you need and we'll help prepare structured requirements for matching.
                  </p>
                  <Link to="/marketplace/customer/requirements/new">
                    <Button size="sm" className="mt-2 text-xs bg-indigo-600 text-white font-bold">
                      <Plus className="w-3.5 h-3.5 mr-1" />
                      Tell Us Your Tax Need
                    </Button>
                  </Link>
                </div>
              )}
            </div>
          )}

          {/* Tab 2: Consultations */}
          {activeTab === 'consultations' && (
            <div className="space-y-3">
              {dashboard?.recentConsultations && dashboard.recentConsultations.length > 0 ? (
                dashboard.recentConsultations.map((c) => (
                  <div
                    key={c.id}
                    className="p-4 rounded-xl border border-slate-100 hover:border-slate-200 transition-all bg-slate-50/50 flex flex-col md:flex-row md:items-center justify-between gap-3"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-900">{c.topic}</span>
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-purple-50 text-purple-700 border border-purple-100">
                          {c.consultationStatus}
                        </span>
                      </div>
                      <p className="text-xs text-slate-600">With {c.practiceDisplayName || 'Tax Practice'}</p>
                      <div className="text-[10px] text-slate-400 flex items-center gap-2">
                        <Clock className="w-3 h-3" />
                        <span>
                          {c.bookingDate} at {c.startTime} ({c.consultationMode})
                        </span>
                      </div>
                    </div>

                    <div className="text-right">
                      <span className="text-xs font-bold text-slate-900 block">₹{c.feeAmount || 0}</span>
                      <span className="text-[10px] text-emerald-600 font-semibold">{c.paymentStatus}</span>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-center py-10 space-y-3">
                  <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
                    <Calendar className="w-6 h-6" />
                  </div>
                  <p className="text-xs text-slate-500 font-medium">No consultations scheduled yet.</p>
                  <Link to="/marketplace">
                    <Button size="sm" className="text-xs bg-brand-600 text-white">
                      Book Consultation
                    </Button>
                  </Link>
                </div>
              )}
            </div>
          )}

          {/* Tab 3: Proposals */}
          {activeTab === 'proposals' && (
            <div className="space-y-3">
              {dashboard?.recentProposals && dashboard.recentProposals.length > 0 ? (
                dashboard.recentProposals.map((p) => (
                  <div
                    key={p.id}
                    className="p-4 rounded-xl border border-slate-100 hover:border-slate-200 transition-all bg-slate-50/50 flex flex-col md:flex-row md:items-center justify-between gap-3"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-900">{p.proposalTitle}</span>
                        <span
                          className={clsx(
                            'px-2 py-0.5 rounded-full text-[10px] font-bold',
                            p.proposalStatus === 'ACCEPTED'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-100'
                              : 'bg-amber-50 text-amber-700 border border-amber-100'
                          )}
                        >
                          {p.proposalStatus}
                        </span>
                      </div>
                      <p className="text-xs text-slate-600">From {p.practiceDisplayName || 'Tax Practice'}</p>
                      <div className="text-[10px] text-slate-400">
                        {p.scopeOfWork ? p.scopeOfWork.slice(0, 80) + '...' : ''}
                      </div>
                    </div>

                    <div className="text-right flex items-center gap-3">
                      <div>
                        <span className="text-xs font-bold text-slate-900 block">₹{p.feeAmount}</span>
                        <span className="text-[10px] text-slate-400">{p.pricingType}</span>
                      </div>
                      <Link to={`/marketplace/onboarding/${p.accessToken}`}>
                        <Button size="sm" className="text-xs bg-indigo-600 hover:bg-indigo-700 text-white font-bold">
                          Review Proposal
                        </Button>
                      </Link>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-center py-10 space-y-3">
                  <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
                    <FileText className="w-6 h-6" />
                  </div>
                  <p className="text-xs text-slate-500 font-medium">No proposals received yet.</p>
                </div>
              )}
            </div>
          )}
        </Card>
      </div>

      {/* Cancel Enquiry Modal */}
      {cancelTarget && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl border border-slate-200 max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2">
                <XCircle className="w-5 h-5 text-rose-600" />
                <h3 className="text-base font-bold text-slate-900">Cancel Tax Enquiry</h3>
              </div>
              <button onClick={() => setCancelTarget(null)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <p className="text-xs text-slate-600">
              Are you sure you want to cancel enquiry <strong>{cancelTarget.referenceNumber}</strong>? The practice will be notified.
            </p>

            <form onSubmit={handleCancelEnquiry} className="space-y-3">
              <div>
                <label className="text-xs font-bold text-slate-700">Reason for Cancellation (Optional)</label>
                <textarea
                  rows={2}
                  placeholder="e.g. Requirement fulfilled elsewhere, postponed filing..."
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t">
                <Button variant="secondary" size="sm" onClick={() => setCancelTarget(null)}>
                  Keep Enquiry
                </Button>
                <Button variant="primary" size="sm" disabled={isSubmittingAction} className="bg-rose-600 hover:bg-rose-700 text-white font-bold">
                  {isSubmittingAction ? 'Cancelling...' : 'Confirm Cancellation'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Verified Review Modal */}
      {reviewTarget && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl border border-slate-200 max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2">
                <Award className="w-5 h-5 text-amber-500" />
                <div>
                  <h3 className="text-base font-bold text-slate-900">Leave Verified Review</h3>
                  <span className="text-[10px] font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full">
                    Verified Completed Engagement
                  </span>
                </div>
              </div>
              <button onClick={() => setReviewTarget(null)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmitReview} className="space-y-3">
              <div>
                <label className="text-xs font-bold text-slate-700">Rating *</label>
                <div className="flex items-center gap-2 mt-1">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      type="button"
                      key={star}
                      onClick={() => setReviewForm({ ...reviewForm, rating: star })}
                      className="p-1"
                    >
                      <Star
                        className={clsx(
                          'w-6 h-6 transition-all',
                          star <= reviewForm.rating
                            ? 'text-amber-500 fill-amber-500'
                            : 'text-slate-300'
                        )}
                      />
                    </button>
                  ))}
                  <span className="text-xs font-bold text-slate-700 ml-2">{reviewForm.rating} / 5 Stars</span>
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700">Review Title</label>
                <input
                  type="text"
                  placeholder="e.g. Excellent return filing experience"
                  value={reviewForm.reviewTitle || ''}
                  onChange={(e) => setReviewForm({ ...reviewForm, reviewTitle: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs"
                />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700">Your Feedback *</label>
                <textarea
                  rows={3}
                  required
                  placeholder="Share details of your experience with the CA/tax practitioner..."
                  value={reviewForm.reviewComment}
                  onChange={(e) => setReviewForm({ ...reviewForm, reviewComment: e.target.value })}
                  className="w-full mt-1 px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t">
                <Button variant="secondary" size="sm" onClick={() => setReviewTarget(null)}>
                  Cancel
                </Button>
                <Button variant="primary" size="sm" disabled={isSubmittingAction || !reviewForm.reviewComment.trim()} className="bg-amber-500 hover:bg-amber-600 text-white font-bold">
                  {isSubmittingAction ? 'Publishing...' : 'Publish Verified Review'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Timeline Modal */}
      {timelineTarget && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl border border-slate-200 max-w-lg w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b pb-3">
              <div>
                <span className="font-mono text-xs font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded">
                  {timelineTarget.referenceNumber}
                </span>
                <h3 className="text-base font-bold text-slate-900 mt-1">
                  Enquiry Status Timeline
                </h3>
              </div>
              <button onClick={() => setTimelineTarget(null)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4 max-h-96 overflow-y-auto pr-2">
              {timelineTarget.timeline?.map((item, idx) => (
                <div key={idx} className="flex gap-3">
                  <div className="flex flex-col items-center">
                    <div
                      className={clsx(
                        'w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0',
                        item.completed
                          ? 'bg-emerald-600 text-white'
                          : item.current
                          ? 'bg-indigo-600 text-white ring-4 ring-indigo-100'
                          : 'bg-slate-200 text-slate-500'
                      )}
                    >
                      {item.completed ? '✓' : idx + 1}
                    </div>
                    {idx < (timelineTarget.timeline?.length || 0) - 1 && (
                      <div
                        className={clsx(
                          'w-0.5 h-10 my-1',
                          item.completed ? 'bg-emerald-500' : 'bg-slate-200'
                        )}
                      />
                    )}
                  </div>
                  <div className="space-y-0.5 pb-2">
                    <div className="text-xs font-bold text-slate-900">{item.title}</div>
                    <p className="text-[11px] text-slate-500">{item.description}</p>
                    {item.timestamp && (
                      <div className="text-[10px] text-slate-400 font-mono">
                        {new Date(item.timestamp).toLocaleString()}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>

            <div className="pt-3 border-t text-right">
              <Button size="sm" variant="secondary" onClick={() => setTimelineTarget(null)}>
                Close
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Secure Messages Drawer Modal */}
      {selectedEnquiryForMessages && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl border border-slate-200 max-w-xl w-full flex flex-col max-h-[85vh] shadow-2xl overflow-hidden">
            {/* Modal Header */}
            <div className="p-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-2xl bg-indigo-50 text-indigo-600">
                  <MessageSquare className="w-5 h-5" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs font-bold text-indigo-700 bg-indigo-100/80 px-2 py-0.5 rounded">
                      {selectedEnquiryForMessages.referenceNumber}
                    </span>
                    <h3 className="text-sm font-bold text-slate-900">
                      Messages with {selectedEnquiryForMessages.practiceName || 'Practice'}
                    </h3>
                  </div>
                  <p className="text-[11px] text-slate-500 mt-0.5">
                    {selectedEnquiryForMessages.taxServiceName || selectedEnquiryForMessages.serviceCategory} • Status: <span className="font-semibold">{selectedEnquiryForMessages.enquiryStatus}</span>
                  </p>
                </div>
              </div>
              <button
                onClick={() => setSelectedEnquiryForMessages(null)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Messages Thread Content */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-slate-50/50 min-h-[250px]">
              {isLoadingMessages ? (
                <div className="flex flex-col items-center justify-center h-48 text-slate-400">
                  <Clock className="w-6 h-6 animate-spin mb-2" />
                  <span className="text-xs">Loading conversation history...</span>
                </div>
              ) : !messageThread || messageThread.messages.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-48 text-center p-4">
                  <div className="p-3 bg-indigo-50 text-indigo-600 rounded-full mb-2">
                    <MessageSquare className="w-6 h-6" />
                  </div>
                  <p className="text-xs font-bold text-slate-700">No messages yet</p>
                  <p className="text-[11px] text-slate-500 mt-1 max-w-xs">
                    Send a message to your assigned tax practitioner regarding this enquiry.
                  </p>
                </div>
              ) : (
                messageThread.messages.map((msg) => {
                  const isCustomer = msg.senderType === 'CUSTOMER';
                  return (
                    <div
                      key={msg.id}
                      className={clsx('flex flex-col max-w-[80%]', isCustomer ? 'ml-auto items-end' : 'mr-auto items-start')}
                    >
                      <div className="flex items-center gap-1.5 mb-1 px-1">
                        <span className="text-[10px] font-bold text-slate-600">
                          {msg.senderName}
                        </span>
                        {!isCustomer && (
                          <span className="text-[9px] font-semibold bg-emerald-100 text-emerald-800 px-1.5 py-0.2 rounded">
                            Practitioner
                          </span>
                        )}
                        <span className="text-[9px] text-slate-400">
                          {new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                      <div
                        className={clsx(
                          'p-3 rounded-2xl text-xs leading-relaxed shadow-sm',
                          isCustomer
                            ? 'bg-indigo-600 text-white rounded-tr-xs'
                            : 'bg-white text-slate-800 border border-slate-200 rounded-tl-xs'
                        )}
                      >
                        {msg.messageBody}
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* Message Input Box */}
            <div className="p-3 border-t border-slate-200 bg-white">
              {selectedEnquiryForMessages.enquiryStatus === 'CANCELLED' || selectedEnquiryForMessages.enquiryStatus === 'REJECTED' ? (
                <div className="p-2.5 rounded-xl bg-slate-100 text-center text-xs text-slate-500 font-medium">
                  This enquiry is {selectedEnquiryForMessages.enquiryStatus.toLowerCase()}. New messages cannot be sent.
                </div>
              ) : (
                <form onSubmit={handleSendMessage} className="flex items-center gap-2">
                  <input
                    type="text"
                    placeholder={`Message ${selectedEnquiryForMessages.practiceName || 'your practitioner'}...`}
                    value={messageText}
                    onChange={(e) => setMessageText(e.target.value)}
                    className="flex-1 px-3.5 py-2.5 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                  <Button
                    type="submit"
                    variant="primary"
                    size="sm"
                    disabled={!messageText.trim() || isSendingMessage}
                    className="bg-indigo-600 hover:bg-indigo-700 text-white font-bold px-4 py-2.5"
                  >
                    {isSendingMessage ? (
                      <Clock className="w-4 h-4 animate-spin" />
                    ) : (
                      <Send className="w-4 h-4" />
                    )}
                  </Button>
                </form>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
