import React, { useState } from 'react';
import {
  X,
  FileText,
  Calendar,
  Plus,
  Trash2,
  CheckCircle,
  AlertCircle,
  Clock,
  Sparkles,
  HelpCircle,
} from 'lucide-react';
import { CreateDocumentRequest, CreateDocumentRequestItem } from '../../types';
import { documentRequestApi } from '../../api/endpoints';

interface RequestDocumentsModalProps {
  isOpen: boolean;
  onClose: () => void;
  clientId: string;
  clientName: string;
  onSuccess: () => void;
}

interface TemplatePreset {
  id: string;
  name: string;
  category: string;
  defaultPurpose: string;
  items: CreateDocumentRequestItem[];
}

const TEMPLATES: TemplatePreset[] = [
  {
    id: 'itr_individual',
    name: 'ITR — Individual / Salaried',
    category: 'Income Tax',
    defaultPurpose: 'ITR FY 2026-27 Filing Preparation',
    items: [
      { documentType: 'FORM_16', title: 'Form 16 (Part A & B)', description: 'Signed TDS certificate from employer', required: true },
      { documentType: 'AIS_TIS', title: 'Annual Information Statement (AIS / TIS)', description: 'Downloaded from Income Tax portal', required: true },
      { documentType: 'FORM_26AS', title: 'Form 26AS Tax Credit Statement', description: 'Annual tax deduction and TCS statement', required: true },
      { documentType: 'BANK_STATEMENT', title: 'Bank Account Statements', description: 'Savings/Salary statements for the entire financial year', required: true },
      { documentType: 'OTHER', title: 'Capital Gains Statement / Broker Ledger', description: 'From Zerodha/Groww/CAMS/KFintech if shares/mutual funds traded', required: false },
      { documentType: 'OTHER', title: 'Chapter VI-A Investment Proofs', description: 'LIC, ELSS, PPF, NPS, Mediclaim (80D) receipts', required: false },
      { documentType: 'OTHER', title: 'House Rent Receipts / Agreement', description: 'For HRA exemption claim (if paying rent)', required: false },
    ],
  },
  {
    id: 'itr_business',
    name: 'ITR — Business / Professional',
    category: 'Income Tax',
    defaultPurpose: 'Tax Audit & ITR FY 2025-26 Compliance',
    items: [
      { documentType: 'FINANCIAL_STATEMENTS', title: 'Audited Financial Statements', description: 'Balance Sheet, Profit & Loss, and Schedules', required: true },
      { documentType: 'TAX_AUDIT_REPORT', title: 'Tax Audit Report (Form 3CB-3CD)', description: 'Draft/Final certified audit report', required: true },
      { documentType: 'FORM_26AS', title: 'Form 26AS & AIS/TIS', description: 'Updated tax credit statements', required: true },
      { documentType: 'BANK_STATEMENT', title: 'Current & OD/CC Bank Statements', description: 'All operational bank accounts for the financial year', required: true },
      { documentType: 'OTHER', title: 'Depreciation & Fixed Asset Register', description: 'Additions/deletions with supporting purchase bills', required: false },
    ],
  },
  {
    id: 'gst_monthly',
    name: 'GST — Monthly Return Filing',
    category: 'GST Compliance',
    defaultPurpose: 'GST Monthly Return Data Collection',
    items: [
      { documentType: 'GST_INVOICE_PURCHASE', title: 'Purchase Invoices / Register', description: 'Excel/Tally export with inward supply details and ITC', required: true },
      { documentType: 'GST_INVOICE_SALE', title: 'Sales Register / Outward Invoices', description: 'Monthly sales summary with HSN and tax breakdown', required: true },
      { documentType: 'BANK_STATEMENT', title: 'Bank Statement for the Month', description: 'For reconciliations and tax challan verification', required: true },
    ],
  },
  {
    id: 'tds_quarterly',
    name: 'TDS — Quarterly Filing',
    category: 'TDS Compliance',
    defaultPurpose: 'TDS Quarterly Return Preparation',
    items: [
      { documentType: 'OTHER', title: 'Salary / Contractor Payment Register', description: 'Gross payments, deductions, and section mappings (192, 194C, 194J)', required: true },
      { documentType: 'CHALLAN_RECEIPT', title: 'Challan 281 Payment Receipts', description: 'Paid challan receipts with BSR code and CIN', required: true },
    ],
  },
];

export const RequestDocumentsModal: React.FC<RequestDocumentsModalProps> = ({
  isOpen,
  onClose,
  clientId,
  clientName,
  onSuccess,
}) => {
  const [selectedTemplate, setSelectedTemplate] = useState<string>('itr_individual');
  const [purpose, setPurpose] = useState<string>(TEMPLATES[0].defaultPurpose);
  const [dueDate, setDueDate] = useState<string>(
    new Date(Date.now() + 15 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  );
  const [message, setMessage] = useState<string>('');
  const [financialYear, setFinancialYear] = useState<string>('2026-27');
  const [items, setItems] = useState<CreateDocumentRequestItem[]>([...TEMPLATES[0].items]);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleTemplateSelect = (templateId: string) => {
    setSelectedTemplate(templateId);
    const tmpl = TEMPLATES.find((t) => t.id === templateId);
    if (tmpl) {
      setPurpose(tmpl.defaultPurpose);
      setItems([...tmpl.items]);
    }
  };

  const handleAddItem = () => {
    setItems([
      ...items,
      {
        documentType: 'OTHER',
        title: '',
        description: '',
        required: true,
      },
    ]);
  };

  const handleRemoveItem = (index: number) => {
    setItems(items.filter((_, i) => i !== index));
  };

  const handleItemChange = (index: number, field: keyof CreateDocumentRequestItem, value: any) => {
    const updated = [...items];
    updated[index] = { ...updated[index], [field]: value };
    setItems(updated);
  };

  const setDueDateOffset = (days: number) => {
    const d = new Date();
    d.setDate(d.getDate() + days);
    setDueDate(d.toISOString().split('T')[0]);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!purpose.trim()) {
      setError('Please provide a purpose for the document request.');
      return;
    }

    const validItems = items.filter((it) => it.title.trim().length > 0);
    if (validItems.length === 0) {
      setError('Please provide at least one valid document item to request.');
      return;
    }

    try {
      setIsSubmitting(true);
      const payload: CreateDocumentRequest = {
        clientId,
        purpose: purpose.trim(),
        dueDate: dueDate || undefined,
        message: message.trim() || undefined,
        financialYear: financialYear || undefined,
        items: validItems,
      };

      await documentRequestApi.create(payload);
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to dispatch document request. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4">
      <div className="bg-white rounded-t-3xl sm:rounded-2xl shadow-2xl max-w-3xl w-full overflow-hidden border border-slate-200 flex flex-col max-h-[90dvh] animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="px-6 py-5 bg-gradient-to-r from-slate-900 via-slate-800 to-[#082e5b] text-white flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 bg-emerald-500/20 text-emerald-400 rounded-xl border border-emerald-500/30">
              <FileText className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-lg font-bold tracking-wide flex items-center gap-2">
                Request Documents from Client
                <span className="text-xs bg-emerald-500/20 text-emerald-300 font-semibold px-2.5 py-0.5 rounded-full border border-emerald-400/30">
                  Client Document Request V1
                </span>
              </h2>
              <p className="text-xs text-slate-300">
                Client: <span className="font-semibold text-white">{clientName}</span>
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-slate-700/60 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-6">
          {error && (
            <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl flex items-start space-x-3 text-rose-800 text-sm">
              <AlertCircle className="w-5 h-5 text-rose-600 flex-shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {/* Quick Template Presets */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-2 flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-emerald-600" />
              Quick Tax Category Presets
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-2.5">
              {TEMPLATES.map((tmpl) => (
                <button
                  type="button"
                  key={tmpl.id}
                  onClick={() => handleTemplateSelect(tmpl.id)}
                  className={`p-3 rounded-xl border text-left transition-all ${
                    selectedTemplate === tmpl.id
                      ? 'border-emerald-600 bg-emerald-50/70 text-emerald-950 shadow-sm ring-1 ring-emerald-500'
                      : 'border-slate-200 hover:border-slate-300 bg-white text-slate-700'
                  }`}
                >
                  <span className="block text-[10px] font-bold text-slate-400 uppercase">{tmpl.category}</span>
                  <span className="block text-xs font-bold mt-0.5 truncate">{tmpl.name}</span>
                  <span className="block text-[11px] text-slate-500 mt-1 font-medium">
                    {tmpl.items.length} checklist items
                  </span>
                </button>
              ))}
            </div>
          </div>

          {/* Purpose & Financial Year */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="md:col-span-2">
              <label className="block text-xs font-bold text-slate-700 mb-1">
                Purpose of Request <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                value={purpose}
                onChange={(e) => setPurpose(e.target.value)}
                placeholder="e.g. ITR FY 2026-27 Filing Preparation"
                className="w-full px-3.5 py-2.5 text-sm border border-slate-300 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 font-medium"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Financial Year</label>
              <select
                value={financialYear}
                onChange={(e) => setFinancialYear(e.target.value)}
                className="w-full px-3.5 py-2.5 text-sm border border-slate-300 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 font-medium bg-white"
              >
                <option value="2026-27">FY 2026-27 (AY 2027-28)</option>
                <option value="2025-26">FY 2025-26 (AY 2026-27)</option>
                <option value="2024-25">FY 2024-25 (AY 2025-26)</option>
              </select>
            </div>
          </div>

          {/* Due Date Picker & Shortcuts */}
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="block text-xs font-bold text-slate-700 flex items-center gap-1.5">
                <Calendar className="w-3.5 h-3.5 text-slate-500" />
                Submission Due Date
              </label>
              <div className="flex space-x-1.5 text-xs font-semibold">
                <button
                  type="button"
                  onClick={() => setDueDateOffset(7)}
                  className="px-2 py-0.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-md transition-colors"
                >
                  +7 Days
                </button>
                <button
                  type="button"
                  onClick={() => setDueDateOffset(15)}
                  className="px-2 py-0.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-md transition-colors"
                >
                  +15 Days
                </button>
                <button
                  type="button"
                  onClick={() => setDueDateOffset(30)}
                  className="px-2 py-0.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-md transition-colors"
                >
                  +30 Days
                </button>
              </div>
            </div>
            <input
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              className="w-full px-3.5 py-2.5 text-sm border border-slate-300 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 font-medium"
            />
          </div>

          {/* Custom Note/Message */}
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">
              Custom Message for Client <span className="text-slate-400 font-normal">(Included in Email & Portal)</span>
            </label>
            <textarea
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="e.g. Dear client, please ensure all bank statements include closing balance as on 31st March."
              rows={2}
              className="w-full px-3.5 py-2 text-sm border border-slate-300 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
            />
          </div>

          {/* Checklist Items Builder */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-600">
                Requested Document Checklist ({items.length} items)
              </label>
              <button
                type="button"
                onClick={handleAddItem}
                className="inline-flex items-center space-x-1.5 text-xs font-bold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 px-3 py-1.5 rounded-lg transition-colors border border-emerald-200"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add Custom Document</span>
              </button>
            </div>

            <div className="space-y-3">
              {items.map((item, index) => (
                <div
                  key={index}
                  className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl flex items-start space-x-3 transition-all hover:border-slate-300"
                >
                  <div className="p-2 bg-white rounded-lg border border-slate-200 text-slate-500 flex-shrink-0 mt-1">
                    <FileText className="w-4 h-4 text-emerald-600" />
                  </div>

                  <div className="flex-1 grid grid-cols-1 md:grid-cols-12 gap-2.5">
                    <div className="md:col-span-6">
                      <input
                        type="text"
                        value={item.title}
                        onChange={(e) => handleItemChange(index, 'title', e.target.value)}
                        placeholder="Document Title (e.g. Form 16 Part A & B)"
                        className="w-full px-3 py-1.5 text-xs border border-slate-300 rounded-lg focus:ring-1 focus:ring-emerald-500 font-semibold bg-white"
                        required
                      />
                    </div>

                    <div className="md:col-span-4">
                      <input
                        type="text"
                        value={item.description || ''}
                        onChange={(e) => handleItemChange(index, 'description', e.target.value)}
                        placeholder="Instructions / Notes for client"
                        className="w-full px-3 py-1.5 text-xs border border-slate-300 rounded-lg focus:ring-1 focus:ring-emerald-500 bg-white text-slate-600"
                      />
                    </div>

                    <div className="md:col-span-2 flex items-center space-x-2">
                      <label className="inline-flex items-center space-x-1.5 text-xs cursor-pointer select-none">
                        <input
                          type="checkbox"
                          checked={item.required}
                          onChange={(e) => handleItemChange(index, 'required', e.target.checked)}
                          className="w-3.5 h-3.5 text-emerald-600 rounded border-slate-300 focus:ring-emerald-500"
                        />
                        <span className="text-[11px] font-semibold text-slate-700">Mandatory</span>
                      </label>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => handleRemoveItem(index)}
                    disabled={items.length <= 1}
                    className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-rose-50 transition-colors disabled:opacity-30"
                    title="Remove item"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          </div>
        </form>

        {/* Footer Actions */}
        <div className="px-6 py-4 bg-slate-50 border-t border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="text-xs text-slate-500 flex items-center gap-1.5">
            <CheckCircle className="w-4 h-4 text-emerald-600" />
            <span>Client will receive In-App alert & Taxoryn branded Email notification</span>
          </div>

          <div className="flex items-center space-x-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 text-xs font-bold text-slate-600 hover:text-slate-800 bg-white border border-slate-300 rounded-xl hover:bg-slate-100 transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={handleSubmit}
              disabled={isSubmitting}
              className="px-5 py-2 text-xs font-bold text-slate-900 bg-[#00d1a3] hover:bg-[#00b388] rounded-xl shadow-sm transition-all flex items-center space-x-2 disabled:opacity-60"
            >
              {isSubmitting ? (
                <>
                  <div className="w-4 h-4 border-2 border-slate-900 border-t-transparent rounded-full animate-spin" />
                  <span>Dispatching Request...</span>
                </>
              ) : (
                <>
                  <FileText className="w-4 h-4" />
                  <span>Send Document Request</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};