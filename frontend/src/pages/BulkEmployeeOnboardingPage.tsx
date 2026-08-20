import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  Users,
  UploadCloud,
  FileSpreadsheet,
  Download,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  ShieldCheck,
  Building2,
  Briefcase,
  Mail,
  Phone,
  Sparkles,
  UserCheck,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { StatusBadge } from '../components/common/StatusBadge';
import { teamApi } from '../api/endpoints';
import { useBranding } from '../context/BrandingContext';
import { useAuth } from '../context/AuthContext';
import { Employee, BulkEmployeeImportResult } from '../types';
import clsx from 'clsx';

interface ParsedStaffRow {
  id: number;
  employeeCode: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  department: string;
  designation: string;
  status: 'ACTIVE' | 'INACTIVE' | 'ON_LEAVE' | 'TERMINATED';
  isValid: boolean;
  validationError?: string;
  isExisting?: boolean;
}

export const BulkEmployeeOnboardingPage: React.FC = () => {
  const [existingEmployees, setExistingEmployees] = useState<Employee[]>([]);
  const [parsedRows, setParsedRows] = useState<ParsedStaffRow[]>([]);
  const [csvFile, setCsvFile] = useState<File | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [progress, setProgress] = useState(0);
  const [result, setResult] = useState<BulkEmployeeImportResult | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const { currentTheme } = useBranding();
  const { practiceName } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    loadExistingStaff();
  }, []);

  const loadExistingStaff = async () => {
    try {
      setIsLoading(true);
      const res = await teamApi.getEmployees();
      setExistingEmployees(res.content || []);
    } catch (err) {
      console.error('Failed to load existing employees', err);
    } finally {
      setIsLoading(false);
    }
  };

  // CSV / Spreadsheet Parser
  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setCsvFile(file);

    const reader = new FileReader();
    reader.onload = (evt) => {
      try {
        const text = evt.target?.result as string;
        const lines = text.split(/\r\n|\n/).filter((l) => l.trim().length > 0);
        if (lines.length <= 1) return;

        // Clean & Normalize header columns
        const headerCols = lines[0].split(',').map((h) => h.trim().toLowerCase().replace(/[^a-z0-9]/g, ''));
        const codeIdx = headerCols.findIndex((h) => h.includes('code') || h.includes('id') || h.includes('emp'));
        const firstIdx = headerCols.findIndex((h) => h.includes('first') || h.includes('fname') || h.includes('name'));
        const lastIdx = headerCols.findIndex((h) => h.includes('last') || h.includes('lname') || h.includes('surname'));
        const emailIdx = headerCols.findIndex((h) => h.includes('email') || h.includes('mail'));
        const phoneIdx = headerCols.findIndex((h) => h.includes('phone') || h.includes('mob') || h.includes('contact'));
        const deptIdx = headerCols.findIndex((h) => h.includes('dept') || h.includes('department'));
        const desigIdx = headerCols.findIndex((h) => h.includes('desig') || h.includes('role') || h.includes('title'));
        const statusIdx = headerCols.findIndex((h) => h.includes('status'));

        const rows: ParsedStaffRow[] = [];
        const seenCodes = new Set<string>();
        const seenEmails = new Set<string>();

        for (let i = 1; i < lines.length; i++) {
          const cols = lines[i].split(',').map((c) => c.trim().replace(/^["']|["']$/g, ''));
          if (cols.length < 2 || cols.every((c) => c === '')) continue;

          const rawCode = (codeIdx >= 0 ? cols[codeIdx] : cols[0]) || `EMP-${100 + i}`;
          const employeeCode = rawCode.trim().toUpperCase();

          const firstName = (firstIdx >= 0 ? cols[firstIdx] : cols[1]) || '';
          const lastName = (lastIdx >= 0 && lastIdx !== firstIdx ? cols[lastIdx] : cols[2]) || '';
          const email = ((emailIdx >= 0 ? cols[emailIdx] : cols[3]) || '').toLowerCase().trim();
          const phone = (phoneIdx >= 0 ? cols[phoneIdx] : cols[4]) || '';
          const department = (deptIdx >= 0 ? cols[deptIdx] : cols[5]) || 'Taxation';
          const designation = (desigIdx >= 0 ? cols[desigIdx] : cols[6]) || 'Tax Associate';
          const statusRaw = ((statusIdx >= 0 ? cols[statusIdx] : cols[7]) || 'ACTIVE').toUpperCase();
          const status = (['ACTIVE', 'INACTIVE', 'ON_LEAVE', 'TERMINATED'].includes(statusRaw) ? statusRaw : 'ACTIVE') as any;

          // Validations
          let isValid = true;
          let validationError = '';

          const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
          if (!firstName || firstName.length < 2) {
            isValid = false;
            validationError = 'First name required';
          } else if (!email || !emailRegex.test(email)) {
            isValid = false;
            validationError = 'Valid email required';
          } else if (seenCodes.has(employeeCode)) {
            isValid = false;
            validationError = `Duplicate Code in file (${employeeCode})`;
          } else if (seenEmails.has(email)) {
            isValid = false;
            validationError = `Duplicate Email in file (${email})`;
          }

          seenCodes.add(employeeCode);
          seenEmails.add(email);

          // Check if already in DB
          const isExisting = existingEmployees.some(
            (e) => e.employeeCode?.toUpperCase() === employeeCode || e.email?.toLowerCase() === email
          );

          rows.push({
            id: i,
            employeeCode,
            firstName,
            lastName,
            email,
            phone,
            department,
            designation,
            status,
            isValid: isValid && !isExisting,
            validationError: isExisting ? 'Already Onboarded' : validationError,
            isExisting,
          });
        }
        setParsedRows(rows);
      } catch (err) {
        alert('Failed to parse employee spreadsheet.');
      }
    };
    reader.readAsText(file);
  };

  // Download Sample CSV Template
  const handleDownloadCsv = () => {
    const headers = 'Employee Code,First Name,Last Name,Email,Phone,Department,Designation,Status\n';
    const sampleRows = [
      'EMP-101,Rohan,Deshmukh,rohan.d@taxpractice.com,+919876543210,Taxation,Senior CA Partner / Practitioner,ACTIVE\n',
      'EMP-102,Priya,Sharma,priya.s@taxpractice.com,+919812345678,GST & Indirect Tax,GST Filing Specialist,ACTIVE\n',
      'EMP-103,Amit,Verma,amit.v@taxpractice.com,+919823456789,Audit & Assurance,Audit Manager,ACTIVE\n',
      'EMP-104,Sneha,Gupta,sneha.g@taxpractice.com,+919834567890,Direct Tax,Senior Tax Advocate,ACTIVE\n',
    ].join('');

    const blob = new Blob([headers + sampleRows], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', 'Taxoryn_Staff_Migration_Template.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Execute Bulk Onboarding
  const handleExecuteImport = async () => {
    const validRows = parsedRows.filter((r) => r.isValid);
    if (validRows.length === 0) {
      alert('No valid employee records to onboard.');
      return;
    }

    setIsSubmitting(true);
    setProgress(15);

    const payload = validRows.map((r) => ({
      employeeCode: r.employeeCode,
      firstName: r.firstName,
      lastName: r.lastName || undefined,
      email: r.email,
      phone: r.phone || undefined,
      department: r.department,
      designation: r.designation,
      status: r.status,
    }));

    try {
      // 1. Try High-Speed Bulk Endpoint
      const res = await teamApi.bulkImportEmployees(payload as any);
      setProgress(100);
      setResult(res);
    } catch (bulkErr) {
      // 2. Resilient Sequential Fallback
      console.warn('Batch employee endpoint fallback to sequential calls', bulkErr);

      const created: Employee[] = [];
      const errors: string[] = [];
      let skipped = 0;

      for (let i = 0; i < validRows.length; i++) {
        const item = validRows[i];
        try {
          const emp = await teamApi.createEmployee({
            employeeCode: item.employeeCode,
            firstName: item.firstName,
            lastName: item.lastName || undefined,
            email: item.email,
            phone: item.phone || undefined,
            department: item.department,
            designation: item.designation,
            status: item.status,
          });
          created.push(emp);
        } catch (err: any) {
          const msg = err.response?.data?.message || err.message;
          if (msg.toLowerCase().includes('already exists') || msg.toLowerCase().includes('duplicate')) {
            skipped++;
          } else {
            errors.push(`Row ${item.id} (${item.email}): ${msg}`);
          }
        }
        setProgress(Math.round(((i + 1) / validRows.length) * 100));
      }

      setResult({
        totalProcessed: validRows.length,
        totalCreated: created.length,
        totalFailed: errors.length,
        totalSkipped: skipped,
        createdEmployees: created,
        errors,
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-8 max-w-6xl mx-auto animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Link to="/team" className="text-xs text-slate-400 hover:text-slate-600 font-semibold">
              Team & Staff
            </Link>
            <span className="text-xs text-slate-300">/</span>
            <span className="text-xs font-bold text-slate-700">Bulk Onboarding Hub</span>
          </div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900 mt-1">
            Bulk Employee & Practitioner Onboarding Hub
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Migrate and onboard your practice CA partners, advocates, audit managers, and article assistants from spreadsheets.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Link to="/team">
            <Button variant="outline" size="sm">
              View Team Directory
            </Button>
          </Link>
        </div>
      </div>

      {!result ? (
        <div className="space-y-6">
          {/* Upload & Template Cards */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* File Dropzone */}
            <Card
              title="1. Upload Staff Spreadsheet"
              subtitle="Supports CSV or Excel files with employee details"
              className="lg:col-span-2"
            >
              <div
                onClick={() => fileInputRef.current?.click()}
                className={clsx(
                  'border-2 border-dashed rounded-2xl p-8 flex flex-col items-center justify-center cursor-pointer transition-all text-center',
                  csvFile ? 'border-brand-500 bg-brand-50/20' : 'border-slate-300 hover:border-slate-400 hover:bg-slate-50/50'
                )}
              >
                <input
                  type="file"
                  ref={fileInputRef}
                  accept=".csv, .txt, .xlsx"
                  onChange={handleFileUpload}
                  className="hidden"
                />
                <div className="w-12 h-12 rounded-xl bg-brand-50 text-brand-600 flex items-center justify-center mb-3">
                  <UploadCloud className="w-6 h-6" />
                </div>
                {csvFile ? (
                  <div>
                    <p className="font-bold text-sm text-slate-900">{csvFile.name}</p>
                    <p className="text-xs text-slate-500 mt-0.5">{parsedRows.length} staff records detected</p>
                  </div>
                ) : (
                  <div>
                    <p className="font-bold text-sm text-slate-800">Drag & drop your staff roster spreadsheet</p>
                    <p className="text-xs text-slate-400 mt-1">or click to browse from your computer</p>
                  </div>
                )}
              </div>
            </Card>

            {/* Template Download Card */}
            <Card
              title="Staff Migration Templates"
              subtitle="Formatted for tax practice hierarchy"
              className="lg:col-span-1"
            >
              <div className="space-y-3 text-xs">
                <p className="text-slate-600">
                  Pre-configured with CA partners, GST specialists, tax advocates, and trainees.
                </p>
                <div className="space-y-2 pt-1">
                  <a
                    href="/Taxoryn_Sample_Staff_Bulk_Upload.xlsx"
                    download="Taxoryn_Sample_Staff_Bulk_Upload.xlsx"
                    className="w-full inline-flex items-center justify-center gap-1.5 px-3 py-2 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-300 rounded-lg text-xs font-bold transition-colors shadow-2xs"
                  >
                    <FileSpreadsheet className="w-4 h-4 text-emerald-600" />
                    <span>Download Excel (.xlsx)</span>
                  </a>
                  <Button
                    variant="outline"
                    size="sm"
                    className="w-full"
                    leftIcon={<Download className="w-3.5 h-3.5" />}
                    onClick={handleDownloadCsv}
                  >
                    Download CSV (.csv)
                  </Button>
                </div>
              </div>
            </Card>
          </div>

          {/* Pre-Import Data Grid */}
          {parsedRows.length > 0 && (
            <Card
              title={
                <div className="flex items-center justify-between w-full">
                  <span>
                    2. Pre-Import Verification ({parsedRows.filter((r) => r.isValid).length}/{parsedRows.length} Ready to Onboard)
                  </span>
                </div>
              }
              subtitle="Review practitioner and staff credentials before creating accounts"
              action={
                <Button
                  onClick={handleExecuteImport}
                  isLoading={isSubmitting}
                  style={{ backgroundColor: currentTheme.primaryColor }}
                  rightIcon={<ArrowRight className="w-4 h-4" />}
                >
                  Onboard {parsedRows.filter((r) => r.isValid).length} Staff Members Now
                </Button>
              }
              noPadding
            >
              {isSubmitting && (
                <div className="p-4 bg-slate-50 border-b border-slate-200 space-y-2">
                  <p className="text-xs font-bold text-slate-800">
                    Onboarding team into {practiceName}... ({progress}%)
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
                      <th className="px-4 py-3">Employee Code</th>
                      <th className="px-4 py-3">Full Name</th>
                      <th className="px-4 py-3">Official Email</th>
                      <th className="px-4 py-3">Phone</th>
                      <th className="px-4 py-3">Department</th>
                      <th className="px-4 py-3">Designation / Role</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {parsedRows.map((row) => (
                      <tr
                        key={row.id}
                        className={clsx(
                          !row.isValid ? 'bg-rose-50/40' : 'hover:bg-slate-50'
                        )}
                      >
                        <td className="px-4 py-3">
                          {row.isValid ? (
                            <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200 inline-flex items-center gap-1">
                              <CheckCircle2 className="w-3 h-3" /> Ready
                            </span>
                          ) : (
                            <span className="text-[10px] font-bold text-rose-700 bg-rose-50 px-2 py-0.5 rounded-full border border-rose-200 inline-flex items-center gap-1">
                              <AlertCircle className="w-3 h-3" /> {row.validationError}
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3 font-mono font-bold text-slate-900">{row.employeeCode}</td>
                        <td className="px-4 py-3 font-bold text-slate-800">
                          {row.firstName} {row.lastName}
                        </td>
                        <td className="px-4 py-3 font-mono text-slate-700">{row.email}</td>
                        <td className="px-4 py-3 font-mono text-slate-500">{row.phone || '—'}</td>
                        <td className="px-4 py-3">
                          <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                            {row.department}
                          </span>
                        </td>
                        <td className="px-4 py-3 font-medium text-slate-900">{row.designation}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          )}
        </div>
      ) : (
        /* Summary Report */
        <Card
          title="Staff Onboarding Summary Report"
          subtitle={`Practitioners and team onboarded into ${practiceName}`}
        >
          <div className="space-y-6">
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
              <div className="p-4 bg-blue-50 border border-blue-200 rounded-xl text-center">
                <span className="text-xs font-bold text-blue-700 uppercase">Total Attempted</span>
                <p className="text-3xl font-black text-blue-900 mt-1">{result.totalProcessed}</p>
              </div>
              <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-center">
                <span className="text-xs font-bold text-emerald-700 uppercase">Successfully Onboarded</span>
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
                  setParsedRows([]);
                  setCsvFile(null);
                  loadExistingStaff();
                }}
              >
                Onboard More Staff
              </Button>
              <Button
                onClick={() => navigate('/team')}
                style={{ backgroundColor: currentTheme.primaryColor }}
                rightIcon={<ArrowRight className="w-4 h-4" />}
              >
                Go to Team Management Directory
              </Button>
            </div>
          </div>
        </Card>
      )}
    </div>
  );
};
