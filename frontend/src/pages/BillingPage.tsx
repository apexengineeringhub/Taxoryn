import React, { useState, useEffect } from 'react';
import { Receipt, Plus, DollarSign, CheckCircle2, AlertCircle } from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import { billingApi } from '../api/endpoints';
import { Invoice } from '../types';

export const BillingPage: React.FC = () => {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);
  const [paymentAmount, setPaymentAmount] = useState<number>(0);
  const [paymentMode, setPaymentMode] = useState('BANK_TRANSFER');
  const [referenceNo, setReferenceNo] = useState('');

  useEffect(() => {
    loadInvoices();
  }, []);

  const loadInvoices = async () => {
    try {
      setIsLoading(true);
      const res = await billingApi.getInvoices();
      setInvoices(res.content || []);
    } catch (err) {
      console.error('Failed to load invoices', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleRecordPayment = async () => {
    if (!selectedInvoice || paymentAmount <= 0) return;
    try {
      await billingApi.recordPayment(selectedInvoice.id, {
        amount: paymentAmount,
        paymentMode,
        referenceNumber: referenceNo,
        paymentDate: new Date().toISOString().split('T')[0],
      });
      setIsPaymentModalOpen(false);
      loadInvoices();
    } catch (err) {
      alert('Failed to record payment');
    }
  };

  const formatCurrency = (val: number = 0) => {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(val);
  };

  const columns: Column<Invoice>[] = [
    {
      header: 'Invoice #',
      accessor: (row) => (
        <span className="font-mono font-bold text-xs text-brand-700 bg-brand-50 px-2 py-0.5 rounded border border-brand-200">
          {row.invoiceNumber}
        </span>
      ),
    },
    {
      header: 'Client',
      accessor: (row) => <span className="font-bold text-slate-900">{row.clientName || 'Practice Client'}</span>,
    },
    {
      header: 'Date',
      accessor: (row) => <span className="font-mono text-xs text-slate-600">{row.invoiceDate}</span>,
    },
    {
      header: 'Total Fee (₹)',
      accessor: (row) => <span className="font-mono font-bold text-slate-900">{formatCurrency(row.total)}</span>,
      align: 'right',
    },
    {
      header: 'Paid (₹)',
      accessor: (row) => <span className="font-mono font-bold text-emerald-700">{formatCurrency(row.paidAmount)}</span>,
      align: 'right',
    },
    {
      header: 'Balance Due (₹)',
      accessor: (row) => (
        <span className="font-mono font-bold text-rose-600">
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
        <div className="flex items-center justify-end gap-2">
          {row.status !== 'PAID' && row.balanceDue > 0 && (
            <button
              onClick={() => {
                setSelectedInvoice(row);
                setPaymentAmount(row.balanceDue);
                setIsPaymentModalOpen(true);
              }}
              className="px-2 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 rounded text-xs font-semibold inline-flex items-center gap-1 transition-colors"
            >
              <DollarSign className="w-3 h-3" /> Record Payment
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
          <h1 className="text-2xl font-black tracking-tight text-slate-900">Billing & Invoices</h1>
          <p className="text-xs text-slate-500 mt-1">
            Professional fee invoicing with GST line items (SAC 998231), payment receipts, and debtor ledgers.
          </p>
        </div>
        <Button leftIcon={<Plus className="w-4 h-4" />}>Create Invoice</Button>
      </div>

      {/* Invoices Data Table */}
      <DataTable
        columns={columns}
        data={invoices}
        isLoading={isLoading}
        searchPlaceholder="Search invoices by number or client..."
      />

      {/* Record Payment Modal */}
      <Modal
        isOpen={isPaymentModalOpen}
        onClose={() => setIsPaymentModalOpen(false)}
        title="Record Fee Payment"
        subtitle={`Invoice ${selectedInvoice?.invoiceNumber} (Balance Due: ${formatCurrency(selectedInvoice?.balanceDue)})`}
      >
        <div className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Payment Amount (₹) *</label>
            <input
              type="number"
              value={paymentAmount}
              onChange={(e) => setPaymentAmount(Number(e.target.value))}
              className="w-full text-xs font-bold px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Payment Mode</label>
            <select
              value={paymentMode}
              onChange={(e) => setPaymentMode(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg"
            >
              <option value="BANK_TRANSFER">Bank Transfer / NEFT / RTGS</option>
              <option value="UPI">UPI / QR Code</option>
              <option value="CHEQUE">Cheque</option>
              <option value="CASH">Cash</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Reference / UTR Number (Optional)</label>
            <input
              type="text"
              placeholder="e.g. UTR123456789"
              value={referenceNo}
              onChange={(e) => setReferenceNo(e.target.value)}
              className="w-full text-xs px-3 py-2 border border-slate-200 rounded-lg"
            />
          </div>

          <div className="pt-4 flex justify-end gap-2">
            <Button variant="outline" onClick={() => setIsPaymentModalOpen(false)}>Cancel</Button>
            <Button onClick={handleRecordPayment}>Confirm Payment</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
