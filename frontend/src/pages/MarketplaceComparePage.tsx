import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  ShieldCheck,
  Star,
  MapPin,
  Check,
  X,
  Calendar,
  MessageSquare,
  Sparkles,
  Award,
  Layers,
  Briefcase,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { marketplacePublicApi } from '../api/endpoints';
import { MarketplaceProfile } from '../types';
import clsx from 'clsx';

export const MarketplaceComparePage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const [profiles, setProfiles] = useState<MarketplaceProfile[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const idsParam = searchParams.get('ids');

  useEffect(() => {
    const loadComparisonProfiles = async () => {
      setIsLoading(true);
      try {
        if (!idsParam) {
          // If no IDs provided, fetch top 3 featured profiles to compare
          const featured = await marketplacePublicApi.getFeatured();
          setProfiles(featured.slice(0, 3));
        } else {
          const ids = idsParam.split(',').filter(Boolean);
          const loaded = await Promise.all(ids.map((id) => marketplacePublicApi.getById(id)));
          setProfiles(loaded);
        }
      } catch (err) {
        console.error('Failed to load comparison profiles', err);
      } finally {
        setIsLoading(false);
      }
    };

    loadComparisonProfiles();
  }, [idsParam]);

  const removeProfile = (id: string) => {
    const updated = profiles.filter((p) => p.id !== id);
    setProfiles(updated);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex items-center justify-center">
        <div className="text-center space-y-3">
          <div className="animate-spin w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full mx-auto" />
          <p className="text-sm font-medium text-slate-500">Loading comparison matrix...</p>
        </div>
      </div>
    );
  }

  if (profiles.length === 0) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex items-center justify-center p-4">
        <div className="bg-white dark:bg-slate-900 p-8 rounded-3xl border border-slate-200 dark:border-slate-800 text-center space-y-4 max-w-md">
          <h2 className="text-xl font-bold text-slate-900 dark:text-white">No Profiles Selected</h2>
          <p className="text-sm text-slate-500">Please select at least 2 tax professionals from the directory to compare.</p>
          <Button variant="primary" onClick={() => navigate('/marketplace')}>
            Explore Tax Directory
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 pb-24">
      {/* Top Header */}
      <div className="bg-slate-900 border-b border-slate-800 py-4 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <button
            onClick={() => navigate('/marketplace')}
            className="flex items-center gap-2 text-xs font-semibold text-slate-300 hover:text-white transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Tax Directory</span>
          </button>
          <div className="text-xs font-bold text-slate-400">
            Comparing {profiles.length} Tax Practitioners
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-8 space-y-8">
        <div className="text-center space-y-2">
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white">
            Side-by-Side Professional Comparison
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 max-w-2xl mx-auto">
            Evaluate qualifications, verified credentials, pricing structures, and specializations to choose the perfect tax partner.
          </p>
        </div>

        {/* Comparison Matrix Table */}
        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-xl overflow-x-auto">
          <table className="w-full border-collapse">
            <thead>
              <tr className="border-b border-slate-200 dark:border-slate-800">
                <th className="p-6 text-left text-xs font-bold text-slate-400 uppercase tracking-wider w-1/4 bg-slate-50/50 dark:bg-slate-800/30">
                  Feature / Attribute
                </th>
                {profiles.map((p) => (
                  <th key={p.id} className="p-6 text-left w-1/4 align-top">
                    <div className="space-y-3">
                      <div className="flex items-start justify-between gap-2">
                        <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-600 text-white font-bold text-lg flex items-center justify-center shadow-md">
                          {p.displayName.charAt(0)}
                        </div>
                        {profiles.length > 1 && (
                          <button
                            onClick={() => removeProfile(p.id)}
                            className="text-slate-400 hover:text-rose-500 text-xs p-1"
                          >
                            <X className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                      <div>
                        <h3 className="text-base font-bold text-slate-900 dark:text-white">{p.displayName}</h3>
                        <p className="text-xs text-slate-500">{p.professionalType?.replace(/_/g, ' ')}</p>
                      </div>
                      <Button
                        size="sm"
                        variant="primary"
                        onClick={() => navigate(`/marketplace/profile/${p.id}`)}
                        className="w-full text-xs rounded-xl"
                      >
                        View Full Profile
                      </Button>
                    </div>
                  </th>
                ))}
              </tr>
            </thead>

            <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-xs">
              {/* KYC Verification */}
              <tr>
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300 bg-slate-50/50 dark:bg-slate-800/30">
                  KYC Verification Badge
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4">
                    {p.verificationStatus === 'VERIFIED' ? (
                      <span className="inline-flex items-center gap-1 font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/60 px-2.5 py-1 rounded-full border border-emerald-200 dark:border-emerald-800">
                        <ShieldCheck className="w-3.5 h-3.5" />
                        ICAI / ICSI Verified
                      </span>
                    ) : (
                      <span className="text-slate-400 font-medium">Pending Verification</span>
                    )}
                  </td>
                ))}
              </tr>

              {/* Experience */}
              <tr>
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300 bg-slate-50/50 dark:bg-slate-800/30">
                  Years of Experience
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4 font-semibold text-slate-900 dark:text-white">
                    {p.experienceYears} Years in Practice
                  </td>
                ))}
              </tr>

              {/* Location */}
              <tr>
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300 bg-slate-50/50 dark:bg-slate-800/30">
                  Office Location
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4 flex items-center gap-1.5 text-slate-600 dark:text-slate-300">
                    <MapPin className="w-3.5 h-3.5 text-rose-500 shrink-0" />
                    <span>{p.city}, {p.state}</span>
                  </td>
                ))}
              </tr>

              {/* Client Rating */}
              <tr>
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300 bg-slate-50/50 dark:bg-slate-800/30">
                  Average Rating & Reviews
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4">
                    <div className="flex items-center gap-1 font-bold text-amber-500">
                      <Star className="w-4 h-4 fill-current" />
                      <span>{p.averageRating?.toFixed(1) || '5.0'}</span>
                      <span className="text-slate-400 font-normal">({p.totalReviews} reviews)</span>
                    </div>
                  </td>
                ))}
              </tr>

              {/* Starting Package Fee */}
              <tr>
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300 bg-slate-50/50 dark:bg-slate-800/30">
                  Starting Service Fee
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4 font-extrabold text-sm text-indigo-600 dark:text-indigo-400">
                    ₹{p.startingFee?.toLocaleString('en-IN') || '999'}
                  </td>
                ))}
              </tr>

              {/* Hourly Advisory Rate */}
              <tr>
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300 bg-slate-50/50 dark:bg-slate-800/30">
                  Hourly Advisory Rate
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4 font-bold text-slate-900 dark:text-white">
                    ₹{p.hourlyRate?.toLocaleString('en-IN') || '1500'} / hr
                  </td>
                ))}
              </tr>

              {/* Consultation Fee */}
              <tr>
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300 bg-slate-50/50 dark:bg-slate-800/30">
                  30-Min Strategy Consultation
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4">
                    {p.consultationEnabled ? (
                      <span className="font-bold text-emerald-600 dark:text-emerald-400">
                        Available (₹{p.consultationFee})
                      </span>
                    ) : (
                      <span className="text-slate-400">By Request Only</span>
                    )}
                  </td>
                ))}
              </tr>

              {/* Specializations */}
              <tr>
                <td className="p-4 font-bold text-slate-700 dark:text-slate-300 bg-slate-50/50 dark:bg-slate-800/30">
                  Key Specializations
                </td>
                {profiles.map((p) => (
                  <td key={p.id} className="p-4">
                    <div className="flex flex-wrap gap-1">
                      {p.specializations?.map((s, i) => (
                        <span
                          key={i}
                          className="text-[10px] bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 px-2 py-0.5 rounded"
                        >
                          {s.replace(/_/g, ' ')}
                        </span>
                      ))}
                    </div>
                  </td>
                ))}
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
