import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  BookOpen,
  Search,
  Users,
  Briefcase,
  ArrowRight,
  Menu,
  X,
  Sparkles,
  ChevronRight,
  HelpCircle,
  FileText,
  Video,
  Layers,
  LayoutDashboard,
  User as UserIcon,
  LogOut,
  ShieldCheck,
} from 'lucide-react';
import { Button } from '../common/Button';
import { TaxorynLogo } from '../common/TaxorynLogo';
import { useAuth } from '../../context/AuthContext';
import clsx from 'clsx';

interface LearnHeaderProps {
  initialSearch?: string;
  onSearch?: (query: string) => void;
}

export const LearnHeader: React.FC<LearnHeaderProps> = ({ initialSearch = '', onSearch }) => {
  const navigate = useNavigate();
  const { user, isAuthenticated, isLoading, logout } = useAuth();
  const [searchQuery, setSearchQuery] = useState(initialSearch);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (onSearch) {
      onSearch(searchQuery);
    } else {
      navigate(`/learn/content?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  // Derive Persona & Routing
  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const isMarketplaceCustomer = userRoleCodes.includes('MARKETPLACE_CUSTOMER');
  const isPlatformUser = userRoleCodes.some((r: string) => r.startsWith('TAXORYN_') || r === 'SUPER_ADMIN' || r === 'PLATFORM_ADMIN');
  const isPracticeUser = !isPlatformUser && !isMarketplaceCustomer && (userRoleCodes.length > 0 || !!user?.organizationId);

  let dashboardUrl = '/marketplace/customer/dashboard';
  let dashboardLabel = 'Dashboard';
  let profileUrl = '/marketplace/customer/profile';
  let roleBadge = 'Customer';

  if (isPlatformUser) {
    dashboardUrl = '/admin/overview';
    dashboardLabel = 'Admin Hub';
    profileUrl = '/account-security';
    roleBadge = 'Platform Admin';
  } else if (isPracticeUser) {
    dashboardUrl = '/dashboard';
    dashboardLabel = 'Practice Hub';
    profileUrl = '/account-security';
    roleBadge = 'Practice';
  }

  const displayName = user?.firstName
    ? `${user.firstName} ${user.lastName || ''}`.trim()
    : user?.email?.split('@')[0] || 'My Account';
  const userInitials = (user?.firstName ? user.firstName[0] : (user?.email ? user.email[0] : 'U')).toUpperCase();

  return (
    <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200/90 shadow-2xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 sm:h-20">
          {/* Brand Logo & Learn Badge */}
          <div className="flex items-center gap-3">
            <Link to="/learn" className="flex items-center gap-3 group">
              <div className="w-10 h-10 rounded-xl bg-[#082E5B] flex items-center justify-center p-1 shadow-xs shrink-0 border border-white/10 group-hover:scale-105 transition-transform">
                <TaxorynLogo variant="symbol" theme="dark" size="xs" />
              </div>
              <div className="flex flex-col">
                <div className="flex items-center gap-2">
                  <div className="flex items-center tracking-wider leading-none">
                    <span className="font-black text-lg tracking-[0.14em] text-[#07152B]">TAXO</span>
                    <span className="font-black text-lg tracking-[0.14em] text-[#00D1A3]">RYN</span>
                  </div>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-[#E6FBF6] text-[#00B388] border border-[#CCF7ED]">
                    Learn
                  </span>
                </div>
                <span className="text-[9px] font-bold text-slate-400 tracking-[0.16em] uppercase hidden sm:inline mt-0.5">
                  SIMPLIFYING TAX PRACTICE MANAGEMENT
                </span>
              </div>
            </Link>
          </div>

          {/* Desktop Search Bar */}
          <form onSubmit={handleSearchSubmit} className="hidden md:flex items-center flex-1 max-w-md mx-8">
            <div className="relative w-full">
              <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
              <input
                type="text"
                placeholder="Search tax topics (e.g. GST return, ITR slabs, TDS)..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs sm:text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all shadow-2xs"
              />
            </div>
          </form>

          {/* Desktop Nav Links & Dynamic Auth CTAs */}
          <div className="hidden lg:flex items-center gap-5">
            <nav className="flex items-center gap-4 text-xs font-bold text-slate-600">
              <Link to="/learn" className="hover:text-brand-600 transition-colors">
                Topics
              </Link>
              <Link to="/learn/content" className="hover:text-brand-600 transition-colors">
                Browse All
              </Link>
              <Link to="/marketplace" className="hover:text-brand-600 transition-colors flex items-center gap-1">
                <span>Find Professional</span>
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
              </Link>
            </nav>

            <div className="h-5 w-px bg-slate-200" />

            {/* Three-State Authentication Controls */}
            {isLoading ? (
              /* State 1: Loading Skeleton (Zero Sign In Flash) */
              <div className="flex items-center gap-2 animate-pulse">
                <div className="w-20 h-8 bg-slate-100 rounded-xl" />
                <div className="w-24 h-8 bg-slate-100 rounded-xl" />
              </div>
            ) : isAuthenticated && user ? (
              /* State 2: Authenticated Persona Controls */
              <div className="flex items-center gap-2.5">
                <Link to={dashboardUrl}>
                  <Button variant="primary" size="sm" className="text-xs font-bold bg-brand-600 hover:bg-brand-700 text-white gap-1.5 shadow-xs rounded-xl">
                    <LayoutDashboard className="w-3.5 h-3.5" />
                    <span>{dashboardLabel}</span>
                  </Button>
                </Link>

                <Link
                  to={profileUrl}
                  className="flex items-center gap-2 pl-2 pr-3 py-1 bg-slate-50 hover:bg-slate-100 border border-slate-200/80 rounded-xl text-xs font-semibold text-slate-700 transition-colors shadow-2xs"
                  title={`Signed in as ${user.email}`}
                >
                  <div className="w-6 h-6 rounded-full bg-brand-600 text-white flex items-center justify-center font-bold text-[10px]">
                    {userInitials}
                  </div>
                  <span className="max-w-[110px] truncate">{displayName}</span>
                </Link>

                <button
                  type="button"
                  onClick={logout}
                  title="Sign Out"
                  className="p-2 rounded-xl text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors"
                  aria-label="Sign Out"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </div>
            ) : (
              /* State 3: Anonymous Public State */
              <div className="flex items-center gap-2.5">
                <Link to="/login">
                  <Button variant="secondary" size="sm" className="text-xs font-bold rounded-xl">
                    Sign In
                  </Button>
                </Link>
                <Link to="/marketplace">
                  <Button variant="primary" size="sm" className="text-xs font-bold bg-brand-600 hover:bg-brand-700 text-white gap-1.5 shadow-xs rounded-xl">
                    <span>Get Tax Help</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Button>
                </Link>
              </div>
            )}
          </div>

          {/* Mobile Hamburger Button */}
          <div className="flex lg:hidden items-center gap-2">
            <button
              type="button"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 rounded-xl text-slate-600 hover:bg-slate-100 transition-colors focus:outline-none"
              aria-label="Toggle navigation menu"
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Drawer Menu */}
      {mobileMenuOpen && (
        <div className="lg:hidden border-t border-slate-200 bg-white px-4 pt-3 pb-6 space-y-4 animate-fade-in shadow-xl">
          <form onSubmit={handleSearchSubmit} className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
            <input
              type="text"
              placeholder="Search tax topics..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-800 placeholder-slate-400 focus:outline-none"
            />
          </form>

          {/* Authenticated User Banner (Mobile) */}
          {isAuthenticated && user && (
            <div className="p-3 bg-slate-50 border border-slate-200 rounded-2xl flex items-center justify-between">
              <div className="flex items-center gap-2.5 min-w-0">
                <div className="w-8 h-8 rounded-full bg-brand-600 text-white flex items-center justify-center font-bold text-xs shrink-0">
                  {userInitials}
                </div>
                <div className="min-w-0">
                  <p className="text-xs font-bold text-slate-900 truncate">{displayName}</p>
                  <p className="text-[10px] text-slate-500 truncate">{user.email}</p>
                </div>
              </div>
              <span className="px-2 py-0.5 rounded-full text-[9px] font-bold bg-brand-50 text-brand-700 border border-brand-200 shrink-0">
                {roleBadge}
              </span>
            </div>
          )}

          <nav className="flex flex-col space-y-2 text-sm font-bold text-slate-700">
            <Link
              to="/learn"
              onClick={() => setMobileMenuOpen(false)}
              className="px-3 py-2 rounded-lg hover:bg-slate-50 flex items-center justify-between"
            >
              <span>Learn Topics</span>
              <ChevronRight className="w-4 h-4 text-slate-400" />
            </Link>
            <Link
              to="/learn/content"
              onClick={() => setMobileMenuOpen(false)}
              className="px-3 py-2 rounded-lg hover:bg-slate-50 flex items-center justify-between"
            >
              <span>Browse All Articles & Guides</span>
              <ChevronRight className="w-4 h-4 text-slate-400" />
            </Link>
            <Link
              to="/marketplace"
              onClick={() => setMobileMenuOpen(false)}
              className="px-3 py-2 rounded-lg hover:bg-slate-50 flex items-center justify-between text-brand-600"
            >
              <div className="flex items-center gap-2">
                <Users className="w-4 h-4" />
                <span>Find Tax Professional</span>
              </div>
              <ChevronRight className="w-4 h-4 text-brand-400" />
            </Link>

            {isAuthenticated && (
              <>
                <Link
                  to={dashboardUrl}
                  onClick={() => setMobileMenuOpen(false)}
                  className="px-3 py-2 rounded-lg hover:bg-slate-50 flex items-center justify-between text-slate-900"
                >
                  <div className="flex items-center gap-2">
                    <LayoutDashboard className="w-4 h-4 text-brand-600" />
                    <span>{dashboardLabel}</span>
                  </div>
                  <ChevronRight className="w-4 h-4 text-slate-400" />
                </Link>

                <Link
                  to={profileUrl}
                  onClick={() => setMobileMenuOpen(false)}
                  className="px-3 py-2 rounded-lg hover:bg-slate-50 flex items-center justify-between text-slate-900"
                >
                  <div className="flex items-center gap-2">
                    <UserIcon className="w-4 h-4 text-slate-500" />
                    <span>Profile & Account</span>
                  </div>
                  <ChevronRight className="w-4 h-4 text-slate-400" />
                </Link>
              </>
            )}
          </nav>

          <div className="pt-2 border-t border-slate-100 flex flex-col gap-2">
            {isLoading ? (
              <div className="w-full h-10 bg-slate-100 rounded-xl animate-pulse" />
            ) : isAuthenticated && user ? (
              <Button
                variant="outline"
                onClick={() => {
                  setMobileMenuOpen(false);
                  logout();
                }}
                className="w-full justify-center text-rose-600 border-rose-200 hover:bg-rose-50 font-bold gap-2"
              >
                <LogOut className="w-4 h-4" />
                <span>Sign Out</span>
              </Button>
            ) : (
              <>
                <Link to="/marketplace" onClick={() => setMobileMenuOpen(false)}>
                  <Button variant="primary" className="w-full justify-center bg-brand-600 text-white font-bold">
                    Find a Tax Professional
                  </Button>
                </Link>
                <Link to="/login" onClick={() => setMobileMenuOpen(false)}>
                  <Button variant="secondary" className="w-full justify-center font-bold">
                    Sign In
                  </Button>
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  );
};
