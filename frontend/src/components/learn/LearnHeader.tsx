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
} from 'lucide-react';
import { Button } from '../common/Button';
import clsx from 'clsx';

interface LearnHeaderProps {
  initialSearch?: string;
  onSearch?: (query: string) => void;
}

export const LearnHeader: React.FC<LearnHeaderProps> = ({ initialSearch = '', onSearch }) => {
  const navigate = useNavigate();
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

  return (
    <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200/90 shadow-2xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 sm:h-20">
          {/* Brand Logo & Learn Badge */}
          <div className="flex items-center gap-3">
            <Link to="/learn" className="flex items-center gap-2.5 group">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-600 via-indigo-600 to-purple-600 flex items-center justify-center text-white shadow-md shadow-brand-500/20 group-hover:scale-105 transition-transform">
                <BookOpen className="w-5 h-5" />
              </div>
              <div className="flex flex-col">
                <div className="flex items-center gap-1.5">
                  <span className="font-black text-xl tracking-tight text-slate-900">Taxoryn</span>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-brand-50 text-brand-700 border border-brand-200">
                    Learn
                  </span>
                </div>
                <span className="text-[10px] text-slate-400 font-medium hidden sm:inline">Simple Tax Answers & Guides</span>
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

          {/* Desktop Nav Links & CTAs */}
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

            <div className="flex items-center gap-2.5">
              <Link to="/login">
                <Button variant="secondary" size="sm" className="text-xs font-bold">
                  Sign In
                </Button>
              </Link>
              <Link to="/marketplace">
                <Button variant="primary" size="sm" className="text-xs font-bold bg-brand-600 hover:bg-brand-700 text-white gap-1.5 shadow-xs">
                  <span>Get Tax Help</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Button>
              </Link>
            </div>
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
          </nav>

          <div className="pt-2 border-t border-slate-100 flex flex-col gap-2">
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
          </div>
        </div>
      )}
    </header>
  );
};
