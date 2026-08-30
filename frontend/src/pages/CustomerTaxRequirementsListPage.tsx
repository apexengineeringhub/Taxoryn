import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  FileText,
  Plus,
  Clock,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Eye,
  ArrowRight,
  Send,
  Trash2,
  Calendar,
  Layers,
  MapPin,
  LogOut,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { customerTaxRequirementApi } from '../api/endpoints';
import {
  CustomerTaxRequirementSummary,
  CustomerTaxRequirement,
  TaxRequirementStatus,
} from '../types';
import { useAuth } from '../context/AuthContext';
import clsx from 'clsx';

export const CustomerTaxRequirementsListPage: React.FC = () => {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const [requirements, setRequirements] = useState<CustomerTaxRequirementSummary[]>([]);
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [page, setPage] = useState<number>(0);

  // Detail / View Modal State
  const [viewingItem, setViewingItem] = useState<CustomerTaxRequirement | null>(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState<boolean>(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  useEffect(() => {
    loadRequirements();
  }, [activeTab, page]);

  const loadRequirements = async () => {
    try {
      setIsLoading(true);
      const statusParam = activeTab === 'ALL' ? undefined : (activeTab as TaxRequirementStatus);
      const res = await customerTaxRequirementApi.list({
        status: statusParam,
        page,
        size: 10,
      });
      setRequirements(res.content || []);
      setTotalElements(res.totalElements || 0);
    } catch (err) {
      console.error('Failed to load customer requirements', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleOpenDetail = async (id: string) => {
    setActionError(null);
    try {
      setIsLoadingDetail(true);
      const item = await customerTaxRequirementApi.getById(id);
      setViewingItem(item);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to fetch requirement details.');
    } finally {
      setIsLoadingDetail(false);
    }
  };

  const handleSubmitDraft = async (id: string) => {
    if (!window.confirm('Submit this requirement to the marketplace?')) return;
    try {
      await customerTaxRequirementApi.submit(id);
      setActionSuccess('Requirement submitted successfully!');
      if (viewingItem && viewingItem.id === id) {
        setViewingItem(null);
      }
      await loadRequirements();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to submit requirement.');
    }
  };

  const handleCancelRequirement = async (id: string) => {
    if (!window.confirm('Are you sure you want to cancel this tax requirement?')) return;
    try {
      await customerTaxRequirementApi.cancel(id);
      setActionSuccess('Tax requirement cancelled.');
      if (viewingItem && viewingItem.id === id) {
        setViewingItem(null);
      }
      await loadRequirements();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to cancel requirement.');
    }
  };

  const getStatusBadge = (status: TaxRequirementStatus) => {
    switch (status) {
      case 'DRAFT':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-200">
            DRAFT
          </span>
        );
      case 'SUBMITTED':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
            SUBMITTED
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-600 border border-slate-200">
            CANCELLED
          </span>
        );
      case 'CLOSED':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-800 border border-indigo-200">
            CLOSED
          </span>
        );
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 pb-20">
      {/* Header */}
      <div className="bg-white border-b border-slate-200">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-xl font-bold text-slate-900">My Tax Requirements</h1>
            <p className="text-xs text-slate-500">Track and manage your tax filings, advisory, and compliance requests.</p>
          </div>

          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigate('/marketplace/customer/dashboard')}
            >
              Dashboard
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={() => navigate('/marketplace/customer/requirements/new')}
              className="bg-indigo-600 hover:bg-indigo-700 text-white"
            >
              <Plus className="w-4 h-4 mr-1.5" />
              Tell Us Your Tax Need
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => logout()}
              className="text-xs text-rose-600 border-rose-200 hover:bg-rose-50 hover:text-rose-700 flex items-center gap-1.5"
            >
              <LogOut className="w-3.5 h-3.5" />
              <span>Sign Out</span>
            </Button>
          </div>
        </div>

        {/* Tab Filters */}
        <div className="max-w-6xl mx-auto px-4 sm:px-6 flex items-center gap-2 border-t border-slate-100 pt-2 pb-3 overflow-x-auto">
          {['ALL', 'DRAFT', 'SUBMITTED', 'CANCELLED', 'CLOSED'].map((tab) => (
            <button
              key={tab}
              onClick={() => {
                setActiveTab(tab);
                setPage(0);
              }}
              className={clsx(
                'px-4 py-1.5 rounded-xl text-xs font-bold transition shrink-0',
                activeTab === tab
                  ? 'bg-indigo-600 text-white shadow-xs'
                  : 'text-slate-600 hover:bg-slate-100'
              )}
            >
              {tab.charAt(0) + tab.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      {/* Main Content Area */}
      <div className="max-w-6xl mx-auto px-4 sm:px-6 mt-8 space-y-6">
        {actionSuccess && (
          <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 flex items-center justify-between">
            <span>{actionSuccess}</span>
            <button onClick={() => setActionSuccess(null)} className="font-bold ml-2">✕</button>
          </div>
        )}

        {isLoading ? (
          <div className="p-16 text-center text-xs text-slate-400">
            <div className="w-5 h-5 border-2 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
            <span>Loading your tax requirements...</span>
          </div>
        ) : requirements.length === 0 ? (
          <div className="bg-white rounded-3xl border border-slate-200 p-12 text-center space-y-4 shadow-sm">
            <div className="w-14 h-14 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mx-auto">
              <FileText className="w-7 h-7" />
            </div>
            <div className="space-y-1">
              <h3 className="text-base font-bold text-slate-900">No Tax Requirements Found</h3>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                You haven't posted any {activeTab !== 'ALL' ? activeTab.toLowerCase() : ''} tax assistance requests yet.
              </p>
            </div>
            <Button
              variant="primary"
              size="md"
              onClick={() => navigate('/marketplace/customer/requirements/new')}
              className="px-6 rounded-xl"
            >
              <Plus className="w-4 h-4 mr-1.5" />
              Post Your First Tax Need
            </Button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {requirements.map((req) => (
              <div
                key={req.id}
                className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm hover:shadow-md transition-all flex flex-col justify-between space-y-4"
              >
                <div className="space-y-2">
                  <div className="flex items-start justify-between gap-2">
                    <div className="space-y-0.5">
                      <span className="font-mono text-[9px] font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-700 uppercase">
                        {req.categoryName || 'Tax Service'}
                      </span>
                      <h3 className="text-sm font-bold text-slate-900 line-clamp-1">{req.taxServiceName}</h3>
                    </div>
                    {getStatusBadge(req.status)}
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-xs text-slate-600 pt-1">
                    <div>
                      <span className="text-[10px] text-slate-400 uppercase font-bold block">Financial Year</span>
                      <span className="font-bold text-slate-800">{req.financialYearDisplay || 'N/A'}</span>
                    </div>

                    <div>
                      <span className="text-[10px] text-slate-400 uppercase font-bold block">Profile Type</span>
                      <span className="font-medium text-slate-800">
                        {req.customerTypeDisplayName || req.customerType?.replace(/_/g, ' ') || 'Individual'}
                      </span>
                    </div>
                  </div>

                  {req.city && (
                    <div className="flex items-center gap-1 text-[11px] text-slate-500 pt-1">
                      <MapPin className="w-3.5 h-3.5 text-rose-500 shrink-0" />
                      <span>{req.city}{req.state ? `, ${req.state}` : ''}</span>
                    </div>
                  )}
                </div>

                <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                  <span className="text-[10px] text-slate-400">
                    Created: {new Date(req.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
                  </span>

                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleOpenDetail(req.id)}
                      className="text-xs"
                    >
                      <Eye className="w-3.5 h-3.5 mr-1" />
                      View
                    </Button>

                    {req.editable && (
                      <Button
                        variant="primary"
                        size="sm"
                        onClick={() => handleSubmitDraft(req.id)}
                        className="text-xs bg-emerald-600 hover:bg-emerald-700 text-white"
                      >
                        <Send className="w-3.5 h-3.5 mr-1" />
                        Submit
                      </Button>
                    )}

                    {req.cancellable && (
                      <button
                        onClick={() => handleCancelRequirement(req.id)}
                        className="text-slate-400 hover:text-rose-600 p-1.5 transition text-xs"
                        title="Cancel requirement"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Detail Modal */}
      {viewingItem && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl border border-slate-200 max-w-lg w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-start justify-between gap-2 border-b border-slate-100 pb-3">
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-mono text-[9px] font-bold px-2 py-0.5 rounded bg-indigo-100 text-indigo-800">
                    {viewingItem.service?.code}
                  </span>
                  {getStatusBadge(viewingItem.status)}
                </div>
                <h3 className="text-base font-bold text-slate-900 pt-1">{viewingItem.service?.name}</h3>
              </div>
              <button
                onClick={() => setViewingItem(null)}
                className="text-slate-400 hover:text-slate-700 text-sm font-bold p-1"
              >
                ✕
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-3 bg-slate-50 p-3 rounded-2xl border border-slate-100">
                <div>
                  <span className="text-[10px] uppercase font-bold text-slate-400 block">Financial Year</span>
                  <span className="font-bold text-slate-900">{viewingItem.financialYearDisplay || 'N/A'}</span>
                </div>
                <div>
                  <span className="text-[10px] uppercase font-bold text-slate-400 block">Taxpayer Classification</span>
                  <span className="font-bold text-slate-900">
                    {viewingItem.customerTypeDisplayName || viewingItem.customerType?.replace(/_/g, ' ') || 'Not Specified'}
                  </span>
                </div>
              </div>

              {viewItemDetailLocation(viewingItem)}

              <div>
                <span className="text-[10px] uppercase font-bold text-slate-400 block mb-1">Requirement Notes</span>
                <div className="p-3 bg-slate-50 rounded-2xl border border-slate-100 text-slate-700 whitespace-pre-wrap leading-relaxed">
                  {viewingItem.description || 'No additional notes provided.'}
                </div>
              </div>

              <div className="text-[10px] text-slate-400 pt-1 flex justify-between">
                <span>Reference: {viewingItem.id}</span>
                <span>Submitted: {new Date(viewingItem.createdAt).toLocaleString('en-IN')}</span>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-100">
              {viewingItem.editable && (
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => handleSubmitDraft(viewingItem.id)}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white"
                >
                  <Send className="w-3.5 h-3.5 mr-1" />
                  Submit Requirement
                </Button>
              )}

              {viewingItem.cancellable && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleCancelRequirement(viewingItem.id)}
                  className="text-rose-600 border-rose-200 hover:bg-rose-50"
                >
                  Cancel Requirement
                </Button>
              )}

              <Button variant="outline" size="sm" onClick={() => setViewingItem(null)}>
                Close
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const viewItemDetailLocation = (item: CustomerTaxRequirement) => {
  if (!item.city && !item.state) return null;
  return (
    <div>
      <span className="text-[10px] uppercase font-bold text-slate-400 block mb-1">Preferred Location</span>
      <div className="flex items-center gap-1.5 text-slate-700 font-medium">
        <MapPin className="w-3.5 h-3.5 text-rose-500 shrink-0" />
        <span>{item.city ? `${item.city}, ` : ''}{item.state || ''}</span>
      </div>
    </div>
  );
};
