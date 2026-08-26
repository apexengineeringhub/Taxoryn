import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  BookOpen,
  Search,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  FileText,
  Video,
  HelpCircle,
  Bell,
  Users,
  CheckCircle2,
  ChevronDown,
  Building2,
  Receipt,
  Percent,
  Calculator,
  Briefcase,
  Layers,
} from 'lucide-react';
import { Button } from '../../components/common/Button';
import { LearnHeader } from '../../components/learn/LearnHeader';
import { LearnContentCard } from '../../components/learn/LearnContentCard';
import { publicLearnApi } from '../../api/endpoints';
import { LearnContentSummary, LearnPublicCategory } from '../../types';
import clsx from 'clsx';

export const LearnLandingPage: React.FC = () => {
  const navigate = useNavigate();

  const [categories, setCategories] = useState<LearnPublicCategory[]>([]);
  const [featuredContent, setFeaturedContent] = useState<LearnContentSummary[]>([]);
  const [taxUpdates, setTaxUpdates] = useState<LearnContentSummary[]>([]);
  const [faqItems, setFaqItems] = useState<LearnContentSummary[]>([]);
  const [openFaqId, setOpenFaqId] = useState<string | null>(null);

  const [heroSearch, setHeroSearch] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadLandingData();
  }, []);

  const loadLandingData = async () => {
    try {
      setIsLoading(true);
      const [catList, contentRes, updatesRes, faqsRes] = await Promise.all([
        publicLearnApi.getCategories().catch(() => []),
        publicLearnApi.getContentList({ size: 6 }).catch(() => ({ content: [] })),
        publicLearnApi.getContentList({ contentType: 'TAX_UPDATE', size: 3 }).catch(() => ({ content: [] })),
        publicLearnApi.getContentList({ contentType: 'FAQ', size: 4 }).catch(() => ({ content: [] })),
      ]);

      setCategories(catList || []);
      setFeaturedContent(contentRes?.content || []);
      setTaxUpdates(updatesRes?.content || []);
      setFaqItems(faqsRes?.content || []);
      if (faqsRes?.content?.length) {
        setOpenFaqId(faqsRes.content[0].id);
      }
    } catch (err) {
      console.error('Failed to load learn landing data', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (heroSearch.trim()) {
      navigate(`/learn/content?q=${encodeURIComponent(heroSearch.trim())}`);
    } else {
      navigate('/learn/content');
    }
  };

  // Fallback visual category cards if backend has no category seed
  const displayCategories = categories.length > 0 ? categories : [
    { id: '1', code: 'GST', name: 'GST', description: 'Learn about GST registration, returns, and monthly compliance.', icon: 'Receipt', publishedContentCount: 12 },
    { id: '2', code: 'INCOME_TAX', name: 'Income Tax', description: 'Understand ITR slabs, salary deductions, and filing rules.', icon: 'Calculator', publishedContentCount: 18 },
    { id: '3', code: 'TDS', name: 'TDS', description: 'Learn how TDS withholding works, rates, and 26AS credit.', icon: 'Percent', publishedContentCount: 9 },
    { id: '4', code: 'ITR', name: 'ITR Filing', description: 'Simple step-by-step guides for filing your tax return.', icon: 'FileText', publishedContentCount: 15 },
    { id: '5', code: 'TAX_AUDIT', name: 'Tax Audit', description: 'Understand when and why statutory tax audits are required.', icon: 'ShieldCheck', publishedContentCount: 6 },
    { id: '6', code: 'BUSINESS_TAX', name: 'Business Tax', description: 'Tax information and compliance for startups & companies.', icon: 'Briefcase', publishedContentCount: 8 },
  ];

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col selection:bg-brand-500 selection:text-white">
      {/* 1. Header */}
      <LearnHeader />

      {/* 2. Hero Section */}
      <section className="relative overflow-hidden bg-gradient-to-b from-white via-slate-50 to-slate-100 border-b border-slate-200/80 py-16 sm:py-24">
        {/* Subtle background glow */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[300px] bg-brand-500/10 rounded-full blur-3xl pointer-events-none" />

        <div className="relative max-w-4xl mx-auto px-4 sm:px-6 text-center space-y-6">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-brand-50 border border-brand-200/80 text-brand-700 text-xs font-bold shadow-2xs">
            <Sparkles className="w-3.5 h-3.5 text-brand-600" />
            <span>Taxoryn Learn Knowledge Hub</span>
          </div>

          <h1 className="text-3xl sm:text-5xl font-black text-slate-900 tracking-tight leading-tight sm:leading-tight">
            Simple answers to your tax questions.
          </h1>

          <p className="text-base sm:text-xl text-slate-600 max-w-2xl mx-auto leading-relaxed">
            Understand GST, ITR, TDS and other tax topics in simple language — without confusing jargon.
          </p>

          {/* Hero Search Box */}
          <form onSubmit={handleSearchSubmit} className="max-w-2xl mx-auto pt-2">
            <div className="relative flex items-center bg-white p-2 rounded-2xl border-2 border-slate-200 focus-within:border-brand-500 focus-within:ring-4 focus-within:ring-brand-500/10 shadow-lg transition-all">
              <Search className="w-5 h-5 text-slate-400 ml-3 shrink-0" />
              <input
                type="text"
                placeholder="Search tax topics (e.g. How to file GST, 80C deductions, TDS rates)..."
                value={heroSearch}
                onChange={(e) => setHeroSearch(e.target.value)}
                className="w-full px-3 py-2 text-sm sm:text-base text-slate-900 placeholder-slate-400 bg-transparent focus:outline-none"
              />
              <Button type="submit" variant="primary" className="bg-brand-600 hover:bg-brand-700 text-white font-bold shrink-0 rounded-xl px-5">
                <span>Search</span>
              </Button>
            </div>
          </form>

          {/* Quick Action CTAs */}
          <div className="flex flex-wrap items-center justify-center gap-4 pt-2">
            <a href="#topics">
              <Button variant="secondary" className="font-bold text-xs sm:text-sm rounded-xl px-5">
                <span>Explore Tax Topics</span>
              </Button>
            </a>
            <Link to="/marketplace">
              <Button variant="primary" className="font-bold text-xs sm:text-sm bg-slate-900 hover:bg-slate-800 text-white gap-2 rounded-xl px-5">
                <Users className="w-4 h-4 text-emerald-400" />
                <span>Find a Tax Professional</span>
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* 3. Visual Content Categories Section */}
      <section id="topics" className="py-16 sm:py-20 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 w-full space-y-10">
        <div className="text-center max-w-2xl mx-auto space-y-2">
          <h2 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
            Explore by Tax Category
          </h2>
          <p className="text-sm sm:text-base text-slate-500">
            Select a topic to find easy-to-understand articles, step-by-step guides, and video walkthroughs.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {displayCategories.map((cat) => (
            <Link
              key={cat.id}
              to={`/learn/content?categoryId=${cat.id}`}
              className="group bg-white rounded-2xl p-6 border border-slate-200/90 shadow-card hover:shadow-hover hover:border-brand-400 transition-all duration-300 flex flex-col justify-between"
            >
              <div className="space-y-3">
                <div className="w-12 h-12 rounded-xl bg-brand-50 border border-brand-100 flex items-center justify-center text-brand-600 group-hover:scale-110 group-hover:bg-brand-600 group-hover:text-white transition-all">
                  <BookOpen className="w-6 h-6" />
                </div>
                <h3 className="text-lg font-black text-slate-900 group-hover:text-brand-600 transition-colors">
                  {cat.name}
                </h3>
                <p className="text-xs sm:text-sm text-slate-500 leading-relaxed">
                  {cat.description || `Learn about ${cat.name} rules, step-by-step guides, and requirements.`}
                </p>
              </div>

              <div className="pt-5 mt-4 border-t border-slate-100 flex items-center justify-between text-xs font-bold">
                <span className="text-slate-400">
                  {cat.publishedContentCount ? `${cat.publishedContentCount} topics` : 'Explore guides'}
                </span>
                <span className="text-brand-600 group-hover:translate-x-1 transition-transform flex items-center gap-1">
                  <span>Browse {cat.name}</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </span>
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* 4. Featured Tax Topics Section */}
      <section className="py-16 bg-white border-y border-slate-200/80">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
          <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
            <div>
              <div className="inline-flex items-center gap-1.5 text-xs font-bold text-brand-600 uppercase tracking-wider mb-1">
                <Sparkles className="w-3.5 h-3.5" />
                <span>Featured for You</span>
              </div>
              <h2 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
                Popular Tax Guides & Articles
              </h2>
            </div>
            <Link to="/learn/content">
              <Button variant="secondary" size="sm" className="font-bold text-xs gap-1.5">
                <span>View All Content</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Button>
            </Link>
          </div>

          {isLoading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {[1, 2, 3].map((n) => (
                <div key={n} className="bg-slate-100 rounded-2xl h-80 animate-pulse border border-slate-200" />
              ))}
            </div>
          ) : featuredContent.length === 0 ? (
            <div className="text-center py-12 bg-slate-50 rounded-2xl border border-dashed border-slate-200 p-8 space-y-3">
              <BookOpen className="w-10 h-10 text-slate-400 mx-auto" />
              <h3 className="text-base font-bold text-slate-700">Educational Guides Coming Soon</h3>
              <p className="text-xs text-slate-500 max-w-md mx-auto">
                Our editorial tax team is publishing new beginner guides, return filing walkthroughs, and GST updates.
              </p>
              <Link to="/marketplace">
                <Button variant="primary" size="sm" className="mt-2 bg-brand-600 text-white font-bold">
                  Explore Tax Professionals in the Marketplace
                </Button>
              </Link>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {featuredContent.map((item) => (
                <LearnContentCard key={item.id} content={item} />
              ))}
            </div>
          )}
        </div>
      </section>

      {/* 5. FAQs & Recent Tax Updates 2-Column Section */}
      <section className="py-16 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 w-full">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
          {/* FAQs Column */}
          <div className="space-y-6">
            <div className="space-y-1">
              <div className="inline-flex items-center gap-1.5 text-xs font-bold text-amber-700 uppercase tracking-wider">
                <HelpCircle className="w-3.5 h-3.5" />
                <span>Frequently Asked Questions</span>
              </div>
              <h3 className="text-2xl font-black text-slate-900 tracking-tight">
                Quick Answers to Common Doubts
              </h3>
            </div>

            {faqItems.length === 0 ? (
              <div className="bg-white rounded-2xl p-6 border border-slate-200 text-sm text-slate-500">
                Browse our comprehensive question and answer repository across GST, ITR, and TDS compliance.
              </div>
            ) : (
              <div className="space-y-3">
                {faqItems.map((faq) => {
                  const isOpen = openFaqId === faq.id;
                  return (
                    <div
                      key={faq.id}
                      className="bg-white rounded-2xl border border-slate-200/90 overflow-hidden shadow-card transition-all"
                    >
                      <button
                        type="button"
                        onClick={() => setOpenFaqId(isOpen ? null : faq.id)}
                        className="w-full p-5 text-left flex items-center justify-between gap-4 font-bold text-sm sm:text-base text-slate-900 hover:text-brand-600 transition-colors"
                        aria-expanded={isOpen}
                      >
                        <span>{faq.title}</span>
                        <ChevronDown
                          className={clsx('w-4 h-4 text-slate-400 shrink-0 transition-transform duration-200', isOpen && 'rotate-180 text-brand-600')}
                        />
                      </button>
                      {isOpen && (
                        <div className="px-5 pb-5 pt-1 text-xs sm:text-sm text-slate-600 leading-relaxed border-t border-slate-100 bg-slate-50/50">
                          <p>{faq.summary || 'Click below to view the detailed explanation and rules for this question.'}</p>
                          <Link
                            to={`/learn/content/${faq.slug}`}
                            className="inline-flex items-center gap-1 font-bold text-xs text-brand-600 mt-3 hover:text-brand-700"
                          >
                            <span>Read Full Explanation</span>
                            <ArrowRight className="w-3 h-3" />
                          </Link>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Recent Tax Updates Column */}
          <div className="space-y-6">
            <div className="space-y-1">
              <div className="inline-flex items-center gap-1.5 text-xs font-bold text-emerald-700 uppercase tracking-wider">
                <Bell className="w-3.5 h-3.5" />
                <span>Recent Tax Updates</span>
              </div>
              <h3 className="text-2xl font-black text-slate-900 tracking-tight">
                Notifications & Due Dates
              </h3>
            </div>

            {taxUpdates.length === 0 ? (
              <div className="bg-white rounded-2xl p-6 border border-slate-200 text-sm text-slate-500">
                Stay tuned for notifications on GST due date extensions, ITR form changes, and budget updates.
              </div>
            ) : (
              <div className="space-y-4">
                {taxUpdates.map((update) => (
                  <Link
                    key={update.id}
                    to={`/learn/content/${update.slug}`}
                    className="block group bg-white rounded-2xl p-5 border border-slate-200/90 shadow-card hover:border-emerald-400 hover:shadow-hover transition-all"
                  >
                    <div className="flex items-start justify-between gap-3 mb-2">
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-emerald-50 text-emerald-800 border border-emerald-200">
                        {update.categoryName || 'Tax Update'}
                      </span>
                      {update.publishedAt && (
                        <span className="text-[11px] font-semibold text-slate-400">
                          {new Date(update.publishedAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
                        </span>
                      )}
                    </div>
                    <h4 className="text-sm sm:text-base font-bold text-slate-900 group-hover:text-emerald-700 transition-colors">
                      {update.title}
                    </h4>
                    {update.summary && (
                      <p className="text-xs text-slate-500 mt-1 line-clamp-2 leading-relaxed">
                        {update.summary}
                      </p>
                    )}
                  </Link>
                ))}
              </div>
            )}
          </div>
        </div>
      </section>

      {/* 6. Marketplace Conversion CTA Banner */}
      <section className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 text-white py-16 px-4 sm:px-6 lg:px-8 border-t border-slate-800">
        <div className="max-w-4xl mx-auto text-center space-y-6">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300 text-xs font-bold border border-emerald-500/30">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>Verified Tax Professionals across India</span>
          </div>

          <h2 className="text-3xl sm:text-4xl font-black tracking-tight">
            Need hands-on help with your taxes?
          </h2>

          <p className="text-sm sm:text-base text-slate-300 max-w-2xl mx-auto leading-relaxed">
            Connect with certified Chartered Accountants and Tax Practitioners for return filing, GST registration, tax audits, and notice handling.
          </p>

          <div className="pt-2 flex flex-wrap items-center justify-center gap-4">
            <Link to="/marketplace">
              <Button variant="primary" size="lg" className="bg-emerald-600 hover:bg-emerald-700 text-white font-black text-sm px-8 rounded-xl shadow-lg shadow-emerald-900/30 gap-2">
                <span>Find a Tax Professional</span>
                <ArrowRight className="w-4 h-4" />
              </Button>
            </Link>
            <Link to="/learn/content">
              <Button variant="secondary" size="lg" className="bg-white/10 hover:bg-white/20 text-white border-white/20 font-bold text-sm px-8 rounded-xl">
                <span>Browse All Guides</span>
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* 7. Footer */}
      <footer className="bg-slate-950 text-slate-400 py-10 border-t border-slate-900 text-xs">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-center sm:text-left">
          <div className="flex items-center gap-2">
            <span className="font-black text-slate-200">Taxoryn Learn</span>
            <span>•</span>
            <span>Educational Tax Resource</span>
          </div>
          <div className="flex items-center gap-6">
            <Link to="/learn" className="hover:text-white transition-colors">Topics</Link>
            <Link to="/learn/content" className="hover:text-white transition-colors">Articles & Guides</Link>
            <Link to="/marketplace" className="hover:text-white transition-colors">Find a CA</Link>
            <Link to="/login" className="hover:text-white transition-colors">Portal Login</Link>
          </div>
          <div>
            © {new Date().getFullYear()} Taxoryn. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  );
};
