import React, { useState } from 'react';
import {
  X,
  FileText,
  Sparkles,
  Send,
  AlertCircle,
  FileCheck,
  Award,
  Scroll,
  FolderPlus,
} from 'lucide-react';
import { DocumentCategory, CreateClientAcknowledgementRequest } from '../../types';
import { documentRequestApi } from '../../api/endpoints';

interface RequestDocumentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

const CATEGORY_ITEMS: {
  category: DocumentCategory;
  label: string;
  desc: string;
  icon: React.ElementType;
}[] = [
  {
    category: 'ACKNOWLEDGEMENT',
    label: 'Filing Acknowledgement',
    desc: 'ITR-V, TDS Return Ack, GST Acknowledgement',
    icon: FileCheck,
  },
  {
    category: 'RETURN_COPY',
    label: 'Filed Return Copy',
    desc: 'ITR Filing copy, GSTR-1/3B filed copy',
    icon: FileText,
  },
  {
    category: 'TAX_DOCUMENT',
    label: 'Tax Document / Computation',
    desc: 'Income computation sheet, tax challans',
    icon: Scroll,
  },
  {
    category: 'CERTIFICATE',
    label: 'Certificate / Proof',
    desc: 'GST Certificate, Net Worth Certificate, 15CB',
    icon: Award,
  },
  {
    category: 'OTHER',
    label: 'Other Document',
    desc: 'Custom financial statement or report',
    icon: FolderPlus,
  },
];

const PRESETS = [
  {
    label: 'ITR Acknowledgement (AY 2026-27)',
    category: 'ACKNOWLEDGEMENT' as DocumentCategory,
    purpose: 'ITR Acknowledgement (ITR-V) AY 2026-27',
    fy: '2025-26',
    ay: '2026-27',
    docType: 'ITR_ACKNOWLEDGEMENT',
  },
  {
    label: 'Computation of Total Income',
    category: 'TAX_DOCUMENT' as DocumentCategory,
    purpose: 'Computation of Total Income AY 2026-27',
    fy: '2025-26',
    ay: '2026-27',
    docType: 'ITR_COMPUTATION_SHEET',
  },
  {
    label: 'GSTR-3B Return Summary',
    category: 'RETURN_COPY' as DocumentCategory,
    purpose: 'Filed GSTR-3B Summary Copy',
    fy: '2025-26',
    period: 'Current Quarter',
    docType: 'OTHER',
  },
  {
    label: 'TDS Return Acknowledgement',
    category: 'ACKNOWLEDGEMENT' as DocumentCategory,
    purpose: 'Quarterly TDS Return (26Q/24Q) Acknowledgement',
    fy: '2025-26',
    docType: 'TDS_RETURN_ACKNOWLEDGEMENT',
  },
];

export const RequestDocumentModal: React.FC<RequestDocumentModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [category, setCategory] = useState<DocumentCategory>('ACKNOWLEDGEMENT');
  const [purpose, setPurpose] = useState<string>('ITR Acknowledgement AY 2026-27');
  const [financialYear, setFinancialYear] = useState<string>('2025-26');
  const [assessmentYear, setAssessmentYear] = useState<string>('2026-27');
  const [taxPeriod, setTaxPeriod] = useState<string>('');
  const [message, setMessage] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const applyPreset = (preset: typeof PRESETS[0]) => {
    setCategory(preset.category);
    setPurpose(preset.purpose);
    if (preset.fy) setFinancialYear(preset.fy);
    if (preset.ay) setAssessmentYear(preset.ay);
    if (preset.period) setTaxPeriod(preset.period);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!purpose.trim()) {
      setError('Please provide a subject or purpose for this request.');
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);

      const payload: CreateClientAcknowledgementRequest = {
        category,
        purpose: purpose.trim(),
        financialYear: financialYear || undefined,
        assessmentYear: assessmentYear || undefined,
        taxPeriod: taxPeriod || undefined,
        message: message.trim() || undefined,
      };

      await documentRequestApi.requestAcknowledgement(payload);
      onSuccess();
      onClose();
    } catch (err: any) {
      console.error('Failed to submit acknowledgement request', err);
      setError(err?.response?.data?.message || err?.message || 'Failed to submit document request');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-2xl w-full max-w-xl max-h-[90vh] flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-500/10 text-blue-600 dark:text-blue-400 flex items-center justify-center">
              <FileText className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900 dark:text-white">
                Request a Document from Your Practitioner
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Request an ITR acknowledgement, tax computation sheet, or certificate
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-5">
          {error && (
            <div className="p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/50 rounded-xl text-xs text-red-600 dark:text-red-400 flex items-start gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {/* Quick Presets */}
          <div>
            <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-700 dark:text-slate-300 mb-2">
              <Sparkles className="w-3.5 h-3.5 text-blue-500" />
              <span>Quick Presets</span>
            </div>
            <div className="flex flex-wrap gap-1.5">
              {PRESETS.map((preset, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => applyPreset(preset)}
                  className="text-xs px-2.5 py-1.5 bg-blue-50 hover:bg-blue-100 dark:bg-blue-950/40 dark:hover:bg-blue-900/50 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-900/50 rounded-lg transition-colors"
                >
                  {preset.label}
                </button>
              ))}
            </div>
          </div>

          {/* Category Selector Cards */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-2">
              What type of document do you need? <span className="text-red-500">*</span>
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              {CATEGORY_ITEMS.map(item => {
                const isSelected = category === item.category;
                const Icon = item.icon;
                return (
                  <div
                    key={item.category}
                    onClick={() => setCategory(item.category)}
                    className={`p-3 rounded-xl border cursor-pointer transition-all flex items-start gap-2.5 ${
                      isSelected
                        ? 'border-blue-500 bg-blue-50/40 dark:bg-blue-950/30 shadow-sm'
                        : 'border-slate-200 dark:border-slate-700/60 hover:border-slate-300 dark:hover:border-slate-600 bg-slate-50/50 dark:bg-slate-800/20'
                    }`}
                  >
                    <div
                      className={`p-2 rounded-lg ${
                        isSelected
                          ? 'bg-blue-500 text-white'
                          : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400'
                      }`}
                    >
                      <Icon className="w-4 h-4" />
                    </div>
                    <div className="min-w-0">
                      <p
                        className={`text-xs font-bold ${
                          isSelected ? 'text-blue-700 dark:text-blue-300' : 'text-slate-800 dark:text-slate-200'
                        }`}
                      >
                        {item.label}
                      </p>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-tight mt-0.5">
                        {item.desc}
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Subject / Purpose */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              Subject / Purpose <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={purpose}
              onChange={e => setPurpose(e.target.value)}
              placeholder="e.g. ITR Acknowledgement AY 2026-27"
              className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>

          {/* Financial Year / Assessment Year / Period */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Financial Year
              </label>
              <input
                type="text"
                placeholder="2025-26"
                value={financialYear}
                onChange={e => setFinancialYear(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Assessment Year
              </label>
              <input
                type="text"
                placeholder="2026-27"
                value={assessmentYear}
                onChange={e => setAssessmentYear(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Tax Period / Quarter
              </label>
              <input
                type="text"
                placeholder="e.g. Q1, July 2026"
                value={taxPeriod}
                onChange={e => setTaxPeriod(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          {/* Optional Message */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Note to Practitioner <span className="text-slate-400 font-normal">(Optional)</span>
            </label>
            <textarea
              rows={2}
              value={message}
              onChange={e => setMessage(e.target.value)}
              placeholder="e.g. Please share the verified ITR acknowledgement for loan processing."
              className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </form>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/50">
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="px-4 py-2 text-xs font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-800 rounded-xl transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={isSubmitting}
            className="px-5 py-2 text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded-xl transition-all shadow-md shadow-blue-500/20 flex items-center gap-2"
          >
            <Send className="w-4 h-4" />
            {isSubmitting ? 'Submitting...' : 'Submit Request'}
          </button>
        </div>
      </div>
    </div>
  );
};
