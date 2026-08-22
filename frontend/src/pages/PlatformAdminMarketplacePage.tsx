import React, { useState, useEffect } from 'react';
import {
  ShieldCheck,
  Award,
  CheckCircle2,
  XCircle,
  Clock,
  Sparkles,
  TrendingUp,
  FileText,
  Search,
  ExternalLink,
  SlidersHorizontal,
  Building,
  Star,
  Users,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { marketplaceAdminApi, marketplacePublicApi } from '../api/endpoints';
import { MarketplaceVerification, MarketplaceProfile, MarketplaceStats } from '../types';
import clsx from 'clsx';

export const PlatformAdminMarketplacePage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'VERIFICATIONS' | 'LISTINGS' | 'PLATFORM_KPIS'>('VERIFICATIONS');
  const [verifications, setVerifications] = useState<MarketplaceVerification[]>([]);
  const [profiles, setProfiles] = useState<MarketplaceProfile[]>([]);
  const [stats, setStats] = useState<MarketplaceStats | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [successBanner, setSuccessBanner] = useState<string | null>(null);

  // Rejection Modal
  const [rejectingItem, setRejectingItem] = useState<MarketplaceVerification | null>(null);
  const [rejectionReason, setRejectionReason] = useState<string>('');

  const loadAdminData = async () => {
    setIsLoading(true);
    try {
      const [verRes, profRes, statRes] = await Promise.all([
        marketplaceAdminApi.getPendingVerifications({ size: 50 }).catch(() => ({ content: [] })),
        marketplacePublicApi.search({ size: 100 }).catch(() => ({ content: [] })),
        marketplaceAdminApi.getPlatformStats().catch(() => null),
      ]);

      setVerifications(verRes.content || []);
      setProfiles(profRes.content || []);
      setStats(statRes);
    } catch (err) {
      console.error('Failed to load admin marketplace data', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAdminData();
  }, []);

  const handleApproveVerification = async (v: MarketplaceVerification) => {
    setIsProcessing(true);
    try {
      await marketplaceAdminApi.processVerification(v.id, {
        verificationStatus: 'VERIFIED',
      });
      setSuccessBanner(`Approved verified practitioner status for ${v.organizationName || 'Practice'}!`);
      await loadAdminData();
    } catch (err) {
      alert('Failed to approve verification.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleRejectVerification = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rejectingItem) return;
    setIsProcessing(true);
    try {
      await marketplaceAdminApi.processVerification(rejectingItem.id, {
        verificationStatus: 'REJECTED',
        rejectionReason,
      });
      setRejectingItem(null);
      setRejectionReason('');
      setSuccessBanner('Credential submission rejected.');
      await loadAdminData();
    } catch (err) {
      alert('Failed to reject verification.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleToggleFeatured = async (profileId: string, currentFeatured: boolean) => {
    try {
      await marketplaceAdminApi.toggleFeatured(profileId, !currentFeatured);
      await loadAdminData();
    } catch (err) {
      alert('Failed to update featured status.');
    }
  };

  const handleTogglePublish = async (profileId: string, currentPublished: boolean) => {
    try {
      await marketplaceAdminApi.togglePublish(profileId, !currentPublished);
      await loadAdminData();
    } catch (err) {
      alert('Failed to update publish status.');
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6 pb-20">
      {/* Platform Admin Header */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 p-6 sm:p-8 rounded-3xl text-white shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-6 border border-slate-800">
        <div className="space-y-2">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/20 border border-indigo-400/30 text-indigo-300 text-xs font-semibold">
            <ShieldCheck className="w-3.5 h-3.5" />
            Taxoryn Super Admin Platform Governance
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold">Marketplace Directory & KYC Moderation</h1>
          <p className="text-xs sm:text-sm text-slate-300 max-w-2xl">
            Review and authenticate official ICAI COP / ICSI membership credentials, manage directory rankings, and monitor nationwide tax discovery metrics.
          </p>
        </div>
      </div>

      {successBanner && (
        <div className="bg-emerald-50 dark:bg-emerald-950/50 p-4 rounded-2xl border border-emerald-200 dark:border-emerald-800 flex items-center justify-between text-sm text-emerald-800 dark:text-emerald-200">
          <div className="flex items-center gap-2 font-medium">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span>{successBanner}</span>
          </div>
          <button onClick={() => setSuccessBanner(null)} className="text-emerald-600">×</button>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-1">
          <div className="text-xs font-bold uppercase text-slate-400">Total Listed Firms</div>
          <div className="text-2xl font-extrabold text-slate-900 dark:text-white">{stats?.totalListedPractitioners || profiles.length}</div>
          <div className="text-[11px] text-slate-500">Live in public directory</div>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-1">
          <div className="text-xs font-bold uppercase text-slate-400">Pending KYC Verifications</div>
          <div className="text-2xl font-extrabold text-amber-600">{stats?.totalPendingVerifications || verifications.length}</div>
          <div className="text-[11px] text-slate-500">Awaiting Super Admin review</div>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-1">
          <div className="text-xs font-bold uppercase text-slate-400">Verified Practices</div>
          <div className="text-2xl font-extrabold text-emerald-600">{stats?.totalVerifiedPractitioners || 0}</div>
          <div className="text-[11px] text-emerald-600 font-semibold">ICAI / ICSI Authenticated</div>
        </div>

        <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-1">
          <div className="text-xs font-bold uppercase text-slate-400">Total Inbound Inquiries</div>
          <div className="text-2xl font-extrabold text-indigo-600">{stats?.totalInboundLeads || 0}</div>
          <div className="text-[11px] text-slate-500">{stats?.leadConversionRate || 0}% Lead conversion rate</div>
        </div>
      </div>

      {/* Nav Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-200 dark:border-slate-800 pb-3">
        <button
          onClick={() => setActiveTab('VERIFICATIONS')}
          className={clsx(
            'px-5 py-2.5 rounded-2xl text-xs sm:text-sm font-bold transition-all flex items-center gap-2',
            activeTab === 'VERIFICATIONS'
              ? 'bg-indigo-600 text-white shadow-md'
              : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
          )}
        >
          <ShieldCheck className="w-4 h-4" />
          <span>Pending KYC Queue ({verifications.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('LISTINGS')}
          className={clsx(
            'px-5 py-2.5 rounded-2xl text-xs sm:text-sm font-bold transition-all flex items-center gap-2',
            activeTab === 'LISTINGS'
              ? 'bg-indigo-600 text-white shadow-md'
              : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
          )}
        >
          <Building className="w-4 h-4" />
          <span>Directory Listings & Featured Ranks ({profiles.length})</span>
        </button>
      </div>

      {/* Tab 1: Verifications Queue */}
      {activeTab === 'VERIFICATIONS' && (
        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
          {verifications.length === 0 ? (
            <div className="p-12 text-center space-y-3">
              <CheckCircle2 className="w-10 h-10 text-emerald-500 mx-auto" />
              <h3 className="text-base font-bold text-slate-900 dark:text-white">KYC Queue Up to Date</h3>
              <p className="text-xs text-slate-500">All submitted practitioner credentials have been processed.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-slate-200 dark:border-slate-800 bg-slate-50/75 dark:bg-slate-800/40 text-slate-400 font-bold uppercase tracking-wider">
                    <th className="p-4">Practice / Organization</th>
                    <th className="p-4">Statutory Body</th>
                    <th className="p-4">Membership No. (MRN)</th>
                    <th className="p-4">COP / FRN</th>
                    <th className="p-4">Certificate Document</th>
                    <th className="p-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {verifications.map((v) => (
                    <tr key={v.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30">
                      <td className="p-4">
                        <div className="font-bold text-slate-900 dark:text-white">{v.organizationName || 'Practice'}</div>
                        <div className="text-[11px] text-slate-400">{new Date(v.createdAt).toLocaleDateString('en-IN')}</div>
                      </td>
                      <td className="p-4 font-semibold text-slate-700 dark:text-slate-300">{v.professionalBody}</td>
                      <td className="p-4 font-mono font-bold text-indigo-600 dark:text-indigo-400">{v.membershipNumber}</td>
                      <td className="p-4 text-slate-600 dark:text-slate-400">
                        {v.copNumber ? `COP: ${v.copNumber}` : ''} {v.firmRegistrationNumber ? `| FRN: ${v.firmRegistrationNumber}` : ''}
                      </td>
                      <td className="p-4">
                        {v.documentUrl ? (
                          <a
                            href={v.documentUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-1 text-indigo-600 hover:underline font-semibold"
                          >
                            <FileText className="w-3.5 h-3.5" />
                            <span>View Certificate</span>
                            <ExternalLink className="w-3 h-3" />
                          </a>
                        ) : (
                          <span className="text-slate-400">Self-attested</span>
                        )}
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <Button
                            size="sm"
                            variant="primary"
                            disabled={isProcessing}
                            onClick={() => handleApproveVerification(v)}
                            className="bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs"
                          >
                            <CheckCircle2 className="w-3.5 h-3.5 mr-1" />
                            Approve Verified Badge
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            disabled={isProcessing}
                            onClick={() => setRejectingItem(v)}
                            className="text-rose-600 hover:bg-rose-50 rounded-xl text-xs"
                          >
                            <XCircle className="w-3.5 h-3.5 mr-1" />
                            Reject
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Tab 2: Directory Listings & Featured Toggle */}
      {activeTab === 'LISTINGS' && (
        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 dark:border-slate-800 bg-slate-50/75 dark:bg-slate-800/40 text-slate-400 font-bold uppercase tracking-wider">
                  <th className="p-4">Firm Profile</th>
                  <th className="p-4">City / State</th>
                  <th className="p-4">Designation</th>
                  <th className="p-4">Rating</th>
                  <th className="p-4">Verified</th>
                  <th className="p-4">Featured</th>
                  <th className="p-4">Publish State</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {profiles.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30">
                    <td className="p-4">
                      <div className="font-bold text-slate-900 dark:text-white">{p.displayName}</div>
                      <div className="text-[11px] text-slate-400">{p.slug}</div>
                    </td>
                    <td className="p-4 text-slate-600 dark:text-slate-300">{p.city}, {p.state}</td>
                    <td className="p-4 text-slate-600 dark:text-slate-300">{p.professionalType?.replace(/_/g, ' ')}</td>
                    <td className="p-4 font-bold text-amber-500 flex items-center gap-1">
                      <Star className="w-3.5 h-3.5 fill-current" />
                      <span>{p.averageRating}</span>
                    </td>
                    <td className="p-4">
                      <span
                        className={clsx(
                          'px-2 py-0.5 rounded text-[10px] font-bold',
                          p.verificationStatus === 'VERIFIED'
                            ? 'bg-emerald-50 text-emerald-700'
                            : 'bg-slate-100 text-slate-500'
                        )}
                      >
                        {p.verificationStatus}
                      </span>
                    </td>
                    <td className="p-4">
                      <button
                        onClick={() => handleToggleFeatured(p.id, p.isFeatured)}
                        className={clsx(
                          'px-2.5 py-1 rounded-xl text-xs font-bold transition-all flex items-center gap-1',
                          p.isFeatured
                            ? 'bg-amber-500 text-white shadow-sm'
                            : 'bg-slate-100 dark:bg-slate-800 text-slate-500 hover:bg-slate-200'
                        )}
                      >
                        <Sparkles className="w-3.5 h-3.5" />
                        {p.isFeatured ? 'Featured' : 'Standard'}
                      </button>
                    </td>
                    <td className="p-4">
                      <button
                        onClick={() => handleTogglePublish(p.id, p.isPublished)}
                        className={clsx(
                          'px-2.5 py-1 rounded-xl text-xs font-bold transition-all',
                          p.isPublished
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-rose-50 text-rose-700 border border-rose-200'
                        )}
                      >
                        {p.isPublished ? 'Published' : 'Hidden / Suspended'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Rejection Modal */}
      {rejectingItem && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-md w-full p-6 space-y-4 shadow-2xl">
            <h3 className="text-base font-bold text-slate-900 dark:text-white">Reject Credential Verification</h3>
            <p className="text-xs text-slate-500">Provide reason for rejecting membership credentials for this firm.</p>

            <form onSubmit={handleRejectVerification} className="space-y-3">
              <textarea
                required
                rows={3}
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
                placeholder="e.g. Uploaded COP certificate is expired or MRN does not match the official ICAI register..."
              />

              <div className="flex justify-end gap-2 pt-2">
                <Button type="button" variant="outline" size="sm" onClick={() => setRejectingItem(null)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" disabled={isProcessing} className="bg-rose-600 hover:bg-rose-700 text-white">
                  Confirm Rejection
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
