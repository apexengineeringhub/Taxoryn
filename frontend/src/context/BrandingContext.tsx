import React, { createContext, useContext, useState, useEffect } from 'react';

export interface ThemeTemplate {
  id: string;
  name: string;
  category: string;
  mode: 'light' | 'dark';
  primaryColor: string;
  primaryHover: string;
  sidebarBg: string;
  sidebarHeaderBg: string;
  sidebarBorder: string;
  sidebarText: string;
  sidebarTextActive: string;
  sidebarItemHoverBg: string;
  accentColor: string;
  previewColors: string[];
}

export const THEME_TEMPLATES: ThemeTemplate[] = [
  // --- Taxoryn Signature Brand Themes ---
  {
    id: 'taxoryn-signature-light',
    name: 'Taxoryn Signature Light (Navy & Teal)',
    category: 'Official Brand Theme',
    mode: 'light',
    primaryColor: '#00D1A3',
    primaryHover: '#00B388',
    sidebarBg: '#FFFFFF',
    sidebarHeaderBg: '#082E5B',
    sidebarBorder: '#E2E8F0',
    sidebarText: '#334155',
    sidebarTextActive: '#FFFFFF',
    sidebarItemHoverBg: '#F1F5F9',
    accentColor: '#082E5B',
    previewColors: ['#082E5B', '#00D1A3', '#F8FAFC'],
  },
  {
    id: 'taxoryn-signature-dark',
    name: 'Taxoryn Signature Dark (Navy & Emerald)',
    category: 'Official Brand Theme',
    mode: 'dark',
    primaryColor: '#00D1A3',
    primaryHover: '#00B388',
    sidebarBg: '#07152B',
    sidebarHeaderBg: '#082E5B',
    sidebarBorder: 'rgba(255, 255, 255, 0.1)',
    sidebarText: '#94A3B8',
    sidebarTextActive: '#FFFFFF',
    sidebarItemHoverBg: 'rgba(255, 255, 255, 0.08)',
    accentColor: '#00D1A3',
    previewColors: ['#082E5B', '#07152B', '#00D1A3'],
  },

  // --- Professional Light Themes ---
  {
    id: 'light-nordic',
    name: 'Nordic Clean White & Sapphire',
    category: 'Ultra-Clean Light Theme',
    mode: 'light',
    primaryColor: '#2563EB',
    primaryHover: '#1D4ED8',
    sidebarBg: '#FFFFFF',
    sidebarHeaderBg: '#F8FAFC',
    sidebarBorder: '#E2E8F0',
    sidebarText: '#475569',
    sidebarTextActive: '#FFFFFF',
    sidebarItemHoverBg: '#F1F5F9',
    accentColor: '#3B82F6',
    previewColors: ['#2563EB', '#FFFFFF', '#F1F5F9'],
  },
  {
    id: 'light-slate',
    name: 'Executive Platinum & Indigo',
    category: 'Corporate Advisory Light',
    mode: 'light',
    primaryColor: '#4F46E5',
    primaryHover: '#4338CA',
    sidebarBg: '#F8FAFC',
    sidebarHeaderBg: '#EEF2F6',
    sidebarBorder: '#E2E8F0',
    sidebarText: '#334155',
    sidebarTextActive: '#FFFFFF',
    sidebarItemHoverBg: '#E2E8F0',
    accentColor: '#6366F1',
    previewColors: ['#4F46E5', '#F8FAFC', '#E2E8F0'],
  },
  {
    id: 'light-sage',
    name: 'Sage Mint & Emerald Audit',
    category: 'Clean Financial Light',
    mode: 'light',
    primaryColor: '#059669',
    primaryHover: '#047857',
    sidebarBg: '#F0FDF4',
    sidebarHeaderBg: '#DCFCE7',
    sidebarBorder: '#BBF7D0',
    sidebarText: '#166534',
    sidebarTextActive: '#FFFFFF',
    sidebarItemHoverBg: '#DCFCE7',
    accentColor: '#10B981',
    previewColors: ['#059669', '#F0FDF4', '#BBF7D0'],
  },
  {
    id: 'light-oxford',
    name: 'Oxford Pure White & Navy',
    category: 'Classic Chartered Light',
    mode: 'light',
    primaryColor: '#1E40AF',
    primaryHover: '#1E3A8A',
    sidebarBg: '#FFFFFF',
    sidebarHeaderBg: '#F1F5F9',
    sidebarBorder: '#CBD5E1',
    sidebarText: '#1E293B',
    sidebarTextActive: '#FFFFFF',
    sidebarItemHoverBg: '#F1F5F9',
    accentColor: '#3B82F6',
    previewColors: ['#1E40AF', '#FFFFFF', '#CBD5E1'],
  },

  // --- Executive Dark Themes ---
  {
    id: 'sapphire',
    name: 'Sapphire Royal & Obsidian',
    category: 'Executive Dark Theme',
    mode: 'dark',
    primaryColor: '#2563EB',
    primaryHover: '#1D4ED8',
    sidebarBg: '#0B132B',
    sidebarHeaderBg: '#070C1A',
    sidebarBorder: 'rgba(255, 255, 255, 0.1)',
    sidebarText: '#94A3B8',
    sidebarTextActive: '#FFFFFF',
    sidebarItemHoverBg: 'rgba(255, 255, 255, 0.08)',
    accentColor: '#3B82F6',
    previewColors: ['#2563EB', '#0B132B', '#3B82F6'],
  },
  {
    id: 'emerald',
    name: 'Emerald Wealth & Forest',
    category: 'Wealth & Audit Dark',
    mode: 'dark',
    primaryColor: '#059669',
    primaryHover: '#047857',
    sidebarBg: '#062C24',
    sidebarHeaderBg: '#031C17',
    sidebarBorder: 'rgba(255, 255, 255, 0.1)',
    sidebarText: '#A7F3D0',
    sidebarTextActive: '#FFFFFF',
    sidebarItemHoverBg: 'rgba(255, 255, 255, 0.08)',
    accentColor: '#10B981',
    previewColors: ['#059669', '#062C24', '#10B981'],
  },
  {
    id: 'violet',
    name: 'Imperial Violet & Amethyst',
    category: 'High-Growth Tech Dark',
    mode: 'dark',
    primaryColor: '#7C3AED',
    primaryHover: '#6D28D9',
    sidebarBg: '#1E1035',
    sidebarHeaderBg: '#130924',
    sidebarBorder: 'rgba(255, 255, 255, 0.1)',
    sidebarText: '#DDD6FE',
    sidebarTextActive: '#FFFFFF',
    sidebarItemHoverBg: 'rgba(255, 255, 255, 0.08)',
    accentColor: '#8B5CF6',
    previewColors: ['#7C3AED', '#1E1035', '#8B5CF6'],
  },
  {
    id: 'ruby',
    name: 'Ruby Crimson & Charcoal',
    category: 'Tax Litigation Dark',
    mode: 'dark',
    primaryColor: '#DC2626',
    primaryHover: '#B91C1C',
    sidebarBg: '#18181B',
    sidebarHeaderBg: '#09090B',
    sidebarBorder: 'rgba(255, 255, 255, 0.1)',
    sidebarText: '#A1A1AA',
    sidebarTextActive: '#FFFFFF',
    sidebarItemHoverBg: 'rgba(255, 255, 255, 0.08)',
    accentColor: '#EF4444',
    previewColors: ['#DC2626', '#18181B', '#EF4444'],
  },
  {
    id: 'amber',
    name: 'Amber Gold & Titanium',
    category: 'Private Client Dark',
    mode: 'dark',
    primaryColor: '#D97706',
    primaryHover: '#B45309',
    sidebarBg: '#0F172A',
    sidebarHeaderBg: '#020617',
    sidebarBorder: 'rgba(255, 255, 255, 0.1)',
    sidebarText: '#94A3B8',
    sidebarTextActive: '#FFFFFF',
    sidebarItemHoverBg: 'rgba(255, 255, 255, 0.08)',
    accentColor: '#F59E0B',
    previewColors: ['#D97706', '#0F172A', '#F59E0B'],
  },
];

interface BrandingContextType {
  currentTheme: ThemeTemplate;
  practiceLogo: string | null;
  employeeAvatars: Record<string, string>;
  activeTabFilter: 'ALL' | 'LIGHT' | 'DARK';
  setActiveTabFilter: (mode: 'ALL' | 'LIGHT' | 'DARK') => void;
  setTheme: (themeId: string) => void;
  setPracticeLogo: (logoUrl: string | null) => void;
  setEmployeeAvatar: (idOrEmail: string, avatarUrl: string) => void;
  getEmployeeAvatar: (idOrEmail?: string) => string | null;
}

const BrandingContext = createContext<BrandingContextType | undefined>(undefined);

export const BrandingProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [themeId, setThemeId] = useState<string>('taxoryn-signature-light');
  const [activeTabFilter, setActiveTabFilter] = useState<'ALL' | 'LIGHT' | 'DARK'>('ALL');
  const [practiceLogo, setPracticeLogoState] = useState<string | null>(null);
  const [employeeAvatars, setEmployeeAvatars] = useState<Record<string, string>>({});

  useEffect(() => {
    const savedTheme = localStorage.getItem('taxoryn_theme_id');
    const savedLogo = localStorage.getItem('taxoryn_practice_logo');
    const savedAvatars = localStorage.getItem('taxoryn_employee_avatars');

    if (savedTheme && THEME_TEMPLATES.some((t) => t.id === savedTheme)) {
      setThemeId(savedTheme);
    }
    if (savedLogo) {
      setPracticeLogoState(savedLogo);
    }
    if (savedAvatars) {
      try {
        setEmployeeAvatars(JSON.parse(savedAvatars));
      } catch (e) {
        localStorage.removeItem('taxoryn_employee_avatars');
      }
    }
  }, []);

  const currentTheme = THEME_TEMPLATES.find((t) => t.id === themeId) || THEME_TEMPLATES[0];

  // Apply CSS root variables when theme changes
  useEffect(() => {
    const root = document.documentElement;
    root.style.setProperty('--color-primary', currentTheme.primaryColor);
    root.style.setProperty('--color-primary-hover', currentTheme.primaryHover);
    root.style.setProperty('--color-sidebar', currentTheme.sidebarBg);
    root.style.setProperty('--color-sidebar-header', currentTheme.sidebarHeaderBg);
    root.style.setProperty('--color-sidebar-border', currentTheme.sidebarBorder);
    root.style.setProperty('--color-accent', currentTheme.accentColor);
  }, [currentTheme]);

  const setTheme = (id: string) => {
    if (THEME_TEMPLATES.some((t) => t.id === id)) {
      setThemeId(id);
      localStorage.setItem('taxoryn_theme_id', id);
    }
  };

  const setPracticeLogo = (logoUrl: string | null) => {
    setPracticeLogoState(logoUrl);
    if (logoUrl) {
      localStorage.setItem('taxoryn_practice_logo', logoUrl);
    } else {
      localStorage.removeItem('taxoryn_practice_logo');
    }
  };

  const setEmployeeAvatar = (idOrEmail: string, avatarUrl: string) => {
    setEmployeeAvatars((prev) => {
      const updated = { ...prev, [idOrEmail.toLowerCase()]: avatarUrl };
      localStorage.setItem('taxoryn_employee_avatars', JSON.stringify(updated));
      return updated;
    });
  };

  const getEmployeeAvatar = (idOrEmail?: string): string | null => {
    if (!idOrEmail) return null;
    return employeeAvatars[idOrEmail.toLowerCase()] || null;
  };

  return (
    <BrandingContext.Provider
      value={{
        currentTheme,
        practiceLogo,
        employeeAvatars,
        activeTabFilter,
        setActiveTabFilter,
        setTheme,
        setPracticeLogo,
        setEmployeeAvatar,
        getEmployeeAvatar,
      }}
    >
      {children}
    </BrandingContext.Provider>
  );
};

export const useBranding = () => {
  const context = useContext(BrandingContext);
  if (!context) {
    throw new Error('useBranding must be used within a BrandingProvider');
  }
  return context;
};
