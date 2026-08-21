import React, { useState, useEffect, useMemo } from 'react';
import {
  Receipt,
  Plus,
  DollarSign,
  Printer,
  Sparkles,
  Zap,
  CheckCircle2,
  AlertCircle,
  Clock,
  Send,
  XCircle,
  Trash2,
  ExternalLink,
} from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { billingApi, clientApi } from '../api/endpoints';
import { Invoice, InvoiceLineItem, Client, BulkCreateInvoicesRequest } from '../types';
import { useAuth } from '../context/AuthContext';
import { printTaxInvoice } from '../utils/invoicePrinter';

// Service presets for quick invoice creation & bulk invoicing
const SERVICE_PRESETS = [
  {
    service: 'GST_FILING' as const,
    label: 'Monthly GST Retainer (GSTR-1 & 3B)',
    description: 'Monthly GSTR-1 & GSTR-3B return preparation, ITC reconciliation and portal filing',
    sacCode: '998231',
    defaultFee: 2500,
    taxRate: 18,
  },
  {
    service: 'ITR_FILING' as const,
    label: 'Annual Income Tax Return (ITR)',
    description: 'Annual Income Tax computation, AIS/TIS review, return preparation & CPC e-filing',
    sacCode: '998231',
    defaultFee: 4000,
    taxRate: 18,
  },
  {
    service: 'TDS' as const,
    label: 'Quarterly TDS Return (24Q / 26Q)',
    description: 'Quarterly TDS calculation, challan verification, FVU file generation & Form 16/16A generation',
    sacCode: '998231',
    defaultFee: 3500,
    taxRate: 18,
  },
  {
    service: 'AUDIT' as const,
    label: 'Statutory Tax Audit (Section 44AB)',
    description: 'Comprehensive Tax Audit under Section 44AB, verification of books & Form 3CA/3CD e-filing',
    sacCode: '998222',
    defaultFee: 25000,
    taxRate: 18,
  },
  {
    service: 'ACCOUNTING' as const,
    label: 'Monthly Bookkeeping & Accounting',
    description: 'Monthly bookkeeping, bank reconciliations, ledger scrutiny & trial balance preparation',
    sacCode: '998221',
    defaultFee: 5000,
    taxRate: 18,
  },
  {
    service: 'ROC_COMPLIANCE' as const,
    label: 'Annual ROC Compliance & MCA Filings',
    description: 'Annual ROC compliance, Director KYC (DIR-3 KYC), AOC-4 & MGT-7 filings with MCA',
    sacCode: '998232',
    defaultFee: 8000,
    taxRate: 18,
  },
  {
    service: 'CONSULTING' as const,
    label: 'Tax Advisory & Virtual CFO Services',
    description: 'Direct & Indirect Tax advisory, structure planning, notice response & virtual CFO consulting',
    sacCode: '998231',
    defaultFee: 7500,
    taxRate: 18,
  },
  {
    service: 'OTHER' as const,
    label: 'Custom Professional Services',
    description: 'Professional legal and tax compliance services',
    sacCode: '998231',
    defaultFee: 3000,
    taxRate: 18,
  },
];

// Helper: Amount to Words INR
function numberToWordsINR(num: number): string {
  if (!num || num <= 0) return 'Zero Rupees Only';
  const a = ['', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine', 'Ten', 'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen', 'Seventeen', 'Eighteen', 'Nineteen'];
  const b = ['', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety'];

  const inWords = (n: number): string => {
    if (n < 20) return a[n];
    if (n < 100) return b[Math.floor(n / 10)] + (n % 10 !== 0 ? ' ' + a[n % 10] : '');
    if (n < 1000) return a[Math.floor(n / 100)] + ' Hundred' + (n % 100 !== 0 ? ' and ' + inWords(n % 100) : '');
    if (n < 100000) return inWords(Math.floor(n / 1000)) + ' Thousand' + (n % 1000 !== 0 ? ' ' + inWords(n % 1000) : '');
    if (n < 10000000) return inWords(Math.floor(n / 100000)) + ' Lakh' + (n % 100000 !== 0 ? ' ' + inWords(n % 100000) : '');
    return inWords(Math.floor(n / 10000000)) + ' Crore' + (n % 10000000 !== 0 ? ' ' + inWords(n % 10000000) : '');
  };

  const integerPart = Math.floor(num);
  const words = inWords(integerPart);
  return `Rupees ${words} Only`;
}

export const BillingPage: React.FC = () => {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [clients, setClients] = useState<Client[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [activeTab, setActiveTab] = useState<'ALL' | 'ISSUED' | 'PAID' | 'PARTIALLY_PAID' | 'DRAFT' | 'OVERDUE'>('ALL');

  // Modals
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isBulkModalOpen, setIsBulkModalOpen] = useState(false);
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
  const [isPrintModalOpen, setIsPrintModalOpen] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);

  // Payment Recording State
  const [paymentAmount, setPaymentAmount] = useState<number>(0);
  const [paymentMode, setPaymentMode] = useState('BANK_TRANSFER');
  const [referenceNo, setReferenceNo] = useState('');
  const [paymentNotes, setPaymentNotes] = useState('');

  // Individual Create Invoice Form State
  const [newClientId, setNewClientId] = useState('');
  const [newInvoiceDate, setNewInvoiceDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [newDueDate, setNewDueDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 15);
    return d.toISOString().split('T')[0];
  });
  const [newTerms, setNewTerms] = useState('1. Payment is due within 15 days of invoice date.\n2. Please mention Invoice Number in bank transfer remark.\n3. Late payment surcharge @ 18% p.a. applicable after due date.');
  const [newNotes, setNewNotes] = useState('Thank you for trusting us with your tax and compliance matters.');
  const [newItems, setNewItems] = useState<Array<{
    service: InvoiceLineItem['service'];
    description: string;
    quantity: number;
    unitPrice: number;
    taxRate: number;
  }>>([
    {
      service: 'GST_FILING',
      description: 'Monthly GSTR-1 & GSTR-3B return preparation and portal filing',
      quantity: 1,
      unitPrice: 2500,
      taxRate: 18,
    },
  ]);

  // Bulk Invoicing State
  const [bulkSelectedClientIds, setBulkSelectedClientIds] = useState<string[]>([]);
  const [bulkServiceIndex, setBulkServiceIndex] = useState(0);
  const [bulkCustomDesc, setBulkCustomDesc] = useState(SERVICE_PRESETS[0].description);
  const [bulkFee, setBulkFee] = useState<number>(SERVICE_PRESETS[0].defaultFee);
  const [bulkTaxRate, setBulkTaxRate] = useState<number>(SERVICE_PRESETS[0].taxRate);
  const [bulkInvoiceDate, setBulkInvoiceDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [bulkDueDate, setBulkDueDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 15);
    return d.toISOString().split('T')[0];
  });
  const [bulkAutoIssue, setBulkAutoIssue] = useState(true);

  const { practiceName } = useAuth();

  useEffect(() => {
    loadInvoices();
    loadClients();
  }, []);

  const loadInvoices = async () => {
    try {
      setIsLoading(true);
      const res = await billingApi.getInvoices({ size: 100 });
      const list = Array.isArray(res) ? res : (res?.content || []);
      setInvoices(list);
    } catch (err) {
      console.error('Failed to load invoices', err);
      setInvoices([]);
    } finally {
      setIsLoading(false);
    }
  };

  const loadClients = async () => {
    try {
      const res = await clientApi.getAll({ size: 100 });
      const list = Array.isArray(res) ? res : (res?.content || []);
      setClients(list);
      setBulkSelectedClientIds(list.map((c) => c.id));
    } catch (err) {
      console.error('Failed to load clients', err);
    }
  };

  const formatCurrency = (val: number = 0) => {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(val);
  };

  // KPI Calculations
  const stats = useMemo(() => {
    const totalBilled = invoices.reduce((sum, inv) => inv.status !== 'CANCELLED' && inv.status !== 'DRAFT' ? sum + Number(inv.total || 0) : sum, 0);
    const totalCollected = invoices.reduce((sum, inv) => inv.status !== 'CANCELLED' ? sum + Number(inv.paidAmount || 0) : sum, 0);
    const totalOutstanding = invoices.reduce((sum, inv) => inv.status !== 'CANCELLED' && inv.status !== 'DRAFT' ? sum + Number(inv.balanceDue || 0) : sum, 0);
    const overdueCount = invoices.filter((inv) => inv.status === 'OVERDUE' || (inv.status === 'ISSUED' && inv.dueDate && new Date(inv.dueDate) < new Date())).length;
    return { totalBilled, totalCollected, totalOutstanding, overdueCount, totalCount: invoices.length };
  }, [invoices]);

  // Tab Filtering
  const filteredInvoices = useMemo(() => {
    if (activeTab === 'ALL') return invoices;
    if (activeTab === 'OVERDUE') {
      return invoices.filter((inv) => inv.status === 'OVERDUE' || (inv.status === 'ISSUED' && inv.dueDate && new Date(inv.dueDate) < new Date()));
    }
    return invoices.filter((inv) => inv.status === activeTab);
  }, [invoices, activeTab]);

  // Tab live counters
  const tabCounts = useMemo(() => {
    return {
      ALL: invoices.length,
      ISSUED: invoices.filter((i) => i.status === 'ISSUED').length,
      PAID: invoices.filter((i) => i.status === 'PAID').length,
      PARTIALLY_PAID: invoices.filter((i) => i.status === 'PARTIALLY_PAID').length,
      DRAFT: invoices.filter((i) => i.status === 'DRAFT').length,
      OVERDUE: invoices.filter((i) => i.status === 'OVERDUE' || (i.status === 'ISSUED' && i.dueDate && new Date(i.dueDate) < new Date())).length,
    };
  }, [invoices]);

  // Individual Form Calculations
  const newInvoiceCalculations = useMemo(() => {
    let subtotal = 0;
    let totalTax = 0;
    for (const it of newItems) {
      const lineSub = Number(it.quantity || 1) * Number(it.unitPrice || 0);
      const lineTax = (lineSub * Number(it.taxRate || 18)) / 100;
      subtotal += lineSub;
      totalTax += lineTax;
    }
    const grandTotal = subtotal + totalTax;
    return { subtotal, totalTax, grandTotal };
  }, [newItems]);

  // Handlers
  const handleAddItem = () => {
    setNewItems([
      ...newItems,
      {
        service: 'CONSULTING',
        description: 'Professional consulting & advisory services',
        quantity: 1,
        unitPrice: 2000,
        taxRate: 18,
      },
    ]);
  };

  const handleRemoveItem = (index: number) => {
    if (newItems.length === 1) return;
    setNewItems(newItems.filter((_, i) => i !== index));
  };

  const handleItemChange = (index: number, field: string, value: any) => {
    const updated = [...newItems];
    updated[index] = { ...updated[index], [field]: value };

    if (field === 'service') {
      const preset = SERVICE_PRESETS.find((p) => p.service === value);
      if (preset) {
        updated[index].description = preset.description;
        updated[index].unitPrice = preset.defaultFee;
        updated[index].taxRate = preset.taxRate;
      }
    }
    setNewItems(updated);
  };

  const handleCreateIndividualInvoice = async (issueDirectly: boolean = false) => {
    if (!newClientId) {
      alert('Please select a client.');
      return;
    }
    if (newItems.some((it) => !it.unitPrice || it.unitPrice <= 0)) {
      alert('Please provide valid unit prices for all line items.');
      return;
    }

    try {
      setIsSubmitting(true);
      const payload: any = {
        clientId: newClientId,
        invoiceDate: newInvoiceDate,
        dueDate: newDueDate,
        items: newItems.map((it) => ({
          service: it.service,
          description: it.description,
          quantity: Number(it.quantity),
          unitPrice: Number(it.unitPrice),
          taxRate: Number(it.taxRate),
        })),
        notes: newNotes,
        terms: newTerms,
      };

      const created = await billingApi.createInvoice(payload);
      if (issueDirectly && created.id) {
        try {
          await billingApi.issueInvoice(created.id);
        } catch {}
      }

      alert(`Successfully created Invoice ${created.invoiceNumber || ''}!`);
      setIsCreateModalOpen(false);
      await loadInvoices();
    } catch (err: any) {
      alert(`Failed to create invoice: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleBulkGenerate = async () => {
    if (bulkSelectedClientIds.length === 0) {
      alert('Please select at least one client.');
      return;
    }
    if (bulkFee <= 0) {
      alert('Please specify a valid fee amount.');
      return;
    }

    try {
      setIsSubmitting(true);
      const selectedPreset = SERVICE_PRESETS[bulkServiceIndex];
      const payload: BulkCreateInvoicesRequest = {
        clientIds: bulkSelectedClientIds,
        invoiceDate: bulkInvoiceDate,
        dueDate: bulkDueDate,
        items: [
          {
            service: selectedPreset.service,
            description: bulkCustomDesc || selectedPreset.description,
            quantity: 1,
            unitPrice: Number(bulkFee),
            taxRate: Number(bulkTaxRate),
          },
        ],
        autoIssue: bulkAutoIssue,
        notes: 'Monthly compliance fee retainer.',
        terms: 'Payment due within 15 days of invoice date.',
      };

      const result = await billingApi.bulkCreateInvoices(payload);
      alert(`Successfully generated ${result.totalCreated} invoices (Total: ${formatCurrency(result.totalBilledAmount)})!`);
      setIsBulkModalOpen(false);
      await loadInvoices();
    } catch (err: any) {
      alert(`Bulk invoicing note: ${err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSeedDemoInvoices = async () => {
    try {
      setIsSubmitting(true);
      await billingApi.seedDemoInvoices();
      alert('Successfully seeded 4 demo practice tax invoices with sample payment receipts!');
      await loadInvoices();
    } catch (err: any) {
      if (clients.length > 0) {
        for (let i = 0; i < Math.min(clients.length, 3); i++) {
          try {
            const c = clients[i];
            const preset = SERVICE_PRESETS[i % SERVICE_PRESETS.length];
            await billingApi.createInvoice({
              clientId: c.id,
              invoiceDate: new Date().toISOString().split('T')[0],
              dueDate: new Date(Date.now() + 15 * 86400000).toISOString().split('T')[0],
              items: [
                {
                  service: preset.service,
                  description: preset.description,
                  quantity: 1,
                  unitPrice: preset.defaultFee,
                  taxRate: preset.taxRate,
                },
              ] as any,
              notes: 'Demo practice invoice for testing.',
            });
          } catch {}
        }
      }
      alert('Seeded practice demo invoices!');
      await loadInvoices();
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRecordPayment = async () => {
    if (!selectedInvoice || paymentAmount <= 0) return;
    try {
      setIsSubmitting(true);
      await billingApi.recordPayment(selectedInvoice.id, {
        amount: paymentAmount,
        paymentMode,
        referenceNumber: referenceNo,
        paymentDate: new Date().toISOString().split('T')[0],
        notes: paymentNotes,
      });
      alert(`Recorded payment of ${formatCurrency(paymentAmount)} against ${selectedInvoice.invoiceNumber}!`);
      setIsPaymentModalOpen(false);
      await loadInvoices();
    } catch (err: any) {
      alert(`Failed to record payment: ${err.response?.data?.message || err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleIssueInvoice = async (inv: Invoice) => {
    try {
      await billingApi.issueInvoice(inv.id);
      alert(`Invoice ${inv.invoiceNumber} is now ISSUED to client!`);
      await loadInvoices();
    } catch (err: any) {
      alert(`Failed to issue invoice: ${err.message}`);
    }
  };

  const handleCancelInvoice = async (inv: Invoice) => {
    if (!window.confirm(`Are you sure you want to cancel invoice ${inv.invoiceNumber}?`)) return;
    try {
      await billingApi.cancelInvoice(inv.id);
      await loadInvoices();
    } catch (err: any) {
      alert(`Failed to cancel invoice: ${err.message}`);
    }
  };

  const getFullInvoiceData = async (inv: Invoice): Promise<Invoice> => {
    if (inv.items && inv.items.length > 0) return inv;
    try {
      const full = await billingApi.getInvoiceById(inv.id);
      return full || inv;
    } catch {
      return inv;
    }
  };

  const handleOpenPrintModal = async (inv: Invoice) => {
    setSelectedInvoice(inv);
    setIsPrintModalOpen(true);
    const full = await getFullInvoiceData(inv);
    setSelectedInvoice(full);
  };

  const handleDirectPrint = async (inv: Invoice) => {
    const full = await getFullInvoiceData(inv);
    printTaxInvoice(full, practiceName || 'MAA MUNDESHWARI TAX CONSULTANCY');
  };

  // Table Columns
  const columns: Column<Invoice>[] = [
    {
      header: 'Invoice #',
      accessor: (row) => (
        <div className="flex flex-col">
          <span className="font-mono font-bold text-xs text-brand-700 bg-brand-50 px-2 py-0.5 rounded border border-brand-200 w-fit">
            {row.invoiceNumber}
          </span>
          <span className="text-[10px] text-slate-400 mt-0.5">SAC 998231</span>
        </div>
      ),
    },
    {
      header: 'Client & Entity',
      accessor: (row) => (
        <div className="flex flex-col">
          <span className="font-bold text-slate-900 text-xs">{row.clientName || 'Practice Client'}</span>
          <span className="font-mono text-[10px] text-slate-500">
            {row.clientGstin ? `GSTIN: ${row.clientGstin}` : row.clientPan ? `PAN: ${row.clientPan}` : 'Client Master'}
          </span>
        </div>
      ),
    },
    {
      header: 'Dates',
      accessor: (row) => (
        <div className="flex flex-col text-[11px]">
          <span className="text-slate-600 font-mono">Issued: {row.invoiceDate}</span>
          <span className="text-rose-600 font-mono text-[10px]">Due: {row.dueDate || 'Immediate'}</span>
        </div>
      ),
    },
    {
      header: 'Total Fee (₹)',
      accessor: (row) => (
        <div className="flex flex-col text-right">
          <span className="font-mono font-bold text-slate-900 text-xs">{formatCurrency(row.total)}</span>
          <span className="text-[10px] text-slate-400 font-mono">Tax: {formatCurrency(row.tax)}</span>
        </div>
      ),
      align: 'right',
    },
    {
      header: 'Paid (₹)',
      accessor: (row) => <span className="font-mono font-bold text-emerald-700 text-xs">{formatCurrency(row.paidAmount)}</span>,
      align: 'right',
    },
    {
      header: 'Balance Due (₹)',
      accessor: (row) => (
        <span className={`font-mono font-bold text-xs ${row.balanceDue > 0 ? 'text-rose-600 font-extrabold' : 'text-slate-400'}`}>
          {formatCurrency(row.balanceDue)}
        </span>
      ),
      align: 'right',
    },
    {
      header: 'Status',
      accessor: (row) => <StatusBadge status={row.status} size="sm" />,
      align: 'center',
    },
    {
      header: 'Actions',
      align: 'right',
      cell: (row) => (
        <div className="flex items-center justify-end gap-1.5 flex-wrap">
          {/* Quick Print Button */}
          <button
            onClick={() => handleDirectPrint(row)}
            title="Direct Print GST Tax Invoice"
            className="px-2.5 py-1 bg-slate-900 hover:bg-slate-800 text-white rounded text-xs font-semibold inline-flex items-center gap-1 transition-colors shadow-sm"
          >
            <Printer className="w-3.5 h-3.5" /> Print
          </button>

          {/* View Preview Modal */}
          <button
            onClick={() => handleOpenPrintModal(row)}
            title="Preview Tax Invoice"
            className="px-2 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-300 rounded text-xs font-medium inline-flex items-center gap-1 transition-colors"
          >
            <ExternalLink className="w-3 h-3 text-slate-500" /> Preview
          </button>

          {/* Record Payment */}
          {row.status !== 'PAID' && row.status !== 'CANCELLED' && row.balanceDue > 0 && (
            <button
              onClick={() => {
                setSelectedInvoice(row);
                setPaymentAmount(row.balanceDue);
                setIsPaymentModalOpen(true);
              }}
              title="Record Client Payment"
              className="px-2 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 rounded text-xs font-semibold inline-flex items-center gap-1 transition-colors"
            >
              <DollarSign className="w-3.5 h-3.5" /> Pay
            </button>
          )}

          {/* Issue Draft */}
          {row.status === 'DRAFT' && (
            <button
              onClick={() => handleIssueInvoice(row)}
              title="Finalize and Issue Invoice"
              className="px-2 py-1 bg-brand-50 hover:bg-brand-100 text-brand-700 border border-brand-200 rounded text-xs font-semibold inline-flex items-center gap-1 transition-colors"
            >
              <Send className="w-3 h-3" /> Issue
            </button>
          )}

          {/* Cancel Invoice */}
          {row.status !== 'PAID' && row.status !== 'CANCELLED' && (
            <button
              onClick={() => handleCancelInvoice(row)}
              title="Cancel Invoice"
              className="p-1 hover:bg-rose-50 text-rose-500 hover:text-rose-700 rounded transition-colors"
            >
              <XCircle className="w-4 h-4" />
            </button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-black tracking-tight text-slate-900">Billing & Invoices</h1>
            <span className="px-2 py-0.5 bg-emerald-50 text-emerald-700 text-xs font-bold rounded-full border border-emerald-200">
              GST SAC 998231 Compliant
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Generate individual & bulk tax invoices, record payment receipts, and print full GST-compliant fee vouchers.
          </p>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          <Button
            variant="outline"
            onClick={handleSeedDemoInvoices}
            disabled={isSubmitting}
            leftIcon={<Sparkles className="w-4 h-4 text-purple-600" />}
          >
            Seed Demo Invoices
          </Button>

          <Button
            variant="outline"
            onClick={() => setIsBulkModalOpen(true)}
            leftIcon={<Zap className="w-4 h-4 text-amber-500" />}
          >
            Bulk Invoicing Hub
          </Button>

          <Button
            onClick={() => setIsCreateModalOpen(true)}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            + Create Invoice
          </Button>
        </div>
      </div>

      {/* KPI Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500">Total Billed</span>
            <div className="p-2 bg-brand-50 rounded-lg text-brand-600">
              <Receipt className="w-4 h-4" />
            </div>
          </div>
          <p className="text-xl font-black text-slate-900 mt-2">{formatCurrency(stats.totalBilled)}</p>
          <span className="text-[10px] text-slate-400 font-mono">{stats.totalCount} total practice invoices</span>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500">Total Collected</span>
            <div className="p-2 bg-emerald-50 rounded-lg text-emerald-600">
              <CheckCircle2 className="w-4 h-4" />
            </div>
          </div>
          <p className="text-xl font-black text-emerald-600 mt-2">{formatCurrency(stats.totalCollected)}</p>
          <span className="text-[10px] text-slate-400 font-mono">{tabCounts.PAID} fully paid invoices</span>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500">Outstanding Balance</span>
            <div className="p-2 bg-amber-50 rounded-lg text-amber-600">
              <Clock className="w-4 h-4" />
            </div>
          </div>
          <p className="text-xl font-black text-amber-600 mt-2">{formatCurrency(stats.totalOutstanding)}</p>
          <span className="text-[10px] text-slate-400 font-mono">Receivable from active clients</span>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500">Overdue Invoices</span>
            <div className="p-2 bg-rose-50 rounded-lg text-rose-600">
              <AlertCircle className="w-4 h-4" />
            </div>
          </div>
          <p className="text-xl font-black text-rose-600 mt-2">{stats.overdueCount}</p>
          <span className="text-[10px] text-rose-500 font-medium">Payment past statutory due date</span>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-1 border-b border-slate-200 overflow-x-auto pb-px">
        {[
          { key: 'ALL', label: 'All Invoices', count: tabCounts.ALL },
          { key: 'ISSUED', label: 'Issued (Unpaid)', count: tabCounts.ISSUED },
          { key: 'PAID', label: 'Paid', count: tabCounts.PAID },
          { key: 'PARTIALLY_PAID', label: 'Partially Paid', count: tabCounts.PARTIALLY_PAID },
          { key: 'DRAFT', label: 'Drafts', count: tabCounts.DRAFT },
          { key: 'OVERDUE', label: 'Overdue', count: tabCounts.OVERDUE },
        ].map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key as any)}
            className={`px-4 py-2.5 text-xs font-bold border-b-2 transition-all whitespace-nowrap flex items-center gap-2 ${
              activeTab === tab.key
                ? 'border-brand-600 text-brand-700 bg-brand-50/50'
                : 'border-transparent text-slate-500 hover:text-slate-900 hover:border-slate-300'
            }`}
          >
            {tab.label}
            <span className={`px-1.5 py-0.2 rounded-full text-[10px] font-mono ${
              activeTab === tab.key ? 'bg-brand-600 text-white' : 'bg-slate-200 text-slate-700'
            }`}>
              {tab.count}
            </span>
          </button>
        ))}
      </div>

      {/* Empty State Banner when 0 invoices */}
      {invoices.length === 0 && !isLoading && (
        <div className="bg-gradient-to-r from-brand-50 via-purple-50 to-indigo-50 border border-brand-200 rounded-2xl p-8 text-center space-y-4">
          <div className="w-14 h-14 bg-white shadow-sm border border-brand-200 rounded-2xl flex items-center justify-center mx-auto text-brand-600">
            <Receipt className="w-7 h-7" />
          </div>
          <div className="max-w-md mx-auto">
            <h3 className="text-base font-extrabold text-slate-900">No Tax Invoices Issued Yet</h3>
            <p className="text-xs text-slate-600 mt-1">
              Start billing your clients for GST retainers, ITR computations, audits, or legal advisory. You can generate individual invoices or batch invoice all clients at once.
            </p>
          </div>
          <div className="flex items-center justify-center gap-3 pt-2 flex-wrap">
            <Button
              onClick={() => setIsCreateModalOpen(true)}
              leftIcon={<Plus className="w-4 h-4" />}
            >
              Create First Invoice
            </Button>
            <Button
              variant="outline"
              onClick={() => setIsBulkModalOpen(true)}
              leftIcon={<Zap className="w-4 h-4 text-amber-500" />}
            >
              Bulk Invoice Practice Clients
            </Button>
            <Button
              variant="outline"
              onClick={handleSeedDemoInvoices}
              leftIcon={<Sparkles className="w-4 h-4 text-purple-600" />}
            >
              Seed 4 Demo Practice Invoices
            </Button>
          </div>
        </div>
      )}

      {/* Invoices Data Table */}
      <DataTable
        columns={columns}
        data={filteredInvoices}
        isLoading={isLoading}
        searchPlaceholder="Search by invoice number, client name, PAN, GSTIN..."
      />

      {/* =========================================================================
          1. CREATE INDIVIDUAL INVOICE MODAL
          ========================================================================= */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Create Professional Tax Invoice"
        subtitle="GST-compliant billing for accounting, audit, tax advisory, and return filing"
        maxWidth="2xl"
      >
        <div className="space-y-5 max-h-[80vh] overflow-y-auto pr-1">
          {/* Client & Date Selection */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="sm:col-span-1">
              <label className="block text-xs font-semibold text-slate-700 mb-1">Target Client *</label>
              <select
                value={newClientId}
                onChange={(e) => setNewClientId(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 bg-white"
              >
                <option value="">-- Select Client --</option>
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.displayName} ({c.pan || 'No PAN'})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Invoice Date *</label>
              <input
                type="date"
                value={newInvoiceDate}
                onChange={(e) => setNewInvoiceDate(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Payment Due Date *</label>
              <input
                type="date"
                value={newDueDate}
                onChange={(e) => setNewDueDate(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg font-mono"
              />
            </div>
          </div>

          {/* Line Items Table */}
          <div className="border border-slate-200 rounded-xl overflow-hidden">
            <div className="bg-slate-50 px-4 py-2.5 border-b border-slate-200 flex items-center justify-between">
              <span className="text-xs font-bold text-slate-800">Professional Service Line Items</span>
              <button
                type="button"
                onClick={handleAddItem}
                className="text-xs text-brand-600 hover:text-brand-800 font-semibold inline-flex items-center gap-1"
              >
                <Plus className="w-3.5 h-3.5" /> Add Service Row
              </button>
            </div>

            <div className="p-3 space-y-3">
              {newItems.map((item, idx) => (
                <div key={idx} className="p-3 bg-slate-50/70 border border-slate-200 rounded-lg space-y-2">
                  <div className="grid grid-cols-1 sm:grid-cols-12 gap-2 items-center">
                    <div className="sm:col-span-4">
                      <label className="block text-[10px] font-semibold text-slate-500 mb-0.5">Service Type</label>
                      <select
                        value={item.service}
                        onChange={(e) => handleItemChange(idx, 'service', e.target.value)}
                        className="w-full text-xs px-2 py-1.5 border border-slate-300 rounded bg-white"
                      >
                        {SERVICE_PRESETS.map((p) => (
                          <option key={p.service} value={p.service}>
                            {p.label}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="sm:col-span-3">
                      <label className="block text-[10px] font-semibold text-slate-500 mb-0.5">Qty / Hours</label>
                      <input
                        type="number"
                        min="1"
                        value={item.quantity}
                        onChange={(e) => handleItemChange(idx, 'quantity', Number(e.target.value))}
                        className="w-full text-xs px-2 py-1.5 border border-slate-300 rounded font-mono"
                      />
                    </div>

                    <div className="sm:col-span-2">
                      <label className="block text-[10px] font-semibold text-slate-500 mb-0.5">Rate (₹)</label>
                      <input
                        type="number"
                        min="0"
                        value={item.unitPrice}
                        onChange={(e) => handleItemChange(idx, 'unitPrice', Number(e.target.value))}
                        className="w-full text-xs px-2 py-1.5 border border-slate-300 rounded font-mono font-bold"
                      />
                    </div>

                    <div className="sm:col-span-2">
                      <label className="block text-[10px] font-semibold text-slate-500 mb-0.5">GST Rate</label>
                      <select
                        value={item.taxRate}
                        onChange={(e) => handleItemChange(idx, 'taxRate', Number(e.target.value))}
                        className="w-full text-xs px-2 py-1.5 border border-slate-300 rounded bg-white font-mono"
                      >
                        <option value="18">18% (Standard)</option>
                        <option value="12">12%</option>
                        <option value="5">5%</option>
                        <option value="0">0% (Exempt)</option>
                      </select>
                    </div>

                    <div className="sm:col-span-1 flex justify-end pt-4">
                      <button
                        type="button"
                        onClick={() => handleRemoveItem(idx)}
                        disabled={newItems.length === 1}
                        className="p-1 text-slate-400 hover:text-rose-600 disabled:opacity-30"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>

                  <div>
                    <label className="block text-[10px] font-semibold text-slate-500 mb-0.5">Detailed Description</label>
                    <input
                      type="text"
                      value={item.description}
                      onChange={(e) => handleItemChange(idx, 'description', e.target.value)}
                      placeholder="e.g. GSTR-1 & 3B Monthly Compliance for August 2026"
                      className="w-full text-xs px-2 py-1.5 border border-slate-300 rounded"
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Calculations Summary Card */}
          <div className="bg-slate-900 text-white p-4 rounded-xl space-y-2">
            <div className="flex justify-between text-xs text-slate-300">
              <span>Taxable Subtotal:</span>
              <span className="font-mono font-semibold">{formatCurrency(newInvoiceCalculations.subtotal)}</span>
            </div>
            <div className="flex justify-between text-xs text-slate-300">
              <span>GST (CGST 9% + SGST 9% / IGST 18%):</span>
              <span className="font-mono font-semibold">{formatCurrency(newInvoiceCalculations.totalTax)}</span>
            </div>
            <div className="border-t border-slate-700 pt-2 flex justify-between text-sm font-bold text-emerald-400">
              <span>Grand Total Amount Due:</span>
              <span className="font-mono text-base">{formatCurrency(newInvoiceCalculations.grandTotal)}</span>
            </div>
            <p className="text-[10px] text-slate-400 font-mono text-right italic">
              {numberToWordsINR(newInvoiceCalculations.grandTotal)}
            </p>
          </div>

          {/* Notes & Payment Terms */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Invoice Notes / Memo</label>
              <textarea
                rows={2}
                value={newNotes}
                onChange={(e) => setNewNotes(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Payment Terms & Bank Details</label>
              <textarea
                rows={2}
                value={newTerms}
                onChange={(e) => setNewTerms(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg font-mono text-[11px]"
              />
            </div>
          </div>

          {/* Form Actions */}
          <div className="pt-3 border-t border-slate-200 flex justify-end gap-2">
            <Button variant="outline" onClick={() => setIsCreateModalOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="outline"
              onClick={() => handleCreateIndividualInvoice(false)}
              disabled={isSubmitting}
            >
              Save as Draft
            </Button>
            <Button
              onClick={() => handleCreateIndividualInvoice(true)}
              disabled={isSubmitting}
              leftIcon={<Send className="w-4 h-4" />}
            >
              Create & Issue Invoice
            </Button>
          </div>
        </div>
      </Modal>

      {/* =========================================================================
          2. BULK INVOICING MODAL
          ========================================================================= */}
      <Modal
        isOpen={isBulkModalOpen}
        onClose={() => setIsBulkModalOpen(false)}
        title="⚡ Bulk Invoice Practice Clients"
        subtitle="Batch generate recurring retainer or compliance fee invoices across multiple clients"
        maxWidth="2xl"
      >
        <div className="space-y-4 max-h-[80vh] overflow-y-auto pr-1">
          {/* Service Template Selection */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Select Service Template *</label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {SERVICE_PRESETS.map((preset, idx) => (
                <button
                  key={preset.service}
                  type="button"
                  onClick={() => {
                    setBulkServiceIndex(idx);
                    setBulkCustomDesc(preset.description);
                    setBulkFee(preset.defaultFee);
                    setBulkTaxRate(preset.taxRate);
                  }}
                  className={`p-3 rounded-lg border text-left transition-all flex flex-col justify-between ${
                    bulkServiceIndex === idx
                      ? 'border-brand-500 bg-brand-50/60 ring-2 ring-brand-500/20'
                      : 'border-slate-200 hover:border-slate-300 bg-white'
                  }`}
                >
                  <span className="font-bold text-xs text-slate-900">{preset.label}</span>
                  <div className="flex items-center justify-between mt-2 pt-1 border-t border-slate-100 text-[11px]">
                    <span className="font-mono font-bold text-brand-700">{formatCurrency(preset.defaultFee)}</span>
                    <span className="text-slate-400 font-mono">SAC {preset.sacCode}</span>
                  </div>
                </button>
              ))}
            </div>
          </div>

          {/* Pricing & Dates */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 p-3 bg-slate-50 border border-slate-200 rounded-xl">
            <div>
              <label className="block text-[11px] font-semibold text-slate-600 mb-1">Fee per Client (₹)</label>
              <input
                type="number"
                min="1"
                value={bulkFee}
                onChange={(e) => setBulkFee(Number(e.target.value))}
                className="w-full text-xs px-2.5 py-1.5 border border-slate-300 rounded font-mono font-bold bg-white"
              />
            </div>
            <div>
              <label className="block text-[11px] font-semibold text-slate-600 mb-1">Invoice Date</label>
              <input
                type="date"
                value={bulkInvoiceDate}
                onChange={(e) => setBulkInvoiceDate(e.target.value)}
                className="w-full text-xs px-2.5 py-1.5 border border-slate-300 rounded font-mono bg-white"
              />
            </div>
            <div>
              <label className="block text-[11px] font-semibold text-slate-600 mb-1">Payment Due Date</label>
              <input
                type="date"
                value={bulkDueDate}
                onChange={(e) => setBulkDueDate(e.target.value)}
                className="w-full text-xs px-2.5 py-1.5 border border-slate-300 rounded font-mono bg-white"
              />
            </div>
          </div>

          {/* Client Target Checklist */}
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="text-xs font-semibold text-slate-700">
                Target Clients ({bulkSelectedClientIds.length} of {clients.length} selected)
              </label>
              <button
                type="button"
                onClick={() => {
                  if (bulkSelectedClientIds.length === clients.length) {
                    setBulkSelectedClientIds([]);
                  } else {
                    setBulkSelectedClientIds(clients.map((c) => c.id));
                  }
                }}
                className="text-xs text-brand-600 hover:text-brand-800 font-semibold"
              >
                {bulkSelectedClientIds.length === clients.length ? 'Deselect All' : 'Select All Clients'}
              </button>
            </div>

            <div className="border border-slate-200 rounded-lg max-h-40 overflow-y-auto divide-y divide-slate-100 bg-white">
              {clients.map((c) => (
                <label key={c.id} className="flex items-center gap-3 px-3 py-2 hover:bg-slate-50 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={bulkSelectedClientIds.includes(c.id)}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setBulkSelectedClientIds([...bulkSelectedClientIds, c.id]);
                      } else {
                        setBulkSelectedClientIds(bulkSelectedClientIds.filter((id) => id !== c.id));
                      }
                    }}
                    className="rounded text-brand-600 focus:ring-brand-500"
                  />
                  <div className="flex-1 flex items-center justify-between text-xs">
                    <span className="font-semibold text-slate-800">{c.displayName}</span>
                    <span className="font-mono text-[11px] text-slate-400">{c.pan || c.clientType}</span>
                  </div>
                </label>
              ))}
            </div>
          </div>

          {/* Options */}
          <div className="flex items-center gap-2 pt-1">
            <input
              type="checkbox"
              id="bulkAutoIssue"
              checked={bulkAutoIssue}
              onChange={(e) => setBulkAutoIssue(e.target.checked)}
              className="rounded text-brand-600 focus:ring-brand-500"
            />
            <label htmlFor="bulkAutoIssue" className="text-xs text-slate-700 cursor-pointer">
              Automatically transition status from <strong>DRAFT</strong> to <strong>ISSUED</strong> upon creation
            </label>
          </div>

          {/* Live Preview Summary */}
          <div className="p-3 bg-brand-50 border border-brand-200 rounded-xl text-xs text-brand-900 flex items-center justify-between">
            <div>
              <span className="font-bold">Total Batch Billing Value: </span>
              <span className="font-mono font-bold text-sm">
                {formatCurrency(bulkSelectedClientIds.length * (bulkFee * (1 + bulkTaxRate / 100)))}
              </span>
            </div>
            <span className="text-slate-500 font-mono">
              {bulkSelectedClientIds.length} invoices @ {formatCurrency(bulkFee * (1 + bulkTaxRate / 100))} each (incl. 18% GST)
            </span>
          </div>

          {/* Bulk Actions */}
          <div className="pt-3 border-t border-slate-200 flex justify-end gap-2">
            <Button variant="outline" onClick={() => setIsBulkModalOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleBulkGenerate}
              disabled={isSubmitting || bulkSelectedClientIds.length === 0}
              leftIcon={<Zap className="w-4 h-4" />}
            >
              Generate {bulkSelectedClientIds.length} Invoices
            </Button>
          </div>
        </div>
      </Modal>

      {/* =========================================================================
          3. RECORD PAYMENT RECEIPT MODAL
          ========================================================================= */}
      <Modal
        isOpen={isPaymentModalOpen}
        onClose={() => setIsPaymentModalOpen(false)}
        title="Record Fee Payment Receipt"
        subtitle={`Invoice ${selectedInvoice?.invoiceNumber} (Client: ${selectedInvoice?.clientName})`}
        maxWidth="lg"
      >
        <div className="space-y-4">
          <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg flex items-center justify-between text-xs">
            <div>
              <span className="text-slate-500">Invoice Total: </span>
              <span className="font-mono font-bold text-slate-800">{formatCurrency(selectedInvoice?.total)}</span>
            </div>
            <div>
              <span className="text-slate-500">Balance Due: </span>
              <span className="font-mono font-bold text-rose-600">{formatCurrency(selectedInvoice?.balanceDue)}</span>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Receipt Amount (₹) *</label>
            <input
              type="number"
              value={paymentAmount}
              onChange={(e) => setPaymentAmount(Number(e.target.value))}
              className="w-full text-xs font-mono font-bold px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Payment Mode</label>
              <select
                value={paymentMode}
                onChange={(e) => setPaymentMode(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg bg-white"
              >
                <option value="BANK_TRANSFER">Bank Transfer / NEFT / RTGS</option>
                <option value="UPI">UPI / QR Code</option>
                <option value="CHEQUE">Cheque / Demand Draft</option>
                <option value="CASH">Cash Payment</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Reference / UTR Number</label>
              <input
                type="text"
                placeholder="e.g. UTR9988112233"
                value={referenceNo}
                onChange={(e) => setReferenceNo(e.target.value)}
                className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg font-mono"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Payment Remarks (Optional)</label>
            <input
              type="text"
              placeholder="e.g. HDFC Bank clearance receipt"
              value={paymentNotes}
              onChange={(e) => setPaymentNotes(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-300 rounded-lg"
            />
          </div>

          <div className="pt-3 flex justify-end gap-2 border-t border-slate-200">
            <Button variant="outline" onClick={() => setIsPaymentModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleRecordPayment} disabled={isSubmitting}>
              Confirm Receipt
            </Button>
          </div>
        </div>
      </Modal>

      {/* =========================================================================
          4. TAX INVOICE PREVIEW MODAL
          ========================================================================= */}
      <Modal
        isOpen={isPrintModalOpen}
        onClose={() => setIsPrintModalOpen(false)}
        title="GST Tax Invoice Preview"
        subtitle={`Invoice ${selectedInvoice?.invoiceNumber}`}
        maxWidth="4xl"
      >
        <div className="space-y-4">
          {/* Top Modal Controls */}
          <div className="flex items-center justify-between bg-slate-50 p-3 rounded-lg border border-slate-200">
            <span className="text-xs text-slate-600">
              GST Rule 46 compliant layout with SAC 998231, bank payment instructions & ledger.
            </span>
            <div className="flex items-center gap-2">
              <Button variant="outline" size="sm" onClick={() => setIsPrintModalOpen(false)}>
                Close
              </Button>
              <Button
                size="sm"
                onClick={() => selectedInvoice && printTaxInvoice(selectedInvoice, practiceName || 'MAA MUNDESHWARI TAX CONSULTANCY')}
                leftIcon={<Printer className="w-4 h-4" />}
              >
                Print / Save as PDF
              </Button>
            </div>
          </div>

          {/* On-Screen Invoice Preview */}
          {selectedInvoice && (
            <div className="bg-white p-6 border border-slate-200 rounded-xl shadow-sm text-slate-900 space-y-6 max-h-[68vh] overflow-y-auto">
              {/* Header */}
              <div className="border-b-2 border-slate-900 pb-4 flex justify-between items-start gap-4">
                <div>
                  <h2 className="text-lg font-black uppercase tracking-tight text-slate-900">
                    {practiceName || 'MAA MUNDESHWARI TAX CONSULTANCY'}
                  </h2>
                  <p className="text-xs text-slate-600 font-semibold mt-0.5">Chartered Accountants & Tax Practitioners</p>
                  <div className="text-[11px] text-slate-500 space-y-0.5 mt-2">
                    <p>Corporate Office: Express Towers, Nariman Point, Mumbai - 400021</p>
                    <p>PAN: <strong className="font-mono text-slate-800">AABFA1234K</strong> | GSTIN: <strong className="font-mono text-slate-800">27AABFA1234K1Z5</strong></p>
                    <p>Email: billing@taxpractice.com | Phone: +91 98201 12233</p>
                  </div>
                </div>

                <div className="text-right">
                  <div className="inline-block bg-slate-900 text-white font-black px-3.5 py-1 rounded text-xs tracking-wider uppercase">
                    TAX INVOICE
                  </div>
                  <p className="text-[10px] text-slate-400 mt-1 uppercase font-semibold">Original for Recipient</p>
                  <div className="mt-2 text-xs space-y-0.5 font-mono">
                    <p><strong>Invoice No:</strong> {selectedInvoice.invoiceNumber}</p>
                    <p><strong>Date:</strong> {selectedInvoice.invoiceDate}</p>
                    <p><strong>Due Date:</strong> {selectedInvoice.dueDate || 'Immediate'}</p>
                    <p><strong>State Code:</strong> 27 (Maharashtra)</p>
                  </div>
                </div>
              </div>

              {/* Bill-To Client & Place of Supply */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 bg-slate-50 p-4 rounded-lg border border-slate-200 text-xs">
                <div>
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block mb-1">
                    Billed To (Client Details):
                  </span>
                  <p className="text-sm font-black text-slate-900">{selectedInvoice.clientName || 'Practice Client'}</p>
                  <div className="text-slate-600 mt-1 space-y-0.5 text-[11px]">
                    <p>Address: Registered Business Office / Commercial Address</p>
                    <p>PAN: <strong className="font-mono text-slate-800">{selectedInvoice.clientPan || 'N/A'}</strong></p>
                    <p>GSTIN: <strong className="font-mono text-slate-800">{selectedInvoice.clientGstin || 'Unregistered'}</strong></p>
                  </div>
                </div>

                <div className="sm:text-right">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block mb-1">
                    Supply Details & Status:
                  </span>
                  <p className="text-xs font-semibold text-slate-800">Place of Supply: Maharashtra (27)</p>
                  <p className="text-xs text-slate-600 mt-0.5">Category: Legal & Tax Representation Services</p>
                  <div className="mt-2">
                    <StatusBadge status={selectedInvoice.status} size="sm" />
                  </div>
                </div>
              </div>

              {/* Service Line Items Table */}
              <div className="border border-slate-200 rounded-lg overflow-hidden">
                <table className="w-full border-collapse text-xs">
                  <thead>
                    <tr className="bg-slate-100 text-slate-800 font-bold border-b border-slate-200">
                      <th className="p-2 text-center border-r border-slate-200 w-10">#</th>
                      <th className="p-2 text-left border-r border-slate-200">Description of Professional Services</th>
                      <th className="p-2 text-center border-r border-slate-200 w-20">SAC</th>
                      <th className="p-2 text-center border-r border-slate-200 w-14">Qty</th>
                      <th className="p-2 text-right border-r border-slate-200 w-24">Rate (₹)</th>
                      <th className="p-2 text-right border-r border-slate-200 w-24">Taxable (₹)</th>
                      <th className="p-2 text-center border-r border-slate-200 w-16">GST</th>
                      <th className="p-2 text-right w-28">Amount (₹)</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200">
                    {(selectedInvoice.items && selectedInvoice.items.length > 0 ? selectedInvoice.items : [
                      {
                        service: 'GST_FILING' as const,
                        description: 'Professional Legal & Tax Practice Services Rendered',
                        hsnSacCode: '998231',
                        quantity: 1,
                        unitPrice: selectedInvoice.subtotal,
                        taxRate: 18,
                        amount: selectedInvoice.total,
                      },
                    ]).map((item, idx) => (
                      <tr key={idx} className="hover:bg-slate-50/50">
                        <td className="p-2 text-center border-r border-slate-200 font-mono text-[11px]">{idx + 1}</td>
                        <td className="p-2 border-r border-slate-200">
                          <p className="font-bold text-slate-900">{item.service || 'PROFESSIONAL_SERVICE'}</p>
                          <p className="text-[11px] text-slate-600 mt-0.5">{item.description}</p>
                        </td>
                        <td className="p-2 text-center border-r border-slate-200 font-mono text-[11px] text-slate-600">
                          {item.hsnSacCode || '998231'}
                        </td>
                        <td className="p-2 text-center border-r border-slate-200 font-mono text-[11px]">{item.quantity || 1}</td>
                        <td className="p-2 text-right border-r border-slate-200 font-mono text-[11px]">{formatCurrency(item.unitPrice)}</td>
                        <td className="p-2 text-right border-r border-slate-200 font-mono text-[11px]">
                          {formatCurrency(Number(item.quantity || 1) * Number(item.unitPrice || 0))}
                        </td>
                        <td className="p-2 text-center border-r border-slate-200 font-mono text-[11px]">{item.taxRate || 18}%</td>
                        <td className="p-2 text-right font-mono font-bold text-slate-900 text-xs">{formatCurrency(item.amount || selectedInvoice.total)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Totals & Tax Calculation Breakdown */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
                <div className="space-y-3">
                  {/* Amount in Words */}
                  <div className="bg-slate-50 p-3 rounded-lg border border-slate-200">
                    <span className="text-[10px] font-bold text-slate-500 uppercase block mb-0.5">Total Amount in Words:</span>
                    <p className="text-xs font-bold text-slate-800 italic">
                      {numberToWordsINR(selectedInvoice.total)}
                    </p>
                  </div>

                  {/* Bank Transfer Details */}
                  <div className="border border-slate-200 p-3 rounded-lg text-[11px] space-y-1 bg-white">
                    <p className="font-bold text-xs text-slate-900 border-b pb-1">Bank Payment Details (RTGS / NEFT / UPI):</p>
                    <p><strong>Bank Name:</strong> HDFC Bank Ltd</p>
                    <p><strong>Account Name:</strong> {practiceName || 'MAA MUNDESHWARI TAX CONSULTANCY'}</p>
                    <p><strong>Account No:</strong> <span className="font-mono">50200012345678</span></p>
                    <p><strong>IFSC Code:</strong> <span className="font-mono">HDFC0001234</span></p>
                    <p><strong>UPI ID:</strong> <span className="font-mono">apextax@hdfcbank</span></p>
                  </div>
                </div>

                {/* Amount Table */}
                <div className="border border-slate-200 rounded-lg overflow-hidden bg-white">
                  <table className="w-full text-xs">
                    <tbody className="divide-y divide-slate-100">
                      <tr>
                        <td className="p-2 text-slate-600 font-medium">Taxable Amount:</td>
                        <td className="p-2 text-right font-mono font-bold">{formatCurrency(selectedInvoice.subtotal)}</td>
                      </tr>
                      <tr>
                        <td className="p-2 text-slate-600">Central GST (CGST @ 9%):</td>
                        <td className="p-2 text-right font-mono">{formatCurrency(selectedInvoice.tax / 2)}</td>
                      </tr>
                      <tr>
                        <td className="p-2 text-slate-600">State GST (SGST @ 9%):</td>
                        <td className="p-2 text-right font-mono">{formatCurrency(selectedInvoice.tax / 2)}</td>
                      </tr>
                      <tr className="bg-slate-100 text-sm font-black text-slate-900 border-t-2 border-slate-900">
                        <td className="p-2">Total Invoice Value (₹):</td>
                        <td className="p-2 text-right font-mono">{formatCurrency(selectedInvoice.total)}</td>
                      </tr>
                      <tr>
                        <td className="p-2 text-emerald-700 font-medium">Amount Received / Paid:</td>
                        <td className="p-2 text-right font-mono font-bold text-emerald-700">{formatCurrency(selectedInvoice.paidAmount)}</td>
                      </tr>
                      <tr className="bg-rose-50 text-rose-700 font-bold">
                        <td className="p-2">Net Balance Payable:</td>
                        <td className="p-2 text-right font-mono font-black text-sm">{formatCurrency(selectedInvoice.balanceDue)}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Payment History Log (if any) */}
              {selectedInvoice.payments && selectedInvoice.payments.length > 0 && (
                <div className="border border-emerald-200 bg-emerald-50/50 p-3 rounded-lg text-xs space-y-1">
                  <span className="font-bold text-emerald-900 block">Recorded Payment Receipts:</span>
                  <div className="divide-y divide-emerald-100">
                    {selectedInvoice.payments.map((pmt, pIdx) => (
                      <div key={pIdx} className="flex justify-between py-1 font-mono text-[11px]">
                        <span>{pmt.paymentDate} - {pmt.paymentMode} {pmt.referenceNumber ? `(Ref: ${pmt.referenceNumber})` : ''}</span>
                        <strong className="text-emerald-800">{formatCurrency(pmt.amount)}</strong>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Terms and Signature Footer */}
              <div className="grid grid-cols-2 gap-6 pt-4 border-t border-slate-300 text-xs">
                <div className="space-y-1 text-[10px] text-slate-500">
                  <span className="font-bold uppercase text-slate-700 block">Terms & Conditions:</span>
                  <p>1. All payments should be made in favor of the firm via NEFT/RTGS/UPI.</p>
                  <p>2. Payment is due within 15 days of invoice date.</p>
                  <p>3. This is a computer-generated tax invoice issued in accordance with GST Rule 46.</p>
                </div>

                <div className="text-right flex flex-col justify-between items-end h-20">
                  <span className="text-[11px] font-bold text-slate-800">
                    For {practiceName || 'MAA MUNDESHWARI TAX CONSULTANCY'}
                  </span>
                  <div className="border-t border-slate-400 pt-1 w-44 text-center text-[10px] text-slate-500">
                    Authorized Signatory / Partner
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
};
