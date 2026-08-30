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
  Copy,
  AlertTriangle,
  FileDown,
  Info,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { StatusBadge } from '../components/common/StatusBadge';
import { clientApi } from '../api/endpoints';
import { useBranding } from '../context/BrandingContext';
import { useAuth } from '../context/AuthContext';
import { BulkImportResult, BulkImportError } from '../types';
import { parseSpreadsheetToRows } from '../utils/spreadsheetParser';
import * as XLSX from 'xlsx';
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
  notes?: string;
  isValid: boolean;
  isDuplicate: boolean;
  errors: string[];
}

export const ClientMigrationHubPage: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [parsedRows, setParsedRows] = useState<ParsedClientRow[]>([]);
  const [isParsing, setIsParsing] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const [importProgress, setImportProgress] = useState(0);
  const [importResult, setImportResult] = useState<BulkImportResult | null>(null);
  const [activeTab, setActiveTab] = useState<'ALL' | 'VALID' | 'DUPLICATES' | 'ERRORS'>('ALL');

  const fileInputRef = useRef<HTMLInputElement>(null);
  const { currentTheme } = useBranding();
  const { practiceName } = useAuth();
  const navigate = useNavigate();

  // Normalize Client Type string
  const normalizeClientType = (type: string = '', hasGstin: boolean = false): string => {
    const clean = type.trim().toUpperCase().replace(/[\s\-_]/g, '');
    if (clean.includes('PVT') || clean.includes('PRIVATELIMITED')) return 'PRIVATE_LIMITED';
    if (clean.includes('PUBL') || clean.includes('PUBLICLIMITED')) return 'PUBLIC_LIMITED';
    if (clean.includes('LLP')) return 'LLP';
    if (clean.includes('PROP') || clean.includes('PROPRIETOR')) return 'PROPRIETORSHIP';
    if (clean.includes('INDIV') || clean.includes('PERSON') || clean.includes('SALARIED')) return 'INDIVIDUAL';
    if (clean.includes('HUF')) return 'HUF';
    if (clean.includes('TRUST')) return 'TRUST';
    if (clean.includes('SOCIETY')) return 'SOCIETY';
    if (clean.includes('PARTNER')) return 'PARTNERSHIP';
    return hasGstin ? 'PRIVATE_LIMITED' : 'INDIVIDUAL';
  };

  // Normalize Indian mobile number
  const normalizePhone = (val: string = ''): string => {
    const digits = val.replace(/[^0-9]/g, '');
    if (digits.length === 12 && digits.startsWith('91')) return digits.substring(2);
    if (digits.length === 11 && digits.startsWith('0')) return digits.substring(1);
    return digits;
  };

  // Download Sample Template XLSX
  const handleDownloadExcelTemplate = () => {
    const sampleData = [
      {
        'Display Name': 'Zenith Infotech Pvt Ltd',
        'Legal Name': 'Zenith Infotech Private Limited',
        'Trade Name': 'Zenith Software',
        'PAN': 'AAACZ1234D',
        'GSTIN': '27AAACZ1234D1Z8',
        'Client Type': 'PRIVATE_LIMITED',
        'Contact Person': 'Ramesh Gupta',
        'Email': 'finance@zenithinfo.com',
        'Mobile': '9811122233',
        'City': 'Mumbai',
        'State': 'Maharashtra',
        'PIN Code': '400093',
        'Notes': 'Annual Audit & GST Retainer Client',
      },
      {
        'Display Name': 'Bluecrest Logistics LLP',
        'Legal Name': 'Bluecrest Logistics LLP',
        'Trade Name': 'Bluecrest Express',
        'PAN': 'AAALB5678E',
        'GSTIN': '27AAALB5678E1Z4',
        'Client Type': 'LLP',
        'Contact Person': 'Pooja Mehta',
        'Email': 'accounts@bluecrestlog.com',
        'Mobile': '9811144455',
        'City': 'Pune',
        'State': 'Maharashtra',
        'PIN Code': '411001',
        'Notes': 'Monthly GSTR-1 & 3B Filing',
      },
      {
        'Display Name': 'Anand Ramesh Joshi',
        'Legal Name': '',
        'Trade Name': '',
        'PAN': 'ABCPJ9876M',
        'GSTIN': '',
        'Client Type': 'INDIVIDUAL',
        'Contact Person': 'Anand Joshi',
        'Email': 'anand.joshi@gmail.com',
        'Mobile': '9811166677',
        'City': 'Nagpur',
        'State': 'Maharashtra',
        'PIN Code': '440001',
        'Notes': 'Salaried ITR-1 Filing',
      },
      {
        'Display Name': 'Mundeshwari Trading Co',
        'Legal Name': 'Mundeshwari Trading Proprietorship',
        'Trade Name': 'Mundeshwari Traders',
        'PAN': 'AABFM1122K',
        'GSTIN': '27AABFM1122K1Z3',
        'Client Type': 'PROPRIETORSHIP',
        'Contact Person': 'Manoj Kumar',
        'Email': 'contact@mundeshwari.in',
        'Mobile': '9822233344',
        'City': 'Mumbai',
        'State': 'Maharashtra',
        'PIN Code': '400001',
        'Notes': 'Composition Scheme Taxpayer',
      },
    ];

    const worksheet = XLSX.utils.json_to_sheet(sampleData);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Clients');

    worksheet['!cols'] = [
      { wch: 25 },
      { wch: 30 },
      { wch: 20 },
      { wch: 14 },
      { wch: 18 },
      { wch: 18 },
      { wch: 20 },
      { wch: 26 },
      { wch: 14 },
      { wch: 14 },
      { wch: 16 },
      { wch: 12 },
      { wch: 30 },
    ];

    XLSX.writeFile(workbook, 'Taxoryn_Client_Migration_Template.xlsx');
  };

  // Download Sample Template CSV
  const handleDownloadCsvTemplate = () => {
    const headers = 'Display Name,Legal Name,Trade Name,PAN,GSTIN,Client Type,Contact Person,Email,Mobile,City,State,PIN Code,Notes\n';
    const sampleRows = [
      '"Zenith Infotech Pvt Ltd","Zenith Infotech Private Limited","Zenith Software","AAACZ1234D","27AAACZ1234D1Z8","PRIVATE_LIMITED","Ramesh Gupta","finance@zenithinfo.com","9811122233","Mumbai","Maharashtra","400093","Annual Audit & GST Retainer Client"\n',
      '"Bluecrest Logistics LLP","Bluecrest Logistics LLP","Bluecrest Express","AAALB5678E","27AAALB5678E1Z4","LLP","Pooja Mehta","accounts@bluecrestlog.com","9811144455","Pune","Maharashtra","411001","Monthly GSTR-1 & 3B Filing"\n',
      '"Anand Ramesh Joshi","","","ABCPJ9876M","","INDIVIDUAL","Anand Joshi","anand.joshi@gmail.com","9811166677","Nagpur","Maharashtra","440001","Salaried ITR-1 Filing"\n',
      '"Mundeshwari Trading Co","Mundeshwari Trading Proprietorship","Mundeshwari Traders","AABFM1122K","27AABFM1122K1Z3","PROPRIETORSHIP","Manoj Kumar","contact@mundeshwari.in","9822233344","Mumbai","Maharashtra","400001","Composition Scheme Taxpayer"\n',
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

      const headerCols = rowsMatrix[0].map((h) => h.toLowerCase().replace(/[\s_]/g, ''));
      const rows: ParsedClientRow[] = [];
      const seenPans = new Map<string, number>();
      const seenGstins = new Map<string, number>();

      for (let i = 1; i < rowsMatrix.length; i++) {
        const rawCols = rowsMatrix[i];
        if (rawCols.length === 0 || rawCols.every((c) => c.trim() === '')) continue;

        const rowData: Record<string, string> = {};
        headerCols.forEach((h, idx) => {
          rowData[h] = rawCols[idx] || '';
        });

        const displayName = rowData['displayname'] || rowData['name'] || rowData['clientname'] || rawCols[0] || '';
        const legalName = rowData['legalname'] || rawCols[1] || '';
        const tradeName = rowData['tradename'] || '';
        const pan = (rowData['pan'] || rowData['pannumber'] || rawCols[2] || '').toUpperCase().trim();
        const gstin = (rowData['gstin'] || rowData['gstnumber'] || rawCols[3] || '').toUpperCase().trim();
        const clientType = normalizeClientType(rowData['clienttype'] || rowData['type'] || rawCols[4], !!gstin);
        const contactPerson = rowData['contactperson'] || rowData['contact'] || '';
        const email = (rowData['email'] || rowData['contactemail'] || rawCols[5] || '').toLowerCase().trim();
        const rawPhone = rowData['mobile'] || rowData['phone'] || rawCols[6] || '';
        const phone = normalizePhone(rawPhone);
        const city = rowData['city'] || rawCols[7] || '';
        const state = rowData['state'] || rawCols[8] || '';
        const rawPincode = rowData['pincode'] || rowData['pin'] || rawCols[9] || '';
        const pincode = rawPincode.replace(/[^0-9]/g, '');
        const notes = rowData['notes'] || rowData['remarks'] || '';

        const errors: string[] = [];
        let isDuplicate = false;

        // 1. Name Check
        if (!displayName) {
          errors.push('Missing Client / Business Name');
        }

        // 2. PAN Check
        if (!pan) {
          errors.push('Missing PAN Number');
        } else if (!/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/.test(pan)) {
          errors.push(`Invalid PAN: ${pan} (expected 5 letters, 4 digits, 1 letter)`);
        } else {
          if (seenPans.has(pan)) {
            isDuplicate = true;
            errors.push(`Duplicate PAN in file (same as Row ${seenPans.get(pan)})`);
          } else {
            seenPans.set(pan, i + 1);
          }
        }

        // 3. GSTIN Check (Optional, but validated if present)
        if (gstin) {
          if (!/^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/.test(gstin)) {
            errors.push(`Invalid GSTIN: ${gstin}`);
          } else if (pan && /^[A-Z]{5}[0-9]{4}[A-Z]{1}$/.test(pan)) {
            const gstinPan = gstin.substring(2, 12);
            if (gstinPan !== pan) {
              errors.push(`GSTIN PAN (${gstinPan}) does not match Client PAN (${pan})`);
            }
          }

          if (seenGstins.has(gstin)) {
            isDuplicate = true;
            errors.push(`Duplicate GSTIN in file (same as Row ${seenGstins.get(gstin)})`);
          } else {
            seenGstins.set(gstin, i + 1);
          }
        }

        // 4. Mobile Check
        if (phone && !/^[6-9][0-9]{9}$/.test(phone)) {
          errors.push(`Invalid Mobile: ${rawPhone} (expected 10-digit Indian mobile)`);
        }

        // 5. PIN Code Check
        if (pincode && !/^[1-9][0-9]{5}$/.test(pincode)) {
          errors.push(`Invalid PIN Code: ${rawPincode} (expected 6 digits)`);
        }

        rows.push({
          id: i + 1,
          displayName,
          legalName: legalName || undefined,
          tradeName: tradeName || displayName || undefined,
          pan,
          gstin: gstin || undefined,
          clientType,
          email: email || undefined,
          phone: phone || undefined,
          city: city || undefined,
          state: state || undefined,
          pincode: pincode || undefined,
          notes: notes || undefined,
          isValid: errors.length === 0,
          isDuplicate,
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

  // Execute Bulk Migration
  const handleExecuteMigration = async () => {
    const validRows = parsedRows.filter((r) => r.isValid);
    if (validRows.length === 0) {
      alert('No valid client records to import. Please review errors or fix duplicates.');
      return;
    }

    setIsImporting(true);
    setImportProgress(20);

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
      notes: r.notes,
      status: 'ACTIVE' as const,
    }));

    try {
      const result: BulkImportResult = await clientApi.bulkImport(payload);
      setImportProgress(100);
      setImportResult(result);
    } catch (bulkErr: any) {
      console.error('Batch import failed', bulkErr);
      alert('Failed to execute bulk import. Please check connection and try again.');
    } finally {
      setIsImporting(false);
    }
  };

  // Download Error Report CSV
  const handleDownloadErrorReport = () => {
    if (!importResult || !importResult.errors || importResult.errors.length === 0) {
      const previewErrors = parsedRows.filter((r) => !r.isValid);
      if (previewErrors.length === 0) {
        alert('No errors to export.');
        return;
      }

      const headers = 'Row Number,Client Name,PAN,GSTIN,Error Reason\n';
      const rows = previewErrors
        .map((r) => `"${r.id}","${r.displayName}","${r.pan || ''}","${r.gstin || ''}","${r.errors.join('; ')}"`)
        .join('\n');

      const blob = new Blob([headers + rows], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.setAttribute('href', url);
      link.setAttribute('download', 'Taxoryn_Client_Import_Errors.csv');
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      return;
    }

    const headers = 'Row Number,Client Name,PAN,Field,Invalid Value,Reason,Suggested Correction,Is Duplicate\n';
    const rows = importResult.errors
      .map(
        (e) =>
          `"${e.rowNumber}","${e.clientName || ''}","${e.pan || ''}","${e.field || ''}","${e.invalidValue || ''}","${
            e.reason || ''
          }","${e.suggestedCorrection || ''}","${e.duplicate ? 'Yes' : 'No'}"`
      )
      .join('\n');

    const blob = new Blob([headers + rows], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', 'Taxoryn_Client_Import_Error_Report.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const validCount = parsedRows.filter((r) => r.isValid).length;
  const duplicateCount = parsedRows.filter((r) => r.isDuplicate).length;
  const errorCount = parsedRows.filter((r) => !r.isValid && !r.isDuplicate).length;

  const displayedRows = parsedRows.filter((r) => {
    if (activeTab === 'VALID') return r.isValid;
    if (activeTab === 'DUPLICATES') return r.isDuplicate;
    if (activeTab === 'ERRORS') return !r.isValid && !r.isDuplicate;
    return true;
  });

  return (
    <div className="space-y-6 max-w-6xl mx-auto pb-12 animate-fade-in">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Link to="/clients" className="text-xs text-slate-400 hover:text-slate-600 font-semibold">
              Clients Directory
            </Link>
            <span className="text-xs text-slate-300">/</span>
            <span className="text-xs font-bold text-slate-700">Bulk Client Import & Migration</span>
          </div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900 mt-1">
            Client Data Migration Hub
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Onboard hundreds of business and individual client accounts into your practice in seconds.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            leftIcon={<FileSpreadsheet className="w-4 h-4 text-emerald-600" />}
            onClick={handleDownloadExcelTemplate}
            className="font-bold text-xs"
          >
            Download Excel (.xlsx)
          </Button>
          <Button
            variant="outline"
            size="sm"
            leftIcon={<Download className="w-3.5 h-3.5" />}
            onClick={handleDownloadCsvTemplate}
            className="font-bold text-xs"
          >
            Download CSV (.csv)
          </Button>
        </div>
      </div>

      {/* Step 1: Upload & Instructions Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Upload Dropzone */}
        <Card
          title="1. Upload Client Spreadsheet"
          subtitle="Supports .XLSX and .CSV files (Max 10,000 clients per batch)"
          className="lg:col-span-2 bg-white border border-slate-200"
        >
          <div
            onClick={() => fileInputRef.current?.click()}
            className={clsx(
              'border-2 border-dashed rounded-2xl p-8 flex flex-col items-center justify-center cursor-pointer transition-all text-center',
              file ? 'border-[#00D1A3] bg-[#E6FBF6]/30' : 'border-slate-300 hover:border-slate-400 hover:bg-slate-50/50'
            )}
          >
            <input
              type="file"
              ref={fileInputRef}
              accept=".csv, .xlsx, .xls, .txt"
              onChange={handleFileChange}
              className="hidden"
            />
            <div className="w-14 h-14 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center mb-3 shadow-xs">
              <UploadCloud className="w-7 h-7" />
            </div>

            {file ? (
              <div>
                <p className="font-bold text-sm text-slate-900">{file.name}</p>
                <p className="text-xs text-slate-500 mt-0.5">
                  {(file.size / 1024).toFixed(1)} KB • {parsedRows.length} client rows parsed
                </p>
                <span className="inline-block mt-3 text-xs font-bold text-emerald-600 underline">
                  Click to choose a different file
                </span>
              </div>
            ) : (
              <div>
                <p className="font-bold text-sm text-slate-800">
                  Drag and drop your client migration file here
                </p>
                <p className="text-xs text-slate-400 mt-1">or click to browse from your computer</p>
                <div className="flex items-center justify-center gap-2 mt-3">
                  <span className="inline-flex items-center gap-1 px-2.5 py-1 bg-slate-100 rounded-full text-[10px] font-semibold text-slate-600">
                    <FileSpreadsheet className="w-3.5 h-3.5 text-emerald-600" /> Excel (.xlsx)
                  </span>
                  <span className="inline-flex items-center gap-1 px-2.5 py-1 bg-slate-100 rounded-full text-[10px] font-semibold text-slate-600">
                    <Download className="w-3.5 h-3.5 text-blue-600" /> CSV (.csv)
                  </span>
                </div>
              </div>
            )}
          </div>
        </Card>

        {/* Practitioner Guidelines */}
        <Card
          title="Indian Tax Rules & Validation"
          subtitle="Automatic practice integrity checks"
          className="lg:col-span-1 bg-white border border-slate-200"
        >
          <div className="space-y-3 text-xs">
            <div className="flex items-start gap-2 text-slate-700">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <span><strong>PAN Validation:</strong> Verified with standard 10-char format.</span>
            </div>
            <div className="flex items-start gap-2 text-slate-700">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <span><strong>GSTIN Optionality:</strong> Non-GST individual clients imported without GSTIN.</span>
            </div>
            <div className="flex items-start gap-2 text-slate-700">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <span><strong>PAN-GSTIN Match:</strong> Verified that GSTIN matches client PAN.</span>
            </div>
            <div className="flex items-start gap-2 text-slate-700">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <span><strong>Duplicate Safety:</strong> In-file & practice duplicates safely skipped.</span>
            </div>
            <div className="p-3 rounded-xl bg-slate-50 border border-slate-200/80 text-[11px] text-slate-600 mt-3">
              💡 <strong>Tip:</strong> Download the sample Excel template above, paste your client records, and upload directly.
            </div>
          </div>
        </Card>
      </div>

      {/* Step 2: Pre-Import Live Preview & Verification */}
      {parsedRows.length > 0 && !importResult && (
        <Card
          title={
            <div className="flex flex-wrap items-center gap-3">
              <span className="text-sm font-bold text-slate-900">2. Pre-Import Verification & Quality Check</span>
              <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                {validCount} Ready to Import
              </span>
              {duplicateCount > 0 && (
                <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-amber-50 text-amber-700 border border-amber-200">
                  {duplicateCount} Duplicates
                </span>
              )}
              {errorCount > 0 && (
                <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-rose-50 text-rose-700 border border-rose-200">
                  {errorCount} Errors
                </span>
              )}
            </div>
          }
          subtitle="Review parsed rows before committing data to your practice repository"
          action={
            <div className="flex items-center gap-2.5">
              {(duplicateCount > 0 || errorCount > 0) && (
                <Button
                  variant="outline"
                  size="sm"
                  leftIcon={<FileDown className="w-3.5 h-3.5 text-rose-600" />}
                  onClick={handleDownloadErrorReport}
                  className="font-bold text-xs"
                >
                  Download Error List
                </Button>
              )}

              <Button
                onClick={handleExecuteMigration}
                isLoading={isImporting}
                disabled={validCount === 0}
                variant="primary"
                className="font-bold px-5 py-2"
                rightIcon={<ArrowRight className="w-4 h-4" />}
              >
                Import {validCount} Valid Clients
              </Button>
            </div>
          }
          noPadding
        >
          {/* Filter Tabs */}
          <div className="flex items-center gap-2 px-4 py-2.5 bg-slate-50 border-b border-slate-200 text-xs">
            <button
              onClick={() => setActiveTab('ALL')}
              className={clsx(
                'px-3 py-1.5 rounded-lg font-bold transition-all',
                activeTab === 'ALL'
                  ? 'bg-white text-slate-900 shadow-2xs border border-slate-200'
                  : 'text-slate-500 hover:text-slate-800'
              )}
            >
              All Rows ({parsedRows.length})
            </button>
            <button
              onClick={() => setActiveTab('VALID')}
              className={clsx(
                'px-3 py-1.5 rounded-lg font-bold transition-all',
                activeTab === 'VALID'
                  ? 'bg-emerald-600 text-white shadow-2xs'
                  : 'text-emerald-700 hover:bg-emerald-50'
              )}
            >
              Ready ({validCount})
            </button>
            {duplicateCount > 0 && (
              <button
                onClick={() => setActiveTab('DUPLICATES')}
                className={clsx(
                  'px-3 py-1.5 rounded-lg font-bold transition-all',
                  activeTab === 'DUPLICATES'
                    ? 'bg-amber-600 text-white shadow-2xs'
                    : 'text-amber-700 hover:bg-amber-50'
                )}
              >
                Duplicates ({duplicateCount})
              </button>
            )}
            {errorCount > 0 && (
              <button
                onClick={() => setActiveTab('ERRORS')}
                className={clsx(
                  'px-3 py-1.5 rounded-lg font-bold transition-all',
                  activeTab === 'ERRORS'
                    ? 'bg-rose-600 text-white shadow-2xs'
                    : 'text-rose-700 hover:bg-rose-50'
                )}
              >
                Validation Errors ({errorCount})
              </button>
            )}
          </div>

          {isImporting && (
            <div className="p-6 bg-slate-50 border-b border-slate-200 flex flex-col items-center justify-center space-y-2">
              <p className="text-xs font-bold text-slate-800">
                Importing clients into {practiceName}...
              </p>
              <div className="w-full max-w-md h-2 bg-slate-200 rounded-full overflow-hidden">
                <div
                  className="h-full bg-[#00D1A3] transition-all duration-300"
                  style={{ width: `${importProgress}%` }}
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
                  <th className="px-4 py-3">Contact Details</th>
                  <th className="px-4 py-3">Verification Findings</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {displayedRows.map((row) => (
                  <tr
                    key={row.id}
                    className={clsx(
                      'transition-colors',
                      row.isDuplicate
                        ? 'bg-amber-50/40 hover:bg-amber-50/70'
                        : !row.isValid
                        ? 'bg-rose-50/40 hover:bg-rose-50/70'
                        : 'hover:bg-slate-50/70'
                    )}
                  >
                    <td className="px-4 py-3 font-mono text-slate-400 font-bold">{row.id}</td>
                    <td className="px-4 py-3">
                      {row.isValid ? (
                        <span className="inline-flex items-center gap-1 text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                          <CheckCircle2 className="w-3 h-3" /> Ready
                        </span>
                      ) : row.isDuplicate ? (
                        <span className="inline-flex items-center gap-1 text-[10px] font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
                          <Copy className="w-3 h-3" /> Duplicate
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
                      {row.phone && <span className="block text-[10px] text-slate-500 font-mono">{row.phone}</span>}
                      {row.city && <span className="block text-[10px] text-slate-400">{row.city}, {row.state}</span>}
                    </td>
                    <td className="px-4 py-3">
                      {row.errors.length > 0 ? (
                        <span className={clsx('font-semibold text-[11px]', row.isDuplicate ? 'text-amber-700' : 'text-rose-600')}>
                          {row.errors.join('; ')}
                        </span>
                      ) : (
                        <span className="text-emerald-600 font-semibold text-[11px]">✓ Valid for onboarding</span>
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
          title="Client Import Summary Report"
          subtitle={`Batch executed successfully for ${practiceName}`}
          className="bg-white border border-slate-200"
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
                <span className="text-xs font-bold text-rose-700 uppercase">Failed Validation</span>
                <p className="text-3xl font-black text-rose-900 mt-1">{importResult.totalFailed}</p>
              </div>
            </div>

            {/* Error logs if any */}
            {importResult.errors?.length > 0 && (
              <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-3">
                <div className="flex items-center justify-between">
                  <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                    Skipped & Error Logs ({importResult.errors.length})
                  </h4>
                  <Button
                    variant="outline"
                    size="sm"
                    leftIcon={<Download className="w-3.5 h-3.5 text-rose-600" />}
                    onClick={handleDownloadErrorReport}
                    className="font-bold text-xs"
                  >
                    Download Error Report (CSV)
                  </Button>
                </div>
                <div className="max-h-56 overflow-y-auto space-y-2 text-xs text-slate-600">
                  {importResult.errors.map((err, idx) => (
                    <div key={idx} className="p-2.5 bg-white rounded-lg border border-slate-200 space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-slate-900">
                          Row {err.rowNumber}: {err.clientName} (PAN: {err.pan})
                        </span>
                        <span className={clsx('px-2 py-0.5 rounded text-[10px] font-bold', err.duplicate ? 'bg-amber-100 text-amber-800' : 'bg-rose-100 text-rose-800')}>
                          {err.duplicate ? 'Duplicate Skipped' : 'Validation Failed'}
                        </span>
                      </div>
                      <p className="text-rose-600 font-semibold">{err.reason}</p>
                      {err.suggestedCorrection && (
                        <p className="text-slate-500 text-[11px]">
                          <strong>Correction:</strong> {err.suggestedCorrection}
                        </p>
                      )}
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
                variant="primary"
                onClick={() => navigate('/clients')}
                rightIcon={<ArrowRight className="w-4 h-4" />}
                className="font-bold"
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
