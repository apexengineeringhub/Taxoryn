import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  FileText,
  Search,
  CheckCircle2,
  ArrowRight,
  ArrowLeft,
  Calendar,
  User,
  HelpCircle,
  Sparkles,
  Layers,
  MapPin,
  ShieldCheck,
  AlertCircle,
  LogOut,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { Card } from '../components/common/Card';
import {
  taxServicePublicApi,
  customerTaxRequirementApi,
} from '../api/endpoints';
import {
  PublicTaxService,
  PublicTaxServiceCategory,
  CustomerTaxpayerType,
  CustomerTaxRequirement,
  FinancialYearOption,
} from '../types';
import { useAuth } from '../context/AuthContext';
import clsx from 'clsx';

export const CustomerTaxRequirementWizardPage: React.FC = () => {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // Wizard Steps: 1: Service Selection, 2: Requirement Details, 3: Review, 4: Submitted Confirmation
  const [step, setStep] = useState<1 | 2 | 3 | 4>(1);

  // Reference Data
  const [categories, setCategories] = useState<PublicTaxServiceCategory[]>([]);
  const [financialYears, setFinancialYears] = useState<FinancialYearOption[]>([]);
  const [isLoadingRef, setIsLoadingRef] = useState(true);

  // Step 1: Service Selection State
  const [selectedService, setSelectedService] = useState<PublicTaxService | null>(null);
  const [serviceSearch, setServiceSearch] = useState('');
  const [selectedCategoryFilter, setSelectedCategoryFilter] = useState<string>('ALL');

  // Step 2: Requirement Form State
  const [customerType, setCustomerType] = useState<CustomerTaxpayerType | undefined>(undefined);
  const [financialYear, setFinancialYear] = useState<string>('2025-26');
  const [description, setDescription] = useState<string>('');
  const [city, setCity] = useState<string>('');
  const [state, setState] = useState<string>('');

  // Submission State
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [createdRequirement, setCreatedRequirement] = useState<CustomerTaxRequirement | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const customerTypeOptions: { type: CustomerTaxpayerType; label: string; desc: string; icon: string }[] = [
    { type: 'SALARIED', label: 'Salaried Individual', desc: 'Employed, Form 16, Salary & House Rent', icon: '💼' },
    { type: 'SELF_EMPLOYED', label: 'Self-Employed / Professional', desc: 'Doctors, Lawyers, Architects, CA/CS', icon: '🩺' },
    { type: 'BUSINESS_OWNER', label: 'Business Owner / MSME', desc: 'Proprietorship, Partnership, Private Limited', icon: '🏢' },
    { type: 'FREELANCER', label: 'Freelancer / Consultant', desc: 'Independent contractor, Tech/Creative projects', icon: '💻' },
    { type: 'INVESTOR', label: 'Investor / Trader', desc: 'Mutual funds, Stocks, Crypto, Real estate capital gains', icon: '📈' },
    { type: 'OTHER', label: 'Other Taxpayer', desc: 'NRI, Trust, Estate, or General compliance', icon: '🌐' },
  ];

  useEffect(() => {
    loadReferenceData();
  }, []);

  const loadReferenceData = async () => {
    try {
      setIsLoadingRef(true);
      const [cats, fyList] = await Promise.all([
        taxServicePublicApi.getCategories().catch(() => []),
        customerTaxRequirementApi.getFinancialYears().catch(() => []),
      ]);
      setCategories(cats || []);
      setFinancialYears(fyList || []);

      // If current FY is present, default to it
      const currentFy = fyList?.find((f: FinancialYearOption) => f.isCurrent);
      if (currentFy) {
        setFinancialYear(currentFy.code);
      }

      // Check preselected service from query param (e.g. ?taxServiceId=... or ?service=INCOME_TAX_RETURN)
      const preselectedServiceId = searchParams.get('taxServiceId') || searchParams.get('serviceId');
      const preselectedServiceCode = searchParams.get('service') || searchParams.get('taxServiceCode');
      if ((preselectedServiceId || preselectedServiceCode) && cats) {
        for (const cat of cats) {
          const match = cat.services?.find(
            (s: PublicTaxService) =>
              (preselectedServiceId && s.id === preselectedServiceId) ||
              (preselectedServiceCode && s.code.toUpperCase() === preselectedServiceCode.toUpperCase())
          );
          if (match) {
            setSelectedService(match);
            break;
          }
        }
      }
    } catch (err) {
      console.error('Failed to load tax service taxonomy', err);
    } finally {
      setIsLoadingRef(false);
    }
  };

  const handleSelectService = (svc: PublicTaxService) => {
    setSelectedService(svc);
    setErrorMessage(null);
  };

  const handleProceedToStep2 = () => {
    if (!selectedService) {
      setErrorMessage('Please select a tax service you need help with.');
      return;
    }
    setErrorMessage(null);
    setStep(2);
  };

  const handleProceedToReview = () => {
    setErrorMessage(null);
    setStep(3);
  };

  const handleConfirmSubmit = async () => {
    if (!selectedService) return;
    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      // 1. Create requirement in DRAFT
      const draft = await customerTaxRequirementApi.create({
        taxServiceId: selectedService.id,
        customerType,
        financialYear,
        description: description.trim() || undefined,
        city: city.trim() || undefined,
        state: state.trim() || undefined,
        sourceType: searchParams.get('sourceType') || (searchParams.get('taxServiceId') ? 'CONTENT' : undefined),
        sourceContentId: searchParams.get('sourceContentId') || undefined,
      });

      // 2. Submit the requirement to transition DRAFT -> SUBMITTED
      const submitted = await customerTaxRequirementApi.submit(draft.id);
      setCreatedRequirement(submitted);
      setStep(4);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to submit requirement. Please try again.';
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSaveAsDraft = async () => {
    if (!selectedService) return;
    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const draft = await customerTaxRequirementApi.create({
        taxServiceId: selectedService.id,
        customerType,
        financialYear,
        description: description.trim() || undefined,
        city: city.trim() || undefined,
        state: state.trim() || undefined,
      });
      setCreatedRequirement(draft);
      navigate('/marketplace/customer/requirements');
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to save draft.';
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Flattened active services for search & filtering
  const allServices = categories.flatMap((cat) => cat.services || []);
  const filteredServices = allServices.filter((s) => {
    const matchesCategory = selectedCategoryFilter === 'ALL' || s.category === selectedCategoryFilter;
    const matchesSearch =
      !serviceSearch ||
      s.name.toLowerCase().includes(serviceSearch.toLowerCase()) ||
      s.code.toLowerCase().includes(serviceSearch.toLowerCase()) ||
      (s.description && s.description.toLowerCase().includes(serviceSearch.toLowerCase()));
    return matchesCategory && matchesSearch;
  });

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 pb-20">
      {/* Top Header */}
      <div className="bg-white border-b border-slate-200">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 py-6 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              onClick={() => (step > 1 && step < 4 ? setStep((step - 1) as any) : navigate('/marketplace/customer/dashboard'))}
              className="p-2 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <div>
              <h1 className="text-lg sm:text-xl font-bold text-slate-900">Tell Us Your Tax Need</h1>
              <p className="text-xs text-slate-500">Quickly capture what tax assistance you need without legal jargon.</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/marketplace/customer/dashboard')}
              className="text-xs font-semibold text-slate-500 hover:text-slate-800"
            >
              Dashboard
            </button>
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

        {/* Wizard Progress Stepper */}
        <div className="max-w-4xl mx-auto px-4 sm:px-6 pb-4">
          <div className="flex items-center justify-between relative">
            <div className="absolute left-0 top-1/2 -translate-y-1/2 h-0.5 bg-slate-200 w-full -z-0" />
            <div
              className="absolute left-0 top-1/2 -translate-y-1/2 h-0.5 bg-indigo-600 transition-all duration-300 -z-0"
              style={{ width: `${((step - 1) / 3) * 100}%` }}
            />

            {[
              { num: 1, label: '1. Select Service' },
              { num: 2, label: '2. Your Need' },
              { num: 3, label: '3. Review' },
              { num: 4, label: '4. Submitted' },
            ].map((s) => (
              <div key={s.num} className="flex flex-col items-center bg-white px-2 z-10">
                <div
                  className={clsx(
                    'w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-all',
                    step >= s.num
                      ? 'bg-indigo-600 text-white shadow-sm'
                      : 'bg-slate-100 text-slate-400 border border-slate-300'
                  )}
                >
                  {step > s.num ? <CheckCircle2 className="w-4 h-4" /> : s.num}
                </div>
                <span
                  className={clsx(
                    'text-[10px] sm:text-xs font-semibold mt-1',
                    step >= s.num ? 'text-indigo-600' : 'text-slate-400'
                  )}
                >
                  {s.label}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Main Form Container */}
      <div className="max-w-4xl mx-auto px-4 sm:px-6 mt-8">
        {errorMessage && (
          <div className="mb-6 p-4 rounded-2xl bg-rose-50 border border-rose-200 text-xs text-rose-700 flex items-start gap-2.5 shadow-sm">
            <AlertCircle className="w-4 h-4 shrink-0 text-rose-600 mt-0.5" />
            <div className="flex-1 font-medium">{errorMessage}</div>
          </div>
        )}

        {/* STEP 1: SELECT TAX SERVICE */}
        {step === 1 && (
          <div className="space-y-6">
            <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-4">
                <div>
                  <h2 className="text-base font-bold text-slate-900">What tax help do you need?</h2>
                  <p className="text-xs text-slate-500">Pick from standardized Indian direct tax, GST, or compliance services.</p>
                </div>

                {/* Search Box */}
                <div className="relative w-full sm:w-72">
                  <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type="text"
                    value={serviceSearch}
                    onChange={(e) => setServiceSearch(e.target.value)}
                    placeholder="Search ITR, GST, Registration..."
                    className="w-full pl-9 pr-3 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
                  />
                </div>
              </div>

              {/* Category Pills */}
              <div className="flex items-center gap-2 overflow-x-auto pb-2">
                <button
                  onClick={() => setSelectedCategoryFilter('ALL')}
                  className={clsx(
                    'px-3 py-1.5 rounded-xl text-xs font-bold transition shrink-0',
                    selectedCategoryFilter === 'ALL'
                      ? 'bg-indigo-600 text-white shadow-xs'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  )}
                >
                  All Categories
                </button>
                {categories.map((c) => (
                  <button
                    key={c.code}
                    onClick={() => setSelectedCategoryFilter(c.code)}
                    className={clsx(
                      'px-3 py-1.5 rounded-xl text-xs font-bold transition shrink-0',
                      selectedCategoryFilter === c.code
                        ? 'bg-indigo-600 text-white shadow-xs'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    )}
                  >
                    {c.name}
                  </button>
                ))}
              </div>

              {/* Service Cards Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
                {filteredServices.map((svc) => {
                  const isSelected = selectedService?.id === svc.id;
                  return (
                    <div
                      key={svc.id}
                      onClick={() => handleSelectService(svc)}
                      className={clsx(
                        'p-4 rounded-2xl border text-left cursor-pointer transition-all flex flex-col justify-between space-y-2',
                        isSelected
                          ? 'bg-indigo-50/70 border-indigo-600 ring-2 ring-indigo-500/20 shadow-sm'
                          : 'bg-white border-slate-200 hover:border-slate-300 hover:bg-slate-50/60'
                      )}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="space-y-1">
                          <span className="font-mono text-[9px] font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-700 uppercase">
                            {svc.categoryName || svc.category}
                          </span>
                          <h3 className="text-sm font-bold text-slate-900">{svc.name}</h3>
                          {svc.description && (
                            <p className="text-[11px] text-slate-500 line-clamp-2 leading-relaxed">
                              {svc.description}
                            </p>
                          )}
                        </div>

                        <div
                          className={clsx(
                            'w-5 h-5 rounded-full border flex items-center justify-center shrink-0 mt-0.5 transition',
                            isSelected
                              ? 'border-indigo-600 bg-indigo-600 text-white'
                              : 'border-slate-300 bg-white'
                          )}
                        >
                          {isSelected && <CheckCircle2 className="w-3.5 h-3.5" />}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>

              {filteredServices.length === 0 && (
                <div className="p-8 text-center text-xs text-slate-400 space-y-1">
                  <p>No tax services matched your search query.</p>
                  <p className="text-[10px]">Try searching for keywords like "ITR", "GST", "TDS", or "Registration".</p>
                </div>
              )}
            </div>

            <div className="flex justify-end gap-3">
              <Button
                variant="primary"
                size="md"
                onClick={handleProceedToStep2}
                disabled={!selectedService}
                className="px-6 rounded-xl"
              >
                <span>Continue</span>
                <ArrowRight className="w-4 h-4 ml-1.5" />
              </Button>
            </div>
          </div>
        )}

        {/* STEP 2: REQUIREMENT DETAILS */}
        {step === 2 && (
          <div className="space-y-6">
            <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 shadow-sm space-y-6">
              {/* Selected Service Badge */}
              <div className="p-4 bg-indigo-50/60 rounded-2xl border border-indigo-100 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-indigo-600 text-white flex items-center justify-center font-bold">
                    <FileText className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="text-[10px] font-bold text-indigo-700 uppercase tracking-wider">Service Selected</div>
                    <div className="text-sm font-bold text-slate-900">{selectedService?.name}</div>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setStep(1)}
                  className="text-xs text-indigo-600 font-bold hover:underline"
                >
                  Change
                </button>
              </div>

              {/* Question 1: Customer Type */}
              <div className="space-y-2">
                <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                  What best describes you? <span className="text-slate-400 font-normal">(Optional)</span>
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                  {customerTypeOptions.map((opt) => {
                    const isSelected = customerType === opt.type;
                    return (
                      <div
                        key={opt.type}
                        onClick={() => setCustomerType(isSelected ? undefined : opt.type)}
                        className={clsx(
                          'p-3.5 rounded-2xl border cursor-pointer transition-all flex items-start justify-between gap-2',
                          isSelected
                            ? 'bg-indigo-50/80 border-indigo-600 ring-2 ring-indigo-500/20'
                            : 'bg-slate-50/60 border-slate-200 hover:bg-slate-100/60'
                        )}
                      >
                        <div className="flex items-start gap-2.5">
                          <span className="text-xl">{opt.icon}</span>
                          <div>
                            <div className="text-xs font-bold text-slate-900">{opt.label}</div>
                            <div className="text-[10px] text-slate-500">{opt.desc}</div>
                          </div>
                        </div>
                        <div
                          className={clsx(
                            'w-4 h-4 rounded-full border flex items-center justify-center shrink-0 mt-0.5',
                            isSelected ? 'border-indigo-600 bg-indigo-600 text-white' : 'border-slate-300 bg-white'
                          )}
                        >
                          {isSelected && <div className="w-1.5 h-1.5 rounded-full bg-white" />}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Question 2: Financial Year */}
              <div className="space-y-2">
                <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                  Which Financial Year?
                </label>
                <select
                  value={financialYear}
                  onChange={(e) => setFinancialYear(e.target.value)}
                  className="w-full sm:w-64 px-3.5 py-2.5 rounded-xl bg-slate-50 border border-slate-200 text-xs font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
                >
                  {financialYears.map((fy) => (
                    <option key={fy.code} value={fy.code}>
                      {fy.label} {fy.isCurrent ? '(Current Year)' : ''}
                    </option>
                  ))}
                </select>
                <p className="text-[10px] text-slate-400">Income earned between April 1 and March 31 of this financial period.</p>
              </div>

              {/* Question 3: Description / Additional Details */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider">
                    Tell the tax professional anything else they should know
                  </label>
                  <span className="text-[10px] text-slate-400 font-mono">
                    {2000 - description.length} characters left
                  </span>
                </div>
                <textarea
                  rows={4}
                  maxLength={2000}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="e.g. I switched jobs in November, have Form 16 from both companies, and have capital gains from selling mutual funds..."
                  className="w-full p-3.5 rounded-2xl bg-slate-50 border border-slate-200 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 leading-relaxed"
                />
              </div>

              {/* Location Preference (Optional) */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 border-t border-slate-100">
                <div>
                  <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Your City / Location <span className="text-slate-400 font-normal">(Optional)</span>
                  </label>
                  <input
                    type="text"
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="e.g. Bengaluru"
                    className="w-full px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                    State <span className="text-slate-400 font-normal">(Optional)</span>
                  </label>
                  <input
                    type="text"
                    value={state}
                    onChange={(e) => setState(e.target.value)}
                    placeholder="e.g. Karnataka"
                    className="w-full px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs"
                  />
                </div>
              </div>
            </div>

            <div className="flex items-center justify-between gap-3">
              <Button variant="outline" size="md" onClick={() => setStep(1)} className="rounded-xl">
                <ArrowLeft className="w-4 h-4 mr-1.5" />
                Back
              </Button>

              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="md"
                  onClick={handleSaveAsDraft}
                  disabled={isSubmitting}
                  className="rounded-xl text-xs"
                >
                  Save as Draft
                </Button>
                <Button
                  variant="primary"
                  size="md"
                  onClick={handleProceedToReview}
                  className="px-6 rounded-xl"
                >
                  <span>Review Requirement</span>
                  <ArrowRight className="w-4 h-4 ml-1.5" />
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* STEP 3: REVIEW BEFORE SUBMIT */}
        {step === 3 && (
          <div className="space-y-6">
            <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 shadow-sm space-y-6">
              <div className="border-b border-slate-100 pb-4">
                <h2 className="text-base font-bold text-slate-900">Review Your Requirement</h2>
                <p className="text-xs text-slate-500">Please review your details before submitting to the marketplace.</p>
              </div>

              {/* Summary Items */}
              <div className="divide-y divide-slate-100 text-xs space-y-3">
                <div className="flex items-start justify-between py-2">
                  <div>
                    <div className="font-bold text-slate-400 uppercase text-[10px]">Service Requested</div>
                    <div className="font-bold text-slate-900 text-sm">{selectedService?.name}</div>
                    <div className="text-[11px] text-slate-500 font-mono">{selectedService?.code}</div>
                  </div>
                  <button onClick={() => setStep(1)} className="text-indigo-600 font-bold hover:underline">
                    Edit
                  </button>
                </div>

                <div className="flex items-start justify-between py-2">
                  <div>
                    <div className="font-bold text-slate-400 uppercase text-[10px]">Taxpayer Profile</div>
                    <div className="font-bold text-slate-900">
                      {customerTypeOptions.find((o) => o.type === customerType)?.label || 'Not Specified'}
                    </div>
                  </div>
                  <button onClick={() => setStep(2)} className="text-indigo-600 font-bold hover:underline">
                    Edit
                  </button>
                </div>

                <div className="flex items-start justify-between py-2">
                  <div>
                    <div className="font-bold text-slate-400 uppercase text-[10px]">Financial Year</div>
                    <div className="font-bold text-slate-900">FY {financialYear}</div>
                  </div>
                  <button onClick={() => setStep(2)} className="text-indigo-600 font-bold hover:underline">
                    Edit
                  </button>
                </div>

                {description && (
                  <div className="flex items-start justify-between py-2">
                    <div className="max-w-xl">
                      <div className="font-bold text-slate-400 uppercase text-[10px]">Additional Details</div>
                      <div className="text-slate-700 font-medium whitespace-pre-wrap leading-relaxed mt-0.5">
                        {description}
                      </div>
                    </div>
                    <button onClick={() => setStep(2)} className="text-indigo-600 font-bold hover:underline">
                      Edit
                    </button>
                  </div>
                )}

                {(city || state) && (
                  <div className="flex items-start justify-between py-2">
                    <div>
                      <div className="font-bold text-slate-400 uppercase text-[10px]">Preferred Location</div>
                      <div className="font-bold text-slate-900">
                        {city ? `${city}, ` : ''}{state || ''}
                      </div>
                    </div>
                    <button onClick={() => setStep(2)} className="text-indigo-600 font-bold hover:underline">
                      Edit
                    </button>
                  </div>
                )}
              </div>

              {/* Statutory Disclaimer */}
              <div className="p-4 bg-slate-50 rounded-2xl border border-slate-200/80 text-[11px] text-slate-500 leading-relaxed">
                <strong>Disclaimer:</strong> Your answers help us understand your requirement. They do not constitute tax advice or determine your tax liability.
              </div>
            </div>

            <div className="flex items-center justify-between gap-3">
              <Button variant="outline" size="md" onClick={() => setStep(2)} className="rounded-xl">
                <ArrowLeft className="w-4 h-4 mr-1.5" />
                Back
              </Button>

              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="md"
                  onClick={handleSaveAsDraft}
                  disabled={isSubmitting}
                  className="rounded-xl text-xs"
                >
                  Save as Draft
                </Button>
                <Button
                  variant="primary"
                  size="md"
                  onClick={handleConfirmSubmit}
                  disabled={isSubmitting}
                  className="px-8 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold shadow-md shadow-emerald-600/20"
                >
                  {isSubmitting ? 'Submitting...' : 'Submit Requirement'}
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* STEP 4: SUBMITTED CONFIRMATION */}
        {step === 4 && (
          <div className="bg-white p-8 sm:p-10 rounded-3xl border border-slate-200 shadow-xl text-center space-y-6 max-w-lg mx-auto">
            <div className="w-16 h-16 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center mx-auto border-2 border-emerald-200">
              <CheckCircle2 className="w-10 h-10" />
            </div>

            <div className="space-y-1.5">
              <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 uppercase tracking-wider">
                SUBMITTED
              </span>
              <h2 className="text-xl font-extrabold text-slate-900 pt-2">Your Requirement Has Been Saved</h2>
              <p className="text-xs text-slate-500 leading-relaxed max-w-md mx-auto">
                We have recorded your requirement for <strong>{createdRequirement?.service?.name || selectedService?.name}</strong> (FY {financialYear}).
                We'll use this information to help you connect with suitable verified tax practitioners.
              </p>
            </div>

            <div className="p-4 bg-slate-50 rounded-2xl border border-slate-200 text-xs text-slate-600 space-y-1">
              <div className="text-[10px] uppercase font-bold text-slate-400">Reference Tracking ID</div>
              <div className="font-mono font-bold text-slate-900 text-xs">{createdRequirement?.id}</div>
            </div>

            <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
              <Button
                variant="primary"
                size="md"
                onClick={() => navigate('/marketplace/customer/requirements')}
                className="w-full sm:w-auto px-6 rounded-xl"
              >
                View My Requirements
              </Button>
              <Button
                variant="outline"
                size="md"
                onClick={() => navigate('/marketplace/customer/dashboard')}
                className="w-full sm:w-auto px-6 rounded-xl"
              >
                Customer Dashboard
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
