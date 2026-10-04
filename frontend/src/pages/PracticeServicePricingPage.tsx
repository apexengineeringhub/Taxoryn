import React, { useEffect, useState, useMemo } from 'react';
import {
  Edit2,
  RefreshCw,
  Plus,
  Trash2,
  CheckCircle,
  XCircle,
  Search,
  SlidersHorizontal,
  Building2,
  Sparkles,
  Tag,
  DollarSign,
  Percent,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import {
  practiceServicePricingApi,
  servicesApi,
  PracticeServicePrice,
} from '../api/endpoints';
import {
  ServiceDto,
  ServiceCategoryType,
  CreateServiceRequest,
  UpdateServiceRequest,
} from '../types';

const CATEGORIES: { value: ServiceCategoryType; label: string }[] = [
  { value: 'GST', label: 'GST Compliance' },
  { value: 'TDS', label: 'TDS Compliance' },
  { value: 'ITR', label: 'Income Tax (ITR)' },
  { value: 'AUDIT', label: 'Audit & Assurance' },
  { value: 'NOTICE', label: 'Tax Notices & Appeals' },
  { value: 'ADVISORY', label: 'Tax Advisory' },
  { value: 'GOVERNMENT_SERVICES', label: 'Government Services' },
  { value: 'REGISTRATION', label: 'Registrations & Licensing' },
  { value: 'OTHER', label: 'Other Practice Services' },
];

const GST_RATES = [0, 5, 12, 18, 28];

const BILLING_UNITS = [
  'PER_FILING',
  'PER_APPLICATION',
  'PER_RETURN',
  'FLAT_FEE',
  'HOURLY',
  'FIXED',
  'PER_MONTH',
  'PER_YEAR',
];

const formatMoney = (value: number | null | undefined, currency: string = 'INR') =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: currency || 'INR',
    maximumFractionDigits: 2,
  }).format(value || 0);

export const PracticeServicePricingPage: React.FC = () => {
  const [pricingServices, setPricingServices] = useState<PracticeServicePrice[]>([]);
  const [allServices, setAllServices] = useState<ServiceDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  // Filters & Search
  const [activeTab, setActiveTab] = useState<'ALL' | 'TAXORYN' | 'PRACTICE'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');

  // Modals state
  const [editingPricing, setEditingPricing] = useState<PracticeServicePrice | null>(null);
  const [pricingMode, setPricingMode] = useState<'DEFAULT' | 'CUSTOM'>('DEFAULT');
  const [customPriceInput, setCustomPriceInput] = useState('');
  const [pricingEnabled, setPricingEnabled] = useState(true);
  const [savingPricing, setSavingPricing] = useState(false);

  // Custom Service Create/Edit Modal
  const [isServiceModalOpen, setIsServiceModalOpen] = useState(false);
  const [editingCustomService, setEditingCustomService] = useState<ServiceDto | null>(null);
  const [customServiceName, setCustomServiceName] = useState('');
  const [customServiceCode, setCustomServiceCode] = useState('');
  const [customCategory, setCustomCategory] = useState<ServiceCategoryType>('GOVERNMENT_SERVICES');
  const [customBillingUnit, setCustomBillingUnit] = useState('PER_APPLICATION');
  const [customDefaultPrice, setCustomDefaultPrice] = useState('0');
  const [customTaxRate, setCustomTaxRate] = useState<number>(18);
  const [customDescription, setCustomDescription] = useState('');
  const [savingCustomService, setSavingCustomService] = useState(false);
  const [customServiceError, setCustomServiceError] = useState('');

  // Delete Confirmation
  const [serviceToDelete, setServiceToDelete] = useState<ServiceDto | null>(null);
  const [deletingService, setDeletingService] = useState(false);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [pricingList, servicesList] = await Promise.all([
        practiceServicePricingApi.getAll(),
        servicesApi.getAll(),
      ]);
      setPricingServices(pricingList || []);
      setAllServices(servicesList || []);
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Could not load service catalog.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, []);

  // Map services to catalog view
  const practiceCustomServices = useMemo(() => {
    return allServices.filter(s => s.scope === 'PRACTICE' || s.organizationId != null);
  }, [allServices]);

  const taxorynStandardServices = useMemo(() => {
    return pricingServices.filter(p => p.scope !== 'PRACTICE');
  }, [pricingServices]);

  // Combined and filtered list
  const filteredCatalog = useMemo(() => {
    const list: Array<{
      type: 'TAXORYN' | 'PRACTICE';
      id: string;
      code: string;
      name: string;
      description?: string;
      category?: string;
      moduleCode?: string;
      billingUnit?: string | null;
      suggestedPrice?: number;
      practicePrice?: number;
      defaultPrice?: number | null;
      taxRate?: number | null;
      pricingMode?: 'DEFAULT' | 'CUSTOM';
      enabled: boolean;
      serviceDto?: ServiceDto;
      pricingDto?: PracticeServicePrice;
    }> = [];

    if (activeTab === 'ALL' || activeTab === 'TAXORYN') {
      taxorynStandardServices.forEach(p => {
        list.push({
          type: 'TAXORYN',
          id: p.serviceId,
          code: p.serviceCode,
          name: p.serviceName,
          description: p.description,
          category: p.moduleCode,
          moduleCode: p.moduleCode,
          billingUnit: p.billingUnit || 'PER_FILING',
          suggestedPrice: p.suggestedPrice,
          practicePrice: p.practicePrice,
          defaultPrice: p.suggestedPrice,
          taxRate: p.taxRate ?? 18,
          pricingMode: p.pricingMode,
          enabled: p.enabled,
          pricingDto: p,
        });
      });
    }

    if (activeTab === 'ALL' || activeTab === 'PRACTICE') {
      practiceCustomServices.forEach(s => {
        list.push({
          type: 'PRACTICE',
          id: s.id,
          code: s.serviceCode,
          name: s.serviceName,
          description: s.description,
          category: s.category,
          moduleCode: s.moduleCode,
          billingUnit: s.billingUnit || 'PER_APPLICATION',
          suggestedPrice: undefined,
          practicePrice: s.defaultPrice ?? 0,
          defaultPrice: s.defaultPrice ?? 0,
          taxRate: s.taxRate ?? 18,
          pricingMode: 'CUSTOM',
          enabled: s.status === 'ACTIVE',
          serviceDto: s,
        });
      });
    }

    return list.filter(item => {
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const matchesName = item.name.toLowerCase().includes(q);
        const matchesCode = item.code.toLowerCase().includes(q);
        const matchesDesc = (item.description || '').toLowerCase().includes(q);
        if (!matchesName && !matchesCode && !matchesDesc) return false;
      }
      if (categoryFilter !== 'ALL') {
        const itemCat = (item.category || '').toUpperCase();
        if (itemCat !== categoryFilter.toUpperCase()) return false;
      }
      return true;
    });
  }, [activeTab, searchQuery, categoryFilter, taxorynStandardServices, practiceCustomServices]);

  // Open Standard Pricing Edit Modal
  const openEditPricing = (pricing: PracticeServicePrice) => {
    setEditingPricing(pricing);
    setPricingMode(pricing.pricingMode);
    setCustomPriceInput(pricing.pricingMode === 'CUSTOM' ? String(pricing.practicePrice) : '');
    setPricingEnabled(pricing.enabled);
    setError('');
    setMessage('');
  };

  const savePricing = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!editingPricing) return;
    if (
      pricingMode === 'CUSTOM' &&
      (!customPriceInput.trim() ||
        !Number.isFinite(Number(customPriceInput)) ||
        Number(customPriceInput) < 0 ||
        !/^\d+(\.\d{1,2})?$/.test(customPriceInput.trim()))
    ) {
      setError('Enter a valid non-negative price with up to two decimal places.');
      return;
    }
    setSavingPricing(true);
    setError('');
    try {
      await practiceServicePricingApi.update(editingPricing.serviceCode, {
        pricingMode,
        ...(pricingMode === 'CUSTOM' ? { customPrice: Number(customPriceInput) } : {}),
        enabled: pricingEnabled,
      });
      setEditingPricing(null);
      setMessage('Practice service price saved successfully.');
      await loadData();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Could not save service pricing.');
    } finally {
      setSavingPricing(false);
    }
  };

  // Open Add Custom Service Modal
  const openAddCustomService = () => {
    setEditingCustomService(null);
    setCustomServiceName('');
    setCustomServiceCode('');
    setCustomCategory('GOVERNMENT_SERVICES');
    setCustomBillingUnit('PER_APPLICATION');
    setCustomDefaultPrice('0');
    setCustomTaxRate(18);
    setCustomDescription('');
    setCustomServiceError('');
    setIsServiceModalOpen(true);
  };

  // Open Edit Custom Service Modal
  const openEditCustomService = (service: ServiceDto) => {
    setEditingCustomService(service);
    setCustomServiceName(service.serviceName);
    setCustomServiceCode(service.serviceCode);
    setCustomCategory(service.category);
    setCustomBillingUnit(service.billingUnit || 'PER_APPLICATION');
    setCustomDefaultPrice(service.defaultPrice != null ? String(service.defaultPrice) : '0');
    setCustomTaxRate(service.taxRate != null ? service.taxRate : 18);
    setCustomDescription(service.description || '');
    setCustomServiceError('');
    setIsServiceModalOpen(true);
  };

  const saveCustomService = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!customServiceName.trim()) {
      setCustomServiceError('Service name is required.');
      return;
    }
    if (!editingCustomService && !customServiceCode.trim()) {
      setCustomServiceError('Service code is required.');
      return;
    }
    const priceNum = Number(customDefaultPrice);
    if (isNaN(priceNum) || priceNum < 0) {
      setCustomServiceError('Please enter a valid price (>= 0).');
      return;
    }

    setSavingCustomService(true);
    setCustomServiceError('');
    try {
      if (editingCustomService) {
        const payload: UpdateServiceRequest = {
          serviceName: customServiceName.trim(),
          category: customCategory,
          billingUnit: customBillingUnit,
          defaultPrice: priceNum,
          taxRate: customTaxRate,
          description: customDescription.trim() || undefined,
        };
        await servicesApi.update(editingCustomService.id, payload);
        setMessage('Practice service updated successfully.');
      } else {
        const payload: CreateServiceRequest = {
          serviceName: customServiceName.trim(),
          serviceCode: customServiceCode.trim().toUpperCase(),
          category: customCategory,
          billingUnit: customBillingUnit,
          defaultPrice: priceNum,
          taxRate: customTaxRate,
          description: customDescription.trim() || undefined,
        };
        await servicesApi.create(payload);
        setMessage('Custom practice service offering created successfully.');
      }
      setIsServiceModalOpen(false);
      await loadData();
    } catch (err: any) {
      setCustomServiceError(err?.response?.data?.message || 'Failed to save service offering.');
    } finally {
      setSavingCustomService(false);
    }
  };

  const toggleCustomServiceStatus = async (service: ServiceDto) => {
    try {
      const nextStatus = service.status !== 'ACTIVE';
      await servicesApi.toggleStatus(service.id, nextStatus);
      setMessage(`Service ${nextStatus ? 'activated' : 'deactivated'} successfully.`);
      await loadData();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Failed to update service status.');
    }
  };

  const confirmDeleteService = async () => {
    if (!serviceToDelete) return;
    setDeletingService(true);
    try {
      await servicesApi.delete(serviceToDelete.id);
      setMessage(`Custom service '${serviceToDelete.serviceName}' deleted.`);
      setServiceToDelete(null);
      await loadData();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Failed to delete service offering.');
    } finally {
      setDeletingService(false);
    }
  };

  return (
    <main className="mx-auto max-w-7xl space-y-6 p-5 md:p-8">
      {/* Header */}
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Service Offering Catalog & Pricing</h1>
          <p className="mt-1 text-sm text-slate-500">
            Configure standard Taxoryn offerings and create custom professional services for your practice.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            leftIcon={<RefreshCw className="h-4 w-4" />}
            onClick={() => void loadData()}
            disabled={loading}
          >
            Refresh
          </Button>
          <Button
            variant="primary"
            leftIcon={<Plus className="h-4 w-4" />}
            onClick={openAddCustomService}
          >
            Add Practice Service
          </Button>
        </div>
      </header>

      {/* Notifications */}
      {message && (
        <div role="status" className="flex items-center justify-between rounded-lg bg-emerald-50 p-4 text-sm text-emerald-800 border border-emerald-200">
          <span>{message}</span>
          <button onClick={() => setMessage('')} className="text-emerald-600 hover:text-emerald-900 font-semibold text-xs">Dismiss</button>
        </div>
      )}
      {error && !editingPricing && !isServiceModalOpen && !serviceToDelete && (
        <div role="alert" className="flex items-center justify-between rounded-lg bg-rose-50 p-4 text-sm text-rose-700 border border-rose-200">
          <span>{error}</span>
          <button onClick={() => setError('')} className="text-rose-600 hover:text-rose-900 font-semibold text-xs">Dismiss</button>
        </div>
      )}

      {/* Scope Filter Tabs & Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 pb-4">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setActiveTab('ALL')}
            className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors ${
              activeTab === 'ALL'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            All Offerings ({taxorynStandardServices.length + practiceCustomServices.length})
          </button>
          <button
            onClick={() => setActiveTab('TAXORYN')}
            className={`flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg transition-colors ${
              activeTab === 'TAXORYN'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            <Sparkles className="h-4 w-4" />
            Taxoryn Standard ({taxorynStandardServices.length})
          </button>
          <button
            onClick={() => setActiveTab('PRACTICE')}
            className={`flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg transition-colors ${
              activeTab === 'PRACTICE'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            <Building2 className="h-4 w-4" />
            My Practice Custom ({practiceCustomServices.length})
          </button>
        </div>

        {/* Search & Category Filter */}
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative min-w-[220px]">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search services or codes..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              className="w-full rounded-lg border border-slate-300 pl-9 pr-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none"
            />
          </div>

          <div className="flex items-center gap-1.5">
            <SlidersHorizontal className="h-4 w-4 text-slate-400" />
            <select
              value={categoryFilter}
              onChange={e => setCategoryFilter(e.target.value)}
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm text-slate-700 focus:border-indigo-500 focus:outline-none bg-white"
            >
              <option value="ALL">All Categories</option>
              {CATEGORIES.map(c => (
                <option key={c.value} value={c.value}>
                  {c.label}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Catalog Table */}
      <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        {loading ? (
          <div className="flex items-center justify-center p-12 text-sm text-slate-500">
            <RefreshCw className="h-5 w-5 animate-spin mr-2 text-indigo-600" />
            Loading service offering catalog…
          </div>
        ) : filteredCatalog.length === 0 ? (
          <div className="p-12 text-center">
            <Tag className="h-10 w-10 text-slate-300 mx-auto mb-3" />
            <p className="text-base font-medium text-slate-900">No service offerings found</p>
            <p className="mt-1 text-sm text-slate-500">
              {searchQuery || categoryFilter !== 'ALL'
                ? 'Try adjusting your filters or search query.'
                : 'Click "+ Add Practice Service" to introduce your first custom practice offering.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[950px] text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-4 py-3">Service Name & Code</th>
                  <th className="px-4 py-3">Scope</th>
                  <th className="px-4 py-3">Category / Unit</th>
                  <th className="px-4 py-3">Taxoryn Suggested</th>
                  <th className="px-4 py-3">My Practice Fee</th>
                  <th className="px-4 py-3">GST %</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredCatalog.map(item => (
                  <tr key={`${item.type}-${item.code}`} className="hover:bg-slate-50/70 transition-colors">
                    <td className="px-4 py-3.5">
                      <div className="font-semibold text-slate-900">{item.name}</div>
                      <div className="font-mono text-[11px] text-slate-400 mt-0.5">{item.code}</div>
                      {item.description && (
                        <p className="text-xs text-slate-500 mt-0.5 line-clamp-1">{item.description}</p>
                      )}
                    </td>
                    <td className="px-4 py-3.5">
                      {item.type === 'TAXORYN' ? (
                        <span className="inline-flex items-center gap-1 rounded-md bg-blue-50 px-2 py-1 text-xs font-medium text-blue-700 border border-blue-100">
                          <Sparkles className="h-3 w-3" /> Taxoryn Standard
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-md bg-purple-50 px-2 py-1 text-xs font-medium text-purple-700 border border-purple-100">
                          <Building2 className="h-3 w-3" /> Practice Custom
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3.5">
                      <div className="text-slate-700 font-medium text-xs">
                        {CATEGORIES.find(c => c.value === item.category)?.label || item.category || 'General'}
                      </div>
                      <div className="text-[11px] text-slate-400 mt-0.5">
                        {item.billingUnit || 'PER_FILING'}
                      </div>
                    </td>
                    <td className="px-4 py-3.5 text-slate-600">
                      {item.suggestedPrice != null ? formatMoney(item.suggestedPrice) : '—'}
                    </td>
                    <td className="px-4 py-3.5">
                      <div className="font-semibold text-slate-900">
                        {formatMoney(item.practicePrice)}
                      </div>
                      {item.type === 'TAXORYN' && (
                        <span className="text-[11px] text-slate-400">
                          {item.pricingMode === 'DEFAULT' ? '(Default Reference)' : '(Custom Price)'}
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3.5">
                      <span className="inline-flex items-center rounded bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700">
                        {item.taxRate ?? 18}%
                      </span>
                    </td>
                    <td className="px-4 py-3.5">
                      <span
                        className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium ${
                          item.enabled
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-100'
                            : 'bg-slate-100 text-slate-500 border border-slate-200'
                        }`}
                      >
                        {item.enabled ? (
                          <>
                            <CheckCircle className="h-3 w-3" /> Active
                          </>
                        ) : (
                          <>
                            <XCircle className="h-3 w-3" /> Disabled
                          </>
                        )}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-right">
                      <div className="flex items-center justify-end gap-2">
                        {item.type === 'TAXORYN' && item.pricingDto && (
                          <Button
                            variant="outline"
                            size="sm"
                            leftIcon={<Edit2 className="h-3.5 w-3.5" />}
                            onClick={() => openEditPricing(item.pricingDto!)}
                          >
                            Edit Fee
                          </Button>
                        )}

                        {item.type === 'PRACTICE' && item.serviceDto && (
                          <>
                            <Button
                              variant="outline"
                              size="sm"
                              leftIcon={<Edit2 className="h-3.5 w-3.5" />}
                              onClick={() => openEditCustomService(item.serviceDto!)}
                            >
                              Edit
                            </Button>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => toggleCustomServiceStatus(item.serviceDto!)}
                            >
                              {item.enabled ? 'Deactivate' : 'Activate'}
                            </Button>
                            <button
                              onClick={() => setServiceToDelete(item.serviceDto!)}
                              className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                              title="Delete Service Offering"
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Helper Footer */}
      <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-xs text-slate-600 space-y-1">
        <p className="font-semibold text-slate-700">Service Offering Architecture Note:</p>
        <p>
          • <strong>Taxoryn Standard Services</strong> are standardized offerings with suggested reference fees.
          Your practice can set custom fees or rely on suggested prices.
        </p>
        <p>
          • <strong>Practice Custom Services</strong> are created by your firm and fully private to your organization.
          They are immediately selectable in Client Engagements, Work Templates, and Invoices.
        </p>
        <p>
          • Historical invoices are snapshot-based and immutable. Price adjustments apply only to new invoice line items.
        </p>
      </div>

      {/* Standard Service Fee Override Modal */}
      {editingPricing && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs">
          <form onSubmit={savePricing} className="w-full max-w-lg space-y-5 rounded-2xl bg-white p-6 shadow-2xl">
            <div>
              <h2 className="text-lg font-bold text-slate-900">Configure Practice Fee</h2>
              <p className="mt-1 text-sm text-slate-500">{editingPricing.serviceName} ({editingPricing.serviceCode})</p>
            </div>

            <div className="rounded-lg bg-slate-50 p-3 text-sm flex items-center justify-between">
              <span className="text-slate-500">Taxoryn Suggested Price:</span>
              <strong className="text-slate-900 font-semibold">{formatMoney(editingPricing.suggestedPrice, editingPricing.currency)}</strong>
            </div>

            <fieldset className="space-y-3">
              <legend className="mb-2 text-sm font-semibold text-slate-800">Fee Mode</legend>
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="radio"
                  name="pricingMode"
                  checked={pricingMode === 'DEFAULT'}
                  onChange={() => setPricingMode('DEFAULT')}
                />
                <span>Use Taxoryn Suggested Price ({formatMoney(editingPricing.suggestedPrice, editingPricing.currency)})</span>
              </label>
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="radio"
                  name="pricingMode"
                  checked={pricingMode === 'CUSTOM'}
                  onChange={() => setPricingMode('CUSTOM')}
                />
                <span>Set My Own Practice Fee</span>
              </label>
            </fieldset>

            {pricingMode === 'CUSTOM' && (
              <label className="block text-sm font-medium text-slate-700">
                Practice Fee (₹)
                <input
                  className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
                  type="number"
                  min="0"
                  step="0.01"
                  value={customPriceInput}
                  onChange={e => setCustomPriceInput(e.target.value)}
                  placeholder="e.g. 1500.00"
                />
              </label>
            )}

            <label className="flex items-center gap-2 text-sm text-slate-700 cursor-pointer">
              <input
                type="checkbox"
                checked={pricingEnabled}
                onChange={e => setPricingEnabled(e.target.checked)}
              />
              <span>Enable this service for client engagements & invoicing</span>
            </label>

            {error && <p role="alert" className="text-sm text-rose-700">{error}</p>}

            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => setEditingPricing(null)}>
                Cancel
              </Button>
              <Button type="submit" disabled={savingPricing}>
                {savingPricing ? 'Saving…' : 'Save Fee Configuration'}
              </Button>
            </div>
          </form>
        </div>
      )}

      {/* Create / Edit Custom Service Modal */}
      {isServiceModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs">
          <form onSubmit={saveCustomService} className="w-full max-w-xl space-y-4 rounded-2xl bg-white p-6 shadow-2xl">
            <div>
              <h2 className="text-lg font-bold text-slate-900">
                {editingCustomService ? 'Edit Practice Service Offering' : 'Add Practice Service Offering'}
              </h2>
              <p className="mt-1 text-sm text-slate-500">
                {editingCustomService
                  ? 'Update practice service parameters and billing configuration.'
                  : 'Define a custom service offering specific to your practice firm.'}
              </p>
            </div>

            {customServiceError && (
              <div role="alert" className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700 border border-rose-200">
                {customServiceError}
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase">
                  Service Name *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. PAN Card Application"
                  value={customServiceName}
                  onChange={e => setCustomServiceName(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase">
                  Service Code *
                </label>
                <input
                  type="text"
                  required
                  disabled={!!editingCustomService}
                  placeholder="e.g. PAN_CARD_APP"
                  value={customServiceCode}
                  onChange={e => setCustomServiceCode(e.target.value.toUpperCase().replace(/\s+/g, '_'))}
                  className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none uppercase font-mono disabled:bg-slate-100"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase">
                  Category *
                </label>
                <select
                  value={customCategory}
                  onChange={e => setCustomCategory(e.target.value as ServiceCategoryType)}
                  className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none bg-white"
                >
                  {CATEGORIES.map(c => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase">
                  Billing Unit *
                </label>
                <select
                  value={customBillingUnit}
                  onChange={e => setCustomBillingUnit(e.target.value)}
                  className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none bg-white"
                >
                  {BILLING_UNITS.map(u => (
                    <option key={u} value={u}>
                      {u}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase">
                  Default Fee (₹) *
                </label>
                <div className="relative mt-1">
                  <DollarSign className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    required
                    value={customDefaultPrice}
                    onChange={e => setCustomDefaultPrice(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 pl-9 pr-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
                    placeholder="0.00"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase">
                  GST Rate (%) *
                </label>
                <div className="relative mt-1">
                  <Percent className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                  <select
                    value={customTaxRate}
                    onChange={e => setCustomTaxRate(Number(e.target.value))}
                    className="w-full rounded-lg border border-slate-300 pl-9 pr-3 py-2 text-sm focus:border-indigo-500 focus:outline-none bg-white"
                  >
                    {GST_RATES.map(r => (
                      <option key={r} value={r}>
                        {r}% GST
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase">
                Description (Optional)
              </label>
              <textarea
                rows={2}
                placeholder="Explain the scope of this offering for your clients and team..."
                value={customDescription}
                onChange={e => setCustomDescription(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
              />
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsServiceModalOpen(false)}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={savingCustomService}>
                {savingCustomService ? 'Saving…' : editingCustomService ? 'Update Offering' : 'Create Offering'}
              </Button>
            </div>
          </form>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {serviceToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs">
          <div className="w-full max-w-md space-y-4 rounded-2xl bg-white p-6 shadow-2xl">
            <h3 className="text-lg font-bold text-slate-900">Delete Practice Service</h3>
            <p className="text-sm text-slate-600">
              Are you sure you want to delete <strong>{serviceToDelete.serviceName}</strong> (
              <span className="font-mono text-xs">{serviceToDelete.serviceCode}</span>)?
            </p>
            <p className="text-xs text-amber-700 bg-amber-50 p-3 rounded-lg border border-amber-200">
              Note: If this service is referenced in existing client engagements or work templates,
              it will be safely archived or rejected by validation rules.
            </p>
            <div className="flex justify-end gap-2 pt-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => setServiceToDelete(null)}
                disabled={deletingService}
              >
                Cancel
              </Button>
              <Button
                type="button"
                variant="danger"
                onClick={confirmDeleteService}
                disabled={deletingService}
              >
                {deletingService ? 'Deleting…' : 'Delete Service'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
};
