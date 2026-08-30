import React from 'react';
import clsx from 'clsx';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'danger' | 'ghost' | 'navy' | 'teal' | 'rose';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  leftIcon,
  rightIcon,
  className,
  disabled,
  ...props
}) => {
  const baseStyles = 'inline-flex items-center justify-center font-semibold rounded-lg transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed select-none';

  const variantStyles = {
    primary: 'bg-[#00D1A3] hover:bg-[#00B388] text-slate-950 shadow-xs focus:ring-[#00D1A3]/40 active:bg-[#059669]',
    secondary: 'bg-[#082E5B] hover:bg-[#07152B] text-white shadow-xs focus:ring-[#082E5B]/40 active:bg-[#070C1A]',
    navy: 'bg-[#082E5B] hover:bg-[#07152B] text-white shadow-xs focus:ring-[#082E5B]/40 active:bg-[#070C1A]',
    teal: 'bg-[#00D1A3] hover:bg-[#00B388] text-slate-950 shadow-xs focus:ring-[#00D1A3]/40 active:bg-[#059669]',
    outline: 'border border-slate-300 bg-white hover:bg-slate-50 text-slate-700 shadow-xs focus:ring-[#00D1A3]/40',
    danger: 'bg-rose-600 hover:bg-rose-700 text-white shadow-xs focus:ring-rose-500 active:bg-rose-800',
    rose: 'bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 shadow-xs focus:ring-rose-400/40',
    ghost: 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 focus:ring-slate-400',
  };

  const sizeStyles = {
    sm: 'text-xs px-2.5 py-1.5 gap-1.5',
    md: 'text-sm px-3.5 py-2 gap-2',
    lg: 'text-base px-5 py-2.5 gap-2.5',
  };

  return (
    <button
      disabled={disabled || isLoading}
      className={clsx(baseStyles, variantStyles[variant], sizeStyles[size], className)}
      {...props}
    >
      {isLoading ? (
        <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-current" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
        </svg>
      ) : leftIcon}
      {children}
      {!isLoading && rightIcon}
    </button>
  );
};
