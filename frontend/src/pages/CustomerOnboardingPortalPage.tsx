import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  FileText,
  CheckCircle2,
  Clock,
  ShieldCheck,
  Building2,
  Upload,
  Sparkles,
  Lock,
  ArrowRight,
  AlertCircle,
  HelpCircle,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { TaxorynLogo } from '../components/common/TaxorynLogo';
import { marketplaceOnboardingPublicApi } from '../api/endpoints';
import {
  MarketplaceOnboarding,
  MarketplaceProposal,
  UpdateOnboardingDetailsRequest,
  SignEngagementLetterRequest,
} from '../types';
import clsx from 'clsx';

export const CustomerOnboardingPortalPage: React.FC = () => {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Data
  const [proposal, setProposal] = useState<MarketplaceProposal | null>(null);
  const [onboarding, setOnboarding] = useState<MarketplaceOnboarding | null>(null);

  // Stepper state
  const [step, setStep] = useState<number>(1); // 1: Proposal, 2: KYC Details, 3: Document Uploads, 4: Sign Engagement, 5: Complete

  // Forms
  const [detailsForm, setDetailsForm] = useState<UpdateOnboardingDetailsRequest>({
    clientName: '',
    legalName: '',
    entityType: 'INDIVIDUAL',
    pan: '',
    gstin: '',
    tan: '',
    addressLine1: '',
    city: '',
    state: '',
    pincode: '',
  });

  const [signForm, setSignForm] = useState<SignEngagementLetterRequest>({
    signedConsent: false,
    agreedToFees: false,
    signatureName: '',
  });

  const [uploadLoading, setUploadLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  const fetchData = async () => {
    if (!token) return;
    try {
      setLoading(true);
      setError(null);

      // Check if token is proposal (prop_*) or onboarding (onb_*)
      if (token.startsWith('prop_')) {
        const propData = await marketplaceOnboardingPublicApi.getProposalByToken(token);
        setProposal(propData);
        if (propData.proposalStatus === 'ACCEPTED') {
          setStep(2);
        }
      } else {
        const onbData = await marketplaceOnboardingPublicApi.getOnboardingByToken(token);
        setOnboarding(onbData);
        setDetailsForm({
          clientName: onbData.clientName || '',
          legalName: onbData.legalName || onbData.clientName || '',
          entityType: onbData.entityType || 'INDIVIDUAL',
          pan: onbData.pan || '',
          gstin: onbData.gstin || '',
          tan: onbData.tan || '',
          addressLine1: onbData.addressLine1 || '',
          city: onbData.city || '',
          state: onbData.state || '',
          pincode: onbData.pincode || '',
        });

        if (onbData.onboardingStatus === 'APPROVED') {
          setStep(5);
        } else if (onbData.engagementLetterSigned) {
          setStep(4);
        } else if (onbData.pan) {
          setStep(3);
        } else {
          setStep(2);
        }
      }
    } catch (err: any) {
      console.error('Failed to load session', err);
      setError('Invalid or expired onboarding session link. Please contact your tax practitioner.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [token]);

  const handleAcceptProposal = async () => {
    if (!token) return;
    try {
      setActionLoading(true);
      const res = await marketplaceOnboardingPublicApi.respondToProposal(token, {
        isAccepted: true,
      });
      setProposal(res);
      setStep(2);
      alert('Proposal Accepted! Please proceed with your business KYC and engagement details.');
    } catch (err) {
      console.error('Failed to accept proposal', err);
    } finally {
      setActionLoading(false);
    }
  };

  const handleUpdateDetails = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) return;
    try {
      setActionLoading(true);
      const res = await marketplaceOnboardingPublicApi.updateDetails(token, detailsForm);
      setOnboarding(res);
      setStep(3);
    } catch (err) {
      console.error('Failed to save details', err);
      alert('Failed to save details. Please check your inputs.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleFileUpload = async (docType: string, docName: string) => {
    if (!token) return;
    // Simulate upload with clean storage path
    try {
      setUploadLoading(true);
      const fakePath = `/documents/kyc/${docType.toLowerCase()}_${Date.now()}.pdf`;
      await marketplaceOnboardingPublicApi.uploadDocument(token, {
        documentType: docType,
        documentName: docName,
        filePath: fakePath,
        fileSizeBytes: 245000,
        contentType: 'application/pdf',
      });
      const refreshed = await marketplaceOnboardingPublicApi.getOnboardingByToken(token);
      setOnboarding(refreshed);
    } catch (err) {
      console.error('Upload failed', err);
    } finally {
      setUploadLoading(false);
    }
  };

  const handleSignEngagement = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) return;
    if (!signForm.signedConsent || !signForm.agreedToFees) {
      alert('Please check both consent checkboxes to proceed.');
      return;
    }
    try {
      setActionLoading(true);
      const res = await marketplaceOnboardingPublicApi.signEngagement(token, signForm);
      setOnboarding(res);
      setStep(5);
    } catch (err) {
      console.error('Failed to sign engagement', err);
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="text-center space-y-3">
          <div className="w-12 h-12 border-4 border-primary-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
          <p className="text-sm text-gray-500 font-medium">Securing connection to Onboarding Portal...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="max-w-md w-full bg-white p-8 rounded-2xl shadow-xl border border-gray-200 text-center space-y-4">
          <AlertCircle className="w-12 h-12 text-red-500 mx-auto" />
          <h2 className="text-xl font-bold text-gray-900">Session Link Error</h2>
          <p className="text-sm text-gray-600">{error}</p>
          <Button variant="primary" onClick={() => navigate('/marketplace')}>
            Browse Tax Professionals
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-10 px-4 sm:px-6 lg:px-8">
      <div className="max-w-3xl mx-auto space-y-8">
        {/* Brand Banner */}
        <div className="text-center space-y-3 flex flex-col items-center">
          <TaxorynLogo variant="horizontal" theme="light" size="sm" />
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs font-semibold">
            <Lock className="w-3.5 h-3.5" /> 256-Bit Encrypted Client Onboarding
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight break-words leading-tight">
            {proposal?.practiceDisplayName || onboarding?.practiceDisplayName || 'Taxoryn Certified Practice'}
          </h1>
          <p className="text-sm text-gray-500">
            Official Engagement & Secure Statutory Onboarding Portal
          </p>
        </div>

        {/* Stepper Header */}
        <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm">
          <div className="flex items-center justify-between">
            <div className={clsx('flex items-center gap-2 text-xs font-semibold', {
              'text-primary-600': step >= 1,
              'text-gray-400': step < 1,
            })}>
              <span className="w-6 h-6 rounded-full bg-primary-100 flex items-center justify-center">1</span>
              Proposal
            </div>
            <div className="h-0.5 w-8 bg-gray-200" />
            <div className={clsx('flex items-center gap-2 text-xs font-semibold', {
              'text-primary-600': step >= 2,
              'text-gray-400': step < 2,
            })}>
              <span className="w-6 h-6 rounded-full bg-primary-100 flex items-center justify-center">2</span>
              Entity & PAN
            </div>
            <div className="h-0.5 w-8 bg-gray-200" />
            <div className={clsx('flex items-center gap-2 text-xs font-semibold', {
              'text-primary-600': step >= 3,
              'text-gray-400': step < 3,
            })}>
              <span className="w-6 h-6 rounded-full bg-primary-100 flex items-center justify-center">3</span>
              KYC Docs
            </div>
            <div className="h-0.5 w-8 bg-gray-200" />
            <div className={clsx('flex items-center gap-2 text-xs font-semibold', {
              'text-primary-600': step >= 4,
              'text-gray-400': step < 4,
            })}>
              <span className="w-6 h-6 rounded-full bg-primary-100 flex items-center justify-center">4</span>
              Engagement
            </div>
          </div>
        </div>

        {/* Step 1: Proposal Review */}
        {step === 1 && proposal && (
          <div className="bg-white rounded-2xl p-8 border border-gray-200 shadow-lg space-y-6">
            <div className="border-b pb-4">
              <span className="text-xs font-bold uppercase tracking-wider text-primary-600">Engagement Proposal</span>
              <h2 className="text-2xl font-bold text-gray-900 mt-1">{proposal.proposalTitle}</h2>
              <p className="text-xs text-gray-500 mt-1">Prepared for {proposal.clientName} ({proposal.clientEmail})</p>
            </div>

            <div className="space-y-4">
              <div>
                <h4 className="text-sm font-semibold text-gray-900">Scope of Work</h4>
                <p className="text-sm text-gray-600 mt-1 whitespace-pre-line leading-relaxed bg-gray-50 p-4 rounded-xl">
                  {proposal.scopeOfWork}
                </p>
              </div>

              {proposal.deliverables && (
                <div>
                  <h4 className="text-sm font-semibold text-gray-900">Key Deliverables</h4>
                  <p className="text-sm text-gray-600 mt-1 whitespace-pre-line bg-gray-50 p-4 rounded-xl">
                    {proposal.deliverables}
                  </p>
                </div>
              )}

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 bg-primary-50 p-4 rounded-xl border border-primary-100">
                <div>
                  <span className="text-xs text-primary-700 block font-medium">Proposed Professional Fee</span>
                  <span className="text-2xl font-black text-primary-900">₹{proposal.feeAmount.toLocaleString('en-IN')}</span>
                  <span className="text-xs text-primary-600 block mt-0.5">{proposal.pricingType.replace('_', ' ')}</span>
                </div>
                <div>
                  <span className="text-xs text-primary-700 block font-medium">Estimated Completion</span>
                  <span className="text-lg font-bold text-primary-900">{proposal.estimatedTimelineDays} Days</span>
                  <span className="text-xs text-primary-600 block mt-0.5">Turnaround Time</span>
                </div>
              </div>
            </div>

            <div className="pt-4 border-t flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
              <span className="text-xs text-gray-400">
                Valid until {proposal.validUntil ? new Date(proposal.validUntil).toLocaleDateString() : '14 Days'}
              </span>
              <Button
                variant="primary"
                size="lg"
                onClick={handleAcceptProposal}
                disabled={actionLoading}
                className="w-full sm:w-auto bg-emerald-600 hover:bg-emerald-700 text-white flex items-center justify-center gap-2"
              >
                {actionLoading ? 'Accepting...' : 'Accept Proposal & Begin Onboarding'} <ArrowRight className="w-4 h-4" />
              </Button>
            </div>
          </div>
        )}

        {/* Step 2: Entity & PAN Information */}
        {step === 2 && (
          <form onSubmit={handleUpdateDetails} className="bg-white rounded-2xl p-8 border border-gray-200 shadow-lg space-y-6">
            <div>
              <h2 className="text-xl font-bold text-gray-900">Taxpayer & Entity Identification</h2>
              <p className="text-xs text-gray-500 mt-1">
                Provide your official statutory details to populate your engagement ledger.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">Entity / Constitution Type *</label>
                <select
                  value={detailsForm.entityType}
                  onChange={(e) => setDetailsForm({ ...detailsForm, entityType: e.target.value as any })}
                  className="w-full text-sm border border-gray-300 rounded-lg p-2.5 bg-white focus:ring-2 focus:ring-primary-500"
                >
                  <option value="INDIVIDUAL">Individual / Salaried / Freelancer</option>
                  <option value="COMPANY">Private Limited / Public Limited Company</option>
                  <option value="LLP">Limited Liability Partnership (LLP)</option>
                  <option value="FIRM">Partnership Firm / Sole Proprietorship</option>
                  <option value="HUF">Hindu Undivided Family (HUF)</option>
                  <option value="TRUST">Trust / Society / Section 8 NGO</option>
                </select>
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">Full Legal Name *</label>
                <input
                  type="text"
                  required
                  value={detailsForm.clientName}
                  onChange={(e) => setDetailsForm({ ...detailsForm, clientName: e.target.value })}
                  placeholder="e.g., Rohit Sharma / Acme Technologies Pvt Ltd"
                  className="w-full text-sm border border-gray-300 rounded-lg p-2.5"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">Permanent Account Number (PAN) *</label>
                <input
                  type="text"
                  required
                  maxLength={10}
                  value={detailsForm.pan}
                  onChange={(e) => setDetailsForm({ ...detailsForm, pan: e.target.value.toUpperCase() })}
                  placeholder="ABCDE1234F"
                  className="w-full text-sm font-mono uppercase border border-gray-300 rounded-lg p-2.5"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">GSTIN (Optional if unregistered)</label>
                <input
                  type="text"
                  maxLength={15}
                  value={detailsForm.gstin}
                  onChange={(e) => setDetailsForm({ ...detailsForm, gstin: e.target.value.toUpperCase() })}
                  placeholder="27ABCDE1234F1Z5"
                  className="w-full text-sm font-mono uppercase border border-gray-300 rounded-lg p-2.5"
                />
              </div>

              <div className="md:col-span-2">
                <label className="text-xs font-semibold text-gray-700 block mb-1">Registered Address Line 1</label>
                <input
                  type="text"
                  value={detailsForm.addressLine1}
                  onChange={(e) => setDetailsForm({ ...detailsForm, addressLine1: e.target.value })}
                  placeholder="Street / Office number / Landmark"
                  className="w-full text-sm border border-gray-300 rounded-lg p-2.5"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">City</label>
                <input
                  type="text"
                  value={detailsForm.city}
                  onChange={(e) => setDetailsForm({ ...detailsForm, city: e.target.value })}
                  placeholder="e.g. Mumbai"
                  className="w-full text-sm border border-gray-300 rounded-lg p-2.5"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">PIN Code</label>
                <input
                  type="text"
                  value={detailsForm.pincode}
                  onChange={(e) => setDetailsForm({ ...detailsForm, pincode: e.target.value })}
                  placeholder="400001"
                  className="w-full text-sm border border-gray-300 rounded-lg p-2.5"
                />
              </div>
            </div>

            <div className="pt-4 border-t flex justify-end">
              <Button variant="primary" size="lg" type="submit" disabled={actionLoading}>
                {actionLoading ? 'Saving...' : 'Save & Continue to KYC Uploads'}
              </Button>
            </div>
          </form>
        )}

        {/* Step 3: KYC Document Uploads */}
        {step === 3 && (
          <div className="bg-white rounded-2xl p-8 border border-gray-200 shadow-lg space-y-6">
            <div>
              <h2 className="text-xl font-bold text-gray-900">KYC Document Submission</h2>
              <p className="text-xs text-gray-500 mt-1">
                Upload verified identity and address proofs required for professional statutory compliance.
              </p>
            </div>

            <div className="space-y-4">
              {onboarding?.documents?.map((doc) => (
                <div
                  key={doc.id}
                  className="p-4 rounded-xl border border-gray-200 bg-gray-50 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3"
                >
                  <div className="flex items-center gap-3">
                    <FileText className="w-6 h-6 text-primary-600 shrink-0" />
                    <div>
                      <h4 className="text-sm font-semibold text-gray-900">{doc.documentName}</h4>
                      <div className="text-xs text-gray-500">
                        {doc.isRequired ? <span className="text-red-500 font-semibold">Mandatory</span> : 'Optional'}
                        {doc.filePath ? (
                          <span className="text-emerald-600 ml-2 font-medium">✓ Uploaded</span>
                        ) : (
                          <span className="text-gray-400 ml-2">Awaiting upload</span>
                        )}
                      </div>
                    </div>
                  </div>
                  <div className="w-full sm:w-auto flex justify-end">
                    <Button
                      size="sm"
                      variant={doc.filePath ? 'secondary' : 'primary'}
                      disabled={uploadLoading}
                      onClick={() => handleFileUpload(doc.documentType, doc.documentName)}
                      className="w-full sm:w-auto flex items-center justify-center gap-1.5"
                    >
                      <Upload className="w-3.5 h-3.5" />
                      {doc.filePath ? 'Re-upload' : 'Upload File'}
                    </Button>
                  </div>
                </div>
              ))}
            </div>

            <div className="pt-4 border-t flex justify-between">
              <Button variant="secondary" onClick={() => setStep(2)}>
                Back
              </Button>
              <Button
                variant="primary"
                size="lg"
                onClick={() => setStep(4)}
              >
                Proceed to Engagement Sign-Off
              </Button>
            </div>
          </div>
        )}

        {/* Step 4: Engagement Letter & Fee Sign-Off */}
        {step === 4 && (
          <form onSubmit={handleSignEngagement} className="bg-white rounded-2xl p-8 border border-gray-200 shadow-lg space-y-6">
            <div>
              <h2 className="text-xl font-bold text-gray-900">Engagement Terms & Digital Sign-Off</h2>
              <p className="text-xs text-gray-500 mt-1">
                Please review and digitally confirm the professional advisory terms.
              </p>
            </div>

            <div className="bg-gray-50 p-4 rounded-xl border border-gray-200 text-xs text-gray-700 space-y-2 leading-relaxed">
              <h4 className="font-bold text-gray-900">Standard Professional Retainership Terms:</h4>
              <p>1. The client authorizes the practitioner to prepare and file statutory returns with the Income Tax & GST departments.</p>
              <p>2. The client certifies that all books of accounts, invoices, and bank statements provided are true and complete.</p>
              <p>3. Professional fees are agreed upon and payable as per billing milestones.</p>
            </div>

            <div className="space-y-3 pt-2">
              <div className="flex items-start gap-2.5">
                <input
                  type="checkbox"
                  id="consent1"
                  required
                  checked={signForm.signedConsent}
                  onChange={(e) => setSignForm({ ...signForm, signedConsent: e.target.checked })}
                  className="mt-0.5 rounded text-primary-600 focus:ring-primary-500"
                />
                <label htmlFor="consent1" className="text-xs text-gray-700">
                  I accept the Engagement Letter and authorize the practice to act as my authorized tax representative.
                </label>
              </div>

              <div className="flex items-start gap-2.5">
                <input
                  type="checkbox"
                  id="consent2"
                  required
                  checked={signForm.agreedToFees}
                  onChange={(e) => setSignForm({ ...signForm, agreedToFees: e.target.checked })}
                  className="mt-0.5 rounded text-primary-600 focus:ring-primary-500"
                />
                <label htmlFor="consent2" className="text-xs text-gray-700">
                  I agree to the professional fee structure and terms of payment.
                </label>
              </div>
            </div>

            <div className="pt-4 border-t flex justify-between">
              <Button variant="secondary" onClick={() => setStep(3)}>
                Back
              </Button>
              <Button
                variant="primary"
                size="lg"
                type="submit"
                disabled={actionLoading}
                className="bg-emerald-600 hover:bg-emerald-700 text-white"
              >
                {actionLoading ? 'Signing...' : 'Sign & Submit Onboarding Checklist'}
              </Button>
            </div>
          </form>
        )}

        {/* Step 5: Completed / Under Review */}
        {step === 5 && (
          <div className="bg-white rounded-2xl p-8 border border-gray-200 shadow-xl text-center space-y-5">
            <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto">
              <ShieldCheck className="w-10 h-10" />
            </div>

            <div>
              <h2 className="text-2xl font-bold text-gray-900">Onboarding Submitted Successfully!</h2>
              <p className="text-sm text-gray-600 mt-2 max-w-md mx-auto">
                Your statutory documentation and signed engagement terms have been submitted to{' '}
                <strong>{proposal?.practiceDisplayName || onboarding?.practiceDisplayName || 'the Practice'}</strong>.
              </p>
            </div>

            <div className="bg-emerald-50 border border-emerald-200 p-4 rounded-xl max-w-md mx-auto text-xs text-emerald-800 text-left space-y-1">
              <div className="font-bold">Next Steps:</div>
              <div>1. Practice Partner reviews and verifies your uploaded KYC documents.</div>
              <div>2. You will be officially promoted to Client Master upon approval.</div>
              <div>3. Your Client Portal login credentials will be emailed automatically.</div>
            </div>

            <div className="pt-4">
              <Button variant="secondary" onClick={() => navigate('/marketplace')}>
                Return to Marketplace
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
