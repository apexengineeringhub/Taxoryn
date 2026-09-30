import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  FileText,
  Upload,
  Download,
  Archive,
  Loader2,
  AlertCircle,
  Plus,
  Search,
  FolderOpen,
  X,
  CheckCircle2,
} from 'lucide-react';
import { documentApi } from '../../api/endpoints';
import { DocumentItem } from '../../types';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatBytes(bytes: number): string {
  if (!bytes) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(iso?: string): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

const DOCTYPE_LABELS: Record<string, string> = {
  IDENTITY: 'Identity',
  TAX_DOCUMENT: 'Tax Doc',
  FINANCIAL: 'Financial',
  BANK_STATEMENT: 'Bank Stmt',
  INVOICE: 'Invoice',
  RETURN: 'Return',
  NOTICE: 'Notice',
  CERTIFICATE: 'Certificate',
  AGREEMENT: 'Agreement',
  SUPPORTING_DOCUMENT: 'Supporting',
  OTHER: 'Other',
};

const DOCTYPE_COLORS: Record<string, string> = {
  IDENTITY: 'bg-purple-50 text-purple-700 border-purple-200',
  TAX_DOCUMENT: 'bg-blue-50 text-blue-700 border-blue-200',
  FINANCIAL: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  BANK_STATEMENT: 'bg-cyan-50 text-cyan-700 border-cyan-200',
  INVOICE: 'bg-orange-50 text-orange-700 border-orange-200',
  RETURN: 'bg-violet-50 text-violet-700 border-violet-200',
  NOTICE: 'bg-red-50 text-red-700 border-red-200',
  CERTIFICATE: 'bg-teal-50 text-teal-700 border-teal-200',
  AGREEMENT: 'bg-amber-50 text-amber-700 border-amber-200',
  SUPPORTING_DOCUMENT: 'bg-slate-50 text-slate-700 border-slate-200',
  OTHER: 'bg-slate-50 text-slate-600 border-slate-200',
};

// ---------------------------------------------------------------------------
// Upload Dialog
// ---------------------------------------------------------------------------

interface UploadDialogProps {
  engagementId: string;
  clientId?: string;
  onClose: () => void;
  onUploaded: (doc: DocumentItem) => void;
}

const ALLOWED_TYPES = [
  'application/pdf',
  'image/png', 'image/jpeg', 'image/webp',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'text/csv', 'text/plain',
  'application/zip',
];

function UploadDialog({ engagementId, clientId, onClose, onUploaded }: UploadDialogProps) {
  const [file, setFile] = useState<File | null>(null);
  const [docType, setDocType] = useState('OTHER');
  const [notes, setNotes] = useState('');
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const dropRef = useRef<HTMLDivElement>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    const f = e.dataTransfer.files?.[0];
    if (f) validateAndSet(f);
  };

  const validateAndSet = (f: File) => {
    if (f.size > 25 * 1024 * 1024) { setError('File exceeds 25 MB limit.'); return; }
    if (!ALLOWED_TYPES.includes(f.type) && !f.name.match(/\.(pdf|png|jpg|jpeg|webp|doc|docx|xls|xlsx|csv|txt|zip)$/i)) {
      setError('File type not allowed.'); return;
    }
    setError('');
    setFile(f);
  };

  const handleSubmit = async () => {
    if (!file) return;
    setUploading(true);
    setError('');
    try {
      const uploaded = await documentApi.upload(file, {
        engagementId,
        clientId,
        documentType: docType,
        notes: notes || undefined,
      });
      if (uploaded) onUploaded(uploaded);
    } catch (err: any) {
      setError(err?.response?.data?.message || err?.message || 'Upload failed. Please retry.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md border border-slate-200">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <div className="flex items-center gap-2">
            <Upload className="w-4 h-4 text-brand-600" />
            <span className="text-sm font-bold text-slate-900">Upload Document</span>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700 transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Body */}
        <div className="p-5 space-y-4">
          {/* Drop zone */}
          <div
            ref={dropRef}
            onDragOver={(e) => e.preventDefault()}
            onDrop={handleDrop}
            onClick={() => fileRef.current?.click()}
            className={`cursor-pointer border-2 border-dashed rounded-xl p-6 text-center transition-colors ${
              file ? 'border-brand-400 bg-brand-50' : 'border-slate-200 hover:border-brand-300 bg-slate-50'
            }`}
          >
            <input
              ref={fileRef}
              type="file"
              className="hidden"
              accept=".pdf,.png,.jpg,.jpeg,.webp,.doc,.docx,.xls,.xlsx,.csv,.txt,.zip"
              onChange={(e) => { const f = e.target.files?.[0]; if (f) validateAndSet(f); }}
            />
            {file ? (
              <div className="flex items-center justify-center gap-2">
                <CheckCircle2 className="w-5 h-5 text-brand-600 flex-shrink-0" />
                <div className="text-left">
                  <p className="text-xs font-semibold text-slate-900 truncate max-w-[220px]">{file.name}</p>
                  <p className="text-[11px] text-slate-500">{formatBytes(file.size)}</p>
                </div>
              </div>
            ) : (
              <>
                <Upload className="w-6 h-6 text-slate-400 mx-auto mb-2" />
                <p className="text-xs font-medium text-slate-600">Drop file here or click to browse</p>
                <p className="text-[11px] text-slate-400 mt-1">PDF, Word, Excel, Images, ZIP · max 25 MB</p>
              </>
            )}
          </div>

          {/* Document type */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Document Type</label>
            <select
              value={docType}
              onChange={(e) => setDocType(e.target.value)}
              className="w-full border border-slate-200 rounded-xl text-xs px-3 py-2 text-slate-800 focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white"
            >
              {Object.entries(DOCTYPE_LABELS).map(([k, v]) => (
                <option key={k} value={k}>{v}</option>
              ))}
            </select>
          </div>

          {/* Notes */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Notes <span className="font-normal text-slate-400">(optional)</span></label>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={2}
              placeholder="e.g. April GST invoices, signed engagement letter…"
              className="w-full border border-slate-200 rounded-xl text-xs px-3 py-2 text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-500 resize-none"
            />
          </div>

          {error && (
            <div className="flex items-center gap-2 bg-red-50 border border-red-200 rounded-xl px-3 py-2">
              <AlertCircle className="w-3.5 h-3.5 text-red-500 flex-shrink-0" />
              <p className="text-xs text-red-700">{error}</p>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-2 px-5 py-4 border-t border-slate-100">
          <button
            onClick={onClose}
            disabled={uploading}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition-colors disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={!file || uploading}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold bg-brand-600 hover:bg-brand-700 text-white shadow-xs transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {uploading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Upload className="w-3.5 h-3.5" />}
            {uploading ? 'Uploading…' : 'Upload'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Main Component
// ---------------------------------------------------------------------------

interface EngagementDocumentVaultProps {
  engagementId: string;
  clientId?: string;
  className?: string;
}

export function EngagementDocumentVault({ engagementId, clientId, className = '' }: EngagementDocumentVaultProps) {
  const [docs, setDocs] = useState<DocumentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [showUpload, setShowUpload] = useState(false);
  const [archiving, setArchiving] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await documentApi.getByEngagementId(engagementId);
      setDocs(data ?? []);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load documents.');
    } finally {
      setLoading(false);
    }
  }, [engagementId]);

  useEffect(() => { load(); }, [load]);

  const handleArchive = async (id: string) => {
    setArchiving(id);
    try {
      await documentApi.archive(id);
      setDocs((prev) => prev.filter((d) => d.id !== id));
    } catch {
      // ignore — keep doc in list
    } finally {
      setArchiving(null);
    }
  };

  const filtered = docs.filter((d) => {
    if (!search.trim()) return true;
    const q = search.toLowerCase();
    return (
      (d.name || d.filename || d.fileName || '').toLowerCase().includes(q) ||
      (d.documentType || '').toLowerCase().includes(q) ||
      (d.uploadedByName || '').toLowerCase().includes(q)
    );
  });

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Toolbar */}
      <div className="flex items-center gap-3 flex-wrap">
        <div className="flex-1 min-w-[180px] relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400 pointer-events-none" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search documents…"
            className="w-full border border-slate-200 rounded-xl pl-8 pr-3 py-2 text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-500 bg-white"
          />
        </div>
        <button
          onClick={() => setShowUpload(true)}
          className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-brand-600 hover:bg-brand-700 text-white text-xs font-bold shadow-xs transition-colors"
        >
          <Plus className="w-3.5 h-3.5" />
          Upload
        </button>
        <button
          onClick={load}
          className="flex items-center gap-1.5 px-3 py-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50 text-xs font-semibold transition-colors"
        >
          Refresh
        </button>
      </div>

      {/* Content */}
      {loading ? (
        <div className="flex items-center justify-center py-16">
          <Loader2 className="w-6 h-6 animate-spin text-brand-500" />
        </div>
      ) : error ? (
        <div className="flex items-center gap-2 bg-red-50 border border-red-200 rounded-xl px-4 py-3">
          <AlertCircle className="w-4 h-4 text-red-500 flex-shrink-0" />
          <p className="text-xs text-red-700">{error}</p>
          <button onClick={load} className="ml-auto text-xs text-red-600 underline font-semibold">Retry</button>
        </div>
      ) : filtered.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-2xl p-10 text-center space-y-3">
          <FolderOpen className="w-10 h-10 text-slate-300 mx-auto" />
          <p className="text-sm font-semibold text-slate-600">
            {search ? 'No documents match your search.' : 'No documents uploaded yet.'}
          </p>
          {!search && (
            <button
              onClick={() => setShowUpload(true)}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-brand-600 text-white text-xs font-bold hover:bg-brand-700 transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
              Upload First Document
            </button>
          )}
        </div>
      ) : (
        <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden">
          <table className="w-full text-xs">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="text-left px-4 py-3 font-semibold text-slate-600">Name</th>
                <th className="text-left px-4 py-3 font-semibold text-slate-600 hidden sm:table-cell">Type</th>
                <th className="text-left px-4 py-3 font-semibold text-slate-600 hidden md:table-cell">Size</th>
                <th className="text-left px-4 py-3 font-semibold text-slate-600 hidden lg:table-cell">Uploaded By</th>
                <th className="text-left px-4 py-3 font-semibold text-slate-600 hidden lg:table-cell">Date</th>
                <th className="text-right px-4 py-3 font-semibold text-slate-600">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filtered.map((doc) => {
                const displayName = doc.name || doc.fileName || doc.filename || doc.originalFilename || 'Untitled';
                const dtype = doc.documentType || doc.category || 'OTHER';
                const colorClass = DOCTYPE_COLORS[dtype] || DOCTYPE_COLORS.OTHER;
                const label = DOCTYPE_LABELS[dtype] || dtype;
                const isArchiving = archiving === doc.id;

                return (
                  <tr key={doc.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <FileText className="w-4 h-4 text-slate-400 flex-shrink-0" />
                        <span className="font-medium text-slate-800 truncate max-w-[180px]" title={displayName}>
                          {displayName}
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-3 hidden sm:table-cell">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold border ${colorClass}`}>
                        {label}
                      </span>
                    </td>
                    <td className="px-4 py-3 hidden md:table-cell text-slate-500">
                      {doc.fileSizeFormatted || formatBytes(doc.fileSize)}
                    </td>
                    <td className="px-4 py-3 hidden lg:table-cell text-slate-500 truncate max-w-[120px]">
                      {doc.uploadedByName || doc.uploadedBy || '—'}
                    </td>
                    <td className="px-4 py-3 hidden lg:table-cell text-slate-500 whitespace-nowrap">
                      {formatDate(doc.uploadedAt || doc.createdAt)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <a
                          href={documentApi.downloadUrl(doc.id)}
                          target="_blank"
                          rel="noopener noreferrer"
                          title="Download"
                          className="p-1.5 rounded-lg text-slate-500 hover:bg-blue-50 hover:text-blue-600 transition-colors"
                        >
                          <Download className="w-3.5 h-3.5" />
                        </a>
                        <button
                          onClick={() => handleArchive(doc.id)}
                          disabled={isArchiving}
                          title="Archive"
                          className="p-1.5 rounded-lg text-slate-500 hover:bg-amber-50 hover:text-amber-600 transition-colors disabled:opacity-50"
                        >
                          {isArchiving ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Archive className="w-3.5 h-3.5" />}
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          {/* Footer count */}
          <div className="px-4 py-2 border-t border-slate-100 bg-slate-50 text-[11px] text-slate-500">
            {filtered.length} document{filtered.length !== 1 ? 's' : ''}
            {search && docs.length !== filtered.length && ` (${docs.length} total)`}
          </div>
        </div>
      )}

      {/* Upload dialog */}
      {showUpload && (
        <UploadDialog
          engagementId={engagementId}
          clientId={clientId}
          onClose={() => setShowUpload(false)}
          onUploaded={(doc) => {
            setDocs((prev) => [doc, ...prev]);
            setShowUpload(false);
          }}
        />
      )}
    </div>
  );
}

export default EngagementDocumentVault;
