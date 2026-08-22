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
  BadgePercent,
  Receipt,
  UserCheck,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { StatusBadge } from '../components/common/StatusBadge';
import { clientApi, tdsApi } from '../api/endpoints';
import { useBranding } from '../context/BrandingContext';
import { useAuth } from '../context/AuthContext';
import { Client, TdsProfile, TdsReturn, BulkTdsProfileImportResult, BulkTdsReturnImportResult } from '../types';
import { parseSpreadsheetToRows } from '../utils/spreadsheetParser';
import clsx from 'clsx';

interface ParsedTdsProfileRow {
  id: number;
  tan: string;
  clientName: string;
  pan?: string;
  deductorType: 'COMPANY' | 'INDIVIDUAL_HUF' | 'FIRM' | 'LLP' | 'BRANCH_DIVISION' | 'GOVERNMENT_CENTRAL' | 'GOVERNMENT_STATE';
  responsiblePersonName: string;
  responsiblePersonPan?: string;
  responsiblePersonDesignation?: string;
  email?: string;
  mobile?: string;
  matchedClient: Client | null;
  willAutoOnboard: boolean;
  isValid: boolean;
  validationError?: string;
}

interface ParsedTdsReturnRow {
  id: number;
  tan: string;
  clientName: string;
  formType: 'FORM_24Q' | 'FORM_26Q' | 'FORM_27Q' | 'FORM_27EQ';
  quarter: 'Q1' | 'Q2' | 'Q3' | 'Q4';
  financialYear: string;
  assessmentYear: string;
  dueDate: string;
  filingStatus: 'PENDING' | 'DRAFT' | 'CHALLANS_ATTACHED' | 'UNDER_REVIEW' | 'READY_TO_FILE' | 'FILED';
  filingDate?: string;
  tokenNumber?: string;
  totalTaxDeducted: number;
  totalTaxDeposited: number;
  matchedProfile: TdsProfile | null;
  isValid: boolean;
  validationError?: string;
}

export const TdsDataMigrationHubPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'PROFILES' | 'RETURNS'>('PROFILES');
  const [clients, setClients] = useState<Client[]>([]);
  const [tdsProfiles, setTdsProfiles] = useState<TdsProfile[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [profileResult, setProfileResult] = useState<BulkTdsProfileImportResult | null>(null);
  const [returnResult, setReturnResult] = useState<BulkTdsReturnImportResult | null>(null);

  // Profile Upload State
  const [profileFile, setProfileFile] = useState<File | null>(null);
  const [parsedProfiles, setParsedProfiles] = useState<ParsedTdsProfileRow[]>([]);
  const profileInputRef = useRef<HTMLInputElement>(null);

  // Return Upload State
  const [returnFile, setReturnFile] = useState<File | null>(null);
  const [parsedReturns, setParsedReturns] = useState<ParsedTdsReturnRow[]>([]);
  const returnInputRef = useRef<HTMLInputElement>(null);

  const { currentTheme } = useBranding();
  const { practiceName } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    loadPrerequisites();
  }, []);

  const loadPrerequisites = async () => {
    try {
      setIsLoading(true);
      const [clientRes, profRes] = await Promise.all([
        clientApi.getAll({ size: 500 }).catch(() => ({ content: [] })),
        tdsApi.getProfiles({ size: 500 }).catch(() => ({ content: [] })),
      ]);
      setClients(Array.isArray(clientRes) ? clientRes : (clientRes?.content || []));
      setTdsProfiles(Array.isArray(profRes) ? profRes : (profRes?.content || []));
    } catch (err) {
      console.error('Failed to load migration prerequisites', err);
    } finally {
      setIsLoading(false);
    }
  };

  // ---------------------------------------------------------------------------
  // Demo Data Quick Fill Handlers
  // ---------------------------------------------------------------------------
  const handleQuickFillDemoProfiles = () => {
    const demo: ParsedTdsProfileRow[] = [
      {
        id: 1,
        tan: 'BLRP12345A',
        clientName: 'Acme Corporation Pvt Ltd',
        pan: 'AABCA1234K',
        deductorType: 'COMPANY',
        responsiblePersonName: 'Rajesh Sharma',
        responsiblePersonPan: 'ABCPS9876K',
        responsiblePersonDesignation: 'Managing Director',
        email: 'tax@acme.com',
        mobile: '9876543210',
        matchedClient: clients.find((c) => c.tan?.toUpperCase() === 'BLRP12345A' || c.pan?.toUpperCase() === 'AABCA1234K') || null,
        willAutoOnboard: !clients.some((c) => c.tan?.toUpperCase() === 'BLRP12345A' || c.pan?.toUpperCase() === 'AABCA1234K'),
        isValid: true,
      },
      {
        id: 2,
        tan: 'DELC98765B',
        clientName: 'Apex Direct Solutions LLP',
        pan: 'AACCA9876L',
        deductorType: 'LLP',
        responsiblePersonName: 'Amit Verma',
        responsiblePersonPan: 'VERPA1234M',
        responsiblePersonDesignation: 'Designated Partner',
        email: 'accounts@apexdirect.in',
        mobile: '9812345678',
        matchedClient: clients.find((c) => c.tan?.toUpperCase() === 'DELC98765B' || c.pan?.toUpperCase() === 'AACCA9876L') || null,
        willAutoOnboard: !clients.some((c) => c.tan?.toUpperCase() === 'DELC98765B' || c.pan?.toUpperCase() === 'AACCA9876L'),
        isValid: true,
      },
      {
        id: 3,
        tan: 'MUMP45678C',
        clientName: 'Sunil & Associates',
        pan: 'AADFS4567N',
        deductorType: 'FIRM',
        responsiblePersonName: 'Sunil Mehta',
        responsiblePersonPan: 'MEHTS5678P',
        responsiblePersonDesignation: 'Senior Partner',
        email: 'sunil@sunilassociates.com',
        mobile: '9823456789',
        matchedClient: clients.find((c) => c.tan?.toUpperCase() === 'MUMP45678C' || c.pan?.toUpperCase() === 'AADFS4567N') || null,
        willAutoOnboard: !clients.some((c) => c.tan?.toUpperCase() === 'MUMP45678C' || c.pan?.toUpperCase() === 'AADFS4567N'),
        isValid: true,
      },
      {
        id: 4,
        tan: 'HYDA23456D',
        clientName: 'TechNova Systems India Ltd',
        pan: 'AABCT5678G',
        deductorType: 'COMPANY',
        responsiblePersonName: 'Venkatesh Rao',
        responsiblePersonPan: 'RAOPV1234H',
        responsiblePersonDesignation: 'Director Finance',
        email: 'finance@technova.io',
        mobile: '9849012345',
        matchedClient: clients.find((c) => c.tan?.toUpperCase() === 'HYDA23456D' || c.pan?.toUpperCase() === 'AABCT5678G') || null,
        willAutoOnboard: !clients.some((c) => c.tan?.toUpperCase() === 'HYDA23456D' || c.pan?.toUpperCase() === 'AABCT5678G'),
        isValid: true,
      },
    ];
    setParsedProfiles(demo);
    setProfileResult(null);
  };

  const handleQuickFillDemoReturns = () => {
    const demo: ParsedTdsReturnRow[] = [
      {
        id: 1,
        tan: 'BLRP12345A',
        clientName: 'Acme Corporation Pvt Ltd',
        formType: 'FORM_26Q',
        quarter: 'Q1',
        financialYear: '2026-27',
        assessmentYear: '2027-28',
        dueDate: '2026-07-31',
        filingStatus: 'FILED',
        tokenNumber: '010020304050601',
        filingDate: '2026-07-28',
        totalTaxDeducted: 45000,
        totalTaxDeposited: 45000,
        matchedProfile: tdsProfiles.find((p) => p.tan.toUpperCase() === 'BLRP12345A') || null,
        isValid: true,
      },
      {
        id: 2,
        tan: 'BLRP12345A',
        clientName: 'Acme Corporation Pvt Ltd',
        formType: 'FORM_24Q',
        quarter: 'Q1',
        financialYear: '2026-27',
        assessmentYear: '2027-28',
        dueDate: '2026-07-31',
        filingStatus: 'FILED',
        tokenNumber: '010020304050602',
        filingDate: '2026-07-29',
        totalTaxDeducted: 120000,
        totalTaxDeposited: 120000,
        matchedProfile: tdsProfiles.find((p) => p.tan.toUpperCase() === 'BLRP12345A') || null,
        isValid: true,
      },
      {
        id: 3,
        tan: 'DELC98765B',
        clientName: 'Apex Direct Solutions LLP',
        formType: 'FORM_26Q',
        quarter: 'Q1',
        financialYear: '2026-27',
        assessmentYear: '2027-28',
        dueDate: '2026-07-31',
        filingStatus: 'PENDING',
        totalTaxDeducted: 18500,
        totalTaxDeposited: 18500,
        matchedProfile: tdsProfiles.find((p) => p.tan.toUpperCase() === 'DELC98765B') || null,
        isValid: true,
      },
    ];
    setParsedReturns(demo);
    setReturnResult(null);
  };

  // ---------------------------------------------------------------------------
  // Profile CSV Handlers (2D Matrix Parser)
  // ---------------------------------------------------------------------------
  const handleProfileFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setProfileFile(file);
    setProfileResult(null);

    try {
      const rowsMatrix = await parseSpreadsheetToRows(file);
      if (!rowsMatrix || rowsMatrix.length <= 1) {
        alert('Spreadsheet is empty or missing headers.');
        return;
      }

      const headerCols = rowsMatrix[0].map((h) => (h || '').toLowerCase().replace(/[^a-z0-9]/g, ''));
      const tanIdx = headerCols.findIndex((h) => h.includes('tan'));
      const nameIdx = headerCols.findIndex((h) => h.includes('client') || h.includes('name') || h.includes('deductor') || h.includes('company'));
      const panIdx = headerCols.findIndex((h) => h === 'pan' || h.includes('clientpan') || h.includes('entitypan'));
      const catIdx = headerCols.findIndex((h) => h.includes('category') || h.includes('type') || h.includes('constitution'));
      const respNameIdx = headerCols.findIndex((h) => h.includes('responsible') || h.includes('officer') || h.includes('director') || h.includes('person'));
      const respPanIdx = headerCols.findIndex((h) => h.includes('officerpan') || h.includes('responsiblepersonpan'));
      const desigIdx = headerCols.findIndex((h) => h.includes('designation') || h.includes('role'));
      const emailIdx = headerCols.findIndex((h) => h.includes('email'));
      const mobileIdx = headerCols.findIndex((h) => h.includes('mobile') || h.includes('phone'));

      const parsed: ParsedTdsProfileRow[] = [];
      for (let i = 1; i < rowsMatrix.length; i++) {
        const cols = rowsMatrix[i];
        if (!cols || cols.length === 0 || cols.every((c) => !c || c.trim() === '')) continue;

        const tan = ((tanIdx >= 0 ? cols[tanIdx] : cols[0]) || '').toUpperCase().trim();
        const clientName = ((nameIdx >= 0 ? cols[nameIdx] : cols[1]) || `Deductor ${tan}`).trim();
        const pan = (panIdx >= 0 ? cols[panIdx] : '').toUpperCase().trim();
        const rawCategory = ((catIdx >= 0 ? cols[catIdx] : cols[3]) || 'COMPANY').toUpperCase().replace(/[\s-]/g, '_');

        let deductorType: any = 'COMPANY';
        if (rawCategory.includes('INDIVIDUAL') || rawCategory.includes('HUF')) deductorType = 'INDIVIDUAL_HUF';
        else if (rawCategory.includes('LLP')) deductorType = 'LLP';
        else if (rawCategory.includes('FIRM') || rawCategory.includes('PARTNER')) deductorType = 'FIRM';
        else if (rawCategory.includes('GOV') && rawCategory.includes('CENT')) deductorType = 'GOVERNMENT_CENTRAL';
        else if (rawCategory.includes('GOV') && rawCategory.includes('STATE')) deductorType = 'GOVERNMENT_STATE';
        else if (rawCategory.includes('BRANCH') || rawCategory.includes('DIV')) deductorType = 'BRANCH_DIVISION';

        const responsiblePersonName = ((respNameIdx >= 0 ? cols[respNameIdx] : cols[4]) || clientName || 'Director').trim();
        const responsiblePersonPan = (respPanIdx >= 0 ? cols[respPanIdx] : pan || '').toUpperCase().trim();
        const responsiblePersonDesignation = ((desigIdx >= 0 ? cols[desigIdx] : cols[6]) || 'Director').trim();
        const email = (emailIdx >= 0 ? cols[emailIdx] : '').trim();
        const mobile = (mobileIdx >= 0 ? cols[mobileIdx] : '').trim();

        const matchedClient = clients.find(
          (c) =>
            (tan && c.tan?.toUpperCase() === tan) ||
            (pan && c.pan?.toUpperCase() === pan) ||
            (clientName && (c.displayName?.toLowerCase() === clientName.toLowerCase() || c.legalName?.toLowerCase() === clientName.toLowerCase()))
        ) || null;

        const isTanValid = /^[A-Z]{4}[0-9]{5}[A-Z]{1}$/.test(tan);
        const isValid = Boolean(isTanValid && tan.length === 10);
        const validationError = !isTanValid ? 'Invalid 10-char TAN format (expected BLRP12345A)' : undefined;

        parsed.push({
          id: i,
          tan,
          clientName: clientName || matchedClient?.displayName || 'Unknown Deductor',
          pan,
          deductorType,
          responsiblePersonName,
          responsiblePersonPan,
          responsiblePersonDesignation,
          email,
          mobile,
          matchedClient,
          willAutoOnboard: !matchedClient && isValid,
          isValid,
          validationError,
        });
      }

      setParsedProfiles(parsed);
    } catch (err) {
      console.error('Error parsing profile file', err);
    }
  };

  // ---------------------------------------------------------------------------
  // Return CSV Handlers (2D Matrix Parser)
  // ---------------------------------------------------------------------------
  const handleReturnFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setReturnFile(file);
    setReturnResult(null);

    try {
      const rowsMatrix = await parseSpreadsheetToRows(file);
      if (!rowsMatrix || rowsMatrix.length <= 1) {
        alert('Spreadsheet is empty or missing headers.');
        return;
      }

      const headerCols = rowsMatrix[0].map((h) => (h || '').toLowerCase().replace(/[^a-z0-9]/g, ''));
      const tanIdx = headerCols.findIndex((h) => h.includes('tan'));
      const nameIdx = headerCols.findIndex((h) => h.includes('client') || h.includes('name') || h.includes('deductor'));
      const formIdx = headerCols.findIndex((h) => h.includes('form'));
      const quarterIdx = headerCols.findIndex((h) => h.includes('quarter') || h.includes('qtr'));
      const fyIdx = headerCols.findIndex((h) => h.includes('financial') || h.includes('fy'));
      const ayIdx = headerCols.findIndex((h) => h.includes('assessment') || h.includes('ay'));
      const dueDateIdx = headerCols.findIndex((h) => h.includes('due'));
      const statusIdx = headerCols.findIndex((h) => h.includes('status'));
      const tokenIdx = headerCols.findIndex((h) => h.includes('token') || h.includes('prn') || h.includes('receipt') || h.includes('ack'));
      const fileDateIdx = headerCols.findIndex((h) => h.includes('filing') || h.includes('filed'));
      const deductedIdx = headerCols.findIndex((h) => h.includes('deducted'));
      const depositedIdx = headerCols.findIndex((h) => h.includes('deposited') || h.includes('paid'));

      const parsed: ParsedTdsReturnRow[] = [];
      for (let i = 1; i < rowsMatrix.length; i++) {
        const cols = rowsMatrix[i];
        if (!cols || cols.length === 0 || cols.every((c) => !c || c.trim() === '')) continue;

        const tan = ((tanIdx >= 0 ? cols[tanIdx] : cols[0]) || '').toUpperCase().trim();
        const clientName = ((nameIdx >= 0 ? cols[nameIdx] : cols[1]) || `Deductor ${tan}`).trim();
        const rawForm = ((formIdx >= 0 ? cols[formIdx] : cols[2]) || '26Q').toUpperCase().replace('FORM_', '').trim();
        const formType = ('FORM_' + rawForm) as any;
        const quarter = ((quarterIdx >= 0 ? cols[quarterIdx] : cols[3]) || 'Q1').toUpperCase().trim() as any;
        const financialYear = ((fyIdx >= 0 ? cols[fyIdx] : cols[4]) || '2026-27').trim();
        const assessmentYear = ((ayIdx >= 0 ? cols[ayIdx] : cols[5]) || '2027-28').trim();
        const dueDate = ((dueDateIdx >= 0 ? cols[dueDateIdx] : cols[6]) || '2026-07-31').trim();
        const tokenNumber = (tokenIdx >= 0 ? cols[tokenIdx] : '').trim();
        const rawStatus = ((statusIdx >= 0 ? cols[statusIdx] : cols[7]) || (tokenNumber ? 'FILED' : 'PENDING')).toUpperCase().trim() as any;
        const filingDate = (fileDateIdx >= 0 ? cols[fileDateIdx] : '').trim();
        const totalTaxDeducted = Number(deductedIdx >= 0 ? cols[deductedIdx] : 0) || 0;
        const totalTaxDeposited = Number(depositedIdx >= 0 ? cols[depositedIdx] : totalTaxDeducted) || totalTaxDeducted;

        const matchedProfile = tdsProfiles.find((p) => p.tan.toUpperCase() === tan) || null;

        const isTanValid = /^[A-Z]{4}[0-9]{5}[A-Z]{1}$/.test(tan);
        let isValid = isTanValid;
        let validationError = !isTanValid ? 'Invalid TAN format' : undefined;

        if (isValid && !matchedProfile) {
          isValid = false;
          validationError = 'TAN Profile not found. Please import TAN master first.';
        }

        parsed.push({
          id: i,
          tan,
          clientName: clientName || matchedProfile?.clientName || 'TAN Deductor',
          formType: ['FORM_24Q', 'FORM_26Q', 'FORM_27Q', 'FORM_27EQ'].includes(formType) ? formType : 'FORM_26Q',
          quarter: ['Q1', 'Q2', 'Q3', 'Q4'].includes(quarter) ? quarter : 'Q1',
          financialYear,
          assessmentYear,
          dueDate,
          filingStatus: ['PENDING', 'DRAFT', 'CHALLANS_ATTACHED', 'UNDER_REVIEW', 'READY_TO_FILE', 'FILED'].includes(rawStatus) ? rawStatus : 'PENDING',
          filingDate: filingDate || undefined,
          tokenNumber: tokenNumber || undefined,
          totalTaxDeducted,
          totalTaxDeposited,
          matchedProfile,
          isValid,
          validationError,
        });
      }

      setParsedReturns(parsed);
    } catch (err) {
      console.error('Error parsing return file', err);
    }
  };

  // Submit Bulk Profiles
  const handleImportProfiles = async () => {
    setIsSubmitting(true);
    try {
      const requests = [];
      for (const row of parsedProfiles.filter((p) => p.isValid)) {
        let clientId = row.matchedClient?.id;

        // Auto-onboard Client if not matched
        if (!clientId) {
          const newClient = await clientApi.create({
            displayName: row.clientName,
            legalName: row.clientName,
            pan: row.pan,
            tan: row.tan,
            email: row.email,
            phone: row.mobile,
            clientType: row.deductorType === 'COMPANY' ? 'PRIVATE_LIMITED' : 'INDIVIDUAL',
            status: 'ACTIVE',
          });
          clientId = newClient.id;
        }

        requests.push({
          clientId,
          tan: row.tan,
          deductorType: row.deductorType,
          responsiblePersonName: row.responsiblePersonName,
          responsiblePersonPan: row.responsiblePersonPan,
          responsiblePersonDesignation: row.responsiblePersonDesignation,
          responsiblePersonEmail: row.email,
          responsiblePersonMobile: row.mobile,
          status: 'ACTIVE' as any,
        });
      }

      const res = await tdsApi.bulkImportProfiles(requests);
      setProfileResult(res);
      await loadPrerequisites();
    } catch (err) {
      console.error('Failed to bulk import profiles', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Submit Bulk Returns
  const handleImportReturns = async () => {
    setIsSubmitting(true);
    try {
      const requests = parsedReturns
        .filter((r) => r.isValid && r.matchedProfile)
        .map((row) => ({
          clientId: row.matchedProfile!.clientId,
          tdsProfileId: row.matchedProfile!.id,
          formType: row.formType,
          quarter: row.quarter,
          financialYear: row.financialYear,
          assessmentYear: row.assessmentYear,
          dueDate: row.dueDate,
          filingStatus: row.filingStatus,
          filingDate: row.filingDate,
          tokenNumber: row.tokenNumber,
          totalTaxDeducted: row.totalTaxDeducted,
          totalTaxDeposited: row.totalTaxDeposited,
          totalAmountPaid: row.totalTaxDeducted * 10,
        }));

      const res = await tdsApi.bulkImportReturns(requests);
      setReturnResult(res);
    } catch (err) {
      console.error('Failed to bulk import returns', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Download Sample Templates
  const downloadProfileTemplate = () => {
    const csvContent =
      'TAN,Client Name,PAN,Deductor Type,Responsible Person,Officer PAN,Designation,Email,Mobile\n' +
      'BLRP12345A,Acme Corporation Pvt Ltd,AABCA1234K,COMPANY,Rajesh Sharma,ABCPS9876K,Managing Director,tax@acme.com,9876543210\n' +
      'DELC98765B,Apex Direct Solutions LLP,AACCA9876L,LLP,Amit Verma,VERPA1234M,Designated Partner,accounts@apexdirect.in,9812345678\n' +
      'MUMP45678C,Sunil & Associates,AADFS4567N,FIRM,Sunil Mehta,MEHTS5678P,Senior Partner,sunil@sunilassociates.com,9823456789\n';

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', 'Taxoryn_TDS_Profiles_Template.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const downloadReturnTemplate = () => {
    const csvContent =
      'TAN,Client Name,Form,Quarter,Financial Year,Assessment Year,Due Date,Status,Token Number,Filing Date,TDS Deducted,TDS Deposited\n' +
      'BLRP12345A,Acme Corporation Pvt Ltd,26Q,Q1,2026-27,2027-28,2026-07-31,FILED,010020304050601,2026-07-28,45000,45000\n' +
      'BLRP12345A,Acme Corporation Pvt Ltd,24Q,Q1,2026-27,2027-28,2026-07-31,FILED,010020304050602,2026-07-29,120000,120000\n' +
      'DELC98765B,Apex Direct Solutions LLP,26Q,Q1,2026-27,2027-28,2026-07-31,PENDING,,2026-07-31,18500,18500\n';

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', 'Taxoryn_TDS_Quarterly_Returns_Template.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
        <div>
          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400">
              <UploadCloud className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-slate-900 dark:text-white">TDS Practice Migration Hub</h1>
              <p className="text-sm text-slate-500 dark:text-slate-400">
                Bulk onboarding of client TAN master records and historical quarterly return statements
              </p>
            </div>
          </div>
        </div>

        <Link to="/tds">
          <Button variant="outline" size="sm">
            <ArrowRight className="w-4 h-4 mr-1.5 rotate-180" />
            Back to TDS Hub
          </Button>
        </Link>
      </div>

      {/* Tabs */}
      <div className="border-b border-slate-200 dark:border-slate-800">
        <nav className="flex space-x-6">
          <button
            onClick={() => setActiveTab('PROFILES')}
            className={clsx(
              'flex items-center py-3 px-1 border-b-2 font-medium text-sm transition-colors',
              activeTab === 'PROFILES'
                ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400 dark:border-indigo-400'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            )}
          >
            <Building2 className="w-4 h-4 mr-2" />
            1. TAN Deductor Master Register
          </button>
          <button
            onClick={() => setActiveTab('RETURNS')}
            className={clsx(
              'flex items-center py-3 px-1 border-b-2 font-medium text-sm transition-colors',
              activeTab === 'RETURNS'
                ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400 dark:border-indigo-400'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            )}
          >
            <FileSpreadsheet className="w-4 h-4 mr-2" />
            2. Historical Quarterly Statements (24Q / 26Q / 27Q)
          </button>
        </nav>
      </div>

      {/* TAB 1: TAN PROFILES MIGRATION */}
      {activeTab === 'PROFILES' && (
        <div className="space-y-6">
          {/* Upload Card */}
          <div className="bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
              <div>
                <h2 className="text-base font-bold text-slate-900 dark:text-white">Import Client TAN Master Records</h2>
                <p className="text-xs text-slate-500">
                  Upload CSV or Excel file containing deductor details, 10-character TANs, and principal officers.
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Button variant="outline" size="sm" onClick={handleQuickFillDemoProfiles}>
                  <Sparkles className="w-4 h-4 mr-1.5 text-amber-500" />
                  Try Sample Demo Data
                </Button>
                <Button variant="outline" size="sm" onClick={downloadProfileTemplate}>
                  <Download className="w-4 h-4 mr-1.5" />
                  Download Template
                </Button>
              </div>
            </div>

            <div
              onClick={() => profileInputRef.current?.click()}
              className="border-2 border-dashed border-slate-300 dark:border-slate-700 hover:border-indigo-500 dark:hover:border-indigo-500 p-8 rounded-xl text-center cursor-pointer transition-colors"
            >
              <UploadCloud className="w-10 h-10 text-indigo-500 mx-auto mb-2" />
              <div className="text-sm font-semibold text-slate-900 dark:text-white">
                {profileFile ? profileFile.name : 'Click or Drag & Drop CSV / Excel Spreadsheet'}
              </div>
              <p className="text-xs text-slate-400 mt-1">Supports .csv, .xlsx, .xls</p>
              <input
                ref={profileInputRef}
                type="file"
                accept=".csv,.xlsx,.xls"
                className="hidden"
                onChange={handleProfileFileChange}
              />
            </div>
          </div>

          {/* Success / Result Banner */}
          {profileResult && (
            <div className="bg-emerald-50 dark:bg-emerald-950/40 p-5 rounded-2xl border border-emerald-200 dark:border-emerald-800 text-sm space-y-2">
              <div className="flex items-center space-x-2 text-emerald-800 dark:text-emerald-200 font-bold">
                <CheckCircle2 className="w-5 h-5 text-emerald-600" />
                <span>TAN Master Migration Completed Successfully</span>
              </div>
              <div className="text-xs text-emerald-700 dark:text-emerald-300">
                Processed: {profileResult.totalProcessed} | Created: {profileResult.totalCreated} | Skipped (Existing): {profileResult.totalSkipped} | Failed: {profileResult.totalFailed}
              </div>
            </div>
          )}

          {/* Preview Table */}
          {parsedProfiles.length > 0 && (
            <div className="bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
              <div className="flex justify-between items-center">
                <div>
                  <h3 className="text-base font-bold text-slate-900 dark:text-white">
                    Parsed TAN Records ({parsedProfiles.filter((p) => p.isValid).length} Valid, {parsedProfiles.filter((p) => !p.isValid).length} Errors)
                  </h3>
                  <p className="text-xs text-slate-500">Unmatched clients will be automatically onboarded into practice CRM.</p>
                </div>
                <Button
                  variant="primary"
                  size="sm"
                  disabled={isSubmitting || parsedProfiles.filter((p) => p.isValid).length === 0}
                  onClick={handleImportProfiles}
                >
                  <Sparkles className="w-4 h-4 mr-1.5" />
                  {isSubmitting ? 'Importing...' : `Import ${parsedProfiles.filter((p) => p.isValid).length} TAN Profiles`}
                </Button>
              </div>

              <div className="overflow-x-auto border border-slate-200 dark:border-slate-700 rounded-xl max-h-96 overflow-y-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 dark:bg-slate-800 sticky top-0 font-semibold text-slate-500 border-b border-slate-200 dark:border-slate-700">
                    <tr>
                      <th className="py-2.5 px-3">#</th>
                      <th className="py-2.5 px-3">TAN</th>
                      <th className="py-2.5 px-3">Client Deductor</th>
                      <th className="py-2.5 px-3">Category</th>
                      <th className="py-2.5 px-3">Principal Officer</th>
                      <th className="py-2.5 px-3">CRM Match</th>
                      <th className="py-2.5 px-3">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                    {parsedProfiles.map((p) => (
                      <tr key={p.id} className={clsx(!p.isValid && 'bg-rose-50/50 dark:bg-rose-950/20')}>
                        <td className="py-2.5 px-3 text-slate-400">{p.id}</td>
                        <td className="py-2.5 px-3 font-mono font-bold text-indigo-600 dark:text-indigo-400">{p.tan}</td>
                        <td className="py-2.5 px-3 font-medium text-slate-900 dark:text-white">{p.clientName}</td>
                        <td className="py-2.5 px-3">{p.deductorType}</td>
                        <td className="py-2.5 px-3">{p.responsiblePersonName}</td>
                        <td className="py-2.5 px-3">
                          {p.matchedClient ? (
                            <span className="text-emerald-600 font-medium flex items-center gap-1">
                              <CheckCircle2 className="w-3.5 h-3.5" /> Matched
                            </span>
                          ) : (
                            <span className="text-blue-600 font-medium flex items-center gap-1">
                              <UserCheck className="w-3.5 h-3.5" /> Auto-Onboard
                            </span>
                          )}
                        </td>
                        <td className="py-2.5 px-3">
                          {p.isValid ? (
                            <span className="px-2 py-0.5 text-[10px] font-semibold rounded bg-emerald-50 text-emerald-700">Ready</span>
                          ) : (
                            <span className="px-2 py-0.5 text-[10px] font-semibold rounded bg-rose-50 text-rose-700" title={p.validationError}>
                              {p.validationError}
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: HISTORICAL RETURNS MIGRATION */}
      {activeTab === 'RETURNS' && (
        <div className="space-y-6">
          <div className="bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
              <div>
                <h2 className="text-base font-bold text-slate-900 dark:text-white">Import Historical Quarterly Statements</h2>
                <p className="text-xs text-slate-500">
                  Upload quarterly filings (Form 24Q, 26Q, 27Q, 27EQ) with NSDL token numbers and financial totals.
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Button variant="outline" size="sm" onClick={handleQuickFillDemoReturns}>
                  <Sparkles className="w-4 h-4 mr-1.5 text-amber-500" />
                  Try Sample Demo Data
                </Button>
                <Button variant="outline" size="sm" onClick={downloadReturnTemplate}>
                  <Download className="w-4 h-4 mr-1.5" />
                  Download Template
                </Button>
              </div>
            </div>

            <div
              onClick={() => returnInputRef.current?.click()}
              className="border-2 border-dashed border-slate-300 dark:border-slate-700 hover:border-indigo-500 dark:hover:border-indigo-500 p-8 rounded-xl text-center cursor-pointer transition-colors"
            >
              <UploadCloud className="w-10 h-10 text-indigo-500 mx-auto mb-2" />
              <div className="text-sm font-semibold text-slate-900 dark:text-white">
                {returnFile ? returnFile.name : 'Click or Drag & Drop Returns Spreadsheet'}
              </div>
              <p className="text-xs text-slate-400 mt-1">Supports .csv, .xlsx, .xls</p>
              <input
                ref={returnInputRef}
                type="file"
                accept=".csv,.xlsx,.xls"
                className="hidden"
                onChange={handleReturnFileChange}
              />
            </div>
          </div>

          {/* Success Banner */}
          {returnResult && (
            <div className="bg-emerald-50 dark:bg-emerald-950/40 p-5 rounded-2xl border border-emerald-200 dark:border-emerald-800 text-sm space-y-2">
              <div className="flex items-center space-x-2 text-emerald-800 dark:text-emerald-200 font-bold">
                <CheckCircle2 className="w-5 h-5 text-emerald-600" />
                <span>Quarterly Statements Migration Completed Successfully</span>
              </div>
              <div className="text-xs text-emerald-700 dark:text-emerald-300">
                Processed: {returnResult.totalProcessed} | Created: {returnResult.totalCreated} | Skipped: {returnResult.totalSkipped} | Failed: {returnResult.totalFailed}
              </div>
            </div>
          )}

          {/* Parsed Returns Table */}
          {parsedReturns.length > 0 && (
            <div className="bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
              <div className="flex justify-between items-center">
                <div>
                  <h3 className="text-base font-bold text-slate-900 dark:text-white">
                    Parsed Quarterly Returns ({parsedReturns.filter((r) => r.isValid).length} Valid, {parsedReturns.filter((r) => !r.isValid).length} Errors)
                  </h3>
                  <p className="text-xs text-slate-500">Statements will link to their registered TAN master profile.</p>
                </div>
                <Button
                  variant="primary"
                  size="sm"
                  disabled={isSubmitting || parsedReturns.filter((r) => r.isValid).length === 0}
                  onClick={handleImportReturns}
                >
                  <Sparkles className="w-4 h-4 mr-1.5" />
                  {isSubmitting ? 'Importing...' : `Import ${parsedReturns.filter((r) => r.isValid).length} Statements`}
                </Button>
              </div>

              <div className="overflow-x-auto border border-slate-200 dark:border-slate-700 rounded-xl max-h-96 overflow-y-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 dark:bg-slate-800 sticky top-0 font-semibold text-slate-500 border-b border-slate-200 dark:border-slate-700">
                    <tr>
                      <th className="py-2.5 px-3">#</th>
                      <th className="py-2.5 px-3">TAN</th>
                      <th className="py-2.5 px-3">Form & Quarter</th>
                      <th className="py-2.5 px-3">FY / AY</th>
                      <th className="py-2.5 px-3">TDS Deducted</th>
                      <th className="py-2.5 px-3">Token (PRN)</th>
                      <th className="py-2.5 px-3">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                    {parsedReturns.map((r) => (
                      <tr key={r.id} className={clsx(!r.isValid && 'bg-rose-50/50 dark:bg-rose-950/20')}>
                        <td className="py-2.5 px-3 text-slate-400">{r.id}</td>
                        <td className="py-2.5 px-3 font-mono font-bold text-indigo-600 dark:text-indigo-400">{r.tan}</td>
                        <td className="py-2.5 px-3 font-semibold text-slate-900 dark:text-white">
                          {r.formType.replace('FORM_', 'Form ')} {r.quarter}
                        </td>
                        <td className="py-2.5 px-3">FY {r.financialYear}</td>
                        <td className="py-2.5 px-3 font-semibold">₹{r.totalTaxDeducted.toLocaleString('en-IN')}</td>
                        <td className="py-2.5 px-3 font-mono text-[11px] text-slate-600">{r.tokenNumber || '-'}</td>
                        <td className="py-2.5 px-3">
                          {r.isValid ? (
                            <span className="px-2 py-0.5 text-[10px] font-semibold rounded bg-emerald-50 text-emerald-700">Ready</span>
                          ) : (
                            <span className="px-2 py-0.5 text-[10px] font-semibold rounded bg-rose-50 text-rose-700" title={r.validationError}>
                              {r.validationError}
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
export default TdsDataMigrationHubPage;
