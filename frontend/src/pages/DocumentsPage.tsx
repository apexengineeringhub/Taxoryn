import React, { useState, useEffect } from 'react';
import { FolderLock, Upload, Download, FileText, File, HardDrive } from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { documentApi } from '../api/endpoints';
import { DocumentItem } from '../types';
import clsx from 'clsx';

export const DocumentsPage: React.FC = () => {
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [category, setCategory] = useState('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadCategory, setUploadCategory] = useState('GST');
  const [isUploading, setIsUploading] = useState(false);

  useEffect(() => {
    loadDocuments();
  }, [category]);

  const loadDocuments = async () => {
    try {
      setIsLoading(true);
      const params: any = {};
      if (category !== 'ALL') params.category = category;
      const res = await documentApi.getAll(params);
      setDocuments(res.content || []);
    } catch (err) {
      console.error('Failed to load documents', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFile) return;

    try {
      setIsUploading(true);
      const formData = new FormData();
      formData.append('file', selectedFile);
      formData.append('category', uploadCategory);
      await documentApi.upload(formData);
      setIsUploadModalOpen(false);
      setSelectedFile(null);
      loadDocuments();
    } catch (err) {
      alert('Upload failed');
    } finally {
      setIsUploading(false);
    }
  };

  const formatFileSize = (bytes: number = 0) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  const columns: Column<DocumentItem>[] = [
    {
      header: 'Document Name',
      accessor: (row) => (
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center font-bold text-xs shrink-0">
            <FileText className="w-4 h-4" />
          </div>
          <div>
            <span className="font-bold text-slate-900 block">{row.originalFilename}</span>
            <span className="text-[10px] text-slate-400 block">{row.clientName || 'General Document'}</span>
          </div>
        </div>
      ),
    },
    {
      header: 'Category',
      accessor: (row) => (
        <span className="font-bold text-xs bg-slate-100 text-slate-700 px-2 py-0.5 rounded border border-slate-200 uppercase">
          {row.category}
        </span>
      ),
    },
    {
      header: 'Size',
      accessor: (row) => <span className="font-mono text-xs text-slate-600">{formatFileSize(row.fileSize)}</span>,
    },
    {
      header: 'Uploaded On',
      accessor: (row) => <span className="text-xs text-slate-500">{new Date(row.createdAt).toLocaleDateString()}</span>,
    },
    {
      header: 'Actions',
      align: 'right',
      cell: (row) => (
        <a
          href={documentApi.downloadUrl(row.id)}
          target="_blank"
          rel="noopener noreferrer"
          className="p-1.5 text-slate-500 hover:text-brand-600 hover:bg-slate-100 rounded-md inline-flex items-center gap-1 text-xs font-semibold"
        >
          <Download className="w-4 h-4" />
        </a>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">Document Vault</h1>
          <p className="text-xs text-slate-500 mt-1">
            Encrypted document storage with client folder hierarchy and statutory classification.
          </p>
        </div>
        <Button onClick={() => setIsUploadModalOpen(true)} leftIcon={<Upload className="w-4 h-4" />}>
          Upload Document
        </Button>
      </div>

      {/* Category Filter */}
      <div className="border-b border-slate-200 flex items-center gap-2">
        {['ALL', 'GST', 'ITR', 'FINANCIAL_STATEMENTS', 'KYC'].map((cat) => (
          <button
            key={cat}
            onClick={() => setCategory(cat)}
            className={clsx(
              'px-4 py-2.5 text-xs font-bold border-b-2 transition-all',
              category === cat
                ? 'border-brand-600 text-brand-600 bg-brand-50/50 rounded-t-lg'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            )}
          >
            {cat.replace('_', ' ')}
          </button>
        ))}
      </div>

      {/* Data Table */}
      <DataTable
        columns={columns}
        data={documents}
        isLoading={isLoading}
        searchPlaceholder="Search files by name..."
      />

      {/* Upload Modal */}
      <Modal isOpen={isUploadModalOpen} onClose={() => setIsUploadModalOpen(false)} title="Upload Practice Document" subtitle="Supported formats: PDF, XLSX, ZIP, PNG (Max 25MB)">
        <form onSubmit={handleUpload} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Select File *</label>
            <input
              type="file"
              required
              onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
              className="w-full text-xs file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-brand-50 file:text-brand-700 hover:file:bg-brand-100"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Category</label>
            <select
              value={uploadCategory}
              onChange={(e) => setUploadCategory(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg"
            >
              <option value="GST">GST Filings & Registers</option>
              <option value="ITR">ITR Computations & Form 16</option>
              <option value="FINANCIAL_STATEMENTS">Balance Sheet & P&L</option>
              <option value="KYC">KYC & Registration Certificates</option>
            </select>
          </div>

          <div className="pt-4 flex justify-end gap-2">
            <Button variant="outline" type="button" onClick={() => setIsUploadModalOpen(false)}>Cancel</Button>
            <Button type="submit" isLoading={isUploading}>Upload</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
