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
  Layers,
  Plus,
  Edit2,
  Tag,
  BookmarkCheck,
  Trash2,
  AlertCircle,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { marketplaceAdminApi, marketplacePublicApi, taxServiceAdminApi } from '../api/endpoints';
import {
  MarketplaceVerification,
  MarketplaceProfile,
  MarketplaceStats,
  TaxServiceCategory,
  TaxService,
  TaxServiceAlias,
  CreateTaxServiceCategoryRequest,
  CreateTaxServiceRequest,
  UpdateTaxServiceRequest,
} from '../types';
import { useAuth } from '../context/AuthContext';
import { getWorkspaceDisplayName } from '../config/roleWorkspaceConfig';
import clsx from 'clsx';

export const PlatformAdminMarketplacePage: React.FC = () => {
  const { user } = useAuth();
  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const [activeTab, setActiveTab] = useState<'VERIFICATIONS' | 'LISTINGS' | 'PLATFORM_KPIS' | 'TAX_SERVICES_MASTER'>('VERIFICATIONS');
  const [verifications, setVerifications] = useState<MarketplaceVerification[]>([]);
  const [profiles, setProfiles] = useState<MarketplaceProfile[]>([]);
  const [stats, setStats] = useState<MarketplaceStats | null>(null);
  // Tax Service Master State
  const [categories, setCategories] = useState<TaxServiceCategory[]>([]);
  const [services, setServices] = useState<TaxService[]>([]);
  const [selectedCatFilter, setSelectedCatFilter] = useState<string>('ALL');
  const [serviceSearch, setServiceSearch] = useState<string>('');
  
  // Service Master Modals
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState<boolean>(false);
  const [newCatCode, setNewCatCode] = useState<string>('');
  const [newCatName, setNewCatName] = useState<string>('');
  const [newCatDesc, setNewCatDesc] = useState<string>('');
  
  const [isServiceModalOpen, setIsServiceModalOpen] = useState<boolean>(false);
  const [newSvcCategoryId, setNewSvcCategoryId] = useState<string>('');
  const [newSvcCode, setNewSvcCode] = useState<string>('');
  const [newSvcName, setNewSvcName] = useState<string>('');
  const [newSvcDesc, setNewSvcDesc] = useState<string>('');
  const [newSvcAliases, setNewSvcAliases] = useState<string>('');

  const [editingService, setEditingService] = useState<TaxService | null>(null);
  const [editSvcName, setEditSvcName] = useState<string>('');
  const [editSvcDesc, setEditSvcDesc] = useState<string>('');
  const [editSvcSort, setEditSvcSort] = useState<number>(0);
  const [editSvcCategoryId, setEditSvcCategoryId] = useState<string>('');

  const [aliasModalService, setAliasModalService] = useState<TaxService | null>(null);
  const [serviceAliases, setServiceAliases] = useState<TaxServiceAlias[]>([]);
  const [newAliasInput, setNewAliasInput] = useState<string>('');

  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [successBanner, setSuccessBanner] = useState<string | null>(null);

  // Rejection Modal
  const [rejectingItem, setRejectingItem] = useState<MarketplaceVerification | null>(null);
  const [rejectionReason, setRejectionReason] = useState<string>('');

  const loadAdminData = async () => {
    setIsLoading(true);
    try {
      const [verRes, profRes, statRes, catRes, svcRes] = await Promise.all([
        marketplaceAdminApi.getPendingVerifications({ size: 50 }).catch(() => ({ content: [] })),
        marketplacePublicApi.search({ size: 100 }).catch(() => ({ content: [] })),
        marketplaceAdminApi.getPlatformStats().catch(() => null),
        taxServiceAdminApi.getCategories().catch(() => []),
        taxServiceAdminApi.getTaxServices({ size: 200 }).catch(() => ({ content: [] })),
      ]);

      setVerifications(verRes.content || []);
      setProfiles(profRes.content || []);
      setStats(statRes);
      setCategories(catRes || []);
      setServices(svcRes.content || []);
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

  // --- Tax Service Master Handlers ---
  const handleToggleCategoryStatus = async (cat: TaxServiceCategory) => {
    try {
      await taxServiceAdminApi.toggleCategoryStatus(cat.id, !cat.isActive);
      setSuccessBanner(`Updated ${cat.name} category status!`);
      await loadAdminData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update category status.');
    }
  };

  const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsProcessing(true);
    try {
      await taxServiceAdminApi.createCategory({
        code: newCatCode.trim().toUpperCase(),
        name: newCatName.trim(),
        description: newCatDesc.trim(),
      });
      setIsCategoryModalOpen(false);
      setNewCatCode('');
      setNewCatName('');
      setNewCatDesc('');
      setSuccessBanner('Created tax service category successfully!');
      await loadAdminData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create category.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleToggleServiceStatus = async (svc: TaxService) => {
    try {
      await taxServiceAdminApi.toggleTaxServiceStatus(svc.id, !svc.isActive);
      setSuccessBanner(`Updated ${svc.name} status!`);
      await loadAdminData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update service status.');
    }
  };

  const handleCreateService = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsProcessing(true);
    try {
      const aliasList = newSvcAliases
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
      await taxServiceAdminApi.createTaxService({
        categoryId: newSvcCategoryId,
        code: newSvcCode.trim().toUpperCase(),
        name: newSvcName.trim(),
        description: newSvcDesc.trim(),
        aliases: aliasList,
      });
      setIsServiceModalOpen(false);
      setNewSvcCode('');
      setNewSvcName('');
      setNewSvcDesc('');
      setNewSvcCategoryId('');
      setNewSvcAliases('');
      setSuccessBanner('Created master tax service successfully!');
      await loadAdminData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create service.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleOpenEditService = (svc: TaxService) => {
    setEditingService(svc);
    setEditSvcName(svc.name);
    setEditSvcDesc(svc.description || '');
    setEditSvcSort(svc.sortOrder || 0);
    setEditSvcCategoryId(svc.categoryId);
  };

  const handleUpdateService = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingService) return;
    setIsProcessing(true);
    try {
      await taxServiceAdminApi.updateTaxService(editingService.id, {
        name: editSvcName.trim(),
        description: editSvcDesc.trim(),
        sortOrder: editSvcSort,
        categoryId: editSvcCategoryId,
      });
      setEditingService(null);
      setSuccessBanner('Updated tax service successfully!');
      await loadAdminData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update service.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleOpenAliasesModal = async (svc: TaxService) => {
    setAliasModalService(svc);
    setNewAliasInput('');
    try {
      const aliases = await taxServiceAdminApi.getAliases(svc.id);
      setServiceAliases(aliases || []);
    } catch (err) {
      setServiceAliases([]);
    }
  };

  const handleAddAlias = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!aliasModalService || !newAliasInput.trim()) return;
    try {
      await taxServiceAdminApi.addAlias(aliasModalService.id, {
        alias: newAliasInput.trim(),
      });
      setNewAliasInput('');
      const updated = await taxServiceAdminApi.getAliases(aliasModalService.id);
      setServiceAliases(updated || []);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to add alias.');
    }
  };

  const handleDeleteAlias = async (aliasId: string) => {
    if (!aliasModalService) return;
    try {
      await taxServiceAdminApi.deleteAlias(aliasModalService.id, aliasId);
      const updated = await taxServiceAdminApi.getAliases(aliasModalService.id);
      setServiceAliases(updated || []);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to delete alias.');
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6 pb-20">
      {/* Platform Admin Header */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 p-6 sm:p-8 rounded-3xl text-white shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-6 border border-slate-800">
        <div className="space-y-2">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/20 border border-indigo-400/30 text-indigo-300 text-xs font-semibold">
            <ShieldCheck className="w-3.5 h-3.5" />
            {getWorkspaceDisplayName(userRoleCodes)} Platform Governance
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

        <button
          onClick={() => setActiveTab('TAX_SERVICES_MASTER')}
          className={clsx(
            'px-5 py-2.5 rounded-2xl text-xs sm:text-sm font-bold transition-all flex items-center gap-2',
            activeTab === 'TAX_SERVICES_MASTER'
              ? 'bg-indigo-600 text-white shadow-md'
              : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
          )}
        >
          <Layers className="w-4 h-4" />
          <span>Tax Services Master ({services.length})</span>
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

      {/* Tab 3: Tax Services Master Governance */}
      {activeTab === 'TAX_SERVICES_MASTER' && (
        <div className="space-y-6">
          {/* Categories Overview Bar */}
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 dark:border-slate-800 pb-4">
              <div>
                <h3 className="text-base font-bold text-slate-900 dark:text-white">Tax Service Categories</h3>
                <p className="text-xs text-slate-500">Standardized platform categories for direct and indirect Indian tax compliance.</p>
              </div>
              <Button
                variant="primary"
                size="sm"
                onClick={() => setIsCategoryModalOpen(true)}
              >
                <Plus className="w-4 h-4 mr-1.5" />
                Add Category
              </Button>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {categories.map((cat) => {
                const catServiceCount = services.filter((s) => s.categoryId === cat.id).length;
                return (
                  <div
                    key={cat.id}
                    className={clsx(
                      'p-4 rounded-2xl border transition-all flex items-start justify-between gap-3',
                      cat.isActive
                        ? 'bg-slate-50/70 dark:bg-slate-800/50 border-slate-200 dark:border-slate-700'
                        : 'bg-slate-100/50 dark:bg-slate-900/60 border-slate-200 opacity-60'
                    )}
                  >
                    <div className="space-y-1 flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-[10px] font-bold px-2 py-0.5 rounded bg-indigo-100 text-indigo-800 uppercase">
                          {cat.code}
                        </span>
                        <span className="text-xs font-bold text-slate-900 dark:text-white truncate">
                          {cat.name}
                        </span>
                      </div>
                      <p className="text-[11px] text-slate-500 line-clamp-1">{cat.description || 'No description'}</p>
                      <span className="text-[10px] font-semibold text-slate-400 block">
                        {catServiceCount} Standard Services
                      </span>
                    </div>

                    <button
                      onClick={() => handleToggleCategoryStatus(cat)}
                      className={clsx(
                        'px-2 py-0.5 rounded-full text-[10px] font-bold shrink-0 transition',
                        cat.isActive
                          ? 'bg-emerald-100 text-emerald-800 hover:bg-emerald-200'
                          : 'bg-rose-100 text-rose-800 hover:bg-rose-200'
                      )}
                    >
                      {cat.isActive ? 'Active' : 'Inactive'}
                    </button>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Master Services Table */}
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden space-y-4 p-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 dark:border-slate-800 pb-4">
              <div>
                <h3 className="text-base font-bold text-slate-900 dark:text-white">Controlled Master Tax Services</h3>
                <p className="text-xs text-slate-500">
                  Standardized service entries with immutable codes, customer-friendly descriptions, and search aliases.
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-3">
                {/* Search */}
                <div className="relative">
                  <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type="text"
                    value={serviceSearch}
                    onChange={(e) => setServiceSearch(e.target.value)}
                    placeholder="Search service name or code..."
                    className="pl-8 pr-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 text-xs bg-slate-50 dark:bg-slate-800"
                  />
                </div>

                {/* Category Dropdown */}
                <select
                  value={selectedCatFilter}
                  onChange={(e) => setSelectedCatFilter(e.target.value)}
                  className="px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 text-xs bg-slate-50 dark:bg-slate-800"
                >
                  <option value="ALL">All Categories</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>

                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => {
                    setNewSvcCategoryId(categories[0]?.id || '');
                    setIsServiceModalOpen(true);
                  }}
                >
                  <Plus className="w-4 h-4 mr-1.5" />
                  Add Master Service
                </Button>
              </div>
            </div>

            {/* Services Table */}
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-slate-200 dark:border-slate-800 bg-slate-50/75 dark:bg-slate-800/40 text-slate-400 font-bold uppercase tracking-wider">
                    <th className="p-3.5">Service Code</th>
                    <th className="p-3.5">Service Title & Description</th>
                    <th className="p-3.5">Category</th>
                    <th className="p-3.5">Aliases</th>
                    <th className="p-3.5">Sort</th>
                    <th className="p-3.5">Status</th>
                    <th className="p-3.5 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
                  {services
                    .filter((s) => selectedCatFilter === 'ALL' || s.categoryId === selectedCatFilter)
                    .filter((s) => {
                      if (!serviceSearch) return true;
                      const q = serviceSearch.toLowerCase();
                      return s.code.toLowerCase().includes(q) || s.name.toLowerCase().includes(q) || (s.description && s.description.toLowerCase().includes(q));
                    })
                    .map((s) => (
                      <tr key={s.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition">
                        <td className="p-3.5 font-mono font-bold text-indigo-600 dark:text-indigo-400">
                          {s.code}
                        </td>
                        <td className="p-3.5 max-w-xs">
                          <div className="font-bold text-slate-900 dark:text-white">{s.name}</div>
                          {s.description && (
                            <div className="text-[11px] text-slate-500 line-clamp-1">{s.description}</div>
                          )}
                        </td>
                        <td className="p-3.5">
                          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-700">
                            {s.categoryName || categories.find((c) => c.id === s.categoryId)?.name || 'Category'}
                          </span>
                        </td>
                        <td className="p-3.5">
                          <button
                            onClick={() => handleOpenAliasesModal(s)}
                            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl text-[11px] font-bold bg-indigo-50 text-indigo-700 hover:bg-indigo-100 transition"
                          >
                            <Tag className="w-3 h-3" />
                            <span>Manage Aliases</span>
                          </button>
                        </td>
                        <td className="p-3.5 font-mono text-slate-500 font-semibold">
                          {s.sortOrder}
                        </td>
                        <td className="p-3.5">
                          <button
                            onClick={() => handleToggleServiceStatus(s)}
                            className={clsx(
                              'px-2.5 py-1 rounded-xl text-xs font-bold transition-all',
                              s.isActive
                                ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                                : 'bg-rose-50 text-rose-700 border border-rose-200'
                            )}
                          >
                            {s.isActive ? 'Active' : 'Inactive'}
                          </button>
                        </td>
                        <td className="p-3.5 text-right">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleOpenEditService(s)}
                            className="text-xs"
                          >
                            <Edit2 className="w-3.5 h-3.5 mr-1" />
                            Edit
                          </Button>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
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

      {/* 1. Create Tax Service Category Modal */}
      {isCategoryModalOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-md w-full p-6 space-y-4 shadow-2xl">
            <h3 className="text-base font-bold text-slate-900 dark:text-white">Add Tax Service Category</h3>
            <p className="text-xs text-slate-500">Create a high-level category to organize Indian tax and compliance services.</p>

            <form onSubmit={handleCreateCategory} className="space-y-3 text-xs">
              <div>
                <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">
                  Category Code (UPPERCASE_SNAKE) <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={newCatCode}
                  onChange={(e) => setNewCatCode(e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '_'))}
                  placeholder="e.g. CUSTOMS_EXCISE"
                  className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 font-mono"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">
                  Category Name <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={newCatName}
                  onChange={(e) => setNewCatName(e.target.value)}
                  placeholder="e.g. Customs & Foreign Trade"
                  className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">
                  Description
                </label>
                <textarea
                  rows={2}
                  value={newCatDesc}
                  onChange={(e) => setNewCatDesc(e.target.value)}
                  placeholder="Brief summary of services in this category"
                  className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button type="button" variant="outline" size="sm" onClick={() => setIsCategoryModalOpen(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" disabled={isProcessing}>
                  Create Category
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 2. Create Master Tax Service Modal */}
      {isServiceModalOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-lg w-full p-6 space-y-4 shadow-2xl">
            <h3 className="text-base font-bold text-slate-900 dark:text-white">Create Master Tax Service</h3>
            <p className="text-xs text-slate-500">
              Register a new standardized service. The service code will become immutable once created.
            </p>

            <form onSubmit={handleCreateService} className="space-y-3 text-xs">
              <div>
                <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">
                  Category <span className="text-rose-500">*</span>
                </label>
                <select
                  required
                  value={newSvcCategoryId}
                  onChange={(e) => setNewSvcCategoryId(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700"
                >
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>{c.name} ({c.code})</option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">
                    Service Code (Immutable) <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={newSvcCode}
                    onChange={(e) => setNewSvcCode(e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '_'))}
                    placeholder="e.g. TDS_RETURN_FILING"
                    className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 font-mono"
                  />
                </div>

                <div>
                  <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">
                    Service Title <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={newSvcName}
                    onChange={(e) => setNewSvcName(e.target.value)}
                    placeholder="e.g. TDS Return Filing"
                    className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700"
                  />
                </div>
              </div>

              <div>
                <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">
                  Description
                </label>
                <textarea
                  rows={2}
                  value={newSvcDesc}
                  onChange={(e) => setNewSvcDesc(e.target.value)}
                  placeholder="Customer-friendly summary of deliverables and scope"
                  className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">
                  Search Aliases (Comma-separated)
                </label>
                <input
                  type="text"
                  value={newSvcAliases}
                  onChange={(e) => setNewSvcAliases(e.target.value)}
                  placeholder="e.g. TDS, 26Q, 24Q, TDS Filing"
                  className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700"
                />
                <p className="text-[10px] text-slate-400 mt-1">Users searching for these keywords will match this master service.</p>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button type="button" variant="outline" size="sm" onClick={() => setIsServiceModalOpen(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" disabled={isProcessing}>
                  Create Service
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 3. Edit Master Tax Service Modal */}
      {editingService && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-lg w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-slate-900 dark:text-white">Edit Master Tax Service</h3>
              <span className="font-mono text-xs font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                {editingService.code}
              </span>
            </div>
            <p className="text-xs text-slate-500">Service code is immutable to preserve integrity across historical bookings and proposals.</p>

            <form onSubmit={handleUpdateService} className="space-y-3 text-xs">
              <div>
                <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">
                  Category <span className="text-rose-500">*</span>
                </label>
                <select
                  required
                  value={editSvcCategoryId}
                  onChange={(e) => setEditSvcCategoryId(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700"
                >
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>{c.name} ({c.code})</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">
                  Service Title <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={editSvcName}
                  onChange={(e) => setEditSvcName(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">
                  Description
                </label>
                <textarea
                  rows={3}
                  value={editSvcDesc}
                  onChange={(e) => setEditSvcDesc(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">
                  Sort Order Index
                </label>
                <input
                  type="number"
                  value={editSvcSort}
                  onChange={(e) => setEditSvcSort(parseInt(e.target.value, 10) || 0)}
                  className="w-full px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button type="button" variant="outline" size="sm" onClick={() => setEditingService(null)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" disabled={isProcessing}>
                  Save Changes
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 4. Manage Aliases Modal */}
      {aliasModalService && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-white">
                Search Aliases for {aliasModalService.name}
              </h3>
              <p className="text-xs text-slate-500 font-mono mt-0.5">{aliasModalService.code}</p>
            </div>

            {/* Add Alias Form */}
            <form onSubmit={handleAddAlias} className="flex gap-2">
              <input
                type="text"
                required
                value={newAliasInput}
                onChange={(e) => setNewAliasInput(e.target.value)}
                placeholder="Add alias (e.g. 'ITR-1', 'GSTR-3B')"
                className="flex-1 px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-xs"
              />
              <Button type="submit" variant="primary" size="sm">
                Add
              </Button>
            </form>

            {/* Existing Aliases List */}
            <div className="space-y-2 max-h-60 overflow-y-auto pt-2">
              {serviceAliases.length === 0 ? (
                <div className="p-4 text-center text-xs text-slate-400 bg-slate-50 dark:bg-slate-800/40 rounded-xl">
                  No search aliases added yet.
                </div>
              ) : (
                serviceAliases.map((a) => (
                  <div
                    key={a.id}
                    className="flex items-center justify-between p-2.5 bg-slate-50 dark:bg-slate-800/60 rounded-xl border border-slate-100 dark:border-slate-700/60 text-xs"
                  >
                    <div className="space-y-0.5">
                      <span className="font-semibold text-slate-800 dark:text-slate-200">{a.alias}</span>
                      <span className="text-[10px] text-slate-400 block font-mono">
                        normalized: {a.normalizedAlias}
                      </span>
                    </div>

                    <button
                      type="button"
                      onClick={() => handleDeleteAlias(a.id)}
                      className="text-rose-500 hover:text-rose-700 p-1 transition"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))
              )}
            </div>

            <div className="flex justify-end pt-2">
              <Button type="button" variant="outline" size="sm" onClick={() => setAliasModalService(null)}>
                Close
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
