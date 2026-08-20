import React, { useState, useEffect } from 'react';
import { ShieldAlert, Eye, Terminal, User, Clock } from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { Modal } from '../components/common/Modal';
import { auditApi } from '../api/endpoints';
import { AuditLog } from '../types';

export const AuditLogsPage: React.FC = () => {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);

  useEffect(() => {
    loadLogs();
  }, [page, pageSize]);

  const loadLogs = async () => {
    try {
      setIsLoading(true);
      const res = await auditApi.getLogs({ page, size: pageSize });
      setLogs(res.content || []);
      setTotalElements(res.totalElements);
    } catch (err) {
      console.error('Failed to load audit logs', err);
    } finally {
      setIsLoading(false);
    }
  };

  const columns: Column<AuditLog>[] = [
    {
      header: 'Timestamp',
      accessor: (row) => (
        <span className="font-mono text-xs text-slate-700">
          {new Date(row.timestamp).toLocaleString()}
        </span>
      ),
    },
    {
      header: 'Action Operation',
      accessor: (row) => (
        <span className="font-mono font-bold text-xs bg-slate-100 text-slate-900 px-2 py-0.5 rounded border border-slate-200 uppercase">
          {row.action}
        </span>
      ),
    },
    {
      header: 'Entity Type',
      accessor: (row) => (
        <span className="font-semibold text-xs text-brand-700">{row.entityType}</span>
      ),
    },
    {
      header: 'User Email',
      accessor: (row) => <span className="text-xs text-slate-600">{row.userEmail || 'System'}</span>,
    },
    {
      header: 'IP Address',
      accessor: (row) => <span className="font-mono text-[11px] text-slate-500">{row.ipAddress || '127.0.0.1'}</span>,
    },
    {
      header: 'Details',
      align: 'right',
      cell: (row) => (
        <button
          onClick={() => setSelectedLog(row)}
          className="px-2 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded text-xs font-semibold inline-flex items-center gap-1 transition-colors"
        >
          <Eye className="w-3.5 h-3.5" /> View Diff
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">Enterprise Audit Trails</h1>
          <p className="text-xs text-slate-500 mt-1">
            Immutable compliance record of all client changes, GST/ITR operations, invoice modifications, and security actions.
          </p>
        </div>
      </div>

      {/* Audit Data Table */}
      <DataTable
        columns={columns}
        data={logs}
        totalElements={totalElements}
        pageSize={pageSize}
        pageNumber={page}
        onPageChange={setPage}
        onPageSizeChange={setPageSize}
        isLoading={isLoading}
        searchPlaceholder="Search audit trails by action or entity..."
      />

      {/* Audit Diff Modal */}
      <Modal
        isOpen={!!selectedLog}
        onClose={() => setSelectedLog(null)}
        title="Audit Log State Inspection"
        subtitle={`Action: ${selectedLog?.action} • Request ID: ${selectedLog?.requestId || 'N/A'}`}
        maxWidth="2xl"
      >
        {selectedLog && (
          <div className="space-y-4 text-xs">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <span className="font-bold text-slate-500 block mb-1">Old State / Value</span>
                <pre className="p-3 bg-slate-900 text-slate-300 font-mono rounded-lg overflow-x-auto max-h-60">
                  {selectedLog.oldValue ? JSON.stringify(JSON.parse(selectedLog.oldValue), null, 2) : 'null (Created Record)'}
                </pre>
              </div>
              <div>
                <span className="font-bold text-slate-500 block mb-1">New State / Value</span>
                <pre className="p-3 bg-slate-900 text-emerald-400 font-mono rounded-lg overflow-x-auto max-h-60">
                  {selectedLog.newValue ? JSON.stringify(JSON.parse(selectedLog.newValue), null, 2) : 'null (Deleted Record)'}
                </pre>
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
