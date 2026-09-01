import React, { useEffect } from 'react';
import { X } from 'lucide-react';
import clsx from 'clsx';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: React.ReactNode;
  subtitle?: React.ReactNode;
  children: React.ReactNode;
  footer?: React.ReactNode;
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '4xl';
}

export const Modal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  title,
  subtitle,
  children,
  footer,
  maxWidth = 'lg',
}) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const maxWidthStyles = {
    sm: 'max-w-sm',
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-xl',
    '2xl': 'max-w-2xl',
    '4xl': 'max-w-4xl',
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto flex items-center justify-center p-4 sm:p-6 animate-fade-in">
      <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity" onClick={onClose} />
      <div className={clsx('relative bg-white rounded-2xl shadow-modal w-full overflow-hidden z-10 flex flex-col max-h-[90vh]', maxWidthStyles[maxWidth])}>
        <div className="px-4 py-3 sm:px-6 sm:py-4 border-b border-slate-100 flex items-start justify-between gap-3 bg-slate-50/50">
          <div className="min-w-0">
            <h3 className="text-base sm:text-lg font-bold text-slate-900 break-words">{title}</h3>
            {subtitle && <p className="text-xs text-slate-500 mt-0.5 break-words">{subtitle}</p>}
          </div>
          <button
            onClick={onClose}
            className="shrink-0 text-slate-400 hover:text-slate-600 rounded-lg p-2 -mr-1 hover:bg-slate-100 transition-colors"
            aria-label="Close dialog"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
        <div className="p-4 sm:p-6 overflow-y-auto flex-1">{children}</div>
        {footer && <div className="px-4 py-3 sm:px-6 sm:py-4 border-t border-slate-100 bg-slate-50/50 flex flex-wrap items-center justify-end gap-2 sm:gap-3">{footer}</div>}
      </div>
    </div>
  );
};
