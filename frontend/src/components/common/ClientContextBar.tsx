import React from 'react';
import {
  ShieldCheck,
  Eye,
  Phone,
  KeyRound,
  Plus,
  Building2,
  CheckCircle2,
} from 'lucide-react';
import { Button } from '../common/Button';
import clsx from 'clsx';

export interface ClientOption {
  id: string;
  displayName: string;
  pan?: string;
  legalName?: string;
}

export interface ClientContextBarProps {
  clientName: string;
  legalName?: string;
  clientType?: string;
  pan?: string;
  gstin?: string;
  tan?: string;
  isVerified?: boolean;
  assignedPractitionerName?: string;
  assignedPractitionerPhone?: string;
  isPracticeUser?: boolean;
  clients?: ClientOption[];
  selectedClientId?: string;
  onClientChange?: (clientId: string) => void;
  onProvisionLogin?: () => void;
  onRequestDocument?: () => void;
  isLoading?: boolean;
  className?: string;
}

export const ClientContextBar: React.FC<ClientContextBarProps> = ({
  clientName,
  legalName,
  clientType,
  pan,
  gstin,
  tan,
  isVerified = true,
  assignedPractitionerName,
  assignedPractitionerPhone,
  isPracticeUser = false,
  clients = [],
  selectedClientId = '',
  onClientChange,
  onProvisionLogin,
  onRequestDocument,
  isLoading = false,
  className,
}) => {
  // Format entity type
  const formattedType = clientType
    ? clientType.replace(/_/g, ' ').replace(/\b\w/g, (l) => l.toUpperCase())
    : 'Business Client';

  // Safe fallback values
  const safePan = pan && pan !== 'N/A' ? pan : null;
  const safeGstin = gstin && gstin !== 'N/A' && gstin !== 'Unregistered' ? gstin : null;
  const safeTan = tan && tan !== 'N/A' ? tan : null;

  return (
    <div
      className={clsx(
        'bg-white rounded-xl sm:rounded-2xl border border-slate-200/90 shadow-2xs p-3.5 sm:px-5 sm:py-3 transition-all',
        className
      )}
    >
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3 lg:gap-4">
        {/* Left: Client Identity & Compact Metadata */}
        <div className="min-w-0 flex-1">
          {/* Top Line: Client Name, Legal Name, Entity Type, Verification */}
          <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <div className="flex items-center gap-2 min-w-0">
              <div className="w-7 h-7 rounded-lg bg-brand-50 border border-brand-100 flex items-center justify-center text-brand-700 shrink-0 font-bold text-xs">
                <Building2 className="w-3.5 h-3.5" />
              </div>
              <h2
                className="text-sm sm:text-base font-black text-slate-900 truncate tracking-tight"
                title={legalName && legalName !== clientName ? `${clientName} (${legalName})` : clientName}
              >
                {clientName}
              </h2>
            </div>

            <span className="text-slate-300 hidden sm:inline">·</span>

            <span className="text-[11px] font-semibold text-slate-600 bg-slate-100 px-2 py-0.5 rounded-md shrink-0">
              {formattedType}
            </span>

            {isVerified && (
              <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-50 border border-emerald-200/80 px-2 py-0.5 rounded-md shrink-0">
                <CheckCircle2 className="w-3 h-3 text-emerald-600 shrink-0" />
                <span>Verified</span>
              </span>
            )}

            {isPracticeUser && (
              <span className="inline-flex items-center gap-1 text-[11px] font-bold text-brand-700 bg-brand-50 border border-brand-200/80 px-2 py-0.5 rounded-md shrink-0">
                <Eye className="w-3 h-3 text-brand-600 shrink-0" />
                <span>Practice Preview</span>
              </span>
            )}
          </div>

          {/* Bottom Line: PAN · GSTIN · TAN Inline Meta */}
          <div className="flex flex-wrap items-center gap-x-3 gap-y-1 mt-1 text-xs text-slate-500 font-mono">
            {safePan ? (
              <div className="flex items-center gap-1">
                <span className="font-sans font-bold text-[10px] uppercase text-slate-400">PAN</span>
                <span className="font-bold text-slate-800">{safePan}</span>
              </div>
            ) : (
              <div className="flex items-center gap-1">
                <span className="font-sans font-bold text-[10px] uppercase text-slate-400">PAN</span>
                <span className="text-slate-400">N/A</span>
              </div>
            )}

            <span className="text-slate-200">|</span>

            <div className="flex items-center gap-1">
              <span className="font-sans font-bold text-[10px] uppercase text-slate-400">GSTIN</span>
              {safeGstin ? (
                <span className="font-bold text-slate-800">{safeGstin}</span>
              ) : (
                <span className="text-slate-400 font-sans text-[11px]">Unregistered</span>
              )}
            </div>

            {safeTan && (
              <>
                <span className="text-slate-200">|</span>
                <div className="flex items-center gap-1">
                  <span className="font-sans font-bold text-[10px] uppercase text-slate-400">TAN</span>
                  <span className="font-bold text-slate-800">{safeTan}</span>
                </div>
              </>
            )}

            {/* Assigned CA indicator for client */}
            {assignedPractitionerName && (
              <>
                <span className="text-slate-200 hidden md:inline">|</span>
                <div className="hidden md:flex items-center gap-1.5 font-sans text-slate-600 text-[11px]">
                  <span>CA: <strong className="text-slate-800">{assignedPractitionerName}</strong></span>
                  {assignedPractitionerPhone && (
                    <a
                      href={`tel:${assignedPractitionerPhone}`}
                      className="inline-flex items-center text-brand-600 hover:text-brand-700 ml-0.5"
                      title={`Call ${assignedPractitionerName}`}
                    >
                      <Phone className="w-3 h-3" />
                    </a>
                  )}
                </div>
              </>
            )}
          </div>
        </div>

        {/* Right: Controls & Actions (for Practice Admin Mode) */}
        {isPracticeUser && (
          <div className="flex flex-wrap items-center gap-2 pt-2 lg:pt-0 border-t lg:border-t-0 border-slate-100 shrink-0">
            {/* Client Dropdown Selector */}
            {clients.length > 0 && onClientChange && (
              <div className="flex items-center gap-1.5 bg-slate-50 px-2.5 py-1.5 rounded-lg border border-slate-200/90 text-xs">
                <span className="font-semibold text-slate-500 text-[11px] whitespace-nowrap">Client:</span>
                <select
                  value={selectedClientId}
                  onChange={(e) => onClientChange(e.target.value)}
                  className="font-bold text-slate-900 bg-transparent border-0 focus:ring-0 cursor-pointer text-xs pr-5 py-0 max-w-[160px] sm:max-w-[200px] truncate"
                  title="Switch client preview"
                >
                  {clients.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.displayName} {c.pan ? `(${c.pan})` : ''}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {/* Quick Action Buttons */}
            {onProvisionLogin && (
              <Button
                variant="outline"
                size="sm"
                onClick={onProvisionLogin}
                leftIcon={<KeyRound className="w-3.5 h-3.5" />}
                className="text-xs h-8 px-2.5"
              >
                Login
              </Button>
            )}

            {onRequestDocument && (
              <Button
                variant="primary"
                size="sm"
                onClick={onRequestDocument}
                leftIcon={<Plus className="w-3.5 h-3.5" />}
                className="text-xs h-8 px-2.5"
              >
                Request Doc
              </Button>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
