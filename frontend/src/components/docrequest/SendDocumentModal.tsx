import React, { useState, useEffect } from 'react';
import {
  X,
  Send,
  Upload,
  FileCheck,
  FileText,
  AlertCircle,
  CheckCircle2,
  ShieldCheck,
  Calendar,
  FolderOpen,
  Search,
} from 'lucide-react';
import { DocumentRequest, DocumentCategory, SendDocumentToClientRequest, DocumentItem } from '../../types';
import { documentRequestApi, documentApi, clientApi } from '../../api/endpoints';

interface SendDocumentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  // If fulfilling an existing request:
  fulfillingRequest?: DocumentRequest | null;
  // If sending directly to a specific client:
  preselectedClientId?: string;
  preselectedClientName?: string;
}

const CATEGORY_OPTIONS: { value: DocumentCategory; label: string; description: string }[] = [
  { value: 'ACKNOWLEDGEMENT', label: 'Acknowledgement Receipt', description: 'ITR-V, TDS Return, GST Ack, Notice Ack' },
  { value: 'RETURN_COPY', label: 'Filed Return Copy / Computation', description: 'ITR Filing copy, GSTR-1/3B summary' },
  { value: 'TAX_DOCUMENT', label: 'Tax Document / Computation', description: 'Tax computation sheet, 26AS review, challans' },
  { value: 'CERTIFICATE', label: 'Certificate / Registration', description: 'GST Certificate, PAN/TAN, Form 15CA/CB' },
  { value: 'OTHER', label: 'Other Document', description: 'Advisory, report, or general deliverable' },
];

const DOC_TYPE_OPTIONS = [
  { value: 'ITR_ACKNOWLEDGEMENT', label: 'ITR-V / Acknowledgement' },
  { value: 'ITR_COMPUTATION_SHEET', label: 'ITR Computation Sheet' },
  { value: 'TDS_RETURN_ACKNOWLEDGEMENT', label: 'TDS Return Acknowledgement' },
  { value: 'GST_REGISTRATION_CERTIFICATE', label: 'GST Registration Certificate' },
  { value: 'NOTICE_ACKNOWLEDGEMENT', label: 'Notice Submission Ack' },
  { value: 'TAX_AUDIT_REPORT', label: 'Tax Audit Report (Form 3CD)' },
  { value: 'CHALLAN_RECEIPT', label: 'Tax Challan Payment Receipt' },
  { value: 'FINANCIAL_STATEMENTS', label: 'Audited Financial Statements' },
  { value: 'OTHER', label: 'Other Deliverable' },
];

export const SendDocumentModal: React.FC<SendDocumentModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  fulfillingRequest,
  preselectedClientId,
  preselectedClientName,
}) => {
  const [sourceMode, setSourceMode] = useState<'UPLOAD' | 'VAULT'>('UPLOAD');
  const [selectedClientId, setSelectedClientId] = useState<string>(
    fulfillingRequest?.clientId || preselectedClientId || ''
  );
  const [clientSearch, setClientSearch] = useState<string>('');
  const [clientList, setClientList] = useState<{ id: string; name: string; pan?: string }[]>([]);
  const [category, setCategory] = useState<DocumentCategory>(
    fulfillingRequest?.category || 'ACKNOWLEDGEMENT'
  );
  const [documentType, setDocumentType] = useState<string>('ITR_ACKNOWLEDGEMENT');
  const [title, setTitle] = useState<string>(fulfillingRequest?.purpose || '');
  const [financialYear, setFinancialYear] = useState<string>(fulfillingRequest?.financialYear || '2025-26');
  const [assessmentYear, setAssessmentYear] = useState<string>(fulfillingRequest?.assessmentYear || '2026-27');
  const [taxPeriod, setTaxPeriod] = useState<string>(fulfillingRequest?.taxPeriod || '');
  const [message, setMessage] = useState<string>('');

  // Upload file state
  const [file, setFile] = useState<File | null>(null);
  const [dragOver, setDragOver] = useState<boolean>(false);

  // Vault documents state
  const [vaultDocs, setVaultDocs] = useState<DocumentItem[]>([]);
  const [selectedVaultDocId, setSelectedVaultDocId] = useState<string>('');
  const [vaultSearch, setVaultSearch] = useState<string>('');
  const [loadingVault, setLoadingVault] = useState<boolean>(false);

  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Sync props when opening
  useEffect(() => {
    if (isOpen) {
      setError(null);
      setFile(null);
      setSelectedVaultDocId('');
      if (fulfillingRequest) {
        setSelectedClientId(fulfillingRequest.clientId);
        setCategory(fulfillingRequest.category || 'ACKNOWLEDGEMENT');
        setTitle(fulfillingRequest.purpose ? `Delivered: ${fulfillingRequest.purpose}` : 'ITR Acknowledgement');
        setFinancialYear(fulfillingRequest.financialYear || '2025-26');
        setAssessmentYear(fulfillingRequest.assessmentYear || '2026-27');
        setTaxPeriod(fulfillingRequest.taxPeriod || '');
      } else if (preselectedClientId) {
        setSelectedClientId(preselectedClientId);
        setTitle('');
      } else {
        setSelectedClientId('');
        setTitle('');
        loadClients();
      }
    }
  }, [isOpen, fulfillingRequest, preselectedClientId]);

  // Load vault documents when switching to VAULT mode or changing client
  useEffect(() => {
    if (isOpen && sourceMode === 'VAULT' && selectedClientId) {
      loadVaultDocuments(selectedClientId);
    }
  }, [isOpen, sourceMode, selectedClientId]);

  const loadClients = async () => {
    try {
      const res = await clientApi.getAll({ size: 100 });
      if (res && res.content) {
        setClientList(res.content.map(c => ({ id: c.id, name: c.displayName, pan: c.pan })));
      }
    } catch {
      // Fallback
    }
  };

  const loadVaultDocuments = async (cId: string) => {
    try {
      setLoadingVault(true);
      const docs = await documentApi.getByClientId(cId);
      // Only allow CLEAN scanned docs
      const cleanDocs = (docs || []).filter(d => (d as any).scanStatus === 'CLEAN' || !('scanStatus' in d));
      setVaultDocs(cleanDocs);
    } catch (err: any) {
      console.error('Failed to load client vault docs', err);
    } finally {
      setLoadingVault(false);
    }
  };

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selected = e.target.files[0];
      if (selected.size > 25 * 1024 * 1024) {
        setError('File size exceeds the 25MB limit.');
        return;
      }
      setFile(selected);
      setError(null);
      if (!title) {
        setTitle(selected.name.replace(/\.[^/.]+$/, ''));
      }
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const dropped = e.dataTransfer.files[0];
      if (dropped.size > 25 * 1024 * 1024) {
        setError('File size exceeds the 25MB limit.');
        return;
      }
      setFile(dropped);
      setError(null);
      if (!title) {
        setTitle(dropped.name.replace(/\.[^/.]+$/, ''));
      }
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedClientId) {
      setError('Please select a recipient client.');
      return;
    }
    if (!title.trim()) {
      setError('Please provide a document title or description.');
      return;
    }

    if (sourceMode === 'UPLOAD' && !file) {
      setError('Please select a file to upload and send.');
      return;
    }

    if (sourceMode === 'VAULT' && !selectedVaultDocId) {
      setError('Please select a clean document from the client vault.');
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);

      const payload: SendDocumentToClientRequest = {
        clientId: selectedClientId,
        requestId: fulfillingRequest?.id,
        category,
        documentType,
        title: title.trim(),
        financialYear: financialYear || undefined,
        assessmentYear: assessmentYear || undefined,
        taxPeriod: taxPeriod || undefined,
        message: message.trim() || undefined,
        existingDocumentId: sourceMode === 'VAULT' ? selectedVaultDocId : undefined,
      };

      await documentRequestApi.sendDocument(payload, sourceMode === 'UPLOAD' ? file || undefined : undefined);
      onSuccess();
      onClose();
    } catch (err: any) {
      console.error('Failed to send document to client', err);
      setError(err?.response?.data?.message || err?.message || 'Failed to deliver document to client');
    } finally {
      setIsSubmitting(false);
    }
  };

  const filteredVaultDocs = vaultDocs.filter(d => {
    const name = d.fileName || d.filename || d.originalFilename || '';
    const type = d.documentType || d.category || '';
    return name.toLowerCase().includes(vaultSearch.toLowerCase()) || type.toLowerCase().includes(vaultSearch.toLowerCase());
  });

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-500/10 text-blue-600 dark:text-blue-400 flex items-center justify-center">
              <Send className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">
                {fulfillingRequest ? 'Fulfill Client Document Request' : 'Send Document to Client'}
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {fulfillingRequest
                  ? `Delivering ${fulfillingRequest.category || 'document'} for request ${fulfillingRequest.requestNumber}`
                  : 'Deliver acknowledgements, return copies, or reports directly to client portal'}
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

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-5">
          {error && (
            <div className="p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/50 rounded-xl text-xs text-red-600 dark:text-red-400 flex items-start gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {/* Client Selection (if not fulfilling and not preselected) */}
          {!fulfillingRequest && !preselectedClientId && (
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                Recipient Client <span className="text-red-500">*</span>
              </label>
              <select
                value={selectedClientId}
                onChange={e => setSelectedClientId(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
                required
              >
                <option value="">-- Select Client --</option>
                {clientList.map(c => (
                  <option key={c.id} value={c.id}>
                    {c.name} {c.pan ? `(${c.pan})` : ''}
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* If fulfilling or preselected, display client banner */}
          {(fulfillingRequest || preselectedClientId) && (
            <div className="px-4 py-3 bg-blue-50/50 dark:bg-blue-950/20 border border-blue-200 dark:border-blue-900/30 rounded-xl flex items-center justify-between text-xs">
              <span className="text-slate-600 dark:text-slate-400">Recipient:</span>
              <span className="font-semibold text-blue-700 dark:text-blue-300">
                {fulfillingRequest?.clientName || preselectedClientName || 'Selected Client'}
              </span>
            </div>
          )}

          {/* Category & Document Type */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                Document Category <span className="text-red-500">*</span>
              </label>
              <select
                value={category}
                onChange={e => setCategory(e.target.value as DocumentCategory)}
                className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
              >
                {CATEGORY_OPTIONS.map(opt => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                Document Type
              </label>
              <select
                value={documentType}
                onChange={e => setDocumentType(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
              >
                {DOC_TYPE_OPTIONS.map(opt => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Title / Purpose */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              Document Title / Purpose <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={title}
              onChange={e => setTitle(e.target.value)}
              placeholder="e.g. ITR-V Acknowledgement AY 2026-27"
              className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
              required
            />
          </div>

          {/* Source Mode Toggle */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-2">
              Document Source
            </label>
            <div className="grid grid-cols-2 gap-3 p-1 bg-slate-100 dark:bg-slate-800/60 rounded-xl">
              <button
                type="button"
                onClick={() => setSourceMode('UPLOAD')}
                className={`flex items-center justify-center gap-2 py-2 px-3 rounded-lg text-xs font-semibold transition-all ${
                  sourceMode === 'UPLOAD'
                    ? 'bg-white dark:bg-slate-900 text-blue-600 dark:text-blue-400 shadow-sm'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                <Upload className="w-4 h-4" />
                Upload New File
              </button>
              <button
                type="button"
                onClick={() => setSourceMode('VAULT')}
                className={`flex items-center justify-center gap-2 py-2 px-3 rounded-lg text-xs font-semibold transition-all ${
                  sourceMode === 'VAULT'
                    ? 'bg-white dark:bg-slate-900 text-blue-600 dark:text-blue-400 shadow-sm'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                <FolderOpen className="w-4 h-4" />
                Select from Vault
              </button>
            </div>
          </div>

          {/* Mode 1: File Upload */}
          {sourceMode === 'UPLOAD' && (
            <div>
              <div
                onDragOver={e => {
                  e.preventDefault();
                  setDragOver(true);
                }}
                onDragLeave={() => setDragOver(false)}
                onDrop={handleDrop}
                className={`border-2 border-dashed rounded-2xl p-6 text-center transition-all ${
                  dragOver
                    ? 'border-blue-500 bg-blue-500/5'
                    : file
                    ? 'border-emerald-500/50 bg-emerald-500/5'
                    : 'border-slate-200 dark:border-slate-700 hover:border-slate-300 dark:hover:border-slate-600 bg-slate-50/50 dark:bg-slate-800/30'
                }`}
              >
                <input
                  type="file"
                  id="send-doc-file"
                  className="hidden"
                  onChange={handleFileChange}
                  accept=".pdf,.png,.jpg,.jpeg,.zip,.csv,.xlsx"
                />

                {file ? (
                  <div className="flex items-center justify-between gap-3 text-left">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
                        <FileCheck className="w-5 h-5" />
                      </div>
                      <div>
                        <p className="text-sm font-semibold text-slate-900 dark:text-white truncate max-w-xs">
                          {file.name}
                        </p>
                        <p className="text-xs text-slate-500">
                          {(file.size / (1024 * 1024)).toFixed(2)} MB • Ready for delivery
                        </p>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => setFile(null)}
                      className="text-xs font-semibold text-red-600 hover:text-red-700 dark:text-red-400 hover:underline"
                    >
                      Change
                    </button>
                  </div>
                ) : (
                  <label htmlFor="send-doc-file" className="cursor-pointer space-y-2 block">
                    <div className="w-10 h-10 mx-auto rounded-xl bg-blue-500/10 text-blue-600 dark:text-blue-400 flex items-center justify-center">
                      <Upload className="w-5 h-5" />
                    </div>
                    <div>
                      <span className="text-xs font-semibold text-blue-600 dark:text-blue-400 hover:underline">
                        Click to browse
                      </span>{' '}
                      <span className="text-xs text-slate-500">or drag and drop</span>
                    </div>
                    <p className="text-[11px] text-slate-400">
                      PDF, JPG, PNG, Excel, ZIP up to 25MB
                    </p>
                  </label>
                )}
              </div>

              <div className="mt-2 flex items-center gap-1.5 text-[11px] text-slate-500 dark:text-slate-400">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
                <span>Files are automatically scanned with ClamAV before delivery</span>
              </div>
            </div>
          )}

          {/* Mode 2: Client Vault Selection */}
          {sourceMode === 'VAULT' && (
            <div className="space-y-3">
              <div className="relative">
                <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                <input
                  type="text"
                  placeholder="Search clean documents in client vault..."
                  value={vaultSearch}
                  onChange={e => setVaultSearch(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              {loadingVault ? (
                <div className="py-6 text-center text-xs text-slate-500">Loading client vault documents...</div>
              ) : filteredVaultDocs.length === 0 ? (
                <div className="py-6 text-center text-xs text-slate-500 border border-dashed border-slate-200 dark:border-slate-800 rounded-xl">
                  No clean vault documents found for this client.
                </div>
              ) : (
                <div className="max-h-48 overflow-y-auto space-y-2 pr-1">
                  {filteredVaultDocs.map(doc => {
                    const isSelected = selectedVaultDocId === doc.id;
                    return (
                      <div
                        key={doc.id}
                        onClick={() => setSelectedVaultDocId(doc.id)}
                        className={`p-3 rounded-xl border cursor-pointer transition-all flex items-center justify-between ${
                          isSelected
                            ? 'border-blue-500 bg-blue-50/40 dark:bg-blue-950/30'
                            : 'border-slate-200 dark:border-slate-700/60 hover:border-slate-300 dark:hover:border-slate-600 bg-slate-50/50 dark:bg-slate-800/30'
                        }`}
                      >
                        <div className="flex items-center gap-2.5 min-w-0">
                          <FileText className="w-4 h-4 text-slate-400 shrink-0" />
                          <div className="truncate">
                            <p className="text-xs font-semibold text-slate-800 dark:text-slate-200 truncate">
                              {doc.fileName || doc.filename || doc.originalFilename}
                            </p>
                            <p className="text-[10px] text-slate-500">
                              {doc.documentType || doc.category || 'DOCUMENT'} • {((doc.fileSize || 0) / 1024).toFixed(1)} KB
                            </p>
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="inline-flex items-center gap-1 text-[10px] text-emerald-600 dark:text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full font-medium">
                            <ShieldCheck className="w-3 h-3" /> Clean
                          </span>
                          <input
                            type="radio"
                            name="vaultDoc"
                            checked={isSelected}
                            onChange={() => setSelectedVaultDocId(doc.id)}
                            className="text-blue-600 focus:ring-blue-500"
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}

          {/* Financial Year / Assessment Year / Tax Period */}
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
                Tax Period
              </label>
              <input
                type="text"
                placeholder="Q1, July 2026, Annual"
                value={taxPeriod}
                onChange={e => setTaxPeriod(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          {/* Optional Message */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Message to Client <span className="text-slate-400 font-normal">(Optional)</span>
            </label>
            <textarea
              rows={2}
              value={message}
              onChange={e => setMessage(e.target.value)}
              placeholder="e.g. Please find attached your verified ITR acknowledgement for AY 2026-27."
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
            {isSubmitting ? 'Delivering...' : fulfillingRequest ? 'Fulfill & Deliver' : 'Deliver Document'}
          </button>
        </div>
      </div>
    </div>
  );
};
