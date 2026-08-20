import React from 'react';
import { Badge } from './Badge';
import { CheckCircle2, Clock, AlertTriangle, FileSearch, Archive, ShieldCheck } from 'lucide-react';

interface StatusBadgeProps {
  status: string;
  size?: 'sm' | 'md';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'md' }) => {
  const normalized = status ? status.toUpperCase() : 'UNKNOWN';

  switch (normalized) {
    case 'ACTIVE':
    case 'FILED':
    case 'PAID':
    case 'COMPLETED':
    case 'VERIFIED':
      return (
        <Badge variant="success" size={size}>
          <CheckCircle2 className="w-3 h-3 text-emerald-600" />
          {normalized.replace('_', ' ')}
        </Badge>
      );

    case 'PENDING':
    case 'IN_PROGRESS':
    case 'TODO':
    case 'DOCUMENTS_PENDING':
    case 'DATA_ENTRY':
    case 'PARTIALLY_PAID':
    case 'TRIALING':
    case 'UPCOMING':
    case 'ISSUED':
      return (
        <Badge variant="warning" size={size}>
          <Clock className="w-3 h-3 text-amber-600" />
          {normalized.replace('_', ' ')}
        </Badge>
      );

    case 'OVERDUE':
    case 'SUSPENDED':
    case 'FAILED':
    case 'REJECTED':
    case 'CANCELLED':
    case 'PAST_DUE':
    case 'TERMINATED':
    case 'CRITICAL':
      return (
        <Badge variant="danger" size={size}>
          <AlertTriangle className="w-3 h-3 text-rose-600" />
          {normalized.replace('_', ' ')}
        </Badge>
      );

    case 'UNDER_REVIEW':
    case 'READY_TO_FILE':
    case 'VERIFICATION_PENDING':
    case 'PREPARED':
      return (
        <Badge variant="purple" size={size}>
          <FileSearch className="w-3 h-3 text-purple-600" />
          {normalized.replace('_', ' ')}
        </Badge>
      );

    case 'DRAFT':
    case 'INACTIVE':
    case 'PROSPECT':
    case 'ARCHIVED':
    case 'ON_LEAVE':
    default:
      return (
        <Badge variant="default" size={size}>
          <Archive className="w-3 h-3 text-slate-500" />
          {normalized.replace('_', ' ')}
        </Badge>
      );
  }
};
