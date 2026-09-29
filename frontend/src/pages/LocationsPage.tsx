import React, { useState, useEffect } from 'react';
import {
  MapPin,
  Building2,
  Plus,
  Phone,
  Mail,
  CheckCircle2,
  AlertCircle,
  AlertTriangle,
  RefreshCw,
  Search,
  Star,
  Trash2,
  Edit2,
  Users,
  Shield,
  Layers,
  ArrowRight,
  ExternalLink,
  X,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { StatusBadge } from '../components/common/StatusBadge';
import { locationApi, subscriptionApi } from '../api/endpoints';
import { PracticeBranchLocation, CreateBranchLocationRequest, UpdateBranchLocationRequest, SubscriptionEntitlementsResponse } from '../types';
import { useAuth } from '../context/AuthContext';
import { formatPlanDisplayName } from '../utils/planUtils';
import clsx from 'clsx';

const INDIAN_STATES = [
  'Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chhattisgarh',
  'Goa', 'Gujarat', 'Haryana', 'Himachal Pradesh', 'Jharkhand', 'Karnataka',
  'Kerala', 'Madhya Pradesh', 'Maharashtra', 'Manipur', 'Meghalaya', 'Mizoram',
  'Nagaland', 'Odisha', 'Punjab', 'Rajasthan', 'Sikkim', 'Tamil Nadu',
  'Telangana', 'Tripura', 'Uttar Pradesh', 'Uttarakhand', 'West Bengal',
  'Delhi', 'Jammu & Kashmir', 'Ladakh', 'Chandigarh', 'Puducherry'
];

export const LocationsPage: React.FC = () => {
  const [locations, setLocations] = useState<PracticeBranchLocation[]>([]);
  const [entitlements, setEntitlements] = useState<SubscriptionEntitlementsResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ACTIVE');
  
  // Modal states
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [editingLocation, setEditingLocation] = useState<PracticeBranchLocation | null>(null);
  const [isDeletingLocation, setIsDeletingLocation] = useState<PracticeBranchLocation | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Form state
  const [formData, setFormData] = useState<CreateBranchLocationRequest>({
    name: '',
    code: '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: 'Maharashtra',
    pincode: '',
    phone: '',
    email: '',
    isHeadOffice: false,
  });

  const { organization } = useAuth();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setIsLoading(true);
      const [locationsRes, entitlementsRes] = await Promise.all([
        locationApi.getLocations().catch(() => []),
        subscriptionApi.getEntitlements().catch(() => null),
      ]);
      setLocations(locationsRes || []);
      setEntitlements(entitlementsRes || null);
    } catch (err: any) {
      console.error('Failed to load locations', err);
      showFeedback('error', 'Failed to load practice locations.');
    } finally {
      setIsLoading(false);
    }
  };

  const showFeedback = (type: 'success' | 'error', message: string) => {
    setFeedback({ type, message });
    setTimeout(() => setFeedback(null), 5000);
  };

  const handleOpenAddModal = () => {
    setFormData({
      name: '',
      code: `BR-${(locations.length + 1).toString().padStart(2, '0')}`,
      addressLine1: '',
      addressLine2: '',
      city: '',
      state: 'Maharashtra',
      pincode: '',
      phone: '',
      email: '',
      isHeadOffice: locations.length === 0,
    });
    setIsAddModalOpen(true);
  };

  const handleOpenEditModal = (loc: PracticeBranchLocation) => {
    setEditingLocation(loc);
    setFormData({
      name: loc.name,
      code: loc.code || '',
      addressLine1: loc.addressLine1,
      addressLine2: loc.addressLine2 || '',
      city: loc.city,
      state: loc.state,
      pincode: loc.pincode,
      phone: loc.phone || '',
      email: loc.email || '',
      isHeadOffice: loc.isHeadOffice,
    });
  };

  const handleSaveLocation = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.addressLine1.trim() || !formData.city.trim() || !formData.pincode.trim()) {
      showFeedback('error', 'Please fill in all required address fields.');
      return;
    }

    setIsSubmitting(true);
    try {
      if (editingLocation) {
        await locationApi.updateLocation(editingLocation.id, formData);
        showFeedback('success', `Location "${formData.name}" updated successfully.`);
      } else {
        await locationApi.createLocation(formData);
        showFeedback('success', `New location "${formData.name}" added successfully.`);
      }
      setIsAddModalOpen(false);
      setEditingLocation(null);
      await loadData();
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || 'Failed to save location.';
      showFeedback('error', msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSetPrimary = async (loc: PracticeBranchLocation) => {
    if (loc.isHeadOffice || isSubmitting) return;
    setIsSubmitting(true);
    try {
      await locationApi.setPrimaryLocation(loc.id);
      showFeedback('success', `"${loc.name}" is now designated as Primary Head Office.`);
      await loadData();
    } catch (err: any) {
      showFeedback('error', err?.response?.data?.message || 'Failed to set primary head office.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteLocation = async () => {
    if (!isDeletingLocation || isSubmitting) return;
    setIsSubmitting(true);
    try {
      await locationApi.deleteLocation(isDeletingLocation.id);
      showFeedback('success', `Location "${isDeletingLocation.name}" has been deactivated.`);
      setIsDeletingLocation(null);
      await loadData();
    } catch (err: any) {
      showFeedback('error', err?.response?.data?.message || 'Failed to deactivate location.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const locationEntitlement = entitlements?.entitlements?.find((e) => e.resourceType === 'LOCATION');
  const activeLocationsCount = locations.filter((l) => l.isActive).length;

  const filteredLocations = locations.filter((loc) => {
    const matchesSearch =
      loc.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      loc.city.toLowerCase().includes(searchQuery.toLowerCase()) ||
      loc.state.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (loc.code && loc.code.toLowerCase().includes(searchQuery.toLowerCase()));

    if (statusFilter === 'ACTIVE') return matchesSearch && loc.isActive;
    if (statusFilter === 'INACTIVE') return matchesSearch && !loc.isActive;
    return matchesSearch;
  });

  return (
    <div className="space-y-6 max-w-6xl mx-auto pb-12 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-black text-slate-900">Practice Branch Locations</h1>
            <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-slate-100 text-slate-700 border border-slate-200">
              {activeLocationsCount} Active {activeLocationsCount === 1 ? 'Office' : 'Offices'}
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Manage your practice headquarters and regional branches, office GSTINs, and staff workplace assignments.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={loadData}
            disabled={isLoading}
            className="flex items-center gap-1.5"
          >
            <RefreshCw className={clsx('w-3.5 h-3.5', isLoading && 'animate-spin')} />
            <span>Refresh</span>
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={handleOpenAddModal}
            className="flex items-center gap-1.5 shadow-sm"
          >
            <Plus className="w-4 h-4" />
            <span>Add Branch Office</span>
          </Button>
        </div>
      </div>

      {/* Feedback Banner */}
      {feedback && (
        <div
          className={clsx(
            'p-4 rounded-xl text-xs font-medium flex items-center gap-2.5 transition-all shadow-xs',
            feedback.type === 'success'
              ? 'bg-emerald-50 border border-emerald-200 text-emerald-800'
              : 'bg-rose-50 border border-rose-200 text-rose-800'
          )}
        >
          {feedback.type === 'success' ? (
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          ) : (
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
          )}
          <span>{feedback.message}</span>
        </div>
      )}

      {/* Quota / Entitlement Bar */}
      {locationEntitlement && (
        <div className="bg-white border border-slate-200/90 rounded-2xl p-5 shadow-card flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-brand-50 border border-brand-200 rounded-xl text-brand-700">
              <MapPin className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-sm font-bold text-slate-900">Branch Multi-Location Quota</h3>
                <span className="px-2 py-0.5 text-[10px] font-extrabold uppercase rounded-full bg-brand-100 text-brand-800">
                  {formatPlanDisplayName(entitlements?.plan || 'STARTER')}
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-0.5">
                {locationEntitlement.formattedUsage} ({locationEntitlement.unlimited ? 'Unlimited Allowed' : `${locationEntitlement.limit} Maximum Allowed`})
              </p>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <div className="w-36">
              <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                <div
                  className={clsx(
                    'h-full rounded-full transition-all',
                    locationEntitlement.percentageUsed >= 100
                      ? 'bg-rose-600'
                      : locationEntitlement.percentageUsed >= 80
                      ? 'bg-amber-500'
                      : 'bg-emerald-500'
                  )}
                  style={{ width: `${Math.min(100, Math.max(0, locationEntitlement.percentageUsed))}%` }}
                />
              </div>
            </div>
            <span className="text-xs font-bold text-slate-700">
              {locationEntitlement.unlimited ? 'Unlimited' : `${Math.round(locationEntitlement.percentageUsed)}% Used`}
            </span>
          </div>
        </div>
      )}

      {/* Filters & Search */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search by name, city, state, code..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-white border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none transition-all"
          />
        </div>

        <div className="inline-flex items-center gap-1 p-1 bg-slate-100 border border-slate-200 rounded-xl text-xs font-semibold">
          <button
            onClick={() => setStatusFilter('ACTIVE')}
            className={clsx(
              'px-3 py-1.5 rounded-lg transition-all',
              statusFilter === 'ACTIVE' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
            )}
          >
            Active ({locations.filter((l) => l.isActive).length})
          </button>
          <button
            onClick={() => setStatusFilter('ALL')}
            className={clsx(
              'px-3 py-1.5 rounded-lg transition-all',
              statusFilter === 'ALL' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
            )}
          >
            All ({locations.length})
          </button>
          <button
            onClick={() => setStatusFilter('INACTIVE')}
            className={clsx(
              'px-3 py-1.5 rounded-lg transition-all',
              statusFilter === 'INACTIVE' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
            )}
          >
            Deactivated ({locations.filter((l) => !l.isActive).length})
          </button>
        </div>
      </div>

      {/* Locations Grid */}
      {isLoading ? (
        <div className="p-12 text-center text-slate-400 text-xs">
          <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-brand-600" />
          <span>Loading practice branch locations...</span>
        </div>
      ) : filteredLocations.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-2xl p-12 text-center space-y-3 shadow-card">
          <div className="w-12 h-12 rounded-2xl bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
            <Building2 className="w-6 h-6" />
          </div>
          <h3 className="text-sm font-bold text-slate-900">No practice locations found</h3>
          <p className="text-xs text-slate-500 max-w-sm mx-auto">
            {searchQuery
              ? 'No branch office matches your search criteria.'
              : 'Get started by creating your primary head office or regional branch.'}
          </p>
          {!searchQuery && (
            <Button size="sm" variant="primary" onClick={handleOpenAddModal} className="mt-2">
              <Plus className="w-3.5 h-3.5 mr-1.5" />
              Add Head Office
            </Button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredLocations.map((loc) => {
            return (
              <div
                key={loc.id}
                className={clsx(
                  'bg-white rounded-2xl border p-5 shadow-card relative flex flex-col justify-between transition-all hover:shadow-md',
                  loc.isHeadOffice
                    ? 'border-brand-500 ring-2 ring-brand-500/15'
                    : loc.isActive
                    ? 'border-slate-200/90'
                    : 'border-slate-200 bg-slate-50/70 opacity-80'
                )}
              >
                <div>
                  {/* Top Bar: Badges */}
                  <div className="flex items-start justify-between gap-2 mb-3">
                    <div className="flex items-center gap-1.5 flex-wrap">
                      {loc.isHeadOffice ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase bg-emerald-100 text-emerald-800 border border-emerald-200">
                          <Star className="w-3 h-3 fill-emerald-600 text-emerald-600" />
                          Head Office
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-700">
                          <Building2 className="w-3 h-3 text-slate-500" />
                          Branch Office
                        </span>
                      )}

                      {loc.code && (
                        <span className="px-2 py-0.5 text-[10px] font-mono font-bold bg-slate-100 text-slate-600 rounded">
                          {loc.code}
                        </span>
                      )}
                    </div>

                    {!loc.isActive && (
                      <span className="px-2 py-0.5 text-[10px] font-bold bg-rose-100 text-rose-700 rounded-full">
                        Inactive
                      </span>
                    )}
                  </div>

                  {/* Location Name & City */}
                  <h3 className="font-extrabold text-base text-slate-900 leading-snug">{loc.name}</h3>
                  <p className="text-xs font-semibold text-brand-700 mt-0.5 flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5 shrink-0" />
                    <span>{loc.city}, {loc.state} - {loc.pincode}</span>
                  </p>

                  {/* Detailed Address */}
                  <div className="mt-3 p-3 bg-slate-50 rounded-xl text-[11px] text-slate-600 space-y-1.5 border border-slate-100">
                    <p className="line-clamp-2 leading-relaxed font-medium">
                      {loc.addressLine1}
                      {loc.addressLine2 ? `, ${loc.addressLine2}` : ''}
                    </p>

                    {(loc.phone || loc.email) && (
                      <div className="pt-2 border-t border-slate-200/60 space-y-1 text-slate-500">
                        {loc.phone && (
                          <div className="flex items-center gap-1.5">
                            <Phone className="w-3 h-3 text-slate-400" />
                            <span>{loc.phone}</span>
                          </div>
                        )}
                        {loc.email && (
                          <div className="flex items-center gap-1.5">
                            <Mail className="w-3 h-3 text-slate-400" />
                            <span className="truncate">{loc.email}</span>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>

                {/* Footer Actions */}
                <div className="mt-5 pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                  <div className="flex items-center gap-1.5 text-[11px] text-slate-500 font-medium">
                    <Users className="w-3.5 h-3.5 text-slate-400" />
                    <span>{loc.assignedEmployeeCount || 0} staff</span>
                  </div>

                  <div className="flex items-center gap-1.5">
                    {!loc.isHeadOffice && loc.isActive && (
                      <button
                        onClick={() => handleSetPrimary(loc)}
                        disabled={isSubmitting}
                        title="Designate as primary Head Office"
                        className="px-2 py-1 text-[11px] font-bold text-slate-600 hover:text-emerald-700 hover:bg-emerald-50 rounded-lg transition-all"
                      >
                        Make Primary
                      </button>
                    )}

                    <button
                      onClick={() => handleOpenEditModal(loc)}
                      title="Edit Location"
                      className="p-1.5 text-slate-400 hover:text-brand-600 hover:bg-brand-50 rounded-lg transition-all"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                    </button>

                    {loc.isActive && !loc.isHeadOffice && (
                      <button
                        onClick={() => setIsDeletingLocation(loc)}
                        title="Deactivate Location"
                        className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-all"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Add / Edit Location Modal */}
      {(isAddModalOpen || editingLocation) && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-fade-in">
          <div className="bg-white rounded-2xl shadow-2xl max-w-lg w-full overflow-hidden border border-slate-100 animate-scale-up">
            <div className="flex items-center justify-between p-5 border-b border-slate-100 bg-slate-50/50">
              <div className="flex items-center gap-2.5">
                <div className="p-2 bg-brand-50 text-brand-700 rounded-xl">
                  <Building2 className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-extrabold text-slate-900 text-sm">
                    {editingLocation ? 'Edit Practice Location' : 'Add Practice Location'}
                  </h3>
                  <p className="text-[11px] text-slate-500">
                    {editingLocation ? 'Update branch address and contact info' : 'Register a new branch or office premises'}
                  </p>
                </div>
              </div>

              <button
                onClick={() => {
                  setIsAddModalOpen(false);
                  setEditingLocation(null);
                }}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg hover:bg-slate-100"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSaveLocation} className="p-5 space-y-4 max-h-[80vh] overflow-y-auto">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="sm:col-span-2">
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Location / Branch Name <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Bandra Kurla Complex Branch"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Branch Code</label>
                  <input
                    type="text"
                    placeholder="e.g. MUM-01"
                    value={formData.code || ''}
                    onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    City <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Mumbai"
                    value={formData.city}
                    onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none"
                  />
                </div>

                <div className="sm:col-span-2">
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Address Line 1 <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Suite 402, Trade World Building"
                    value={formData.addressLine1}
                    onChange={(e) => setFormData({ ...formData, addressLine1: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none"
                  />
                </div>

                <div className="sm:col-span-2">
                  <label className="block text-xs font-bold text-slate-700 mb-1">Address Line 2</label>
                  <input
                    type="text"
                    placeholder="e.g. Senapati Bapat Marg, Lower Parel"
                    value={formData.addressLine2 || ''}
                    onChange={(e) => setFormData({ ...formData, addressLine2: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    State <span className="text-rose-500">*</span>
                  </label>
                  <select
                    value={formData.state}
                    onChange={(e) => setFormData({ ...formData, state: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none bg-white"
                  >
                    {INDIAN_STATES.map((st) => (
                      <option key={st} value={st}>
                        {st}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Pincode <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    maxLength={6}
                    placeholder="e.g. 400013"
                    value={formData.pincode}
                    onChange={(e) => setFormData({ ...formData, pincode: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Office Phone</label>
                  <input
                    type="tel"
                    placeholder="e.g. +91 22 2490 1234"
                    value={formData.phone || ''}
                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Office Email</label>
                  <input
                    type="email"
                    placeholder="e.g. mumbai.office@practice.in"
                    value={formData.email || ''}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 outline-none"
                  />
                </div>
              </div>

              <div className="pt-2">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={formData.isHeadOffice || false}
                    onChange={(e) => setFormData({ ...formData, isHeadOffice: e.target.checked })}
                    className="rounded border-slate-300 text-brand-600 focus:ring-brand-500"
                  />
                  <span className="text-xs font-bold text-slate-800">
                    Designate as Primary Head Office
                  </span>
                </label>
                <p className="text-[11px] text-slate-400 ml-5 mt-0.5">
                  Designates this location as the principal office on invoices and official client notices.
                </p>
              </div>

              <div className="flex items-center justify-end gap-2 pt-4 border-t border-slate-100">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    setIsAddModalOpen(false);
                    setEditingLocation(null);
                  }}
                  disabled={isSubmitting}
                >
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" isLoading={isSubmitting}>
                  {editingLocation ? 'Save Changes' : 'Create Location'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Deactivate Confirmation Modal */}
      {isDeletingLocation && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-fade-in">
          <div className="bg-white rounded-2xl shadow-2xl max-w-sm w-full p-5 border border-slate-100 text-center space-y-4 animate-scale-up">
            <div className="w-12 h-12 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center mx-auto border border-rose-100">
              <AlertTriangle className="w-6 h-6" />
            </div>

            <div>
              <h3 className="text-sm font-bold text-slate-900">Deactivate Practice Location?</h3>
              <p className="text-xs text-slate-500 mt-1">
                Are you sure you want to deactivate <strong>"{isDeletingLocation.name}"</strong>? Existing client filings and history remain safe.
              </p>
            </div>

            <div className="flex items-center justify-center gap-2 pt-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setIsDeletingLocation(null)}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              <Button
                variant="danger"
                size="sm"
                onClick={handleDeleteLocation}
                isLoading={isSubmitting}
              >
                Deactivate
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
