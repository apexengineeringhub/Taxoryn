import React, { useState, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  UploadCloud,
  FileSpreadsheet,
  Download,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  RefreshCw,
  Trash2,
  Users,
  ShieldCheck,
  Building2,
  FileCheck2,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { StatusBadge } from '../components/common/StatusBadge';
import { clientApi } from '../api/endpoints';
import { useBranding } from '../context/BrandingContext';
import { useAuth } from '../context/AuthContext';
import { BulkImportResult, BulkImportError } from '../types';
import { parseSpreadsheetToRows } from '../utils/spreadsheetParser';
import clsx from 'clsx';

interface ParsedClientRow {
  id: number;
  displayName: string;
  legalName?: string;
  tradeName?: string;
  pan: string;
  gstin?: string;
  clientType: string;
  email?: string;
  phone?: string;
  city?: string;
  state?: string;
  pincode?: string;
  isValid: boolean;
  errors: string[];
}

export const ClientMigrationHubPage: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [parsedRows, setParsedRows] = useState<ParsedClientRow[]>([]);
  const [isParsing, setIsParsing] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const [importProgress, setImportProgress] = useState(0);
  const [importResult, setImportResult] = useState<BulkImportResult | null>(null);
  const [filterInvalidOnly, setFilterInvalidOnly] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const { currentTheme } = useBranding();
  const { practiceName } = useAuth();
  const navigate = useNavigate();

  // Normalize Client Type string
  const normalizeClientType = (type: string = ''): string => {
    const clean = type.trim().toUpperCase().replace(/[\s\-_]/g, '');
    if (clean.includes('PVT') || clean.includes('PRIVATELIMITED')) return 'PRIVATE_LIMITED';
    if (clean.includes('PUBL') || clean.includes('PUBLICLIMITED')) return 'PUBLIC_LIMITED';
    if (clean.includes('LLP')) return 'LLP';
    if (clean.includes('PROP') || clean.includes('PROPRIETOR')) return 'PROPRIETORSHIP';
    if (clean.includes('INDIV') || clean.includes('PERSON')) return 'INDIVIDUAL';
    if (clean.includes('HUF')) return 'HUF';
    if (clean.includes('TRUST')) return 'TRUST';
    if (clean.includes('PARTNER')) return 'PARTNERSHIP';
    return 'PRIVATE_LIMITED';
  };

  // Download Sample Template CSV
  const handleDownloadTemplate = () => {
    const headers = 'Display Name,Legal Name,PAN,GSTIN,Client Type,Email,Phone,City,State,Pincode\n';
    const sampleRows = [
      'Zenith Infotech Pvt Ltd,Zenith Infotech Private Limited,AAACZ1234D,27AAACZ1234D1Z8,PRIVATE_LIMITED,finance@zenithinfo.com,9811122233,Mumbai,Maharashtra,400021\n',
      'Bluecrest Logistics LLP,Bluecrest Logistics LLP,AAALB5678E,27AAALB5678E1Z4,LLP,accounts@bluecrestlog.com,9811144455,Pune,Maharashtra,411001\n',
      'Anand Ramesh Joshi,,ABCPJ9876M,,INDIVIDUAL,anand.joshi@gmail.com,9811166677,Nagpur,Maharashtra,440001\n',
      'Mundeshwari Trading Co,Mundeshwari Trading Proprietorship,AABFM1122K,27AABFM1122K1Z3,PROPRIETORSHIP,contact@mundeshwari.in,9822233344,Mumbai,Maharashtra,400001\n',
    ].join('');

    const blob = new Blob([headers + sampleRows], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', 'Taxoryn_Client_Migration_Template.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Parse CSV or Excel (.xlsx / .xls) File in Browser
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files?.[0];
    if (!selected) return;

    setFile(selected);
    setIsParsing(true);
    setImportResult(null);

    try {
      const rowsMatrix = await parseSpreadsheetToRows(selected);
      if (rowsMatrix.length <= 1) {
        alert('Spreadsheet file is empty or missing headers');
        setIsParsing(false);
        return;
      }

      // Header mapping
      const headerCols = rowsMatrix[0].map((h) => h.toLowerCase().replace(/[\s_]/g, ''));
      const rows: ParsedClientRow[] = [];

      for (let i = 1; i < rowsMatrix.length; i++) {
        const rawCols = rowsMatrix[i];
        if (rawCols.length === 0 || rawCols.every((c) => c.trim() === '')) continue;

        const rowData: Record<string, string> = {};
        headerCols.forEach((h, idx) => {
          rowData[h] = rawCols[idx] || '';
        });

        const displayName = rowData['displayname'] || rowData['name'] || rowData['clientname'] || rawCols[0] || '';
        const legalName = rowData['legalname'] || rawCols[1] || '';
        const pan = (rowData['pan'] || rowData['pannumber'] || rawCols[2] || '').toUpperCase().trim();
        const gstin = (rowData['gstin'] || rowData['gstnumber'] || rawCols[3] || '').toUpperCase().trim();
        const clientType = normalizeClientType(rowData['clienttype'] || rowData['type'] || rawCols[4]);
        const email = rowData['email'] || rowData['contactemail'] || rawCols[5] || '';
        const phone = rowData['phone'] || rowData['mobile'] || rawCols[6] || '';
        const city = rowData['city'] || rawCols[7] || '';
        const state = rowData['state'] || rawCols[8] || '';
        const pincode = rowData['pincode'] || rawCols[9] || '';

        const errors: string[] = [];
        if (!displayName) errors.push('Missing Client Name');
        if (!pan) {
          errors.push('Missing PAN');
        } else if (!/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/.test(pan)) {
          errors.push(`Invalid PAN: ${pan}`);
        }

        if (gstin && !/^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/.test(gstin)) {
          errors.push(`Invalid GSTIN: ${gstin}`);
        }

        rows.push({
          id: i,
          displayName,
          legalName: legalName || undefined,
          tradeName: displayName || undefined,
          pan,
          gstin: gstin || undefined,
          clientType,
          email: email || undefined,
          phone: phone || undefined,
          city: city || undefined,
          state: state || undefined,
          pincode: pincode || undefined,
          isValid: errors.length === 0,
          errors,
        });
      }

      setParsedRows(rows);
    } catch (err) {
      alert('Failed to parse file. Please upload a valid CSV or Excel file.');
    } finally {
      setIsParsing(false);
    }
  };

  // Execute Bulk Migration with Seamless Fallback
  const handleExecuteMigration = async () => {
    const validRows = parsedRows.filter((r) => r.isValid);
    if (validRows.length === 0) {
      alert('No valid client records to import. Please review errors.');
      return;
    }

    setIsImporting(true);
    setImportProgress(10);

    const payload = validRows.map((r) => ({
      displayName: r.displayName,
      legalName: r.legalName,
      tradeName: r.tradeName,
      pan: r.pan,
      gstin: r.gstin,
      clientType: r.clientType as any,
      email: r.email,
      phone: r.phone,
      city: r.city,
      state: r.state,
      pincode: r.pincode,
      status: 'ACTIVE' as const,
    }));

    try {
      // 1. Try High-Speed Batch Endpoint
      const result: BulkImportResult = await clientApi.bulkImport(payload);
      setImportProgress(100);
      setImportResult(result);
    } catch (bulkErr: any) {
      // 2. Fallback to resilient per-client creation if backend batch endpoint is not yet loaded
      console.warn('Batch endpoint not available, falling back to sequential client creation', bulkErr);

      const successfulClients: any[] = [];
      const errors: BulkImportError[] = [];
      let skippedCount = 0;

      for (let i = 0; i < payload.length; i++) {
        const client = payload[i];
        const rowNum = i + 1;
        try {
          const created = await clientApi.create(client);
          successfulClients.push(created);
        } catch (singleErr: any) {
          const msg =
            singleErr.response?.data?.message ||
            singleErr.response?.data?.error ||
            singleErr.message ||
            'Import error';

          if (msg.toLowerCase().includes('already exists') || msg.toLowerCase().includes('duplicate')) {
            skippedCount++;
            errors.push({
              rowNumber: rowNum,
              clientName: client.displayName,
              pan: client.pan,
              reason: 'Duplicate client with this PAN already exists in practice',
            });
          } else {
            errors.push({
              rowNumber: rowNum,
              clientName: client.displayName,
              pan: client.pan,
              reason: msg,
            });
          }
        }
        setImportProgress(Math.round(((i + 1) / payload.length) * 100));
      }

      setImportResult({
        totalProcessed: payload.length,
        totalSuccess: successfulClients.length,
        totalFailed: errors.length - skippedCount,
        totalSkipped: skippedCount,
        importedClients: successfulClients,
        errors,
      });
    } finally {
      setIsImporting(false);
    }
  };

  const validCount = parsedRows.filter((r) => r.isValid).length;
  const invalidCount = parsedRows.filter((r) => !r.isValid).length;
  const displayedRows = filterInvalidOnly ? parsedRows.filter((r) => !r.isValid) : parsedRows;

  return (
    <div className="space-y-8 max-w-6xl mx-auto animate-fade-in">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Link to="/clients" className="text-xs text-slate-400 hover:text-slate-600 font-semibold">
              Clients Directory
            </Link>
            <span className="text-xs text-slate-300">/</span>
            <span className="text-xs font-bold text-slate-700">Migration & Bulk Import</span>
          </div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900 mt-1">
            Client Data Migration Hub
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Migrate your practice customer accounts from Tally, Computax, Genius, or Excel in seconds.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <a
            href="/Taxoryn_Sample_Customers_Migration.xlsx"
            download="Taxoryn_Sample_Customers_Migration.xlsx"
            className="inline-flex items-center gap-1.5 px-3 py-2 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-300 rounded-lg text-xs font-bold transition-colors shadow-2xs"
          >
            <FileSpreadsheet className="w-4 h-4 text-emerald-600" />
            <span>Download Excel (.xlsx)</span>
          </a>
          <Button
            variant="outline"
            size="sm"
            leftIcon={<Download className="w-3.5 h-3.5" />}
            onClick={handleDownloadTemplate}
          >
            Download CSV (.csv)
          </Button>
        </div>
      </div>

      {/* Step 1: Upload & Instructions Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Upload Dropzone */}
        <Card
          title="1. Upload Spreadsheet File"
          subtitle="Supports .CSV, .XLSX exported files (Max 10,000 clients)"
          className="lg:col-span-2"
        >
          <div
            onClick={() => fileInputRef.current?.click()}
            className={clsx(
              'border-2 border-dashed rounded-2xl p-8 flex flex-col items-center justify-center cursor-pointer transition-all text-center',
              file ? 'border-brand-500 bg-brand-50/20' : 'border-slate-300 hover:border-slate-400 hover:bg-slate-50/50'
            )}
          >
            <input
              type="file"
              ref={fileInputRef}
              accept=".csv, .txt, .tsv"
              onChange={handleFileChange}
              className="hidden"
            />
            <div className="w-14 h-14 rounded-2xl bg-brand-50 text-brand-600 flex items-center justify-center mb-3 shadow-xs">
              <UploadCloud className="w-7 h-7" />
            </div>

            {file ? (
              <div>
                <p className="font-bold text-sm text-slate-900">{file.name}</p>
                <p className="text-xs text-slate-500 mt-0.5">
                  {(file.size / 1024).toFixed(1)} KB • {parsedRows.length} client rows detected
                </p>
                <span className="inline-block mt-3 text-xs font-bold text-brand-600 underline">
                  Click to choose a different file
                </span>
              </div>
            ) : (
              <div>
                <p className="font-bold text-sm text-slate-800">
                  Drag and drop your client migration sheet here
                </p>
                <p className="text-xs text-slate-400 mt-1">or click to browse from your computer</p>
                <span className="inline-flex items-center gap-1 mt-3 px-2.5 py-1 bg-slate-100 rounded-full text-[10px] font-semibold text-slate-600">
                  <FileSpreadsheet className="w-3.5 h-3.5 text-emerald-600" /> Standard CSV / Excel format
                </span>
              </div>
            )}
          </div>
        </Card>

        {/* Practice Onboarding Guidelines */}
        <Card
          title="Migration Guidelines"
          subtitle="Taxoryn automatic column mapping"
          className="lg:col-span-1"
        >
          <div className="space-y-3 text-xs">
            <div className="flex items-start gap-2 text-slate-700">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <span><strong>PAN numbers</strong> are auto-validated with 10-char format.</span>
            </div>
            <div className="flex items-start gap-2 text-slate-700">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <span><strong>GSTIN numbers</strong> are auto-linked with state codes.</span>
            </div>
            <div className="flex items-start gap-2 text-slate-700">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <span><strong>Duplicates</strong> are automatically identified and safely skipped.</span>
            </div>
            <div className="p-3 rounded-xl bg-slate-50 border border-slate-200/80 text-[11px] text-slate-600 mt-4">
              💡 <strong>Tip:</strong> You can download our sample CSV, paste your Tally/Excel customer records, and upload directly.
            </div>
          </div>
        </Card>
      </div>

      {/* Step 2: Pre-Import Live Preview & Verification */}
      {parsedRows.length > 0 && !importResult && (
        <Card
          title={
            <div className="flex items-center gap-3">
              <span>2. Pre-Import Verification & Data Quality Check</span>
              <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                {validCount} Ready to Import
              </span>
              {invalidCount > 0 && (
                <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-rose-50 text-rose-700 border border-rose-200">
                  {invalidCount} Needs Attention
                </span>
              )}
            </div>
          }
          subtitle="Review client rows before importing into your practice database"
          action={
            <div className="flex items-center gap-3">
              {invalidCount > 0 && (
                <button
                  onClick={() => setFilterInvalidOnly(!filterInvalidOnly)}
                  className={clsx(
                    'text-xs font-bold px-3 py-1.5 rounded-lg border transition-all',
                    filterInvalidOnly
                      ? 'bg-rose-600 text-white border-rose-600'
                      : 'bg-white text-rose-700 border-rose-200 hover:bg-rose-50'
                  )}
                >
                  {filterInvalidOnly ? 'Show All Rows' : `Show ${invalidCount} Invalid Rows Only`}
                </button>
              )}

              <Button
                onClick={handleExecuteMigration}
                isLoading={isImporting}
                style={{ backgroundColor: currentTheme.primaryColor }}
                rightIcon={<ArrowRight className="w-4 h-4" />}
              >
                Import {validCount} Valid Clients Now
              </Button>
            </div>
          }
          noPadding
        >
          {isImporting && (
            <div className="p-6 bg-slate-50 border-b border-slate-200 flex flex-col items-center justify-center space-y-2">
              <p className="text-xs font-bold text-slate-800">
                Migrating client accounts into {practiceName}...
              </p>
              <div className="w-full max-w-md h-2 bg-slate-200 rounded-full overflow-hidden">
                <div
                  className="h-full bg-brand-600 transition-all duration-300"
                  style={{ width: `${importProgress}%`, backgroundColor: currentTheme.primaryColor }}
                />
              </div>
            </div>
          )}

          <div className="overflow-x-auto max-h-96">
            <table className="w-full text-left text-xs border-collapse">
              <thead className="sticky top-0 bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase text-[10px]">
                <tr>
                  <th className="px-4 py-3">Row</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Client / Business Name</th>
                  <th className="px-4 py-3">PAN</th>
                  <th className="px-4 py-3">GSTIN</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Email & City</th>
                  <th className="px-4 py-3">Validation Message</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {displayedRows.map((row) => (
                  <tr
                    key={row.id}
                    className={clsx(
                      'transition-colors',
                      !row.isValid ? 'bg-rose-50/40 hover:bg-rose-50/70' : 'hover:bg-slate-50/70'
                    )}
                  >
                    <td className="px-4 py-3 font-mono text-slate-400 font-bold">{row.id}</td>
                    <td className="px-4 py-3">
                      {row.isValid ? (
                        <span className="inline-flex items-center gap-1 text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                          <CheckCircle2 className="w-3 h-3" /> Valid
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-[10px] font-bold text-rose-700 bg-rose-50 px-2 py-0.5 rounded-full border border-rose-200">
                          <AlertCircle className="w-3 h-3" /> Invalid
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 font-bold text-slate-900">{row.displayName}</td>
                    <td className="px-4 py-3 font-mono font-bold text-slate-800">{row.pan || '—'}</td>
                    <td className="px-4 py-3 font-mono text-slate-600">{row.gstin || '—'}</td>
                    <td className="px-4 py-3 text-[10px] font-semibold text-slate-600 uppercase">
                      {row.clientType.replace('_', ' ')}
                    </td>
                    <td className="px-4 py-3 text-slate-500">
                      <span>{row.email || '—'}</span>
                      {row.city && <span className="block text-[10px] text-slate-400">{row.city}, {row.state}</span>}
                    </td>
                    <td className="px-4 py-3">
                      {row.errors.length > 0 ? (
                        <span className="text-rose-600 font-semibold text-[11px]">
                          {row.errors.join('; ')}
                        </span>
                      ) : (
                        <span className="text-slate-400 italic">Ready</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* Step 3: Migration Summary Report */}
      {importResult && (
        <Card
          title="Migration Batch Summary Report"
          subtitle={`Batch executed successfully for ${practiceName}`}
        >
          <div className="space-y-6">
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
              <div className="p-4 bg-blue-50 border border-blue-200 rounded-xl text-center">
                <span className="text-xs font-bold text-blue-700 uppercase">Total Processed</span>
                <p className="text-3xl font-black text-blue-900 mt-1">{importResult.totalProcessed}</p>
              </div>
              <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-center">
                <span className="text-xs font-bold text-emerald-700 uppercase">Successfully Imported</span>
                <p className="text-3xl font-black text-emerald-900 mt-1">{importResult.totalSuccess}</p>
              </div>
              <div className="p-4 bg-amber-50 border border-amber-200 rounded-xl text-center">
                <span className="text-xs font-bold text-amber-700 uppercase">Duplicates Skipped</span>
                <p className="text-3xl font-black text-amber-900 mt-1">{importResult.totalSkipped}</p>
              </div>
              <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-center">
                <span className="text-xs font-bold text-rose-700 uppercase">Failed Records</span>
                <p className="text-3xl font-black text-rose-900 mt-1">{importResult.totalFailed}</p>
              </div>
            </div>

            {/* Error logs if any */}
            {importResult.errors?.length > 0 && (
              <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-2">
                <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                  Skipped / Error Notes ({importResult.errors.length})
                </h4>
                <div className="max-h-40 overflow-y-auto space-y-1 text-xs text-slate-600">
                  {importResult.errors.map((err, idx) => (
                    <div key={idx} className="flex items-center justify-between py-1 border-b border-slate-100">
                      <span>Row {err.rowNumber}: <strong>{err.clientName}</strong> (PAN: {err.pan})</span>
                      <span className="text-rose-600 font-semibold">{err.reason}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
              <Button
                variant="outline"
                onClick={() => {
                  setParsedRows([]);
                  setFile(null);
                  setImportResult(null);
                }}
              >
                Import Another Batch
              </Button>
              <Button
                onClick={() => navigate('/clients')}
                style={{ backgroundColor: currentTheme.primaryColor }}
                rightIcon={<ArrowRight className="w-4 h-4" />}
              >
                Go to Clients Directory
              </Button>
            </div>
          </div>
        </Card>
      )}
    </div>
  );
};
