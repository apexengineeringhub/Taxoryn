import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import {
  Briefcase,
  Plus,
  Search,
  Filter,
  ArrowUpDown,
  Building2,
  Calendar,
  User,
  ShieldCheck,
  Clock,
  AlertCircle,
  CheckCircle2,
  XCircle,
  MoreVertical,
  ExternalLink,
  ChevronLeft,
  ChevronRight,
  Layers,
  Sparkles,
} from 'lucide-react';
import { engagementsApi, servicesApi, clientApi } from '../api/endpoints';
import {
  EngagementDto,
  EngagementStatusType,
  EngagementPriorityType,
  ServiceDto,
  Client,
  CreateEngagementPayload,
} from '../types';
import { StatusBadge } from '../components/common/StatusBadge';
import { Modal } from '../components/common/Modal';
import clsx from 'clsx';

export const EngagementsPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // Filter States
  const [search, setSearch] = useState<string>(searchParams.get('search') || '');
  const [statusFilter, setStatusFilter] = useState<string>(searchParams.get('status') || '');
  const [priorityFilter, setPriorityFilter] = useState<string>(searchParams.get('priority') || '');
  const [serviceFilter, setServiceFilter] = useState<string>(searchParams.get('serviceId') || '');
  const [currentPage, setCurrentPage] = useState<number>(0);
  const pageSize = 12;

  // Data States
  const [engagements, setEngagements] = useState<EngagementDto[]>([]);
  const [services, setServices] = useState<ServiceDto[]>([]);
  const [clients, setClients] = useState<Client[]>([]);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(0);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Create Modal State
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [formData, setFormData] = useState<CreateEngagementPayload>({
    clientId: '',
    serviceId: '',
    name: '',
    description: '',
    status: 'ACTIVE',
    priority: 'MEDIUM',
    startDate: new Date().toISOString().split('T')[0],
    endDate: '',
    notes: '',
  });

  // Load Services and Clients for dropdowns
  useEffect(() => {
    const fetchMetadata = async () => {
      try {
        const [servicesRes, clientsRes] = await Promise.all([
          servicesApi.getAll().catch(() => []),
          clientApi.getAll({ size: 200, status: 'ACTIVE' }).catch(() => ({ content: [] })),
        ]);
        setServices(servicesRes || []);
        setClients(clientsRes.content || []);
      } catch (err) {
        console.error('Failed to load metadata', err);
      }
    };
    fetchMetadata();
  }, []);

  // Load Engagements
  const loadEngagements = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await engagementsApi.getAll({
        search: search.trim() || undefined,
        status: (statusFilter as EngagementStatusType) || undefined,
        priority: (priorityFilter as EngagementPriorityType) || undefined,
        serviceId: serviceFilter || undefined,
        page: currentPage,
        size: pageSize,
        sortBy: 'createdAt',
        sortDirection: 'desc',
      });
      setEngagements(response.content || []);
      setTotalElements(response.totalElements || 0);
      setTotalPages(response.totalPages || 0);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load client engagements');
    } finally {
      setIsLoading(false);
    }
  }, [search, statusFilter, priorityFilter, serviceFilter, currentPage]);

  useEffect(() => {
    loadEngagements();
  }, [loadEngagements]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    loadEngagements();
  };

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.clientId || !formData.name.trim()) {
      alert('Please select a client and enter an engagement mandate name');
      return;
    }

    try {
      setIsSubmitting(true);
      const created = await engagementsApi.create({
        ...formData,
        serviceId: formData.serviceId || undefined,
        startDate: formData.startDate || undefined,
        endDate: formData.endDate || undefined,
        notes: formData.notes?.trim() || undefined,
        description: formData.description?.trim() || undefined,
      });
      setIsCreateModalOpen(false);
      setFormData({
        clientId: '',
        serviceId: '',
        name: '',
        description: '',
        status: 'ACTIVE',
        priority: 'MEDIUM',
        startDate: new Date().toISOString().split('T')[0],
        endDate: '',
        notes: '',
      });
      navigate(`/engagements/${created.id}`);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create engagement');
    } finally {
      setIsSubmitting(false);
    }
  };

  const getPriorityBadgeClass = (priority: EngagementPriorityType) => {
    switch (priority) {
      case 'URGENT':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      case 'HIGH':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'MEDIUM':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      default:
        return 'bg-slate-50 text-slate-600 border-slate-200';
    }
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight flex items-center gap-2.5">
            <Briefcase className="w-7 h-7 text-brand-600" />
            <span>Practice Engagements</span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Centrally manage client service mandates, compliance lifecycles, and professional engagements.
          </p>
        </div>
        <button
          onClick={() => setIsCreateModalOpen(true)}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-xs transition-colors"
        >
          <Plus className="w-4 h-4" />
          <span>New Engagement</span>
        </button>
      </div>

      {/* Filter Bar */}
      <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-2xs space-y-3">
        <div className="flex flex-col lg:flex-row items-center gap-3">
          {/* Search Input */}
          <form onSubmit={handleSearchSubmit} className="relative flex-1 w-full">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by engagement code (ENG-...), mandate name, or client..."
              className="w-full pl-9 pr-4 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-brand-500 bg-slate-50/50"
            />
          </form>

          {/* Service Filter */}
          <div className="flex items-center gap-2 w-full lg:w-auto">
            <select
              value={serviceFilter}
              onChange={(e) => {
                setServiceFilter(e.target.value);
                setCurrentPage(0);
              }}
              className="px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white font-medium text-slate-700 focus:outline-none focus:ring-2 focus:ring-brand-500 w-full sm:w-auto"
            >
              <option value="">All Services ({services.length})</option>
              {services.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.serviceName} ({s.category})
                </option>
              ))}
            </select>

            {/* Status Filter */}
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setCurrentPage(0);
              }}
              className="px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white font-medium text-slate-700 focus:outline-none focus:ring-2 focus:ring-brand-500 w-full sm:w-auto"
            >
              <option value="">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="DRAFT">Draft</option>
              <option value="ON_HOLD">On Hold</option>
              <option value="COMPLETED">Completed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>

            {/* Priority Filter */}
            <select
              value={priorityFilter}
              onChange={(e) => {
                setPriorityFilter(e.target.value);
                setCurrentPage(0);
              }}
              className="px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white font-medium text-slate-700 focus:outline-none focus:ring-2 focus:ring-brand-500 w-full sm:w-auto"
            >
              <option value="">All Priorities</option>
              <option value="URGENT">Urgent</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </div>
        </div>
      </div>

      {/* Engagements Grid / List */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <div
              key={i}
              className="h-48 rounded-2xl bg-white border border-slate-200 p-5 animate-pulse space-y-3"
            >
              <div className="h-4 bg-slate-200 rounded w-1/3" />
              <div className="h-6 bg-slate-200 rounded w-3/4" />
              <div className="h-4 bg-slate-100 rounded w-1/2" />
              <div className="h-10 bg-slate-100 rounded-xl mt-4" />
            </div>
          ))}
        </div>
      ) : error ? (
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-6 text-center space-y-2">
          <AlertCircle className="w-8 h-8 text-rose-500 mx-auto" />
          <p className="text-xs font-bold text-rose-800">{error}</p>
          <button
            onClick={loadEngagements}
            className="text-xs font-bold text-rose-600 underline hover:text-rose-700"
          >
            Try reloading
          </button>
        </div>
      ) : engagements.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-2xl p-12 text-center space-y-4 shadow-2xs">
          <div className="w-14 h-14 bg-brand-50 text-brand-600 rounded-2xl flex items-center justify-center mx-auto border border-brand-100">
            <Briefcase className="w-7 h-7" />
          </div>
          <div className="max-w-md mx-auto space-y-1">
            <h3 className="text-sm font-bold text-slate-900">No Engagements Found</h3>
            <p className="text-xs text-slate-500">
              {search || statusFilter || priorityFilter || serviceFilter
                ? 'No engagements matched your selected filters. Clear your search or filter options.'
                : 'Create your first client engagement mandate to start managing recurring compliance, filings, and audit lifecycles.'}
            </p>
          </div>
          <button
            onClick={() => setIsCreateModalOpen(true)}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-xs transition-colors"
          >
            <Plus className="w-4 h-4" />
            <span>Create First Engagement</span>
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {engagements.map((eng) => (
            <div
              key={eng.id}
              className="bg-white border border-slate-200 hover:border-brand-300 rounded-2xl p-5 shadow-2xs hover:shadow-xs transition-all flex flex-col justify-between space-y-4 group"
            >
              <div className="space-y-3">
                {/* Header: Code & Status */}
                <div className="flex items-center justify-between gap-2">
                  <span className="font-mono text-[11px] font-bold text-brand-700 bg-brand-50 border border-brand-200/80 px-2 py-0.5 rounded-lg">
                    {eng.engagementCode}
                  </span>
                  <div className="flex items-center gap-1.5">
                    <span
                      className={clsx(
                        'text-[10px] font-bold px-2 py-0.5 rounded-md border uppercase tracking-wider',
                        getPriorityBadgeClass(eng.priority)
                      )}
                    >
                      {eng.priority}
                    </span>
                    <StatusBadge status={eng.status} size="sm" />
                  </div>
                </div>

                {/* Mandate Name */}
                <div>
                  <Link
                    to={`/engagements/${eng.id}`}
                    className="text-sm font-bold text-slate-900 group-hover:text-brand-600 transition-colors line-clamp-1"
                    title={eng.name}
                  >
                    {eng.name}
                  </Link>
                  {eng.description && (
                    <p className="text-xs text-slate-500 line-clamp-2 mt-1">{eng.description}</p>
                  )}
                </div>

                {/* Client & Service Info */}
                <div className="p-3 bg-slate-50 rounded-xl space-y-2 border border-slate-100 text-xs">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400 text-[11px]">Client:</span>
                    <Link
                      to={`/clients/${eng.clientId}`}
                      className="font-bold text-slate-800 hover:text-brand-600 truncate max-w-[180px]"
                    >
                      {eng.clientName || 'Client Profile'}
                    </Link>
                  </div>
                  {eng.serviceName && (
                    <div className="flex items-center justify-between">
                      <span className="text-slate-400 text-[11px]">Service:</span>
                      <span className="font-semibold text-slate-700 truncate max-w-[180px]">
                        {eng.serviceName}
                      </span>
                    </div>
                  )}
                  {eng.assignedUserName && (
                    <div className="flex items-center justify-between">
                      <span className="text-slate-400 text-[11px]">Preparer:</span>
                      <span className="font-medium text-slate-700 truncate max-w-[180px]">
                        {eng.assignedUserName}
                      </span>
                    </div>
                  )}
                  {eng.reviewerUserName && (
                    <div className="flex items-center justify-between">
                      <span className="text-slate-400 text-[11px]">Partner Reviewer:</span>
                      <span className="font-medium text-purple-700 truncate max-w-[180px]">
                        {eng.reviewerUserName}
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {/* Footer Actions */}
              <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                <div className="flex items-center gap-1.5 text-slate-400 text-[11px]">
                  <Calendar className="w-3.5 h-3.5" />
                  <span>
                    {eng.startDate ? eng.startDate : 'No start date'}
                    {eng.endDate ? ` → ${eng.endDate}` : ''}
                  </span>
                </div>
                <Link
                  to={`/engagements/${eng.id}`}
                  className="inline-flex items-center gap-1 font-bold text-brand-600 hover:text-brand-700"
                >
                  <span>Overview</span>
                  <ExternalLink className="w-3 h-3" />
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination Footer */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between pt-4 border-t border-slate-200 text-xs">
          <span className="text-slate-500">
            Showing Page <span className="font-bold text-slate-800">{currentPage + 1}</span> of{' '}
            <span className="font-bold text-slate-800">{totalPages}</span> ({totalElements} engagements)
          </span>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
              disabled={currentPage === 0}
              className="p-2 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed text-slate-700"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={currentPage >= totalPages - 1}
              className="p-2 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed text-slate-700"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* New Engagement Modal */}
      {isCreateModalOpen && (
        <Modal
          isOpen={isCreateModalOpen}
          onClose={() => setIsCreateModalOpen(false)}
          title="Create New Practice Engagement"
          maxWidth="2xl"
        >
          <form onSubmit={handleCreateSubmit} className="space-y-4">
            {/* Client Select */}
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                Client Mandate <span className="text-rose-500">*</span>
              </label>
              <select
                required
                value={formData.clientId}
                onChange={(e) => setFormData({ ...formData, clientId: e.target.value })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-brand-500 font-medium"
              >
                <option value="">Select a Client</option>
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.displayName} ({c.pan || 'No PAN'})
                  </option>
                ))}
              </select>
            </div>

            {/* Service Select & Name */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Tax / Professional Service
                </label>
                <select
                  value={formData.serviceId}
                  onChange={(e) => {
                    const selected = services.find((s) => s.id === e.target.value);
                    setFormData({
                      ...formData,
                      serviceId: e.target.value,
                      name: formData.name || (selected ? `${selected.serviceName} FY 2026-27` : ''),
                    });
                  }}
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-brand-500 font-medium"
                >
                  <option value="">Select Service Catalog Item</option>
                  {services.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.serviceName} ({s.category})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Engagement Name <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Annual GST Compliance FY 2026-27"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-brand-500"
                />
              </div>
            </div>

            {/* Description */}
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                Mandate Scope & Description
              </label>
              <textarea
                rows={2}
                placeholder="Scope of work, deliverables, and filing commitments..."
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>

            {/* Status, Priority & Dates */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Initial Status</label>
                <select
                  value={formData.status}
                  onChange={(e) =>
                    setFormData({ ...formData, status: e.target.value as EngagementStatusType })
                  }
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white font-medium"
                >
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="DRAFT">DRAFT</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Priority</label>
                <select
                  value={formData.priority}
                  onChange={(e) =>
                    setFormData({ ...formData, priority: e.target.value as EngagementPriorityType })
                  }
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white font-medium"
                >
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                  <option value="URGENT">URGENT</option>
                  <option value="LOW">LOW</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Start Date</label>
                <input
                  type="date"
                  value={formData.startDate}
                  onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">End Date</label>
                <input
                  type="date"
                  value={formData.endDate}
                  onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200"
                />
              </div>
            </div>

            {/* Internal Notes */}
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Internal Notes</label>
              <input
                type="text"
                placeholder="Billing terms, fee arrangements, or special instructions..."
                value={formData.notes}
                onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>

            {/* Modal Actions */}
            <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setIsCreateModalOpen(false)}
                className="px-4 py-2 text-xs font-bold text-slate-600 hover:text-slate-800 hover:bg-slate-100 rounded-xl transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="px-5 py-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-xs transition-colors disabled:opacity-50"
              >
                {isSubmitting ? 'Creating...' : 'Create Engagement'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

export default EngagementsPage;
