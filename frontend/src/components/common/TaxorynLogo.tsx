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
  const isLightTheme = theme === 'light';
  const textColor = theme === 'dark' ? '#FFFFFF' : theme === 'light' ? '#07152B' : 'currentColor';
  const subtextColor = theme === 'dark' ? '#94A3B8' : theme === 'light' ? '#64748B' : 'currentColor';

  // Dynamic T-gradient colors based on background theme
  const tGradId = isLightTheme ? 'taxoryn_t_grad_light' : 'taxoryn_t_grad_dark';
  const tStartColor = isLightTheme ? '#082E5B' : '#FFFFFF';
  const tMidColor = isLightTheme ? '#07152B' : '#F8FAFC';
  const tEndColor = isLightTheme ? '#061A38' : '#E2E8F0';
  const tStroke = isLightTheme ? '#082E5B' : '#FFFFFF';

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
        {/* T-Letter Gradient (Crisp Brilliant White on Dark, Deep Navy on Light) */}
        <linearGradient id={tGradId} x1="20" y1="12" x2="75" y2="60" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor={tStartColor} />
          <stop offset="60%" stopColor={tMidColor} />
          <stop offset="100%" stopColor={tEndColor} />
        </linearGradient>

        {/* T-Letter 3D Drop Shadow */}
        <filter id="taxoryn_t_shadow" x="12" y="10" width="74" height="58" filterUnits="userSpaceOnUse">
          <feDropShadow dx="1" dy="2.5" stdDeviation="2" floodColor="#000000" floodOpacity={isLightTheme ? "0.18" : "0.38"} />
        </filter>

        {/* R-Letter & Accents Gradient (Vivid Emerald Teal) */}
        <linearGradient id="taxoryn_teal_grad" x1="45" y1="20" x2="85" y2="85" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#00E5B3" />
          <stop offset="45%" stopColor="#00D1A3" />
          <stop offset="100%" stopColor="#009E77" />
        </linearGradient>

        {/* Dynamic Encircling Swoosh Gradient */}
        <linearGradient id="taxoryn_swoosh_grad" x1="15" y1="35" x2="85" y2="85" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#38BDF8" />
          <stop offset="50%" stopColor="#00D1A3" />
          <stop offset="100%" stopColor="#00E5B3" />
        </linearGradient>

        {/* Document Shadow */}
        <filter id="taxoryn_doc_shadow" x="40" y="28" width="38" height="48" filterUnits="userSpaceOnUse">
          <feDropShadow dx="0" dy="3" stdDeviation="2.5" floodColor="#000000" floodOpacity={isLightTheme ? "0.15" : "0.25"} />
        </filter>
      </defs>

      {/* 1. Dynamic Encircling Swoosh (Base Arc) */}
      <path
        d="M 19 36 C 10 56, 16 82, 42 88 C 64 92, 78 82, 86 68"
        stroke="url(#taxoryn_swoosh_grad)"
        strokeWidth="4.5"
        strokeLinecap="round"
        fill="none"
        opacity="0.95"
      />

      {/* 2. Stylized 'R' Arch & Leg */}
      <path
        d="M 48 24 H 65 C 77 24, 85 30, 85 41 C 85 51, 75 57, 61 57 H 49 M 61 57 L 80 80"
        stroke="url(#taxoryn_teal_grad)"
        strokeWidth="11"
        strokeLinecap="round"
        strokeLinejoin="round"
        fill="none"
      />

      {/* 3. Bold Geometric 'T' Top Bar & Stem (Prominent, High-Contrast & Sharp) */}
      <g filter="url(#taxoryn_t_shadow)">
        <path
          d="M 22 14 L 78 14 L 72 23 L 53 23 L 41 52 L 27 60 L 37 23 L 17 23 Z"
          fill={`url(#${tGradId})`}
          stroke={tStroke}
          strokeWidth="0.5"
          strokeLinejoin="round"
        />
      </g>

      {/* 4. Ascending Growth Bars (Analytics / Reconciliation) */}
      <rect x="24" y="66" width="4.5" height="12" rx="2" fill="url(#taxoryn_teal_grad)" />
      <rect x="31" y="56" width="4.5" height="22" rx="2" fill="url(#taxoryn_teal_grad)" />
      <rect x="38" y="46" width="4.5" height="32" rx="2" fill="url(#taxoryn_teal_grad)" />

      {/* 5. Center Tax Document (Folded Corner + Form Lines + Checkmark Badge) */}
      <g filter="url(#taxoryn_doc_shadow)">
        <path
          d="M 44 32 H 72 V 64 L 66 70 H 44 Z"
          fill="#FFFFFF"
          stroke="#CBD5E1"
          strokeWidth="1.2"
        />
        <path
          d="M 66 64 H 72 L 66 70 Z"
          fill="#94A3B8"
        />

        {/* Document Header "TAX" */}
        <text
          x="56"
          y="43"
          textAnchor="middle"
          fill="#07152B"
          fontSize="8"
          fontWeight="900"
          fontFamily="system-ui, sans-serif"
          letterSpacing="0.8"
        >
          TAX
        </text>

        {/* Document Form Lines */}
        <line x1="48" y1="48" x2="68" y2="48" stroke="#94A3B8" strokeWidth="1.5" strokeLinecap="round" />
        <line x1="48" y1="52" x2="68" y2="52" stroke="#CBD5E1" strokeWidth="1.5" strokeLinecap="round" />
        <line x1="48" y1="56" x2="62" y2="56" stroke="#CBD5E1" strokeWidth="1.5" strokeLinecap="round" />

        {/* Circular Checkmark Badge */}
        <circle cx="56" cy="62" r="5" fill="#00D1A3" />
        <path
          d="M 53.5 62 L 55.2 63.7 L 58.7 60.2"
          stroke="#FFFFFF"
          strokeWidth="1.6"
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
