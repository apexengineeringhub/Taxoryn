import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { marketplaceCustomerApi } from '../api/endpoints';
import { CustomerDashboard } from '../types';
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
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Badge } from '../components/common/Badge';

export const MarketplaceCustomerDashboardPage: React.FC = () => {
  const [dashboard, setDashboard] = useState<CustomerDashboard | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'requests' | 'consultations' | 'proposals' | 'reviews'>('requests');

  const fetchDashboard = async () => {
    try {
      setIsLoading(true);
      const data = await marketplaceCustomerApi.getDashboard();
      setDashboard(data);
    } catch (err) {
      console.error('Failed to load customer dashboard', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
  }, []);

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
                  Track your enquiries, consultation bookings, and engagement proposals
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <Link to="/marketplace/customer/profile">
                <Button variant="secondary" size="sm" className="text-xs bg-slate-800 text-slate-200 border-slate-700 hover:bg-slate-700">
                  <User className="w-3.5 h-3.5 mr-1.5" />
                  My Profile
                </Button>
              </Link>
              <Link to="/marketplace">
                <Button size="sm" className="text-xs bg-brand-600 hover:bg-brand-700 text-white shadow-md shadow-brand-500/20">
                  <Compass className="w-3.5 h-3.5 mr-1.5" />
                  Explore Tax Practitioners
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 py-6 space-y-6">
        {/* Profile Completeness Alert if < 100% */}
        {completeness && completeness.percentage < 100 && (
          <div className="p-4 bg-gradient-to-r from-amber-500/10 via-brand-500/10 to-indigo-500/10 border border-amber-200 rounded-2xl flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="space-y-1">
              <div className="flex items-center gap-2 text-xs font-bold text-amber-900">
                <ShieldCheck className="w-4 h-4 text-amber-600" />
                <span>Complete your Customer Profile ({completeness.percentage}%)</span>
              </div>
              <p className="text-xs text-slate-600">
                Add {completeness.missingItems.join(', ')} to receive faster responses from top tax professionals.
              </p>
            </div>
            <Link to="/marketplace/customer/profile">
              <Button size="sm" variant="secondary" className="text-xs whitespace-nowrap bg-white border-amber-300 text-amber-900 hover:bg-amber-50">
                Complete Profile
                <ArrowRight className="w-3.5 h-3.5 ml-1" />
              </Button>
            </Link>
          </div>
        )}

        {/* Metric Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <Card className="p-4 bg-white border-slate-200 flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-blue-50 text-blue-600">
              <FileText className="w-5 h-5" />
            </div>
            <div>
              <div className="text-2xl font-black text-slate-900">{dashboard?.totalRequests || 0}</div>
              <div className="text-xs text-slate-500 font-medium">Enquiries Sent</div>
            </div>
          </Card>

          <Card className="p-4 bg-white border-slate-200 flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-purple-50 text-purple-600">
              <Calendar className="w-5 h-5" />
            </div>
            <div>
              <div className="text-2xl font-black text-slate-900">{dashboard?.totalConsultations || 0}</div>
              <div className="text-xs text-slate-500 font-medium">Consultations</div>
            </div>
          </Card>

          <Card className="p-4 bg-white border-slate-200 flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-emerald-50 text-emerald-600">
              <Award className="w-5 h-5" />
            </div>
            <div>
              <div className="text-2xl font-black text-slate-900">{dashboard?.totalProposals || 0}</div>
              <div className="text-xs text-slate-500 font-medium">Proposals</div>
            </div>
          </Card>

          <Card className="p-4 bg-white border-slate-200 flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-amber-50 text-amber-600">
              <Star className="w-5 h-5" />
            </div>
            <div>
              <div className="text-2xl font-black text-slate-900">{dashboard?.totalReviews || 0}</div>
              <div className="text-xs text-slate-500 font-medium">Reviews Given</div>
            </div>
          </Card>
        </div>

        {/* Tabbed Activity Section */}
        <Card className="p-6 bg-white border-slate-200 space-y-6">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setActiveTab('requests')}
                className={`px-3 py-1.5 text-xs font-bold rounded-lg transition-colors ${
                  activeTab === 'requests'
                    ? 'bg-brand-50 text-brand-700 border border-brand-200'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                Enquiries ({dashboard?.recentLeads?.length || 0})
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('consultations')}
                className={`px-3 py-1.5 text-xs font-bold rounded-lg transition-colors ${
                  activeTab === 'consultations'
                    ? 'bg-brand-50 text-brand-700 border border-brand-200'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                Consultations ({dashboard?.recentConsultations?.length || 0})
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('proposals')}
                className={`px-3 py-1.5 text-xs font-bold rounded-lg transition-colors ${
                  activeTab === 'proposals'
                    ? 'bg-brand-50 text-brand-700 border border-brand-200'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                Proposals ({dashboard?.recentProposals?.length || 0})
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('reviews')}
                className={`px-3 py-1.5 text-xs font-bold rounded-lg transition-colors ${
                  activeTab === 'reviews'
                    ? 'bg-brand-50 text-brand-700 border border-brand-200'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                My Reviews ({dashboard?.recentReviews?.length || 0})
              </button>
            </div>

            <Link to="/marketplace" className="text-xs font-semibold text-brand-600 hover:underline flex items-center gap-1">
              Browse More <ExternalLink className="w-3.5 h-3.5" />
            </Link>
          </div>

          {/* Tab 1: Enquiries */}
          {activeTab === 'requests' && (
            <div className="space-y-3">
              {dashboard?.recentLeads && dashboard.recentLeads.length > 0 ? (
                dashboard.recentLeads.map((lead) => (
                  <div
                    key={lead.id}
                    className="p-4 rounded-xl border border-slate-100 hover:border-slate-200 transition-all bg-slate-50/50 flex flex-col md:flex-row md:items-center justify-between gap-3"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-900">{lead.serviceCategory || 'Tax Advisory'}</span>
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-100">
                          {lead.leadStatus}
                        </span>
                      </div>
                      <p className="text-xs text-slate-600 line-clamp-1">{lead.requirementDescription || 'No description provided'}</p>
                      <div className="text-[10px] text-slate-400 flex items-center gap-2">
                        <span>Submitted on {new Date(lead.createdAt).toLocaleDateString()}</span>
                        {lead.city && <span>• {lead.city}</span>}
                      </div>
                    </div>

                    <div className="text-right flex items-center gap-2">
                      <Link to={`/marketplace/profile/${lead.marketplaceProfileId}`}>
                        <Button size="sm" variant="secondary" className="text-xs">
                          View Practice
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
                  <p className="text-xs text-slate-500 font-medium">You haven't submitted any service enquiries yet.</p>
                  <Link to="/marketplace">
                    <Button size="sm" className="text-xs bg-brand-600 text-white">
                      Explore Marketplace
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
                          className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                            p.proposalStatus === 'ACCEPTED'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-100'
                              : 'bg-amber-50 text-amber-700 border border-amber-100'
                          }`}
                        >
                          {p.proposalStatus}
                        </span>
                      </div>
                      <p className="text-xs text-slate-600">{p.practiceDisplayName || 'Tax Practice'}</p>
                      <div className="text-[10px] text-slate-400">
                        Fee: ₹{p.feeAmount?.toLocaleString('en-IN')} ({p.pricingType})
                      </div>
                    </div>

                    <div>
                      {p.accessToken && (
                        <Link to={`/marketplace/proposal/${p.accessToken}`}>
                          <Button size="sm" className="text-xs bg-brand-600 text-white">
                            Review Proposal
                            <ChevronRight className="w-3.5 h-3.5 ml-1" />
                          </Button>
                        </Link>
                      )}
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-center py-10 space-y-3">
                  <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
                    <Award className="w-6 h-6" />
                  </div>
                  <p className="text-xs text-slate-500 font-medium">No proposals received yet.</p>
                </div>
              )}
            </div>
          )}

          {/* Tab 4: Reviews */}
          {activeTab === 'reviews' && (
            <div className="space-y-3">
              {dashboard?.recentReviews && dashboard.recentReviews.length > 0 ? (
                dashboard.recentReviews.map((r) => (
                  <div
                    key={r.id}
                    className="p-4 rounded-xl border border-slate-100 hover:border-slate-200 transition-all bg-slate-50/50 space-y-2"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-1.5 text-amber-500">
                        {Array.from({ length: 5 }).map((_, i) => (
                          <Star
                            key={i}
                            className={`w-3.5 h-3.5 ${i < r.rating ? 'fill-amber-400 text-amber-400' : 'text-slate-300'}`}
                          />
                        ))}
                        <span className="text-xs font-bold text-slate-700 ml-1">{r.rating}.0</span>
                      </div>
                      <span className="text-[10px] text-slate-400">{new Date(r.createdAt).toLocaleDateString()}</span>
                    </div>
                    {r.reviewTitle && <h4 className="text-xs font-bold text-slate-900">{r.reviewTitle}</h4>}
                    <p className="text-xs text-slate-600">{r.reviewComment}</p>
                  </div>
                ))
              ) : (
                <div className="text-center py-10 space-y-3">
                  <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
                    <Star className="w-6 h-6" />
                  </div>
                  <p className="text-xs text-slate-500 font-medium">You haven't written any reviews yet.</p>
                </div>
              )}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
};
