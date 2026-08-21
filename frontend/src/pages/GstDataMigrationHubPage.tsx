import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  FileText,
  UploadCloud,
  FileSpreadsheet,
  Download,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  ShieldCheck,
  Building2,
  Calendar,
  Layers,
  Sparkles,
  Search,
  CheckSquare,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { StatusBadge } from '../components/common/StatusBadge';
import { clientApi, gstApi } from '../api/endpoints';
import { useBranding } from '../context/BrandingContext';
import { useAuth } from '../context/AuthContext';
import { Client, GstProfile, GstReturnFiling, BulkGstImportResult } from '../types';
import { parseSpreadsheetToRows } from '../utils/spreadsheetParser';
import clsx from 'clsx';

interface ParsedGstProfileRow {
  id: number;
  pan: string;
  gstin: string;
  legalName: string;
  tradeName: string;
  gstScheme: 'REGULAR' | 'COMPOSITION' | 'QRMP' | 'CASUAL';
  filingFrequency: 'MONTHLY' | 'QUARTERLY';
  registrationDate: string;
  stateCode: string;
  matchedClient: Client | null;
  willAutoOnboard: boolean;
  isValid: boolean;
  validationError?: string;
}

interface ParsedGstFilingRow {
  id: number;
  gstin: string;
  returnType: 'GSTR1' | 'GSTR3B' | 'CMP08' | 'GSTR9';
  returnPeriod: string;
  financialYear: string;
  dueDate: string;
  filingStatus: 'FILED' | 'PREPARED' | 'PENDING' | 'OVERDUE';
  taxableValue: number;
  taxLiability: number;
  itcClaimed: number;
  arnNumber?: string;
  matchedProfile: GstProfile | null;
  isValid: boolean;
  validationError?: string;
}

export const GstDataMigrationHubPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'PROFILES' | 'FILINGS'>('PROFILES');
  const [clients, setClients] = useState<Client[]>([]);
  const [gstProfiles, setGstProfiles] = useState<GstProfile[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [progress, setProgress] = useState(0);
  const [result, setResult] = useState<BulkGstImportResult | null>(null);

  // Profile Upload State
  const [profileFile, setProfileFile] = useState<File | null>(null);
  const [parsedProfiles, setParsedProfiles] = useState<ParsedGstProfileRow[]>([]);
  const profileInputRef = useRef<HTMLInputElement>(null);

  // Filing Upload State
  const [filingFile, setFilingFile] = useState<File | null>(null);
  const [parsedFilings, setParsedFilings] = useState<ParsedGstFilingRow[]>([]);
  const filingInputRef = useRef<HTMLInputElement>(null);

  const { currentTheme } = useBranding();
  const { practiceName } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    loadPrerequisites();
  }, []);

  const loadPrerequisites = async () => {
    try {
      setIsLoading(true);
      const [clientRes, profileRes] = await Promise.all([
        clientApi.getAll({ size: 300 }),
        gstApi.getProfiles().catch(() => ({ content: [] })),
      ]);
      setClients(clientRes.content || []);
      setGstProfiles(profileRes.content || []);
    } catch (err) {
      console.error('Failed to load clients and GST profiles', err);
    } finally {
      setIsLoading(false);
    }
  };

  // 1. Parse GST Profiles CSV / Excel (.xlsx / .xls / .csv)
  const handleProfileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setProfileFile(file);

    try {
      const rowsMatrix = await parseSpreadsheetToRows(file);
      if (rowsMatrix.length <= 1) {
        alert('File is empty or missing headers');
        return;
      }

      const headerCols = rowsMatrix[0].map((h) => h.toLowerCase().replace(/[^a-z0-9]/g, ''));
      const panIdx = headerCols.findIndex((h) => h.includes('pan'));
      const gstinIdx = headerCols.findIndex((h) => h.includes('gstin') || h.includes('gst'));
      const legalIdx = headerCols.findIndex((h) => h.includes('legal') || h.includes('company') || h.includes('name'));
      const tradeIdx = headerCols.findIndex((h) => h.includes('trade') || h.includes('brand'));
      const schemeIdx = headerCols.findIndex((h) => h.includes('scheme') || h.includes('type'));
      const freqIdx = headerCols.findIndex((h) => h.includes('freq') || h.includes('period'));
      const regIdx = headerCols.findIndex((h) => h.includes('reg') || h.includes('date'));
      const stateIdx = headerCols.findIndex((h) => h.includes('state') || h.includes('code'));

      const rows: ParsedGstProfileRow[] = [];
      for (let i = 1; i < rowsMatrix.length; i++) {
        const cols = rowsMatrix[i];
        if (cols.length < 2 || cols.every((c) => c.trim() === '')) continue;

        const rawGstin = (gstinIdx >= 0 ? cols[gstinIdx] : cols[1]) || '';
        const gstin = rawGstin.replace(/[^A-Z0-9]/gi, '').toUpperCase().trim();

        const rawPan = (panIdx >= 0 ? cols[panIdx] : cols[0]) || (gstin.length >= 12 ? gstin.substring(2, 12) : '');
        const pan = rawPan.replace(/[^A-Z0-9]/gi, '').toUpperCase().trim();

        const legalName = (legalIdx >= 0 ? cols[legalIdx] : cols[2]) || 'Registered Business';
        const tradeName = (tradeIdx >= 0 ? cols[tradeIdx] : cols[3]) || legalName;
        const schemeRaw = ((schemeIdx >= 0 ? cols[schemeIdx] : cols[4]) || 'REGULAR').toUpperCase();
        const gstScheme = (['REGULAR', 'COMPOSITION', 'QRMP', 'CASUAL'].includes(schemeRaw) ? schemeRaw : 'REGULAR') as any;
        const freqRaw = ((freqIdx >= 0 ? cols[freqIdx] : cols[5]) || 'MONTHLY').toUpperCase();
        const filingFrequency = (['MONTHLY', 'QUARTERLY'].includes(freqRaw) ? freqRaw : 'MONTHLY') as any;
        const registrationDate = (regIdx >= 0 ? cols[regIdx] : cols[6]) || '2020-07-01';
        const stateCode = (stateIdx >= 0 ? cols[stateIdx] : cols[7]) || (gstin.length >= 2 ? gstin.substring(0, 2) : '27');

        // Match with existing client
        const matchedClient = clients.find(
          (c) => c.pan?.replace(/[^A-Z0-9]/gi, '').toUpperCase() === pan
        ) || null;

        const isValidGstin = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/.test(gstin);

        rows.push({
          id: i,
          pan,
          gstin,
          legalName,
          tradeName,
          gstScheme,
          filingFrequency,
          registrationDate,
          stateCode,
          matchedClient,
          willAutoOnboard: !matchedClient && pan.length === 10,
          isValid: isValidGstin,
          validationError: !isValidGstin ? 'Invalid 15-char GSTIN format' : undefined,
        });
      }
      setParsedProfiles(rows);
    } catch (err) {
      alert('Failed to parse spreadsheet file.');
    }
  };

  // 2. Parse Historical GST Filings CSV / Excel (.xlsx / .xls / .csv)
  const handleFilingUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setFilingFile(file);

    try {
      const rowsMatrix = await parseSpreadsheetToRows(file);
      if (rowsMatrix.length <= 1) {
        alert('File is empty or missing headers');
        return;
      }

      const headerCols = rowsMatrix[0].map((h) => h.toLowerCase().replace(/[^a-z0-9]/g, ''));
      const gstinIdx = headerCols.findIndex((h) => h.includes('gstin') || h.includes('gst'));
      const typeIdx = headerCols.findIndex((h) => h.includes('type') || h.includes('return'));
      const periodIdx = headerCols.findIndex((h) => h.includes('period') || h.includes('month'));
      const fyIdx = headerCols.findIndex((h) => h.includes('fy') || h.includes('year'));
      const dueIdx = headerCols.findIndex((h) => h.includes('due') || h.includes('date'));
      const statusIdx = headerCols.findIndex((h) => h.includes('status'));
      const valIdx = headerCols.findIndex((h) => h.includes('value') || h.includes('taxable') || h.includes('turnover'));
      const taxIdx = headerCols.findIndex((h) => h.includes('liability') || h.includes('tax'));
      const itcIdx = headerCols.findIndex((h) => h.includes('itc'));
      const arnIdx = headerCols.findIndex((h) => h.includes('arn') || h.includes('ack'));

      const rows: ParsedGstFilingRow[] = [];
      for (let i = 1; i < rowsMatrix.length; i++) {
        const cols = rowsMatrix[i];
        if (cols.length < 2 || cols.every((c) => c.trim() === '')) continue;

        const rawGstin = (gstinIdx >= 0 ? cols[gstinIdx] : cols[0]) || '';
        const gstin = rawGstin.replace(/[^A-Z0-9]/gi, '').toUpperCase().trim();

        const typeRaw = ((typeIdx >= 0 ? cols[typeIdx] : cols[1]) || 'GSTR3B').toUpperCase().replace(/[^A-Z0-9]/g, '');
        const returnType = (['GSTR1', 'GSTR3B', 'CMP08', 'GSTR9'].includes(typeRaw) ? typeRaw : 'GSTR3B') as any;

        const returnPeriod = (periodIdx >= 0 ? cols[periodIdx] : cols[2]) || '2026-07';
        const financialYear = (fyIdx >= 0 ? cols[fyIdx] : cols[3]) || '2026-27';
        const dueDate = (dueIdx >= 0 ? cols[dueIdx] : cols[4]) || '2026-08-20';

        const statusRaw = ((statusIdx >= 0 ? cols[statusIdx] : cols[5]) || 'FILED').toUpperCase();
        const filingStatus = (['FILED', 'PREPARED', 'PENDING', 'OVERDUE'].includes(statusRaw) ? statusRaw : 'FILED') as any;

        const taxableValue = parseFloat((valIdx >= 0 ? cols[valIdx] : cols[6]) || '0') || 0;
        const taxLiability = parseFloat((taxIdx >= 0 ? cols[taxIdx] : cols[7]) || '0') || 0;
        const itcClaimed = parseFloat((itcIdx >= 0 ? cols[itcIdx] : cols[8]) || '0') || 0;
        const arnNumber = (arnIdx >= 0 ? cols[arnIdx] : cols[9]) || undefined;

        // Match with existing GST profile
        const matchedProfile = gstProfiles.find(
          (p) => p.gstin?.replace(/[^A-Z0-9]/gi, '').toUpperCase() === gstin
        ) || null;

        rows.push({
          id: i,
          gstin,
          returnType,
          returnPeriod,
          financialYear,
          dueDate,
          filingStatus,
          taxableValue,
          taxLiability,
          itcClaimed,
          arnNumber,
          matchedProfile,
          isValid: !!matchedProfile || gstin.length === 15,
          validationError: !matchedProfile && gstin.length !== 15 ? 'GSTIN profile not found in practice' : undefined,
        });
      }
      setParsedFilings(rows);
    } catch (err) {
      alert('Failed to parse spreadsheet file.');
    }
  };

  // 3. Execute Bulk GST Profile Import (With Dual-Path Fallback)
  const handleExecuteProfileImport = async () => {
    const validRows = parsedProfiles.filter((p) => p.isValid);
    if (validRows.length === 0) {
      alert('No valid GST profile records to import.');
      return;
    }

    setIsSubmitting(true);
    setProgress(15);

    const payload = validRows.map((item) => ({
      clientId: item.matchedClient?.id || undefined,
      pan: item.pan,
      gstin: item.gstin,
      legalName: item.legalName,
      tradeName: item.tradeName,
      gstType: item.gstScheme as any,
      filingFrequency: item.filingFrequency as any,
      stateCode: item.stateCode,
      registrationDate: item.registrationDate,
      status: 'ACTIVE' as any,
    }));

    // 1. Try High-Speed Batch Endpoint
    try {
      const res = await gstApi.bulkImportProfiles(payload);
      if (res) {
        setProgress(100);
        setResult(res);
        setIsSubmitting(false);
        return;
      }
    } catch (bulkErr: any) {
      console.warn('Batch GST profile endpoint fallback to resilient sequential execution', bulkErr);
    }

    // 2. Resilient Sequential Execution with Auto-Client Creation Fallback
    const created: string[] = [];
    const errors: string[] = [];
    let skipped = 0;

    const panToClientIdMap: Record<string, string> = {};
    clients.forEach((c) => {
      if (c.pan) panToClientIdMap[c.pan.toUpperCase().trim()] = c.id;
    });

    for (let i = 0; i < validRows.length; i++) {
      const item = validRows[i];
      try {
        let clientId = item.matchedClient?.id || panToClientIdMap[item.pan];

        // If client record doesn't exist yet in local map, resolve or auto-create client
        if (!clientId && item.pan) {
          try {
            const newClient = await clientApi.create({
              displayName: item.tradeName || item.legalName || `Client ${item.pan}`,
              legalName: item.legalName || item.tradeName,
              tradeName: item.tradeName,
              pan: item.pan,
              gstin: item.gstin,
              clientType: 'PRIVATE_LIMITED',
              status: 'ACTIVE',
            });
            if (newClient && newClient.id) {
              clientId = newClient.id;
              panToClientIdMap[item.pan] = newClient.id;
            }
          } catch (createClientErr: any) {
            try {
              const existingClients = await clientApi.getAll({ search: item.pan, size: 10 });
              const found = existingClients.content?.find(
                (c) => c.pan?.replace(/[^A-Z0-9]/gi, '').toUpperCase() === item.pan
              );
              if (found) {
                clientId = found.id;
                panToClientIdMap[item.pan] = found.id;
              }
            } catch (searchErr) {
              console.warn('Client search fallback', searchErr);
            }
          }
        }

        const profile = await gstApi.createProfile({
          clientId: clientId || undefined,
          pan: item.pan,
          gstin: item.gstin,
          legalName: item.legalName,
          tradeName: item.tradeName,
          gstType: item.gstScheme as any,
          taxpayerType: item.gstScheme as any,
          filingFrequency: item.filingFrequency as any,
          stateCode: item.stateCode,
          registrationDate: item.registrationDate,
          status: 'ACTIVE' as any,
        } as any);

        created.push(`${item.gstin} (${item.tradeName})`);
      } catch (err: any) {
        const msg = err.response?.data?.message || err.message;
        if (msg.toLowerCase().includes('already exists') || msg.toLowerCase().includes('duplicate')) {
          skipped++;
        } else {
          errors.push(`Row ${item.id} (${item.gstin}): ${msg}`);
        }
      }
      setProgress(Math.round(((i + 1) / validRows.length) * 100));
    }

    setResult({
      totalProcessed: validRows.length,
      totalCreated: created.length,
      totalSkipped: skipped,
      totalFailed: errors.length,
      importedItems: created,
      errors,
    });
    setIsSubmitting(false);
  };

  // 4. Execute Bulk GST Filing Import (With Dual-Path Fallback)
  const handleExecuteFilingImport = async () => {
    const validRows = parsedFilings.filter((f) => f.isValid);
    if (validRows.length === 0) {
      alert('No valid GST filing records to import.');
      return;
    }

    setIsSubmitting(true);
    setProgress(15);

    const payload = validRows.map((item) => ({
      gstProfileId: item.matchedProfile?.id || undefined,
      gstin: item.gstin,
      returnType: item.returnType as any,
      returnPeriod: item.returnPeriod,
      financialYear: item.financialYear,
      dueDate: item.dueDate,
      filingStatus: item.filingStatus as any,
      totalTaxableValue: item.taxableValue,
      totalTaxLiability: item.taxLiability,
      totalItcClaimed: item.itcClaimed,
      acknowledgementNumber: item.arnNumber,
    }));

    // 1. Try High-Speed Batch Endpoint
    try {
      const res = await gstApi.bulkImportFilings(payload);
      if (res) {
        setProgress(100);
        setResult(res);
        setIsSubmitting(false);
        return;
      }
    } catch (bulkErr: any) {
      console.warn('Batch GST filing endpoint fallback to resilient sequential execution', bulkErr);
    }

    // 2. Resilient Sequential Execution Fallback
    const created: string[] = [];
    const errors: string[] = [];
    let skipped = 0;

    for (let i = 0; i < validRows.length; i++) {
      const item = validRows[i];
      try {
        let profileId = item.matchedProfile?.id;
        if (!profileId) {
          const matched = gstProfiles.find((p) => p.gstin === item.gstin);
          if (matched) profileId = matched.id;
        }

        if (!profileId) {
          errors.push(`Row ${item.id}: GST Profile not found for GSTIN ${item.gstin}. Please import GST Profiles first.`);
          continue;
        }

        const filing = await gstApi.createFiling({
          gstProfileId: profileId,
          returnType: item.returnType,
          returnPeriod: item.returnPeriod,
          financialYear: item.financialYear,
          dueDate: item.dueDate,
          filingStatus: item.filingStatus,
          totalTaxableValue: item.taxableValue,
          totalTaxLiability: item.taxLiability,
          totalItcClaimed: item.itcClaimed,
        });

        // If marked FILED with ARN, update ARN status
        if (item.filingStatus === 'FILED' && item.arnNumber) {
          try {
            await gstApi.recordFiling(filing.id, {
              filingDate: item.dueDate,
              acknowledgementNumber: item.arnNumber,
              filingStatus: 'FILED',
            });
          } catch (e) {
            console.warn('ARN update notice', e);
          }
        }

        created.push(`${item.returnType} ${item.returnPeriod} (${item.gstin})`);
      } catch (err: any) {
        const msg = err.response?.data?.message || err.message;
        if (msg.toLowerCase().includes('already exists') || msg.toLowerCase().includes('duplicate')) {
          skipped++;
        } else {
          errors.push(`Row ${item.id} (${item.returnType} ${item.returnPeriod}): ${msg}`);
        }
      }
      setProgress(Math.round(((i + 1) / validRows.length) * 100));
    }

    setResult({
      totalProcessed: validRows.length,
      totalCreated: created.length,
      totalSkipped: skipped,
      totalFailed: errors.length,
      importedItems: created,
      errors,
    });
    setIsSubmitting(false);
  };

  return (
    <div className="space-y-8 max-w-6xl mx-auto animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Link to="/gst" className="text-xs text-slate-400 hover:text-slate-600 font-semibold">
              GST Compliance Hub
            </Link>
            <span className="text-xs text-slate-300">/</span>
            <span className="text-xs font-bold text-slate-700">GST Data Migration</span>
          </div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900 mt-1">
            Client GST Data & Filings Migration Hub
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Migrate existing client GSTIN registrations and historical GSTR-1, GSTR-3B & CMP-08 filing records into {practiceName}.
          </p>
        </div>

        {/* Tab Switcher */}
        <div className="inline-flex items-center gap-1 p-1 bg-slate-100 border border-slate-200 rounded-xl text-xs font-semibold">
          <button
            onClick={() => {
              setActiveTab('PROFILES');
              setResult(null);
            }}
            className={clsx(
              'px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5',
              activeTab === 'PROFILES' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
            )}
          >
            <Building2 className="w-3.5 h-3.5 text-brand-600" />
            <span>1. Client GST Registrations</span>
          </button>
          <button
            onClick={() => {
              setActiveTab('FILINGS');
              setResult(null);
            }}
            className={clsx(
              'px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5',
              activeTab === 'FILINGS' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
            )}
          >
            <FileText className="w-3.5 h-3.5 text-emerald-600" />
            <span>2. Historical GST Returns</span>
          </button>
        </div>
      </div>

      {/* Tab 1: GST Profiles Migration */}
      {activeTab === 'PROFILES' && !result && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <Card
              title="1. Upload Client GSTIN Spreadsheet"
              subtitle="Supports CSV or Excel spreadsheets containing client GSTINs"
              className="lg:col-span-2"
            >
              <div
                onClick={() => profileInputRef.current?.click()}
                className={clsx(
                  'border-2 border-dashed rounded-2xl p-8 flex flex-col items-center justify-center cursor-pointer transition-all text-center',
                  profileFile ? 'border-brand-500 bg-brand-50/20' : 'border-slate-300 hover:border-slate-400 hover:bg-slate-50/50'
                )}
              >
                <input
                  type="file"
                  ref={profileInputRef}
                  accept=".csv, .txt, .xlsx, .xls"
                  onChange={handleProfileUpload}
                  className="hidden"
                />
                <div className="w-12 h-12 rounded-xl bg-brand-50 text-brand-600 flex items-center justify-center mb-3">
                  <UploadCloud className="w-6 h-6" />
                </div>
                {profileFile ? (
                  <div>
                    <p className="font-bold text-sm text-slate-900">{profileFile.name}</p>
                    <p className="text-xs text-slate-500 mt-0.5">{parsedProfiles.length} GST registrations detected</p>
                  </div>
                ) : (
                  <div>
                    <p className="font-bold text-sm text-slate-800">Drag & drop your GSTIN roster spreadsheet</p>
                    <p className="text-xs text-slate-400 mt-1">or click to browse from your computer</p>
                  </div>
                )}
              </div>
            </Card>

            <Card
              title="GST Profiles Template"
              subtitle="Pre-populated sample data"
              className="lg:col-span-1"
            >
              <div className="space-y-3 text-xs">
                <p className="text-slate-600">
                  Includes Client PAN, 15-character GSTIN, Scheme type, and Filing frequency.
                </p>
                <div className="space-y-2 pt-1">
                  <a
                    href="/Taxoryn_Sample_GST_Profiles_Migration.xlsx"
                    download="Taxoryn_Sample_GST_Profiles_Migration.xlsx"
                    className="w-full inline-flex items-center justify-center gap-1.5 px-3 py-2 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-300 rounded-lg text-xs font-bold transition-colors shadow-2xs"
                  >
                    <FileSpreadsheet className="w-4 h-4 text-emerald-600" />
                    <span>Download Excel (.xlsx)</span>
                  </a>
                  <a
                    href="/sample_gst_profiles_migration.csv"
                    download="sample_gst_profiles_migration.csv"
                    className="w-full inline-flex items-center justify-center gap-1.5 px-3 py-2 bg-slate-50 hover:bg-slate-100 text-slate-800 border border-slate-300 rounded-lg text-xs font-semibold transition-colors"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span>Download CSV (.csv)</span>
                  </a>
                </div>
              </div>
            </Card>
          </div>

          {/* Pre-Import Data Grid */}
          {parsedProfiles.length > 0 && (
            <Card
              title={`2. Pre-Import Verification (${parsedProfiles.filter((p) => p.isValid).length}/${parsedProfiles.length} Ready to Import)`}
              subtitle="Review and align GST registrations before importing"
              action={
                <Button
                  onClick={handleExecuteProfileImport}
                  isLoading={isSubmitting}
                  style={{ backgroundColor: currentTheme.primaryColor }}
                  rightIcon={<ArrowRight className="w-4 h-4" />}
                >
                  Import {parsedProfiles.filter((p) => p.isValid).length} GST Profiles Now
                </Button>
              }
              noPadding
            >
              {isSubmitting && (
                <div className="p-4 bg-slate-50 border-b border-slate-200 space-y-2">
                  <p className="text-xs font-bold text-slate-800">
                    Importing GST registrations into {practiceName}... ({progress}%)
                  </p>
                  <div className="w-full h-2 bg-slate-200 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-brand-600 transition-all duration-300"
                      style={{ width: `${progress}%`, backgroundColor: currentTheme.primaryColor }}
                    />
                  </div>
                </div>
              )}

              <div className="overflow-x-auto max-h-[440px]">
                <table className="w-full text-left text-xs border-collapse">
                  <thead className="sticky top-0 bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase text-[10px]">
                    <tr>
                      <th className="px-4 py-3">Status</th>
                      <th className="px-4 py-3">Client / Business</th>
                      <th className="px-4 py-3">PAN</th>
                      <th className="px-4 py-3">GSTIN</th>
                      <th className="px-4 py-3">Scheme</th>
                      <th className="px-4 py-3">Frequency</th>
                      <th className="px-4 py-3">State</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {parsedProfiles.map((p) => (
                      <tr key={p.id} className={!p.isValid ? 'bg-rose-50/40' : 'hover:bg-slate-50'}>
                        <td className="px-4 py-3">
                          {p.matchedClient ? (
                            <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200 inline-flex items-center gap-1">
                              <CheckCircle2 className="w-3 h-3" /> Matched Client
                            </span>
                          ) : p.willAutoOnboard ? (
                            <span className="text-[10px] font-bold text-blue-700 bg-blue-50 px-2 py-0.5 rounded-full border border-blue-200 inline-flex items-center gap-1">
                              <Sparkles className="w-3 h-3 text-blue-600" /> Auto-Onboard Client
                            </span>
                          ) : (
                            <span className="text-[10px] font-bold text-rose-700 bg-rose-50 px-2 py-0.5 rounded-full border border-rose-200">
                              {p.validationError}
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3 font-bold text-slate-900">{p.tradeName}</td>
                        <td className="px-4 py-3 font-mono font-bold text-slate-700">{p.pan}</td>
                        <td className="px-4 py-3 font-mono font-bold text-brand-600">{p.gstin}</td>
                        <td className="px-4 py-3 text-[10px] font-semibold uppercase">{p.gstScheme}</td>
                        <td className="px-4 py-3 text-[10px] font-semibold uppercase">{p.filingFrequency}</td>
                        <td className="px-4 py-3 font-mono text-slate-500">{p.stateCode}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          )}
        </div>
      )}

      {/* Tab 2: GST Filings Migration */}
      {activeTab === 'FILINGS' && !result && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <Card
              title="1. Upload Historical GST Filings Spreadsheet"
              subtitle="Supports CSV or Excel with Turnover, Tax, ITC and ARN values"
              className="lg:col-span-2"
            >
              <div
                onClick={() => filingInputRef.current?.click()}
                className={clsx(
                  'border-2 border-dashed rounded-2xl p-8 flex flex-col items-center justify-center cursor-pointer transition-all text-center',
                  filingFile ? 'border-brand-500 bg-brand-50/20' : 'border-slate-300 hover:border-slate-400 hover:bg-slate-50/50'
                )}
              >
                <input
                  type="file"
                  ref={filingInputRef}
                  accept=".csv, .txt, .xlsx, .xls"
                  onChange={handleFilingUpload}
                  className="hidden"
                />
                <div className="w-12 h-12 rounded-xl bg-brand-50 text-brand-600 flex items-center justify-center mb-3">
                  <UploadCloud className="w-6 h-6" />
                </div>
                {filingFile ? (
                  <div>
                    <p className="font-bold text-sm text-slate-900">{filingFile.name}</p>
                    <p className="text-xs text-slate-500 mt-0.5">{parsedFilings.length} filing records detected</p>
                  </div>
                ) : (
                  <div>
                    <p className="font-bold text-sm text-slate-800">Drag & drop your GST filings history spreadsheet</p>
                    <p className="text-xs text-slate-400 mt-1">or click to browse from your computer</p>
                  </div>
                )}
              </div>
            </Card>

            <Card
              title="GST Filings Template"
              subtitle="Pre-populated historical returns"
              className="lg:col-span-1"
            >
              <div className="space-y-3 text-xs">
                <p className="text-slate-600">
                  Includes GSTR-1, GSTR-3B, CMP-08 with taxable values and ARN references.
                </p>
                <div className="space-y-2 pt-1">
                  <a
                    href="/Taxoryn_Sample_GST_Filings_Migration.xlsx"
                    download="Taxoryn_Sample_GST_Filings_Migration.xlsx"
                    className="w-full inline-flex items-center justify-center gap-1.5 px-3 py-2 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-300 rounded-lg text-xs font-bold transition-colors shadow-2xs"
                  >
                    <FileSpreadsheet className="w-4 h-4 text-emerald-600" />
                    <span>Download Excel (.xlsx)</span>
                  </a>
                  <a
                    href="/sample_gst_filings_migration.csv"
                    download="sample_gst_filings_migration.csv"
                    className="w-full inline-flex items-center justify-center gap-1.5 px-3 py-2 bg-slate-50 hover:bg-slate-100 text-slate-800 border border-slate-300 rounded-lg text-xs font-semibold transition-colors"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span>Download CSV (.csv)</span>
                  </a>
                </div>
              </div>
            </Card>
          </div>

          {/* Pre-Import Data Grid */}
          {parsedFilings.length > 0 && (
            <Card
              title={`2. Pre-Import Verification (${parsedFilings.filter((f) => f.isValid).length}/${parsedFilings.length} Ready to Import)`}
              subtitle="Review and align filing reconciliation values before migrating"
              action={
                <Button
                  onClick={handleExecuteFilingImport}
                  isLoading={isSubmitting}
                  style={{ backgroundColor: currentTheme.primaryColor }}
                  rightIcon={<ArrowRight className="w-4 h-4" />}
                >
                  Import {parsedFilings.filter((f) => f.isValid).length} GST Filings Now
                </Button>
              }
              noPadding
            >
              <div className="overflow-x-auto max-h-[440px]">
                <table className="w-full text-left text-xs border-collapse">
                  <thead className="sticky top-0 bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase text-[10px]">
                    <tr>
                      <th className="px-4 py-3">Status</th>
                      <th className="px-4 py-3">GSTIN</th>
                      <th className="px-4 py-3">Return Type</th>
                      <th className="px-4 py-3">Period</th>
                      <th className="px-4 py-3">Due Date</th>
                      <th className="px-4 py-3 text-right">Taxable Turnover</th>
                      <th className="px-4 py-3 text-right">Tax Liability</th>
                      <th className="px-4 py-3 text-right">ITC Claimed</th>
                      <th className="px-4 py-3">ARN Number</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {parsedFilings.map((f) => (
                      <tr key={f.id} className={!f.isValid ? 'bg-rose-50/40' : 'hover:bg-slate-50'}>
                        <td className="px-4 py-3">
                          <StatusBadge status={f.filingStatus} size="sm" />
                        </td>
                        <td className="px-4 py-3 font-mono font-bold text-slate-900">{f.gstin}</td>
                        <td className="px-4 py-3 font-bold text-brand-600">{f.returnType}</td>
                        <td className="px-4 py-3 font-mono text-slate-700">{f.returnPeriod}</td>
                        <td className="px-4 py-3 font-mono text-slate-500">{f.dueDate}</td>
                        <td className="px-4 py-3 font-mono font-semibold text-right text-slate-800">
                          ₹{f.taxableValue.toLocaleString('en-IN')}
                        </td>
                        <td className="px-4 py-3 font-mono font-semibold text-right text-slate-800">
                          ₹{f.taxLiability.toLocaleString('en-IN')}
                        </td>
                        <td className="px-4 py-3 font-mono font-semibold text-right text-emerald-700">
                          ₹{f.itcClaimed.toLocaleString('en-IN')}
                        </td>
                        <td className="px-4 py-3 font-mono text-[11px] text-slate-600">
                          {f.arnNumber || <span className="text-slate-300 italic">—</span>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          )}
        </div>
      )}

      {/* Result Report */}
      {result && (
        <Card
          title="GST Data Migration Summary Report"
          subtitle={`GST records migrated into ${practiceName}`}
        >
          <div className="space-y-6">
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
              <div className="p-4 bg-blue-50 border border-blue-200 rounded-xl text-center">
                <span className="text-xs font-bold text-blue-700 uppercase">Total Attempted</span>
                <p className="text-3xl font-black text-blue-900 mt-1">{result.totalProcessed}</p>
              </div>
              <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-center">
                <span className="text-xs font-bold text-emerald-700 uppercase">Successfully Imported</span>
                <p className="text-3xl font-black text-emerald-900 mt-1">{result.totalCreated}</p>
              </div>
              <div className="p-4 bg-amber-50 border border-amber-200 rounded-xl text-center">
                <span className="text-xs font-bold text-amber-700 uppercase">Skipped Duplicates</span>
                <p className="text-3xl font-black text-amber-900 mt-1">{result.totalSkipped || 0}</p>
              </div>
              <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-center">
                <span className="text-xs font-bold text-rose-700 uppercase">Failed</span>
                <p className="text-3xl font-black text-rose-900 mt-1">{result.totalFailed}</p>
              </div>
            </div>

            {result.errors?.length > 0 && (
              <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl space-y-1 text-xs text-rose-700">
                <h4 className="font-bold uppercase tracking-wider">Details & Notices</h4>
                {result.errors.map((err, idx) => (
                  <p key={idx}>{err}</p>
                ))}
              </div>
            )}

            <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
              <Button
                variant="outline"
                onClick={() => {
                  setResult(null);
                  setParsedProfiles([]);
                  setParsedFilings([]);
                  setProfileFile(null);
                  setFilingFile(null);
                  loadPrerequisites();
                }}
              >
                Migrate More GST Data
              </Button>
              <Button
                onClick={() => navigate('/gst')}
                style={{ backgroundColor: currentTheme.primaryColor }}
                rightIcon={<ArrowRight className="w-4 h-4" />}
              >
                Go to GST Compliance Hub
              </Button>
            </div>
          </div>
        </Card>
      )}
    </div>
  );
};
