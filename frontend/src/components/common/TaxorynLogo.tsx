import React from 'react';
import clsx from 'clsx';

export type LogoVariant = 'full' | 'horizontal' | 'symbol' | 'compact';
export type LogoTheme = 'light' | 'dark' | 'auto';
export type LogoSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl' | 'custom';

interface TaxorynLogoProps {
  variant?: LogoVariant;
  theme?: LogoTheme;
  size?: LogoSize;
  className?: string;
  showMotto?: boolean;
  onClick?: () => void;
}

export const TaxorynLogo: React.FC<TaxorynLogoProps> = ({
  variant = 'horizontal',
  theme = 'auto',
  size = 'md',
  className,
  showMotto = true,
  onClick,
}) => {
  // Size mappings
  const sizeMap: Record<LogoSize, { symbol: number; height: string; textClass: string; mottoClass: string }> = {
    xs: { symbol: 24, height: 'h-6', textClass: 'text-sm tracking-wide', mottoClass: 'text-[7px]' },
    sm: { symbol: 32, height: 'h-8', textClass: 'text-base tracking-wide', mottoClass: 'text-[8px]' },
    md: { symbol: 40, height: 'h-10', textClass: 'text-xl tracking-wider', mottoClass: 'text-[9px]' },
    lg: { symbol: 52, height: 'h-13', textClass: 'text-2xl tracking-wider', mottoClass: 'text-[10px]' },
    xl: { symbol: 64, height: 'h-16', textClass: 'text-3xl tracking-widest', mottoClass: 'text-xs' },
    '2xl': { symbol: 88, height: 'h-22', textClass: 'text-4xl tracking-widest', mottoClass: 'text-sm' },
    custom: { symbol: 40, height: '', textClass: 'text-xl', mottoClass: 'text-[9px]' },
  };

  const currentSize = sizeMap[size];

  // Theme color resolutions
  const textColor = theme === 'dark' ? '#FFFFFF' : theme === 'light' ? '#07152B' : 'currentColor';
  const subtextColor = theme === 'dark' ? '#94A3B8' : theme === 'light' ? '#64748B' : 'currentColor';

  // Master SVG TR Symbol with Tax Document, Checkmark, Growth Bars & Swoosh
  const renderSymbol = (dimension: number) => (
    <svg
      width={dimension}
      height={dimension}
      viewBox="0 0 100 100"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className="shrink-0 drop-shadow-sm select-none"
      aria-label="Taxoryn Symbol"
    >
      <defs>
        {/* T-Letter Gradient (Crisp Silver / White to Ice Blue) */}
        <linearGradient id="taxoryn_t_grad" x1="15" y1="10" x2="65" y2="45" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#FFFFFF" />
          <stop offset="100%" stopColor="#E2E8F0" />
        </linearGradient>

        {/* R-Letter & Swoosh Gradient (Signature Emerald Teal) */}
        <linearGradient id="taxoryn_teal_grad" x1="30" y1="20" x2="85" y2="80" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#00D1A3" />
          <stop offset="60%" stopColor="#00B388" />
          <stop offset="100%" stopColor="#059669" />
        </linearGradient>

        {/* Cyan Accent Gradient for Growth & Curves */}
        <linearGradient id="taxoryn_cyan_grad" x1="10" y1="80" x2="50" y2="30" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#00D1A3" />
          <stop offset="100%" stopColor="#38BDF8" />
        </linearGradient>

        {/* Document Shadow */}
        <filter id="taxoryn_doc_shadow" x="38" y="26" width="36" height="46" filterUnits="userSpaceOnUse">
          <feDropShadow dx="0" dy="2" stdDeviation="2" floodColor="#000000" floodOpacity="0.18" />
        </filter>
      </defs>

      {/* 1. Dynamic Encircling Swoosh (Base Arch) */}
      <path
        d="M 18 58 C 14 74, 30 88, 56 86 C 68 85, 78 78, 84 68"
        stroke="url(#taxoryn_cyan_grad)"
        strokeWidth="4"
        strokeLinecap="round"
        fill="none"
        opacity="0.95"
      />

      {/* 2. Stylized 'R' Arch & Leg */}
      <path
        d="M 44 24 C 62 20, 80 26, 82 42 C 84 54, 72 62, 58 63 L 78 86"
        stroke="url(#taxoryn_teal_grad)"
        strokeWidth="11"
        strokeLinecap="round"
        strokeLinejoin="round"
        fill="none"
      />

      {/* 3. Bold Geometric 'T' Top Bar & Stem */}
      <path
        d="M 28 14 L 72 14 L 64 24 L 46 24 L 32 46 L 24 46 Z"
        fill="url(#taxoryn_t_grad)"
      />

      {/* 4. Ascending Growth Bars (Analytics / Reconciliation) */}
      <rect x="22" y="66" width="4" height="12" rx="1.5" fill="#00D1A3" />
      <rect x="29" y="58" width="4" height="20" rx="1.5" fill="#00D1A3" />
      <rect x="36" y="50" width="4" height="28" rx="1.5" fill="#00D1A3" />

      {/* 5. Center Tax Document */}
      <g filter="url(#taxoryn_doc_shadow)">
        <rect x="42" y="30" width="28" height="38" rx="3.5" fill="#FFFFFF" stroke="#CBD5E1" strokeWidth="1.2" />
        
        {/* Document Header "TAX" */}
        <text
          x="56"
          y="41"
          textAnchor="middle"
          fill="#07152B"
          fontSize="7.5"
          fontWeight="900"
          fontFamily="system-ui, sans-serif"
          letterSpacing="0.8"
        >
          TAX
        </text>

        {/* Document Form Lines */}
        <line x1="47" y1="46" x2="65" y2="46" stroke="#94A3B8" strokeWidth="1.5" strokeLinecap="round" />
        <line x1="47" y1="50" x2="65" y2="50" stroke="#CBD5E1" strokeWidth="1.5" strokeLinecap="round" />
        <line x1="47" y1="54" x2="59" y2="54" stroke="#CBD5E1" strokeWidth="1.5" strokeLinecap="round" />

        {/* Circular Checkmark Badge */}
        <circle cx="56" cy="61" r="4.5" fill="#00D1A3" />
        <path
          d="M 53.8 61 L 55.3 62.5 L 58.5 59.5"
          stroke="#FFFFFF"
          strokeWidth="1.4"
          strokeLinecap="round"
          strokeLinejoin="round"
          fill="none"
        />
      </g>
    </svg>
  );

  // Symbol only or compact variant
  if (variant === 'symbol' || variant === 'compact') {
    return (
      <div
        onClick={onClick}
        className={clsx(
          'inline-flex items-center justify-center shrink-0 select-none',
          onClick && 'cursor-pointer hover:opacity-90 transition-opacity',
          className
        )}
      >
        {renderSymbol(currentSize.symbol)}
      </div>
    );
  }

  // Full or Horizontal Brand Variant
  return (
    <div
      onClick={onClick}
      className={clsx(
        'inline-flex items-center gap-3 shrink-0 select-none',
        variant === 'full' ? 'flex-col items-center text-center' : 'flex-row items-center',
        onClick && 'cursor-pointer hover:opacity-95 transition-opacity',
        className
      )}
    >
      {/* TR Symbol */}
      {renderSymbol(currentSize.symbol)}

      {/* Brand Text Block */}
      <div className={clsx('flex flex-col min-w-0', variant === 'full' ? 'items-center text-center' : 'items-start text-left')}>
        {/* Wordmark: TAXORYN */}
        <div className="flex items-center tracking-wider leading-none">
          <span
            className={clsx('font-black tracking-[0.14em]', currentSize.textClass)}
            style={{ color: textColor }}
          >
            TAXO
          </span>
          <span
            className={clsx('font-black tracking-[0.14em] text-[#00D1A3]', currentSize.textClass)}
          >
            RYN
          </span>
        </div>

        {/* Official Brand Motto: SIMPLIFYING TAX PRACTICE MANAGEMENT */}
        {(variant === 'full' || (variant === 'horizontal' && showMotto && size !== 'xs' && size !== 'sm')) && (
          <div className="flex items-center gap-1.5 mt-1">
            <span className="w-2 h-[1px] bg-[#00D1A3] opacity-60 hidden sm:inline-block" />
            <span
              className={clsx(
                'font-bold tracking-[0.18em] uppercase whitespace-nowrap opacity-90',
                currentSize.mottoClass
              )}
              style={{ color: subtextColor }}
            >
              SIMPLIFYING TAX PRACTICE MANAGEMENT
            </span>
            <span className="w-2 h-[1px] bg-[#00D1A3] opacity-60 hidden sm:inline-block" />
          </div>
        )}
      </div>
    </div>
  );
};
