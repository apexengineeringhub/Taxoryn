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
import { clientApi, itrApi } from '../api/endpoints';
import { useBranding } from '../context/BrandingContext';
import { useAuth } from '../context/AuthContext';
import { Client, ItrProfile, ItrReturn, BulkItrImportResult } from '../types';
import { parseSpreadsheetToRows } from '../utils/spreadsheetParser';
import clsx from 'clsx';

interface ParsedItrProfileRow {
  id: number;
  pan: string;
  clientName: string;
  taxpayerType: 'INDIVIDUAL' | 'HUF' | 'FIRM' | 'LLP' | 'COMPANY' | 'TRUST' | 'AOP_BOI';
  defaultItrType: 'ITR_1' | 'ITR_2' | 'ITR_3' | 'ITR_4' | 'ITR_5' | 'ITR_6' | 'ITR_7';
  residentialStatus: 'RESIDENT' | 'NON_RESIDENT' | 'RNOR';
  email?: string;
  phone?: string;
  matchedClient: Client | null;
  willAutoOnboard: boolean;
  isValid: boolean;
  validationError?: string;
}

interface ParsedItrReturnRow {
  id: number;
  pan: string;
  clientName: string;
  assessmentYear: string;
  financialYear: string;
  itrType: 'ITR_1' | 'ITR_2' | 'ITR_3' | 'ITR_4' | 'ITR_5' | 'ITR_6' | 'ITR_7';
  taxpayerType: string;
  filingStatus: 'DOCUMENTS_PENDING' | 'DATA_ENTRY' | 'UNDER_REVIEW' | 'READY_TO_FILE' | 'FILED' | 'VERIFICATION_PENDING' | 'COMPLETED';
  acknowledgementNumber?: string;
  filingDate?: string;
  dueDate?: string;
  notes?: string;
  matchedProfile: ItrProfile | null;
  isValid: boolean;
  validationError?: string;
}

export const ItrDataMigrationHubPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'PROFILES' | 'RETURNS'>('PROFILES');
  const [clients, setClients] = useState<Client[]>([]);
  const [itrProfiles, setItrProfiles] = useState<ItrProfile[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [progress, setProgress] = useState(0);
  const [result, setResult] = useState<BulkItrImportResult | null>(null);

  // Profile Upload State
  const [profileFile, setProfileFile] = useState<File | null>(null);
  const [parsedProfiles, setParsedProfiles] = useState<ParsedItrProfileRow[]>([]);
  const profileInputRef = useRef<HTMLInputElement>(null);

  // Return Upload State
  const [returnFile, setReturnFile] = useState<File | null>(null);
  const [parsedReturns, setParsedReturns] = useState<ParsedItrReturnRow[]>([]);
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
      const [clientRes, profileRes] = await Promise.all([
        clientApi.getAll({ size: 300 }),
        itrApi.getProfiles().catch(() => ({ content: [] })),
      ]);
      setClients(clientRes.content || []);
      setItrProfiles(profileRes.content || []);
    } catch (err) {
      console.error('Failed to load clients and ITR profiles', err);
    } finally {
      setIsLoading(false);
    }
  };

  // ---------------------------------------------------------------------------
  // Demo Data Generator
  // ---------------------------------------------------------------------------
  const handleQuickFillDemoProfiles = () => {
    const demo: ParsedItrProfileRow[] = [
      {
        id: 1,
        pan: 'AAACZ1234D',
        clientName: 'Apex Engineering Solutions Pvt Ltd',
        taxpayerType: 'COMPANY',
        defaultItrType: 'ITR_6',
        residentialStatus: 'RESIDENT',
        email: 'tax@apexengineering.com',
        phone: '9876543210',
        matchedClient: clients.find((c) => c.pan?.toUpperCase() === 'AAACZ1234D') || null,
        willAutoOnboard: !clients.some((c) => c.pan?.toUpperCase() === 'AAACZ1234D'),
        isValid: true,
      },
      {
        id: 2,
        pan: 'AABFA1234F',
        clientName: 'MAA MUNDESHWARI TAX CONSULTANCY',
        taxpayerType: 'FIRM',
        defaultItrType: 'ITR_5',
        residentialStatus: 'RESIDENT',
        email: 'pawanadv@gmail.com',
        phone: '9876500001',
        matchedClient: clients.find((c) => c.pan?.toUpperCase() === 'AABFA1234F') || null,
        willAutoOnboard: !clients.some((c) => c.pan?.toUpperCase() === 'AABFA1234F'),
        isValid: true,
      },
      {
        id: 3,
        pan: 'ABCPJ9876M',
        clientName: 'Pawan Pathak & Associates',
        taxpayerType: 'INDIVIDUAL',
        defaultItrType: 'ITR_4',
        residentialStatus: 'RESIDENT',
        email: 'info@pawanpathak.com',
        phone: '9876500002',
        matchedClient: clients.find((c) => c.pan?.toUpperCase() === 'ABCPJ9876M') || null,
        willAutoOnboard: !clients.some((c) => c.pan?.toUpperCase() === 'ABCPJ9876M'),
        isValid: true,
      },
      {
        id: 4,
        pan: 'AABCM5678K',
        clientName: 'Zenith Infotech Private Limited',
        taxpayerType: 'COMPANY',
        defaultItrType: 'ITR_6',
        residentialStatus: 'RESIDENT',
        email: 'finance@zenithtech.in',
        phone: '9876500003',
        matchedClient: clients.find((c) => c.pan?.toUpperCase() === 'AABCM5678K') || null,
        willAutoOnboard: !clients.some((c) => c.pan?.toUpperCase() === 'AABCM5678K'),
        isValid: true,
      },
      {
        id: 5,
        pan: 'AAACS2345P',
        clientName: 'Skyline Logistics LLP',
        taxpayerType: 'LLP',
        defaultItrType: 'ITR_5',
        residentialStatus: 'RESIDENT',
        email: 'accounts@skylinelogistics.in',
        phone: '9876500004',
        matchedClient: clients.find((c) => c.pan?.toUpperCase() === 'AAACS2345P') || null,
        willAutoOnboard: !clients.some((c) => c.pan?.toUpperCase() === 'AAACS2345P'),
        isValid: true,
      },
      {
        id: 6,
        pan: 'BKRPK8899L',
        clientName: 'Dr. Rajesh Kumar Sharma',
        taxpayerType: 'INDIVIDUAL',
        defaultItrType: 'ITR_1',
        residentialStatus: 'RESIDENT',
        email: 'dr.rajesh@gmail.com',
        phone: '9876500005',
        matchedClient: clients.find((c) => c.pan?.toUpperCase() === 'BKRPK8899L') || null,
        willAutoOnboard: !clients.some((c) => c.pan?.toUpperCase() === 'BKRPK8899L'),
        isValid: true,
      },
      {
        id: 7,
        pan: 'CGTPV4455Q',
        clientName: 'Vikram Malhotra (Capital Gains)',
        taxpayerType: 'INDIVIDUAL',
        defaultItrType: 'ITR_2',
        residentialStatus: 'RESIDENT',
        email: 'vikram.m@outlook.com',
        phone: '9876500006',
        matchedClient: clients.find((c) => c.pan?.toUpperCase() === 'CGTPV4455Q') || null,
        willAutoOnboard: !clients.some((c) => c.pan?.toUpperCase() === 'CGTPV4455Q'),
        isValid: true,
      },
      {
        id: 8,
        pan: 'AAATH1122R',
        clientName: 'Heritage Educational Trust',
        taxpayerType: 'TRUST',
        defaultItrType: 'ITR_7',
        residentialStatus: 'RESIDENT',
        email: 'trustee@heritagetrust.org',
        phone: '9876500007',
        matchedClient: clients.find((c) => c.pan?.toUpperCase() === 'AAATH1122R') || null,
        willAutoOnboard: !clients.some((c) => c.pan?.toUpperCase() === 'AAATH1122R'),
        isValid: true,
      },
    ];
    setParsedProfiles(demo);
    setResult(null);
  };

  const handleQuickFillDemoReturns = () => {
    const demo: ParsedItrReturnRow[] = [
      {
        id: 1,
        pan: 'AAACZ1234D',
        clientName: 'Apex Engineering Solutions Pvt Ltd',
        assessmentYear: '2026-27',
        financialYear: '2025-26',
        itrType: 'ITR_6',
        taxpayerType: 'COMPANY',
        filingStatus: 'FILED',
        acknowledgementNumber: '123456789012345',
        filingDate: '2026-07-28',
        dueDate: '2026-10-31',
        notes: 'Audited Corporate return e-filed and e-verified',
        matchedProfile: itrProfiles.find((p) => p.pan?.toUpperCase() === 'AAACZ1234D') || null,
        isValid: true,
      },
      {
        id: 2,
        pan: 'AABFA1234F',
        clientName: 'MAA MUNDESHWARI TAX CONSULTANCY',
        assessmentYear: '2026-27',
        financialYear: '2025-26',
        itrType: 'ITR_5',
        taxpayerType: 'FIRM',
        filingStatus: 'FILED',
        acknowledgementNumber: '234567890123456',
        filingDate: '2026-07-25',
        dueDate: '2026-07-31',
        notes: 'Partnership firm return filed via DSC',
        matchedProfile: itrProfiles.find((p) => p.pan?.toUpperCase() === 'AABFA1234F') || null,
        isValid: true,
      },
      {
        id: 3,
        pan: 'ABCPJ9876M',
        clientName: 'Pawan Pathak & Associates',
        assessmentYear: '2026-27',
        financialYear: '2025-26',
        itrType: 'ITR_4',
        taxpayerType: 'INDIVIDUAL',
        filingStatus: 'COMPLETED',
        acknowledgementNumber: '345678901234567',
        filingDate: '2026-07-20',
        dueDate: '2026-07-31',
        notes: 'Section 44AD presumptive taxation return completed',
        matchedProfile: itrProfiles.find((p) => p.pan?.toUpperCase() === 'ABCPJ9876M') || null,
        isValid: true,
      },
      {
        id: 4,
        pan: 'AABCM5678K',
        clientName: 'Zenith Infotech Private Limited',
        assessmentYear: '2025-26',
        financialYear: '2024-25',
        itrType: 'ITR_6',
        taxpayerType: 'COMPANY',
        filingStatus: 'COMPLETED',
        acknowledgementNumber: '456789012345678',
        filingDate: '2025-10-29',
        dueDate: '2025-10-31',
        notes: 'Prior AY company return processed by CPC',
        matchedProfile: itrProfiles.find((p) => p.pan?.toUpperCase() === 'AABCM5678K') || null,
        isValid: true,
      },
      {
        id: 5,
        pan: 'AAACS2345P',
        clientName: 'Skyline Logistics LLP',
        assessmentYear: '2026-27',
        financialYear: '2025-26',
        itrType: 'ITR_5',
        taxpayerType: 'LLP',
        filingStatus: 'READY_TO_FILE',
        acknowledgementNumber: '',
        filingDate: '',
        dueDate: '2026-07-31',
        notes: 'Computation vetted and ready for client sign-off',
        matchedProfile: itrProfiles.find((p) => p.pan?.toUpperCase() === 'AAACS2345P') || null,
        isValid: true,
      },
      {
        id: 6,
        pan: 'BKRPK8899L',
        clientName: 'Dr. Rajesh Kumar Sharma',
        assessmentYear: '2026-27',
        financialYear: '2025-26',
        itrType: 'ITR_1',
        taxpayerType: 'INDIVIDUAL',
        filingStatus: 'FILED',
        acknowledgementNumber: '567890123456789',
        filingDate: '2026-07-15',
        dueDate: '2026-07-31',
        notes: 'Salary & Interest income Sahaj return filed',
        matchedProfile: itrProfiles.find((p) => p.pan?.toUpperCase() === 'BKRPK8899L') || null,
        isValid: true,
      },
    ];
    setParsedReturns(demo);
    setResult(null);
  };

  // ---------------------------------------------------------------------------
  // Universal File Parsers (Tab 1: ITR Profiles)
  // ---------------------------------------------------------------------------
  const handleProfileFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setProfileFile(file);
    setResult(null);
    try {
      const rowsMatrix = await parseSpreadsheetToRows(file);
      if (rowsMatrix.length <= 1) {
        alert('File is empty or missing headers');
        return;
      }

      const headerCols = rowsMatrix[0].map((h) => h.toLowerCase().replace(/[^a-z0-9]/g, ''));
      const panIdx = headerCols.findIndex((h) => h.includes('pan'));
      const nameIdx = headerCols.findIndex((h) => h.includes('client') || h.includes('name') || h.includes('taxpayer'));
      const catIdx = headerCols.findIndex((h) => h.includes('category') || h.includes('taxpayer') || h.includes('type') || h.includes('constitution'));
      const formIdx = headerCols.findIndex((h) => h.includes('form') || h.includes('itr'));
      const resIdx = headerCols.findIndex((h) => h.includes('residen') || h.includes('status'));
      const emailIdx = headerCols.findIndex((h) => h.includes('email'));
      const phoneIdx = headerCols.findIndex((h) => h.includes('phone') || h.includes('mobile'));

      const parsed: ParsedItrProfileRow[] = [];
      for (let i = 1; i < rowsMatrix.length; i++) {
        const cols = rowsMatrix[i];
        if (cols.length === 0 || cols.every((c) => c.trim() === '')) continue;

        const pan = ((panIdx >= 0 ? cols[panIdx] : cols[0]) || '').toUpperCase().trim();
        const clientName = (nameIdx >= 0 ? cols[nameIdx] : cols[1]) || `Taxpayer ${pan}`;
        const rawCategory = ((catIdx >= 0 ? cols[catIdx] : cols[2]) || 'INDIVIDUAL').toUpperCase().replace(/[\s-]/g, '_');

        let taxpayerType: any = 'INDIVIDUAL';
        if (rawCategory.includes('COMPANY') || rawCategory.includes('PVT') || rawCategory.includes('LTD')) taxpayerType = 'COMPANY';
        else if (rawCategory.includes('LLP')) taxpayerType = 'LLP';
        else if (rawCategory.includes('FIRM') || rawCategory.includes('PARTNER')) taxpayerType = 'FIRM';
        else if (rawCategory.includes('HUF')) taxpayerType = 'HUF';
        else if (rawCategory.includes('TRUST')) taxpayerType = 'TRUST';
        else if (rawCategory.includes('AOP') || rawCategory.includes('BOI') || rawCategory.includes('SOCIETY')) taxpayerType = 'AOP_BOI';

        const rawForm = ((formIdx >= 0 ? cols[formIdx] : cols[3]) || 'ITR_1').toUpperCase().replace(/[\s-]/g, '_');
        let defaultItrType: any = 'ITR_1';
        if (['ITR_1', 'ITR_2', 'ITR_3', 'ITR_4', 'ITR_5', 'ITR_6', 'ITR_7'].includes(rawForm)) {
          defaultItrType = rawForm;
        } else if (taxpayerType === 'COMPANY') defaultItrType = 'ITR_6';
        else if (taxpayerType === 'LLP' || taxpayerType === 'FIRM') defaultItrType = 'ITR_5';
        else if (taxpayerType === 'TRUST') defaultItrType = 'ITR_7';

        const rawRes = ((resIdx >= 0 ? cols[resIdx] : cols[4]) || 'RESIDENT').toUpperCase();
        let residentialStatus: any = 'RESIDENT';
        if (rawRes.includes('NON') || rawRes.includes('NRI')) residentialStatus = 'NON_RESIDENT';
        else if (rawRes.includes('RNOR')) residentialStatus = 'RNOR';

        const matchedClient = clients.find((c) => c.pan?.toUpperCase() === pan) || null;
        const willAutoOnboard = !matchedClient;

        const isPanValid = /^[A-Z]{5}[0-9]{4}[A-Z]{1}$/.test(pan);
        const isValid = Boolean(isPanValid && pan);
        const validationError = !isPanValid
          ? 'Invalid 10-char PAN format (expected ABCDE1234F)'
          : undefined;

        parsed.push({
          id: i,
          pan,
          clientName,
          taxpayerType,
          defaultItrType,
          residentialStatus,
          email: emailIdx >= 0 ? cols[emailIdx] : '',
          phone: phoneIdx >= 0 ? cols[phoneIdx] : '',
          matchedClient,
          willAutoOnboard,
          isValid,
          validationError,
        });
      }

      setParsedProfiles(parsed);
    } catch (err: any) {
      alert(`Failed to parse file: ${err.message}`);
    }
  };

  // ---------------------------------------------------------------------------
  // Universal File Parsers (Tab 2: Historical Returns)
  // ---------------------------------------------------------------------------
  const handleReturnFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setReturnFile(file);
    setResult(null);
    try {
      const rowsMatrix = await parseSpreadsheetToRows(file);
      if (rowsMatrix.length <= 1) {
        alert('File is empty or missing headers');
        return;
      }

      const headerCols = rowsMatrix[0].map((h) => h.toLowerCase().replace(/[^a-z0-9]/g, ''));
      const panIdx = headerCols.findIndex((h) => h.includes('pan'));
      const nameIdx = headerCols.findIndex((h) => h.includes('client') || h.includes('name') || h.includes('taxpayer'));
      const ayIdx = headerCols.findIndex((h) => h.includes('assessment') || h.includes('ay'));
      const fyIdx = headerCols.findIndex((h) => h.includes('financial') || h.includes('fy'));
      const formIdx = headerCols.findIndex((h) => h.includes('form') || h.includes('itr'));
      const statusIdx = headerCols.findIndex((h) => h.includes('status'));
      const ackIdx = headerCols.findIndex((h) => h.includes('ack') || h.includes('itrv') || h.includes('number'));
      const fileDateIdx = headerCols.findIndex((h) => h.includes('filing') || h.includes('filed'));
      const dueDateIdx = headerCols.findIndex((h) => h.includes('due'));
      const notesIdx = headerCols.findIndex((h) => h.includes('note') || h.includes('remark'));

      const parsed: ParsedItrReturnRow[] = [];
      for (let i = 1; i < rowsMatrix.length; i++) {
        const cols = rowsMatrix[i];
        if (cols.length === 0 || cols.every((c) => c.trim() === '')) continue;

        const pan = ((panIdx >= 0 ? cols[panIdx] : cols[0]) || '').toUpperCase().trim();
        const clientName = (nameIdx >= 0 ? cols[nameIdx] : cols[1]) || `Taxpayer ${pan}`;
        const ay = (ayIdx >= 0 ? cols[ayIdx] : cols[2]) || '2026-27';
        const fy = (fyIdx >= 0 ? cols[fyIdx] : cols[3]) || '2025-26';

        const rawForm = ((formIdx >= 0 ? cols[formIdx] : cols[4]) || 'ITR_1').toUpperCase().replace(/[\s-]/g, '_');
        let itrType: any = 'ITR_1';
        if (['ITR_1', 'ITR_2', 'ITR_3', 'ITR_4', 'ITR_5', 'ITR_6', 'ITR_7'].includes(rawForm)) {
          itrType = rawForm;
        }

        const rawStatus = ((statusIdx >= 0 ? cols[statusIdx] : cols[5]) || 'DOCUMENTS_PENDING').toUpperCase().replace(/[\s-]/g, '_');
        let filingStatus: any = 'DOCUMENTS_PENDING';
        if (['DOCUMENTS_PENDING', 'DATA_ENTRY', 'UNDER_REVIEW', 'READY_TO_FILE', 'FILED', 'VERIFICATION_PENDING', 'COMPLETED'].includes(rawStatus)) {
          filingStatus = rawStatus;
        } else if (ackIdx >= 0 && cols[ackIdx]) {
          filingStatus = 'FILED';
        }

        const ackNo = (ackIdx >= 0 ? cols[ackIdx] : '') || '';
        const filingDate = (fileDateIdx >= 0 ? cols[fileDateIdx] : (filingStatus === 'FILED' ? '2026-07-25' : '')) || '';
        const dueDate = (dueDateIdx >= 0 ? cols[dueDateIdx] : (itrType === 'ITR_6' ? '2026-10-31' : '2026-07-31')) || '';

        const matchedProfile = itrProfiles.find((p) => p.pan?.toUpperCase() === pan) || null;
        const isPanValid = /^[A-Z]{5}[0-9]{4}[A-Z]{1}$/.test(pan);
        const isValid = Boolean(isPanValid && pan && ay);
        const validationError = !isPanValid ? 'Invalid 10-char PAN format' : undefined;

        parsed.push({
          id: i,
          pan,
          clientName,
          assessmentYear: ay,
          financialYear: fy,
          itrType,
          taxpayerType: 'INDIVIDUAL',
          filingStatus,
          acknowledgementNumber: ackNo,
          filingDate,
          dueDate,
          notes: notesIdx >= 0 ? cols[notesIdx] : '',
          matchedProfile,
          isValid,
          validationError,
        });
      }

      setParsedReturns(parsed);
    } catch (err: any) {
      alert(`Failed to parse file: ${err.message}`);
    }
  };

  // ---------------------------------------------------------------------------
  // Ingestion Execution (Tab 1: ITR Profiles)
  // ---------------------------------------------------------------------------
  const handleExecuteProfileMigration = async () => {
    const validRows = parsedProfiles.filter((r) => r.isValid);
    if (validRows.length === 0) {
      alert('No valid ITR profile rows to import.');
      return;
    }

    setIsSubmitting(true);
    setProgress(10);
    const importResult: BulkItrImportResult = {
      totalProcessed: validRows.length,
      totalCreated: 0,
      totalSkipped: 0,
      totalFailed: 0,
      importedItems: [],
      errors: [],
    };

    try {
      const payload = validRows.map((r) => ({
        clientId: r.matchedClient?.id || undefined,
        pan: r.pan,
        displayName: r.clientName,
        legalName: r.clientName,
        taxpayerType: r.taxpayerType,
        defaultItrType: r.defaultItrType,
        residentialStatus: r.residentialStatus,
      }));

      setProgress(40);
      try {
        const batchRes = await itrApi.bulkImportProfiles(payload);
        if (batchRes) {
          importResult.totalCreated = batchRes.totalCreated || 0;
          importResult.totalSkipped = batchRes.totalSkipped || 0;
          importResult.totalFailed = batchRes.totalFailed || 0;
          importResult.importedItems = batchRes.importedItems || [];
          importResult.errors = batchRes.errors || [];
          setProgress(100);
          setResult(importResult);
          loadPrerequisites();
          return;
        }
      } catch (batchErr: any) {
        console.warn('Batch endpoint notice, executing sequential fallback...', batchErr);
      }

      // Sequential Fallback
      let currentProgress = 40;
      const increment = 55 / validRows.length;

      for (const row of validRows) {
        try {
          let clientId = row.matchedClient?.id;
          if (!clientId) {
            let clientType: any = 'INDIVIDUAL';
            if (row.taxpayerType === 'COMPANY') clientType = 'PRIVATE_LIMITED';
            else if (row.taxpayerType === 'LLP') clientType = 'LLP';
            else if (row.taxpayerType === 'FIRM') clientType = 'PARTNERSHIP';
            else if (row.taxpayerType === 'HUF') clientType = 'HUF';
            else if (row.taxpayerType === 'TRUST') clientType = 'TRUST';

            const newClient = await clientApi.create({
              displayName: row.clientName,
              legalName: row.clientName,
              pan: row.pan,
              clientType,
              status: 'ACTIVE',
            });
            clientId = newClient.id;
          }

          const created = await itrApi.createProfile({
            clientId,
            pan: row.pan,
            taxpayerType: row.taxpayerType,
            defaultItrType: row.defaultItrType,
            residentialStatus: row.residentialStatus,
          });

          importResult.totalCreated++;
          importResult.importedItems.push(`${created.pan} (${row.clientName})`);
        } catch (err: any) {
          const errMsg = err.response?.data?.message || err.message;
          if (errMsg?.toLowerCase().includes('already exists') || errMsg?.toLowerCase().includes('duplicate')) {
            importResult.totalSkipped++;
            importResult.errors.push(`PAN ${row.pan} already registered in firm`);
          } else {
            importResult.totalFailed++;
            importResult.errors.push(`PAN ${row.pan}: ${errMsg}`);
          }
        }
        currentProgress += increment;
        setProgress(Math.min(95, Math.round(currentProgress)));
      }

      setProgress(100);
      setResult(importResult);
      loadPrerequisites();
    } catch (err: any) {
      alert(`Migration error: ${err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // ---------------------------------------------------------------------------
  // Ingestion Execution (Tab 2: Historical Returns)
  // ---------------------------------------------------------------------------
  const handleExecuteReturnMigration = async () => {
    const validRows = parsedReturns.filter((r) => r.isValid);
    if (validRows.length === 0) {
      alert('No valid historical ITR return rows to import.');
      return;
    }

    setIsSubmitting(true);
    setProgress(15);
    const importResult: BulkItrImportResult = {
      totalProcessed: validRows.length,
      totalCreated: 0,
      totalSkipped: 0,
      totalFailed: 0,
      importedItems: [],
      errors: [],
    };

    try {
      const payload = validRows.map((r) => ({
        pan: r.pan,
        assessmentYear: r.assessmentYear,
        financialYear: r.financialYear,
        itrType: r.itrType,
        taxpayerType: r.taxpayerType as any,
        status: r.filingStatus as any,
        acknowledgementNumber: r.acknowledgementNumber,
        filingDate: r.filingDate,
        dueDate: r.dueDate,
        notes: r.notes,
      }));

      setProgress(40);
      try {
        const batchRes = await itrApi.bulkImportReturns(payload);
        if (batchRes) {
          importResult.totalCreated = batchRes.totalCreated || 0;
          importResult.totalSkipped = batchRes.totalSkipped || 0;
          importResult.totalFailed = batchRes.totalFailed || 0;
          importResult.importedItems = batchRes.importedItems || [];
          importResult.errors = batchRes.errors || [];
          setProgress(100);
          setResult(importResult);
          return;
        }
      } catch (batchErr: any) {
        console.warn('Batch endpoint notice, executing sequential fallback...', batchErr);
      }

      // Sequential Fallback
      let currentProgress = 40;
      const increment = 55 / validRows.length;

      for (const row of validRows) {
        try {
          const created = await itrApi.createReturn({
            pan: row.pan,
            assessmentYear: row.assessmentYear,
            financialYear: row.financialYear,
            itrType: row.itrType,
            status: row.filingStatus as any,
            acknowledgementNumber: row.acknowledgementNumber,
            filingDate: row.filingDate,
            dueDate: row.dueDate,
            notes: row.notes,
          });

          importResult.totalCreated++;
          importResult.importedItems.push(`${row.clientName} (AY ${created.assessmentYear} - ${created.itrType})`);
        } catch (err: any) {
          const errMsg = err.response?.data?.message || err.message;
          if (errMsg?.toLowerCase().includes('already exists') || errMsg?.toLowerCase().includes('duplicate')) {
            importResult.totalSkipped++;
            importResult.errors.push(`Return for ${row.pan} (AY ${row.assessmentYear}) already exists`);
          } else {
            importResult.totalFailed++;
            importResult.errors.push(`PAN ${row.pan} (AY ${row.assessmentYear}): ${errMsg}`);
          }
        }
        currentProgress += increment;
        setProgress(Math.min(95, Math.round(currentProgress)));
      }

      setProgress(100);
      setResult(importResult);
    } catch (err: any) {
      alert(`Return migration error: ${err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="bg-linear-to-r from-purple-950 via-slate-900 to-brand-950 rounded-2xl p-6 text-white shadow-xl relative overflow-hidden">
        <div className="absolute right-0 top-0 w-96 h-full bg-radial from-purple-500/20 to-transparent pointer-events-none" />
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6 relative z-10">
          <div>
            <div className="flex items-center gap-2 text-purple-300 font-bold text-xs uppercase tracking-wider mb-2">
              <Sparkles className="w-4 h-4" />
              <span>Practice Data Migration Hub</span>
            </div>
            <h1 className="text-2xl lg:text-3xl font-black tracking-tight text-white">
              Income Tax (ITR) Client Migration
            </h1>
            <p className="text-xs text-slate-300 mt-1 max-w-2xl leading-relaxed">
              Import all existing income tax clients, PAN registrations, default ITR-1 to ITR-7 forms, and historical CPC acknowledgment filings from your previous software in seconds.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <Link to="/itr">
              <Button variant="outline" className="bg-white/10 hover:bg-white/20 text-white border-white/20 text-xs">
                Back to ITR Compliance
              </Button>
            </Link>
            <Link to="/gst/migration">
              <Button variant="outline" className="bg-white/10 hover:bg-white/20 text-white border-white/20 text-xs">
                GST Migration Hub
              </Button>
            </Link>
          </div>
        </div>
      </div>

      {/* Tabs Navigation */}
      <div className="border-b border-slate-200 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <button
            onClick={() => {
              setActiveTab('PROFILES');
              setResult(null);
            }}
            className={clsx(
              'flex items-center gap-2 px-5 py-3 text-xs font-bold border-b-2 transition-all',
              activeTab === 'PROFILES'
                ? 'border-purple-600 text-purple-700 bg-purple-50/50 rounded-t-lg'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            )}
          >
            <UserCheck className="w-4 h-4" />
            <span>1. Client PANs & ITR Profiles Migration</span>
            <span className="ml-1.5 px-2 py-0.5 rounded-full bg-purple-100 text-purple-800 text-[10px]">
              {itrProfiles.length} Active
            </span>
          </button>

          <button
            onClick={() => {
              setActiveTab('RETURNS');
              setResult(null);
            }}
            className={clsx(
              'flex items-center gap-2 px-5 py-3 text-xs font-bold border-b-2 transition-all',
              activeTab === 'RETURNS'
                ? 'border-purple-600 text-purple-700 bg-purple-50/50 rounded-t-lg'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            )}
          >
            <FileSpreadsheet className="w-4 h-4" />
            <span>2. Historical ITR Returns & Acknowledgements</span>
          </button>
        </div>

        {/* Action Button: Quick Demo Fill */}
        <div>
          {activeTab === 'PROFILES' ? (
            <Button
              size="sm"
              variant="outline"
              onClick={handleQuickFillDemoProfiles}
              leftIcon={<Sparkles className="w-3.5 h-3.5 text-amber-500" />}
              className="text-xs border-amber-300 bg-amber-50/50 hover:bg-amber-100/50 text-amber-900"
            >
              Quick Fill Demo (8 Practice Taxpayers)
            </Button>
          ) : (
            <Button
              size="sm"
              variant="outline"
              onClick={handleQuickFillDemoReturns}
              leftIcon={<Sparkles className="w-3.5 h-3.5 text-amber-500" />}
              className="text-xs border-amber-300 bg-amber-50/50 hover:bg-amber-100/50 text-amber-900"
            >
              Quick Fill Demo (6 Historical Returns)
            </Button>
          )}
        </div>
      </div>

      {/* Migration Hub Success Summary Banner */}
      {result && (
        <Card className="border-emerald-200 bg-emerald-50/40 p-5">
          <div className="flex items-start gap-4">
            <div className="p-3 bg-emerald-500 text-white rounded-xl shadow-xs">
              <CheckCircle2 className="w-6 h-6" />
            </div>
            <div className="flex-1">
              <h3 className="text-sm font-bold text-emerald-950">
                ITR Data Migration Batch Completed!
              </h3>
              <p className="text-xs text-emerald-800 mt-1">
                Processed {result.totalProcessed} records —{' '}
                <strong className="text-emerald-900 font-black">{result.totalCreated} imported</strong>,{' '}
                {result.totalSkipped} existing duplicates skipped, and {result.totalFailed} failed.
              </p>

              {result.importedItems.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-1.5 max-h-24 overflow-y-auto p-2 bg-white/80 rounded-lg border border-emerald-100 text-[11px] font-mono">
                  {result.importedItems.map((item, idx) => (
                    <span key={idx} className="px-2 py-0.5 bg-emerald-100 text-emerald-800 rounded font-semibold">
                      ✓ {item}
                    </span>
                  ))}
                </div>
              )}

              {result.errors.length > 0 && (
                <div className="mt-3 space-y-1 text-xs text-rose-700 bg-rose-50/80 p-3 rounded-lg border border-rose-100 max-h-28 overflow-y-auto">
                  <div className="font-bold flex items-center gap-1.5">
                    <AlertCircle className="w-3.5 h-3.5" /> Notices & Warnings:
                  </div>
                  {result.errors.map((err, idx) => (
                    <div key={idx} className="font-mono text-[11px]">{err}</div>
                  ))}
                </div>
              )}

              <div className="mt-4 flex items-center gap-3">
                <Link to="/itr">
                  <Button size="sm" style={{ backgroundColor: currentTheme.primaryColor }}>
                    View ITR Compliance Hub
                  </Button>
                </Link>
                <Link to="/clients">
                  <Button size="sm" variant="outline">
                    View Onboarded Clients (360°)
                  </Button>
                </Link>
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* ========================================================================= */}
      {/* TAB 1: ITR PROFILES MIGRATION */}
      {/* ========================================================================= */}
      {activeTab === 'PROFILES' && (
        <div className="space-y-6">
          {/* Upload Dropzone & Templates */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <Card className="lg:col-span-2 p-6 flex flex-col justify-center items-center text-center border-dashed border-2 border-slate-300 hover:border-purple-500 transition-colors bg-slate-50/50">
              <input
                ref={profileInputRef}
                type="file"
                accept=".xlsx, .xls, .csv"
                onChange={handleProfileFileUpload}
                className="hidden"
              />
              <div className="p-4 bg-purple-100 text-purple-700 rounded-full mb-3">
                <UploadCloud className="w-8 h-8" />
              </div>
              <h3 className="text-sm font-bold text-slate-800">
                Upload Client ITR Profiles Spreadsheet
              </h3>
              <p className="text-xs text-slate-500 mt-1 max-w-sm">
                Drag and drop your Excel (.xlsx, .xls) or CSV file containing client PAN, Taxpayer Category, and Default ITR Form.
              </p>
              <div className="mt-4 flex items-center gap-2">
                <Button
                  onClick={() => profileInputRef.current?.click()}
                  size="sm"
                  style={{ backgroundColor: currentTheme.primaryColor }}
                  leftIcon={<FileSpreadsheet className="w-4 h-4" />}
                >
                  Select File from Computer
                </Button>
              </div>
              {profileFile && (
                <div className="mt-3 text-xs font-semibold text-purple-700 bg-purple-50 px-3 py-1 rounded-full border border-purple-200">
                  📁 Loaded: {profileFile.name} ({(profileFile.size / 1024).toFixed(1)} KB)
                </div>
              )}
            </Card>

            {/* Template Download Card */}
            <Card className="p-5 flex flex-col justify-between bg-purple-50/30 border-purple-200">
              <div>
                <div className="flex items-center gap-2 font-bold text-xs text-purple-900 mb-2">
                  <Download className="w-4 h-4" />
                  <span>Download Sample Spreadsheet</span>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  Use our standardized migration template with pre-configured headers, PAN formats, and ITR Form columns for effortless bulk import.
                </p>
                <div className="mt-4 space-y-2">
                  <a
                    href="/sample_itr_profiles_migration.csv"
                    download="Taxoryn_Sample_ITR_Profiles.csv"
                    className="flex items-center justify-between px-3 py-2 bg-white rounded-lg border border-purple-200 hover:border-purple-400 text-xs font-semibold text-purple-950 transition-colors"
                  >
                    <span>Sample CSV Template (.csv)</span>
                    <Download className="w-3.5 h-3.5 text-purple-600" />
                  </a>
                </div>
              </div>

              <div className="pt-4 border-t border-purple-200/60 mt-4 text-[11px] text-purple-800 flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 shrink-0 text-purple-600" />
                <span>Auto-creates Client entity if PAN is not already in your practice</span>
              </div>
            </Card>
          </div>

          {/* Validation & Preview Table */}
          {parsedProfiles.length > 0 && (
            <Card className="overflow-hidden border-slate-200">
              <div className="p-4 bg-slate-50 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                    Staging Preview ({parsedProfiles.length} Records)
                  </h3>
                  <p className="text-[11px] text-slate-500 mt-0.5">
                    {parsedProfiles.filter((r) => r.isValid).length} Valid •{' '}
                    {parsedProfiles.filter((r) => r.willAutoOnboard).length} New Clients to Auto-Onboard •{' '}
                    {parsedProfiles.filter((r) => !r.isValid).length} Invalid
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <Button
                    onClick={handleExecuteProfileMigration}
                    isLoading={isSubmitting}
                    disabled={parsedProfiles.filter((r) => r.isValid).length === 0}
                    style={{ backgroundColor: currentTheme.primaryColor }}
                    leftIcon={<Layers className="w-4 h-4" />}
                  >
                    Execute Bulk Import ({parsedProfiles.filter((r) => r.isValid).length} Profiles)
                  </Button>
                </div>
              </div>

              {isSubmitting && (
                <div className="p-4 bg-purple-50 border-b border-purple-200">
                  <div className="flex items-center justify-between text-xs font-bold text-purple-900 mb-1">
                    <span>Migrating ITR Client Profiles...</span>
                    <span>{progress}%</span>
                  </div>
                  <div className="w-full h-2 bg-purple-200 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-purple-600 transition-all duration-300 rounded-full"
                      style={{ width: `${progress}%` }}
                    />
                  </div>
                </div>
              )}

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-700">
                  <thead className="bg-slate-100 text-slate-600 font-bold border-b border-slate-200">
                    <tr>
                      <th className="px-4 py-3">PAN</th>
                      <th className="px-4 py-3">Client / Taxpayer Name</th>
                      <th className="px-4 py-3">Taxpayer Category</th>
                      <th className="px-4 py-3">Default ITR Form</th>
                      <th className="px-4 py-3">Residential Status</th>
                      <th className="px-4 py-3">Client Status</th>
                      <th className="px-4 py-3">Validation</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {parsedProfiles.map((row) => (
                      <tr key={row.id} className="hover:bg-slate-50/80">
                        <td className="px-4 py-2.5 font-mono font-bold text-slate-900">
                          {row.pan}
                        </td>
                        <td className="px-4 py-2.5 font-semibold text-slate-800">
                          {row.clientName}
                          {row.email && (
                            <span className="block text-[10px] text-slate-400 font-normal">{row.email}</span>
                          )}
                        </td>
                        <td className="px-4 py-2.5">
                          <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700 font-medium text-[11px]">
                            {row.taxpayerType}
                          </span>
                        </td>
                        <td className="px-4 py-2.5">
                          <span className="px-2 py-0.5 rounded bg-purple-50 text-purple-700 font-bold text-[11px] border border-purple-200">
                            {row.defaultItrType.replace('_', ' ')}
                          </span>
                        </td>
                        <td className="px-4 py-2.5 text-slate-600">
                          {row.residentialStatus}
                        </td>
                        <td className="px-4 py-2.5">
                          {row.willAutoOnboard ? (
                            <span className="inline-flex items-center gap-1 text-[11px] font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded border border-amber-200">
                              <Building2 className="w-3 h-3" /> Auto-Onboard Client
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                              <CheckCircle2 className="w-3 h-3" /> Matched: {row.matchedClient?.displayName}
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-2.5">
                          {row.isValid ? (
                            <span className="text-emerald-600 font-bold flex items-center gap-1">
                              <CheckCircle2 className="w-3.5 h-3.5" /> Valid
                            </span>
                          ) : (
                            <span className="text-rose-600 font-bold flex items-center gap-1 text-[11px]">
                              <AlertCircle className="w-3.5 h-3.5" /> {row.validationError}
                            </span>
                          )}
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

      {/* ========================================================================= */}
      {/* TAB 2: HISTORICAL RETURNS MIGRATION */}
      {/* ========================================================================= */}
      {activeTab === 'RETURNS' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <Card className="lg:col-span-2 p-6 flex flex-col justify-center items-center text-center border-dashed border-2 border-slate-300 hover:border-purple-500 transition-colors bg-slate-50/50">
              <input
                ref={returnInputRef}
                type="file"
                accept=".xlsx, .xls, .csv"
                onChange={handleReturnFileUpload}
                className="hidden"
              />
              <div className="p-4 bg-purple-100 text-purple-700 rounded-full mb-3">
                <FileSpreadsheet className="w-8 h-8" />
              </div>
              <h3 className="text-sm font-bold text-slate-800">
                Upload Historical ITR Returns & Acknowledgements
              </h3>
              <p className="text-xs text-slate-500 mt-1 max-w-sm">
                Upload spreadsheet containing past returns (AY 2026-27, 2025-26, 2024-25), ITR Form, Acknowledgement (ITR-V) number, and filing dates.
              </p>
              <div className="mt-4 flex items-center gap-2">
                <Button
                  onClick={() => returnInputRef.current?.click()}
                  size="sm"
                  style={{ backgroundColor: currentTheme.primaryColor }}
                  leftIcon={<UploadCloud className="w-4 h-4" />}
                >
                  Select File from Computer
                </Button>
              </div>
              {returnFile && (
                <div className="mt-3 text-xs font-semibold text-purple-700 bg-purple-50 px-3 py-1 rounded-full border border-purple-200">
                  📁 Loaded: {returnFile.name} ({(returnFile.size / 1024).toFixed(1)} KB)
                </div>
              )}
            </Card>

            <Card className="p-5 flex flex-col justify-between bg-purple-50/30 border-purple-200">
              <div>
                <div className="flex items-center gap-2 font-bold text-xs text-purple-900 mb-2">
                  <Download className="w-4 h-4" />
                  <span>Download Returns Template</span>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  Migrate historical ITR compliance tracking with e-Filing Acknowledgement Numbers and statutory completion dates.
                </p>
                <div className="mt-4 space-y-2">
                  <a
                    href="/sample_itr_returns_migration.csv"
                    download="Taxoryn_Sample_Historical_ITR_Returns.csv"
                    className="flex items-center justify-between px-3 py-2 bg-white rounded-lg border border-purple-200 hover:border-purple-400 text-xs font-semibold text-purple-950 transition-colors"
                  >
                    <span>Sample Returns CSV (.csv)</span>
                    <Download className="w-3.5 h-3.5 text-purple-600" />
                  </a>
                </div>
              </div>

              <div className="pt-4 border-t border-purple-200/60 mt-4 text-[11px] text-purple-800 flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 shrink-0 text-purple-600" />
                <span>Links directly to client ITR profile and populates compliance history</span>
              </div>
            </Card>
          </div>

          {/* Returns Preview Table */}
          {parsedReturns.length > 0 && (
            <Card className="overflow-hidden border-slate-200">
              <div className="p-4 bg-slate-50 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                    Historical Returns Preview ({parsedReturns.length} Records)
                  </h3>
                  <p className="text-[11px] text-slate-500 mt-0.5">
                    {parsedReturns.filter((r) => r.isValid).length} Valid •{' '}
                    {parsedReturns.filter((r) => r.acknowledgementNumber).length} With ITR-V Ack No •{' '}
                    {parsedReturns.filter((r) => !r.isValid).length} Invalid
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <Button
                    onClick={handleExecuteReturnMigration}
                    isLoading={isSubmitting}
                    disabled={parsedReturns.filter((r) => r.isValid).length === 0}
                    style={{ backgroundColor: currentTheme.primaryColor }}
                    leftIcon={<Layers className="w-4 h-4" />}
                  >
                    Execute Returns Import ({parsedReturns.filter((r) => r.isValid).length} Returns)
                  </Button>
                </div>
              </div>

              {isSubmitting && (
                <div className="p-4 bg-purple-50 border-b border-purple-200">
                  <div className="flex items-center justify-between text-xs font-bold text-purple-900 mb-1">
                    <span>Migrating Historical ITR Returns...</span>
                    <span>{progress}%</span>
                  </div>
                  <div className="w-full h-2 bg-purple-200 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-purple-600 transition-all duration-300 rounded-full"
                      style={{ width: `${progress}%` }}
                    />
                  </div>
                </div>
              )}

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-700">
                  <thead className="bg-slate-100 text-slate-600 font-bold border-b border-slate-200">
                    <tr>
                      <th className="px-4 py-3">PAN</th>
                      <th className="px-4 py-3">Taxpayer Name</th>
                      <th className="px-4 py-3">Assessment Year</th>
                      <th className="px-4 py-3">ITR Form</th>
                      <th className="px-4 py-3">Ack / ITR-V Number</th>
                      <th className="px-4 py-3">Filing Date</th>
                      <th className="px-4 py-3">Filing Status</th>
                      <th className="px-4 py-3">Validation</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {parsedReturns.map((row) => (
                      <tr key={row.id} className="hover:bg-slate-50/80">
                        <td className="px-4 py-2.5 font-mono font-bold text-slate-900">
                          {row.pan}
                        </td>
                        <td className="px-4 py-2.5 font-semibold text-slate-800">
                          {row.clientName}
                        </td>
                        <td className="px-4 py-2.5 font-mono font-bold text-purple-900">
                          AY {row.assessmentYear}
                        </td>
                        <td className="px-4 py-2.5">
                          <span className="px-2 py-0.5 rounded bg-purple-50 text-purple-700 font-bold text-[11px] border border-purple-200">
                            {row.itrType.replace('_', ' ')}
                          </span>
                        </td>
                        <td className="px-4 py-2.5 font-mono text-[11px]">
                          {row.acknowledgementNumber ? (
                            <span className="font-bold text-slate-800">{row.acknowledgementNumber}</span>
                          ) : (
                            <span className="text-slate-400 italic">None</span>
                          )}
                        </td>
                        <td className="px-4 py-2.5 font-mono text-slate-600">
                          {row.filingDate || '—'}
                        </td>
                        <td className="px-4 py-2.5">
                          <StatusBadge status={row.filingStatus} size="sm" />
                        </td>
                        <td className="px-4 py-2.5">
                          {row.isValid ? (
                            <span className="text-emerald-600 font-bold flex items-center gap-1">
                              <CheckCircle2 className="w-3.5 h-3.5" /> Valid
                            </span>
                          ) : (
                            <span className="text-rose-600 font-bold flex items-center gap-1 text-[11px]">
                              <AlertCircle className="w-3.5 h-3.5" /> {row.validationError}
                            </span>
                          )}
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
    </div>
  );
};
