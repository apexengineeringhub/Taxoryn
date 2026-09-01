import React, { useState } from 'react';
import { ChevronUp, ChevronDown, ChevronLeft, ChevronRight, Search, SlidersHorizontal } from 'lucide-react';
import clsx from 'clsx';

export interface Column<T> {
  header: string;
  accessor?: keyof T | ((row: T) => React.ReactNode);
  cell?: (row: T) => React.ReactNode;
  sortable?: boolean;
  align?: 'left' | 'center' | 'right';
  width?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  isLoading?: boolean;
  searchPlaceholder?: string;
  onSearch?: (query: string) => void;
  actions?: React.ReactNode;
  totalElements?: number;
  pageSize?: number;
  pageNumber?: number;
  onPageChange?: (newPage: number) => void;
  onPageSizeChange?: (newSize: number) => void;
  emptyMessage?: string;
  onRowClick?: (row: T) => void;
}

export function DataTable<T extends { id?: string | number }>({
  columns,
  data,
  isLoading = false,
  searchPlaceholder = 'Search records...',
  onSearch,
  actions,
  totalElements = 0,
  pageSize = 10,
  pageNumber = 0,
  onPageChange,
  onPageSizeChange,
  emptyMessage = 'No records found',
  onRowClick,
}: DataTableProps<T>) {
  const [searchQuery, setSearchQuery] = useState('');
  const [sortKey, setSortKey] = useState<string | null>(null);
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');
  const [density, setDensity] = useState<'compact' | 'comfortable'>('comfortable');

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(e.target.value);
    if (onSearch) onSearch(e.target.value);
  };

  const handleSort = (key: string) => {
    if (sortKey === key) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortKey(key);
      setSortOrder('asc');
    }
  };

  const totalPages = Math.ceil(totalElements / pageSize) || 1;

  const displayData = React.useMemo(() => {
    if (!searchQuery.trim() || onSearch) return data;
    const q = searchQuery.toLowerCase().trim();
    return data.filter((row: any) => {
      return Object.values(row).some((val) => {
        if (val === null || val === undefined) return false;
        if (typeof val === 'object') return false;
        return String(val).toLowerCase().includes(q);
      });
    });
  }, [data, searchQuery, onSearch]);

  const renderCellContent = (col: Column<T>, row: T): React.ReactNode => {
    if (col.cell) return col.cell(row);
    if (typeof col.accessor === 'function') return col.accessor(row);
    if (col.accessor) return row[col.accessor] as React.ReactNode;
    return null;
  };

  // Heuristic split for the mobile card layout: whichever column is literally
  // labeled "Actions" renders as a full-width row at the bottom of the card
  // (so buttons stay comfortably tappable); the first remaining column becomes
  // the card's title; everything else renders as label/value rows.
  const actionsColumn = columns.find((c) => c.header.trim().toLowerCase() === 'actions');
  const bodyColumns = columns.filter((c) => c !== actionsColumn);
  const [titleColumn, ...detailColumns] = bodyColumns;

  return (
    <div className="bg-white border border-slate-200/90 rounded-xl shadow-card overflow-hidden flex flex-col">
      {/* Table Toolbar */}
      <div className="p-4 border-b border-slate-100 flex flex-col sm:flex-row items-center justify-between gap-3 bg-slate-50/40">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={handleSearchChange}
            placeholder={searchPlaceholder}
            className="w-full pl-9 pr-4 py-1.5 text-xs bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all"
          />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
          <button
            onClick={() => setDensity(density === 'comfortable' ? 'compact' : 'comfortable')}
            title="Toggle Row Density"
            className="p-1.5 text-slate-500 hover:text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-50 text-xs font-medium inline-flex items-center gap-1.5 transition-colors"
          >
            <SlidersHorizontal className="w-3.5 h-3.5" />
            <span className="hidden md:inline capitalize">{density}</span>
          </button>
          {actions}
        </div>
      </div>

      {/* Table Container — desktop/tablet (md and up): full table with horizontal scroll fallback */}
      <div className="hidden md:block overflow-x-auto min-h-[250px]">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50/80 text-[11px] font-semibold text-slate-500 uppercase tracking-wider select-none">
              {columns.map((col, idx) => (
                <th
                  key={idx}
                  style={{ width: col.width }}
                  className={clsx(
                    'px-4 py-3',
                    col.align === 'center' && 'text-center',
                    col.align === 'right' && 'text-right',
                    col.sortable && 'cursor-pointer hover:bg-slate-100/80 transition-colors'
                  )}
                  onClick={() => col.sortable && typeof col.accessor === 'string' && handleSort(col.accessor)}
                >
                  <div className={clsx('inline-flex items-center gap-1', col.align === 'right' && 'justify-end')}>
                    <span>{col.header}</span>
                    {col.sortable && typeof col.accessor === 'string' && sortKey === col.accessor && (
                      sortOrder === 'asc' ? <ChevronUp className="w-3 h-3 text-brand-600" /> : <ChevronDown className="w-3 h-3 text-brand-600" />
                    )}
                  </div>
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-xs text-slate-700">
            {isLoading ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-12 text-center text-slate-400">
                  <div className="inline-flex items-center gap-2">
                    <svg className="animate-spin h-5 w-5 text-brand-600" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
                    </svg>
                    <span>Loading records...</span>
                  </div>
                </td>
              </tr>
            ) : displayData.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-12 text-center text-slate-400 font-medium">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              displayData.map((row, rowIdx) => (
                <tr
                  key={row.id || rowIdx}
                  onClick={() => onRowClick && onRowClick(row)}
                  className={clsx(
                    'table-row-hover transition-colors',
                    onRowClick && 'cursor-pointer',
                    density === 'compact' ? 'py-2' : 'py-3.5'
                  )}
                >
                  {columns.map((col, colIdx) => (
                    <td
                      key={colIdx}
                      className={clsx(
                        'px-4',
                        density === 'compact' ? 'py-2' : 'py-3',
                        col.align === 'center' && 'text-center',
                        col.align === 'right' && 'text-right'
                      )}
                    >
                      {renderCellContent(col, row)}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Card list — mobile/small-tablet (below md): each row becomes a stacked card
          instead of a horizontally-scrolling table, per the responsive table strategy. */}
      <div className="md:hidden min-h-[150px]">
        {isLoading ? (
          <div className="px-4 py-12 flex items-center justify-center gap-2 text-slate-400 text-xs">
            <svg className="animate-spin h-5 w-5 text-brand-600" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
            </svg>
            <span>Loading records...</span>
          </div>
        ) : displayData.length === 0 ? (
          <div className="px-4 py-12 text-center text-slate-400 font-medium text-xs">{emptyMessage}</div>
        ) : (
          <ul className="divide-y divide-slate-100">
            {displayData.map((row, rowIdx) => (
              <li
                key={row.id || rowIdx}
                onClick={() => onRowClick && onRowClick(row)}
                className={clsx('px-4 py-3.5 text-xs text-slate-700 space-y-1.5', onRowClick && 'cursor-pointer active:bg-slate-50')}
              >
                {titleColumn && (
                  <div className="font-semibold text-sm text-slate-900">{renderCellContent(titleColumn, row)}</div>
                )}
                {detailColumns.map((col, colIdx) => {
                  const value = renderCellContent(col, row);
                  if (value === null || value === undefined || value === '') return null;
                  return (
                    <div key={colIdx} className="flex items-center justify-between gap-3">
                      <span className="text-slate-400 shrink-0">{col.header}</span>
                      <span className="text-right min-w-0 truncate">{value}</span>
                    </div>
                  );
                })}
                {actionsColumn && (
                  <div
                    className="pt-2 flex items-center gap-2 flex-wrap"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {renderCellContent(actionsColumn, row)}
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Pagination Footer */}
      <div className="px-4 py-3 border-t border-slate-100 bg-slate-50/50 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-500">
        <div>
          Showing <span className="font-semibold text-slate-700">{data.length > 0 ? pageNumber * pageSize + 1 : 0}</span> to{' '}
          <span className="font-semibold text-slate-700">{Math.min((pageNumber + 1) * pageSize, totalElements || data.length)}</span> of{' '}
          <span className="font-semibold text-slate-700">{totalElements || data.length}</span> records
        </div>

        <div className="flex items-center gap-4">
          <div className="flex items-center gap-1.5">
            <span>Rows:</span>
            <select
              value={pageSize}
              onChange={(e) => onPageSizeChange && onPageSizeChange(Number(e.target.value))}
              className="bg-white border border-slate-200 rounded px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-brand-500"
            >
              <option value={10}>10</option>
              <option value={25}>25</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
            </select>
          </div>

          <div className="flex items-center gap-1">
            <button
              disabled={pageNumber === 0}
              onClick={() => onPageChange && onPageChange(pageNumber - 1)}
              className="p-1 rounded border border-slate-200 bg-white hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="px-2 font-medium text-slate-700">
              {pageNumber + 1} / {totalPages}
            </span>
            <button
              disabled={pageNumber + 1 >= totalPages}
              onClick={() => onPageChange && onPageChange(pageNumber + 1)}
              className="p-1 rounded border border-slate-200 bg-white hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
