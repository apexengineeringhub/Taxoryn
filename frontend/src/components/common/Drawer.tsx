import React, { useEffect } from 'react';
import { X } from 'lucide-react';
import clsx from 'clsx';

interface DrawerProps {
  isOpen: boolean;
  onClose: () => void;
  title: React.ReactNode;
  subtitle?: React.ReactNode;
  children: React.ReactNode;
  footer?: React.ReactNode;
  width?: 'md' | 'lg' | 'xl' | '2xl';
}

export const Drawer: React.FC<DrawerProps> = ({
  isOpen,
  onClose,
  title,
  subtitle,
  children,
  footer,
  width = 'lg',
}) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const widthStyles = {
    md: 'max-w-md',
    lg: 'max-w-xl',
    xl: 'max-w-2xl',
    '2xl': 'max-w-4xl',
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs transition-opacity" onClick={onClose} />
      <div className="fixed inset-y-0 right-0 max-w-full flex pl-0 sm:pl-10">
        <div className={clsx('w-screen bg-white shadow-2xl flex flex-col', widthStyles[width])}>
          <div className="px-4 py-4 sm:px-6 sm:py-5 border-b border-slate-200 flex items-start justify-between gap-3 bg-slate-50/70">
            <div className="min-w-0">
              <h3 className="text-base sm:text-lg font-bold text-slate-900 break-words">{title}</h3>
              {subtitle && <p className="text-xs text-slate-500 mt-0.5 break-words">{subtitle}</p>}
            </div>
            <button
              onClick={onClose}
              className="shrink-0 text-slate-400 hover:text-slate-600 rounded-lg p-2 -mr-1 hover:bg-slate-200/60 transition-colors"
              aria-label="Close panel"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
          <div className="p-4 sm:p-6 overflow-y-auto flex-1">{children}</div>
          {footer && <div className="px-4 py-3 sm:px-6 sm:py-4 border-t border-slate-200 bg-slate-50 flex flex-wrap items-center justify-end gap-2 sm:gap-3">{footer}</div>}
        </div>
      </div>
    </div>
  );
};
