import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  CheckSquare,
  Sparkles,
  Calendar,
  Users,
  UploadCloud,
  FileSpreadsheet,
  Download,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  ShieldCheck,
  Building2,
  Layers,
  Clock,
  UserCheck,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { StatusBadge } from '../components/common/StatusBadge';
import { clientApi, taskApi, teamApi, employeeApi } from '../api/endpoints';
import { useBranding } from '../context/BrandingContext';
import { useAuth } from '../context/AuthContext';
import { Client, Employee, Task, BulkTaskImportResult } from '../types';
import { parseSpreadsheetToRows } from '../utils/spreadsheetParser';
import clsx from 'clsx';

interface TaskTemplatePreset {
  id: string;
  name: string;
  category: 'GST' | 'ITR' | 'AUDIT' | 'COMPLIANCE' | 'BILLING' | 'OTHER';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  defaultTitle: string;
  description: string;
  recommendedClientTypes: string[];
}

const TEMPLATE_PRESETS: TaskTemplatePreset[] = [
  {
    id: 'gstr-3b',
    name: 'GSTR-3B Monthly Return & Tax Payment',
    category: 'GST',
    priority: 'HIGH',
    defaultTitle: 'GSTR-3B Monthly Return & Tax Payment',
    description: 'Reconciliation of GSTR-2B ITC, tax liability computation, challan creation, and monthly GSTR-3B filing.',
    recommendedClientTypes: ['PRIVATE_LIMITED', 'PUBLIC_LIMITED', 'LLP', 'PROPRIETORSHIP', 'PARTNERSHIP'],
  },
  {
    id: 'gstr-1',
    name: 'GSTR-1 Monthly Outward Supplies',
    category: 'GST',
    priority: 'HIGH',
    defaultTitle: 'GSTR-1 Outward Supplies Filing',
    description: 'B2B sales invoice upload, B2CS summary, debit/credit note reconciliation, and GSTR-1 submission.',
    recommendedClientTypes: ['PRIVATE_LIMITED', 'PUBLIC_LIMITED', 'LLP', 'PROPRIETORSHIP', 'PARTNERSHIP'],
  },
  {
    id: 'tds-26q',
    name: 'TDS 26Q Quarterly Return Filing',
    category: 'COMPLIANCE',
    priority: 'HIGH',
    defaultTitle: 'Quarterly TDS 26Q Non-Salary Filing',
    description: 'Verification of challans, deductee pan validation, FVU utility validation, and quarterly 26Q submission.',
    recommendedClientTypes: ['PRIVATE_LIMITED', 'PUBLIC_LIMITED', 'LLP'],
  },
  {
    id: 'itr-corporate',
    name: 'Annual ITR-6 Corporate Tax Return',
    category: 'ITR',
    priority: 'URGENT',
    defaultTitle: 'ITR-6 Company Tax Return Filing AY 2026-27',
    description: 'Computation of total taxable income, MAT computation, balance sheet disclosures, and e-verification.',
    recommendedClientTypes: ['PRIVATE_LIMITED', 'PUBLIC_LIMITED'],
  },
  {
    id: 'itr-individual',
    name: 'ITR-1 / ITR-2 Individual & Salaried Tax Return',
    category: 'ITR',
    priority: 'MEDIUM',
    defaultTitle: 'ITR Individual Return Filing AY 2026-27',
    description: 'AIS/TIS reconciliation, 26AS TDS credits match, capital gains computation, and Form 16 verification.',
    recommendedClientTypes: ['INDIVIDUAL', 'HUF'],
  },
  {
    id: 'advance-tax',
    name: 'Quarterly Advance Tax Installment',
    category: 'COMPLIANCE',
    priority: 'HIGH',
    defaultTitle: 'Advance Tax Installment Computation & Payment',
    description: 'Estimated annual profits projection, section 208 liability calculation, and challan payment generation.',
    recommendedClientTypes: ['PRIVATE_LIMITED', 'LLP', 'INDIVIDUAL', 'PROPRIETORSHIP'],
  },
  {
    id: 'tax-audit',
    name: 'Tax Audit U/S 44AB Preparation',
    category: 'AUDIT',
    priority: 'URGENT',
    defaultTitle: 'Tax Audit Form 3CA/3CD Preparation & Verification',
    description: 'Detailed clause-by-clause 3CD disclosures, ledger vouchers verification, and CA digital signature.',
    recommendedClientTypes: ['PRIVATE_LIMITED', 'PUBLIC_LIMITED', 'LLP', 'PROPRIETORSHIP'],
  },
];

export const BulkTasksGeneratorPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'GENERATOR' | 'CSV_IMPORT'>('GENERATOR');
  const [clients, setClients] = useState<Client[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [selectedClientIds, setSelectedClientIds] = useState<string[]>([]);
  const [clientTypeFilter, setClientTypeFilter] = useState<string>('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [progress, setProgress] = useState(0);
  const [result, setResult] = useState<BulkTaskImportResult | null>(null);

  // Generator Form State
  const [taskTitle, setTaskTitle] = useState('GSTR-3B Monthly Return & Tax Payment');
  const [taskCategory, setTaskCategory] = useState<'GST' | 'ITR' | 'AUDIT' | 'COMPLIANCE' | 'BILLING' | 'OTHER'>('GST');
  const [taskPriority, setTaskPriority] = useState<'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'>('HIGH');
  const [dueDate, setDueDate] = useState(() => {
    const d = new Date();
    d.setDate(20);
    return d.toISOString().split('T')[0];
  });
  const [assignedEmployeeId, setAssignedEmployeeId] = useState<string>('');
  const [description, setDescription] = useState('Reconciliation of GSTR-2B ITC, tax liability computation, and monthly filing.');

  // CSV Importer State
  const [csvFile, setCsvFile] = useState<File | null>(null);
  const [parsedCsvTasks, setParsedCsvTasks] = useState<any[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { currentTheme } = useBranding();
  const { practiceName, user } = useAuth();
  const navigate = useNavigate();

  const currentEmployee = useMemo(() => {
    return employees.find(
      (e) =>
        (e.email && user?.email && e.email.toLowerCase() === user.email.toLowerCase()) ||
        (user?.id && (e as any).userId === user.id)
    );
  }, [employees, user]);

  const assigneeOptions = useMemo(() => {
    const list: Array<{ id: string; name: string; isMe: boolean; designation: string; department?: string }> = [];
    if (user && !currentEmployee) {
      const myName = `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email;
      list.push({
        id: user.id,
        name: myName,
        isMe: true,
        designation: 'Practice Partner / Admin',
        department: 'Practice Management',
      });
    }
    employees.forEach((emp) => {
      const isMe = Boolean(
        (emp.email && user?.email && emp.email.toLowerCase() === user.email.toLowerCase()) ||
        (user?.id && (emp as any).userId === user.id)
      );
      const name = emp.fullName || `${emp.firstName || ''} ${emp.lastName || ''}`.trim() || emp.email;
      list.push({
        id: emp.id,
        name,
        isMe,
        designation: emp.designation || 'Staff',
        department: emp.department || 'Tax',
      });
    });
    return list;
  }, [employees, user, currentEmployee]);

  useEffect(() => {
    loadPrerequisites();
  }, []);

  const loadPrerequisites = async () => {
    try {
      setIsLoading(true);
      const [clientRes, empRes] = await Promise.allSettled([
        clientApi.getAll({ size: 200, status: 'ACTIVE' }),
        employeeApi.getAll({ size: 200 }),
      ]);
      if (clientRes.status === 'fulfilled' && clientRes.value) {
        const cList = Array.isArray(clientRes.value) ? clientRes.value : (clientRes.value?.content || []);
        setClients(cList);
        setSelectedClientIds(cList.map((c: Client) => c.id));
      }
      if (empRes.status === 'fulfilled' && empRes.value) {
        const eList = Array.isArray(empRes.value) ? empRes.value : (empRes.value?.content || []);
        setEmployees(eList);
      }
    } catch (err) {
      console.error('Failed to load clients and staff', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleApplyPreset = (preset: TaskTemplatePreset) => {
    setTaskTitle(preset.defaultTitle);
    setTaskCategory(preset.category);
    setTaskPriority(preset.priority);
    setDescription(preset.description);

    // Auto-select clients matching recommended types
    const matching = clients.filter((c) => preset.recommendedClientTypes.includes(c.clientType));
    if (matching.length > 0) {
      setSelectedClientIds(matching.map((c) => c.id));
    }
  };

  const filteredClients = clients.filter((c) => {
    if (clientTypeFilter === 'ALL') return true;
    return c.clientType === clientTypeFilter;
  });

  const toggleSelectAll = () => {
    if (selectedClientIds.length === filteredClients.length) {
      setSelectedClientIds([]);
    } else {
      setSelectedClientIds(filteredClients.map((c) => c.id));
    }
  };

  const toggleClientSelection = (id: string) => {
    setSelectedClientIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  // 1-Click Multi-Client Generator with Resilient Fallback
  const handleExecuteGenerator = async () => {
    if (selectedClientIds.length === 0) {
      alert('Please select at least one client to generate tasks for.');
      return;
    }
    if (!taskTitle.trim()) {
      alert('Task title is required.');
      return;
    }

    setIsSubmitting(true);
    setProgress(15);

    const payload = {
      clientIds: selectedClientIds,
      assignedTo: assignedEmployeeId || undefined,
      title: taskTitle.trim(),
      description: description.trim() || undefined,
      taskCategory,
      priority: taskPriority,
      dueDate: dueDate || undefined,
    };

    try {
      // 1. Try High-Speed Bulk Endpoint
      const res = await taskApi.generateBulk(payload);
      setProgress(100);
      setResult(res);
    } catch (bulkErr) {
      // 2. Fallback to resilient individual creation
      console.warn('Batch task generator endpoint fallback to sequential calls', bulkErr);

      const created: Task[] = [];
      const errors: string[] = [];

      for (let i = 0; i < selectedClientIds.length; i++) {
        const cId = selectedClientIds[i];
        try {
          const t = await taskApi.create({
            clientId: cId,
            assignedTo: assignedEmployeeId || undefined,
            title: taskTitle.trim(),
            description: description.trim() || undefined,
            taskCategory: taskCategory,
            category: taskCategory,
            priority: taskPriority,
            dueDate,
          } as any);
          created.push(t);
        } catch (err: any) {
          errors.push(`Client ${cId}: ${err.response?.data?.message || err.message}`);
        }
        setProgress(Math.round(((i + 1) / selectedClientIds.length) * 100));
      }

      setResult({
        totalProcessed: selectedClientIds.length,
        totalCreated: created.length,
        totalFailed: errors.length,
        createdTasks: created,
        errors,
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  // Download Task CSV Template
  const handleDownloadTaskTemplate = () => {
    const headers = 'Client PAN,Task Title,Category,Priority,Due Date (YYYY-MM-DD),Description\n';
    const sampleRows = [
      'AAACZ1234D,GSTR-3B Monthly Return Filing,GST,HIGH,2026-08-20,Monthly ITC match and tax payment\n',
      'AAALB5678E,Quarterly TDS 26Q Return,COMPLIANCE,HIGH,2026-07-31,Q1 TDS non-salary filing\n',
      'ABCPJ9876M,Annual ITR-2 Filing AY 2026-27,ITR,URGENT,2026-07-31,Individual income tax return\n',
    ].join('');

    const blob = new Blob([headers + sampleRows], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', 'Taxoryn_Task_Bulk_Template.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Parse CSV or Excel (.xlsx / .xls) Tasks with Intelligent PAN Match & Auto-Onboard Support
  const handleCsvUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setCsvFile(file);

    try {
      const rowsMatrix = await parseSpreadsheetToRows(file);
      if (rowsMatrix.length <= 1) {
        alert('File is empty or missing headers');
        return;
      }

      // Extract and clean headers
      const headerCols = rowsMatrix[0].map((h) => h.toLowerCase().replace(/[^a-z0-9]/g, ''));
      const panIdx = headerCols.findIndex((h) => h.includes('pan') || h.includes('client'));
      const titleIdx = headerCols.findIndex((h) => h.includes('title') || h.includes('task'));
      const catIdx = headerCols.findIndex((h) => h.includes('cat'));
      const prioIdx = headerCols.findIndex((h) => h.includes('prio'));
      const dueIdx = headerCols.findIndex((h) => h.includes('due') || h.includes('date'));
      const descIdx = headerCols.findIndex((h) => h.includes('desc') || h.includes('note'));

      const tasks: any[] = [];
      for (let i = 1; i < rowsMatrix.length; i++) {
        const cols = rowsMatrix[i];
        if (cols.length < 2 || cols.every((c) => c.trim() === '')) continue;

        const rawPan = (panIdx >= 0 ? cols[panIdx] : cols[0]) || '';
        const cleanPan = rawPan.replace(/[^A-Z0-9]/gi, '').toUpperCase().trim();
        const title = (titleIdx >= 0 ? cols[titleIdx] : cols[1]) || 'Compliance Filing';
        const cat = ((catIdx >= 0 ? cols[catIdx] : cols[2]) || 'GST').toUpperCase();
        const prio = ((prioIdx >= 0 ? cols[prioIdx] : cols[3]) || 'HIGH').toUpperCase();
        const due = (dueIdx >= 0 ? cols[dueIdx] : cols[4]) || new Date().toISOString().split('T')[0];
        const desc = (descIdx >= 0 ? cols[descIdx] : cols[5]) || '';

        // Match with existing client by PAN
        const matchedClient = clients.find(
          (c) => c.pan?.replace(/[^A-Z0-9]/gi, '').toUpperCase().trim() === cleanPan
        );

        const isValidPanFormat = /^[A-Z]{5}[0-9]{4}[A-Z]{1}$/.test(cleanPan) || cleanPan.length >= 8;

        tasks.push({
          id: i,
          pan: cleanPan,
          matchedClient: matchedClient || null,
          willAutoOnboard: !matchedClient && isValidPanFormat,
          title,
          category: cat,
          priority: prio,
          dueDate: due,
          description: desc,
          isValid: (!!matchedClient || isValidPanFormat) && title.length >= 2,
        });
      }
      setParsedCsvTasks(tasks);
    } catch (err) {
      alert('Failed to parse task spreadsheet file.');
    }
  };

  // Execute CSV Task Import with Auto-Onboarding Fallback
  const handleImportCsvTasks = async () => {
    const valid = parsedCsvTasks.filter((t) => t.isValid);
    if (valid.length === 0) {
      alert('No valid tasks to import. Please check task format.');
      return;
    }

    setIsSubmitting(true);
    setProgress(15);

    const created: Task[] = [];
    const errors: string[] = [];

    // Cache created client IDs to avoid duplicate creation within the same batch
    const panToClientIdMap: Record<string, string> = {};
    clients.forEach((c) => {
      if (c.pan) panToClientIdMap[c.pan.toUpperCase().trim()] = c.id;
    });

    for (let i = 0; i < valid.length; i++) {
      const item = valid[i];
      try {
        let clientId = item.matchedClient?.id || panToClientIdMap[item.pan];

        // If client doesn't exist yet, auto-create client record
        if (!clientId && item.pan) {
          try {
            const newClient = await clientApi.create({
              displayName: `Client (${item.pan})`,
              pan: item.pan,
              clientType: 'PRIVATE_LIMITED',
              status: 'ACTIVE',
            });
            clientId = newClient.id;
            panToClientIdMap[item.pan] = newClient.id;
          } catch (createClientErr: any) {
            console.warn('Auto-create client fallback', createClientErr);
          }
        }

        const normalizeCategory = (cat: string) => {
          const c = (cat || '').toUpperCase().trim();
          if (['GST', 'ITR', 'AUDIT', 'COMPLIANCE', 'BILLING', 'OTHER'].includes(c)) return c;
          return 'OTHER';
        };

        const normalizePriority = (prio: string) => {
          const p = (prio || '').toUpperCase().trim();
          if (['LOW', 'MEDIUM', 'HIGH', 'URGENT'].includes(p)) return p;
          return 'MEDIUM';
        };

        const taskPayload: any = {
          clientId: clientId || undefined,
          title: item.title.trim(),
          taskCategory: normalizeCategory(item.category),
          category: normalizeCategory(item.category),
          priority: normalizePriority(item.priority),
          dueDate: item.dueDate ? item.dueDate.trim() : undefined,
          description: item.description ? item.description.trim() : undefined,
        };

        const t = await taskApi.create(taskPayload);
        created.push(t);
      } catch (err: any) {
        errors.push(`Row ${item.id} (${item.title}): ${err.response?.data?.message || err.message}`);
      }
      setProgress(Math.round(((i + 1) / valid.length) * 100));
    }

    setResult({
      totalProcessed: valid.length,
      totalCreated: created.length,
      totalFailed: errors.length,
      createdTasks: created,
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
            <Link to="/tasks" className="text-xs text-slate-400 hover:text-slate-600 font-semibold">
              Tasks & Workflow
            </Link>
            <span className="text-xs text-slate-300">/</span>
            <span className="text-xs font-bold text-slate-700">Bulk Task Generator</span>
          </div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900 mt-1">
            Bulk Task Generator & Compliance Hub
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Generate monthly/quarterly statutory filing tasks for multiple clients in 1 click or import task spreadsheets.
          </p>
        </div>

        {/* Mode Selector Tabs */}
        <div className="inline-flex items-center gap-1 p-1 bg-slate-100 border border-slate-200 rounded-xl text-xs font-semibold">
          <button
            onClick={() => {
              setActiveTab('GENERATOR');
              setResult(null);
            }}
            className={clsx(
              'px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5',
              activeTab === 'GENERATOR' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
            )}
          >
            <Sparkles className="w-3.5 h-3.5 text-brand-600" />
            <span>Multi-Client Generator</span>
          </button>
          <button
            onClick={() => {
              setActiveTab('CSV_IMPORT');
              setResult(null);
            }}
            className={clsx(
              'px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5',
              activeTab === 'CSV_IMPORT' ? 'bg-white text-slate-900 shadow-2xs font-bold' : 'text-slate-500 hover:text-slate-700'
            )}
          >
            <FileSpreadsheet className="w-3.5 h-3.5 text-emerald-600" />
            <span>Spreadsheet Import</span>
          </button>
        </div>
      </div>

      {/* Mode A: Multi-Client Task Generator */}
      {activeTab === 'GENERATOR' && !result && (
        <div className="space-y-6">
          {/* Preset Templates */}
          <div>
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-2.5">
              1. Choose Compliance Cycle Preset (Or Custom Template)
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-3">
              {TEMPLATE_PRESETS.map((preset) => (
                <div
                  key={preset.id}
                  onClick={() => handleApplyPreset(preset)}
                  className={clsx(
                    'p-3.5 rounded-xl border cursor-pointer transition-all hover:shadow-2xs bg-white group',
                    taskTitle === preset.defaultTitle
                      ? 'border-brand-600 ring-2 ring-brand-500/20 bg-slate-50/80'
                      : 'border-slate-200 hover:border-slate-300'
                  )}
                >
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 border border-slate-200">
                      {preset.category}
                    </span>
                    <span className="text-[10px] font-semibold text-rose-600">{preset.priority}</span>
                  </div>
                  <h4 className="text-xs font-bold text-slate-900 mt-2 line-clamp-2">{preset.name}</h4>
                  <p className="text-[10px] text-slate-400 mt-1 line-clamp-2">{preset.description}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Form & Client Selection Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Task Configuration Form */}
            <Card
              title="2. Configure Task Parameters"
              subtitle="Applied across all selected clients"
              className="lg:col-span-1"
            >
              <div className="space-y-4 text-xs">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Task Title Template <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={taskTitle}
                    onChange={(e) => setTaskTitle(e.target.value)}
                    className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Category</label>
                    <select
                      value={taskCategory}
                      onChange={(e) => setTaskCategory(e.target.value as any)}
                      className="w-full text-xs px-2.5 py-2 border border-slate-200 rounded-lg bg-white"
                    >
                      <option value="GST">GST</option>
                      <option value="ITR">ITR</option>
                      <option value="AUDIT">AUDIT</option>
                      <option value="COMPLIANCE">COMPLIANCE</option>
                      <option value="BILLING">BILLING</option>
                      <option value="OTHER">OTHER</option>
                    </select>
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Priority</label>
                    <select
                      value={taskPriority}
                      onChange={(e) => setTaskPriority(e.target.value as any)}
                      className="w-full text-xs px-2.5 py-2 border border-slate-200 rounded-lg bg-white"
                    >
                      <option value="LOW">LOW</option>
                      <option value="MEDIUM">MEDIUM</option>
                      <option value="HIGH">HIGH</option>
                      <option value="URGENT">URGENT</option>
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Statutory Due Date</label>
                    <input
                      type="date"
                      value={dueDate}
                      onChange={(e) => setDueDate(e.target.value)}
                      className="w-full text-xs px-2.5 py-2 border border-slate-200 rounded-lg"
                    />
                  </div>

                  <div>
                    <div className="flex items-center justify-between mb-1">
                      <label className="block font-semibold text-slate-700">Assign Staff / Admin</label>
                      {currentEmployee ? (
                        <button
                          type="button"
                          onClick={() => setAssignedEmployeeId(currentEmployee.id)}
                          className="text-[11px] text-brand-600 hover:text-brand-800 font-bold"
                        >
                          ⚡ Assign to Me
                        </button>
                      ) : user ? (
                        <button
                          type="button"
                          onClick={() => setAssignedEmployeeId(user.id)}
                          className="text-[11px] text-brand-600 hover:text-brand-800 font-bold"
                        >
                          ⚡ Assign to Me
                        </button>
                      ) : null}
                    </div>
                    <select
                      value={assignedEmployeeId}
                      onChange={(e) => setAssignedEmployeeId(e.target.value)}
                      className="w-full text-xs px-2.5 py-2 border border-slate-200 rounded-lg bg-white"
                    >
                      <option value="">-- Unassigned / General Pool --</option>
                      {assigneeOptions.map((opt) => (
                        <option key={opt.id} value={opt.id}>
                          {opt.name} {opt.isMe ? '⭐ (You)' : ''} — {opt.designation} ({opt.department})
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Instructions / Description</label>
                  <textarea
                    rows={3}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg focus:ring-2 focus:ring-brand-500 focus:outline-none"
                  />
                </div>

                <div className="pt-3 border-t border-slate-100">
                  <Button
                    onClick={handleExecuteGenerator}
                    isLoading={isSubmitting}
                    className="w-full"
                    style={{ backgroundColor: currentTheme.primaryColor }}
                    leftIcon={<Sparkles className="w-4 h-4" />}
                  >
                    Generate {selectedClientIds.length} Tasks Now
                  </Button>
                </div>
              </div>
            </Card>

            {/* Client Multi-Select Table */}
            <Card
              title={
                <div className="flex items-center justify-between w-full">
                  <span>3. Select Target Clients ({selectedClientIds.length}/{filteredClients.length})</span>
                  <button
                    onClick={toggleSelectAll}
                    className="text-xs font-bold text-brand-600 hover:underline"
                  >
                    {selectedClientIds.length === filteredClients.length ? 'Deselect All' : 'Select All'}
                  </button>
                </div>
              }
              subtitle="Filter by legal constitution or manually pick clients"
              className="lg:col-span-2"
              action={
                <div className="flex items-center gap-1.5">
                  <select
                    value={clientTypeFilter}
                    onChange={(e) => setClientTypeFilter(e.target.value)}
                    className="text-xs px-2.5 py-1 border border-slate-200 rounded-lg bg-white font-semibold"
                  >
                    <option value="ALL">All Entity Types</option>
                    <option value="PRIVATE_LIMITED">Private Limited</option>
                    <option value="PUBLIC_LIMITED">Public Limited</option>
                    <option value="LLP">LLP</option>
                    <option value="PROPRIETORSHIP">Proprietorship</option>
                    <option value="INDIVIDUAL">Individual</option>
                    <option value="TRUST">Trust</option>
                    <option value="HUF">HUF</option>
                  </select>
                </div>
              }
              noPadding
            >
              {isSubmitting && (
                <div className="p-4 bg-slate-50 border-b border-slate-200 space-y-2">
                  <p className="text-xs font-bold text-slate-800">
                    Creating bulk workflow tasks in {practiceName}... ({progress}%)
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
                      <th className="px-4 py-3 w-10">
                        <input
                          type="checkbox"
                          checked={selectedClientIds.length === filteredClients.length && filteredClients.length > 0}
                          onChange={toggleSelectAll}
                          className="rounded text-brand-600 focus:ring-brand-500"
                        />
                      </th>
                      <th className="px-4 py-3">Client / Business Name</th>
                      <th className="px-4 py-3">PAN</th>
                      <th className="px-4 py-3">GSTIN</th>
                      <th className="px-4 py-3">Type</th>
                      <th className="px-4 py-3 text-right">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {filteredClients.map((client) => {
                      const isSelected = selectedClientIds.includes(client.id);

                      return (
                        <tr
                          key={client.id}
                          onClick={() => toggleClientSelection(client.id)}
                          className={clsx(
                            'cursor-pointer transition-colors',
                            isSelected ? 'bg-brand-50/30 hover:bg-brand-50/50' : 'hover:bg-slate-50'
                          )}
                        >
                          <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                            <input
                              type="checkbox"
                              checked={isSelected}
                              onChange={() => toggleClientSelection(client.id)}
                              className="rounded text-brand-600 focus:ring-brand-500"
                            />
                          </td>
                          <td className="px-4 py-3 font-bold text-slate-900">{client.displayName}</td>
                          <td className="px-4 py-3 font-mono font-semibold text-slate-700">{client.pan}</td>
                          <td className="px-4 py-3 font-mono text-slate-500">{client.gstin || '—'}</td>
                          <td className="px-4 py-3 text-[10px] font-semibold text-slate-600 uppercase">
                            {client.clientType.replace('_', ' ')}
                          </td>
                          <td className="px-4 py-3 text-right">
                            <StatusBadge status={client.status} size="sm" />
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </Card>
          </div>
        </div>
      )}

      {/* Mode B: CSV Task Importer */}
      {activeTab === 'CSV_IMPORT' && !result && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <Card
              title="1. Upload Task Spreadsheet"
              subtitle="Supports CSV files with Client PAN matching"
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
                  accept=".csv, .txt"
                  onChange={handleCsvUpload}
                  className="hidden"
                />
                <div className="w-12 h-12 rounded-xl bg-brand-50 text-brand-600 flex items-center justify-center mb-3">
                  <UploadCloud className="w-6 h-6" />
                </div>
                {csvFile ? (
                  <div>
                    <p className="font-bold text-sm text-slate-900">{csvFile.name}</p>
                    <p className="text-xs text-slate-500 mt-0.5">{parsedCsvTasks.length} tasks detected</p>
                  </div>
                ) : (
                  <div>
                    <p className="font-bold text-sm text-slate-800">Drag & drop your tasks spreadsheet</p>
                    <p className="text-xs text-slate-400 mt-1">or click to browse</p>
                  </div>
                )}
              </div>
            </Card>

            <Card
              title="Task Import Templates"
              subtitle="Pre-populated with sample practice tasks"
              className="lg:col-span-1"
            >
              <div className="space-y-3 text-xs">
                <p className="text-slate-600">
                  Tasks are automatically mapped to clients using their <strong>10-digit PAN number</strong>.
                </p>
                <div className="space-y-2 pt-1">
                  <a
                    href="/Taxoryn_Sample_Tasks_Bulk_Upload.xlsx"
                    download="Taxoryn_Sample_Tasks_Bulk_Upload.xlsx"
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
                    onClick={handleDownloadTaskTemplate}
                  >
                    Download CSV (.csv)
                  </Button>
                </div>
              </div>
            </Card>
          </div>

          {/* Parsed CSV Verification */}
          {parsedCsvTasks.length > 0 && (
            <Card
              title={`2. Pre-Import Verification (${parsedCsvTasks.filter((t) => t.isValid).length} Valid Tasks)`}
              subtitle="Review before importing into practice workflow"
              action={
                <Button
                  onClick={handleImportCsvTasks}
                  isLoading={isSubmitting}
                  style={{ backgroundColor: currentTheme.primaryColor }}
                  rightIcon={<ArrowRight className="w-4 h-4" />}
                >
                  Import Valid Tasks Now
                </Button>
              }
              noPadding
            >
              <div className="overflow-x-auto max-h-96">
                <table className="w-full text-left text-xs border-collapse">
                  <thead className="sticky top-0 bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase text-[10px]">
                    <tr>
                      <th className="px-4 py-3">Status</th>
                      <th className="px-4 py-3">Matched Client</th>
                      <th className="px-4 py-3">PAN</th>
                      <th className="px-4 py-3">Task Title</th>
                      <th className="px-4 py-3">Category</th>
                      <th className="px-4 py-3">Priority</th>
                      <th className="px-4 py-3">Due Date</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {parsedCsvTasks.map((t) => (
                      <tr key={t.id} className={!t.isValid ? 'bg-rose-50/40' : 'hover:bg-slate-50'}>
                        <td className="px-4 py-3">
                          {t.matchedClient ? (
                            <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200 inline-flex items-center gap-1">
                              <CheckCircle2 className="w-3 h-3" /> Matched
                            </span>
                          ) : t.willAutoOnboard ? (
                            <span className="text-[10px] font-bold text-blue-700 bg-blue-50 px-2 py-0.5 rounded-full border border-blue-200 inline-flex items-center gap-1">
                              <Sparkles className="w-3 h-3 text-blue-600" /> Auto-Link Client
                            </span>
                          ) : (
                            <span className="text-[10px] font-bold text-rose-700 bg-rose-50 px-2 py-0.5 rounded-full border border-rose-200">
                              Invalid Format
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3 font-bold text-slate-900">
                          {t.matchedClient?.displayName ? (
                            <span>{t.matchedClient.displayName}</span>
                          ) : (
                            <span className="text-blue-700 font-semibold italic">New Client ({t.pan})</span>
                          )}
                        </td>
                        <td className="px-4 py-3 font-mono font-bold text-slate-700">{t.pan}</td>
                        <td className="px-4 py-3 font-semibold text-slate-800">{t.title}</td>
                        <td className="px-4 py-3 text-[10px] font-semibold uppercase">{t.category}</td>
                        <td className="px-4 py-3 text-[10px] font-semibold uppercase">{t.priority}</td>
                        <td className="px-4 py-3 font-mono text-slate-600">{t.dueDate || '—'}</td>
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
          title="Bulk Task Creation Summary Report"
          subtitle={`Workflow tasks generated for ${practiceName}`}
        >
          <div className="space-y-6">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="p-4 bg-blue-50 border border-blue-200 rounded-xl text-center">
                <span className="text-xs font-bold text-blue-700 uppercase">Total Attempted</span>
                <p className="text-3xl font-black text-blue-900 mt-1">{result.totalProcessed}</p>
              </div>
              <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-center">
                <span className="text-xs font-bold text-emerald-700 uppercase">Successfully Created</span>
                <p className="text-3xl font-black text-emerald-900 mt-1">{result.totalCreated}</p>
              </div>
              <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-center">
                <span className="text-xs font-bold text-rose-700 uppercase">Failed</span>
                <p className="text-3xl font-black text-rose-900 mt-1">{result.totalFailed}</p>
              </div>
            </div>

            {result.errors?.length > 0 && (
              <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl space-y-1 text-xs text-rose-700">
                <h4 className="font-bold uppercase tracking-wider">Error Details</h4>
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
                  setParsedCsvTasks([]);
                  setCsvFile(null);
                }}
              >
                Generate Another Batch
              </Button>
              <Button
                onClick={() => navigate('/tasks')}
                style={{ backgroundColor: currentTheme.primaryColor }}
                rightIcon={<ArrowRight className="w-4 h-4" />}
              >
                Go to Tasks & Workflow Dashboard
              </Button>
            </div>
          </div>
        </Card>
      )}
    </div>
  );
};
