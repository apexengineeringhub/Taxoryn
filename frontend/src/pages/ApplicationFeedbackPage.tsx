import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertCircle, ArrowLeft, CheckCircle2, Lightbulb, MessageCircle, Star, Bug } from 'lucide-react';
import { applicationFeedbackApi } from '../api/endpoints';
import { ApplicationFeedbackCategory, ApplicationFeedbackType, CreateApplicationFeedbackRequest } from '../types';
import { Button } from '../components/common/Button';
import { Card } from '../components/common/Card';

const feedbackOptions: Array<{ type: ApplicationFeedbackType; label: string; description: string; icon: React.ElementType }> = [
  { type: 'SUGGESTION', label: 'Suggest an improvement', description: 'Share an idea that could make Taxoryn better.', icon: Lightbulb },
  { type: 'PROBLEM', label: 'Report a problem', description: 'Let us know when something did not work as expected.', icon: Bug },
  { type: 'GENERAL', label: 'General feedback', description: 'Tell us what you think about Taxoryn.', icon: MessageCircle },
  { type: 'EXPERIENCE', label: 'Share your experience', description: 'Tell us how your experience with Taxoryn has been.', icon: Star },
];

const categories: Array<{ value: ApplicationFeedbackCategory; label: string }> = [
  { value: 'APPLICATION_EXPERIENCE', label: 'Overall application experience' },
  { value: 'PRACTICE_SEARCH', label: 'Finding a tax practice' },
  { value: 'PRACTICE_PROFILE', label: 'Practice profile' },
  { value: 'CUSTOMER_PROFILE', label: 'My profile' },
  { value: 'TAX_SERVICE', label: 'Tax services' },
  { value: 'REQUIREMENT', label: 'Tax requirements' },
  { value: 'MATCHING', label: 'Practice matching' },
  { value: 'ENQUIRY', label: 'Enquiries' },
  { value: 'REVIEWS', label: 'Reviews' },
  { value: 'ACCOUNT', label: 'Account or sign-in' },
  { value: 'PERFORMANCE', label: 'Speed or performance' },
  { value: 'OTHER', label: 'Other' },
];

const prompts: Record<ApplicationFeedbackType, string> = {
  SUGGESTION: 'What would you like us to improve?',
  PROBLEM: 'What went wrong?',
  GENERAL: 'Tell us what you think.',
  EXPERIENCE: 'How was your experience with Taxoryn?',
};

export const ApplicationFeedbackPage: React.FC = () => {
  const [selectedType, setSelectedType] = useState<ApplicationFeedbackType | null>(null);
  const [form, setForm] = useState<Partial<CreateApplicationFeedbackRequest>>({});
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  const selectType = (type: ApplicationFeedbackType) => {
    setSelectedType(type);
    setForm((previous) => ({ ...previous, type }));
    setError(null);
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!selectedType || !form.category || !form.title?.trim() || !form.description?.trim()) {
      setError('Please choose a type and complete the category, title, and message.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await applicationFeedbackApi.create(form as CreateApplicationFeedbackRequest, {
        page: window.location.pathname,
        feature: 'MARKETPLACE_CUSTOMER_FEEDBACK',
      });
      setSubmitted(true);
    } catch (requestError: any) {
      setError(requestError?.response?.data?.message || 'We could not submit your feedback. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  if (submitted) {
    return (
      <div className="min-h-screen bg-slate-50 px-4 py-10">
        <div className="max-w-2xl mx-auto">
          <Card className="text-center">
            <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto mb-4" />
            <h1 className="text-xl font-bold text-slate-900">Thank you for your feedback</h1>
            <p className="mt-2 text-sm text-slate-600">Your input helps us improve Taxoryn.</p>
            <Link to="/marketplace/customer/dashboard" className="inline-block mt-6">
              <Button>Back to dashboard</Button>
            </Link>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 px-4 py-8 sm:py-12">
      <div className="max-w-3xl mx-auto space-y-5">
        <Link to="/marketplace/customer/dashboard" className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-600 hover:text-brand-700">
          <ArrowLeft className="w-4 h-4" /> Back to dashboard
        </Link>
        <div>
          <p className="text-xs font-bold uppercase tracking-wider text-brand-600">Taxoryn feedback</p>
          <h1 className="mt-1 text-2xl font-bold text-slate-900">What would you like to tell us?</h1>
          <p className="mt-2 text-sm text-slate-600">This feedback is about using Taxoryn, not the tax practice or professional you worked with.</p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {feedbackOptions.map((option) => {
            const Icon = option.icon;
            const active = selectedType === option.type;
            return (
              <button key={option.type} type="button" onClick={() => selectType(option.type)} className={`text-left p-4 rounded-xl border transition-all ${active ? 'border-brand-500 bg-brand-50 ring-1 ring-brand-300' : 'border-slate-200 bg-white hover:border-brand-300 hover:shadow-sm'}`}>
                <Icon className={`w-5 h-5 mb-2 ${active ? 'text-brand-600' : 'text-slate-500'}`} />
                <p className="text-sm font-bold text-slate-900">{option.label}</p>
                <p className="mt-1 text-xs text-slate-500">{option.description}</p>
              </button>
            );
          })}
        </div>

        {selectedType && (
          <Card title="Your feedback" subtitle={prompts[selectedType]}>
            <form onSubmit={submit} className="space-y-5">
              {error && <div className="flex gap-2 rounded-lg bg-rose-50 border border-rose-200 p-3 text-xs text-rose-700"><AlertCircle className="w-4 h-4 shrink-0" />{error}</div>}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">Category</label>
                <select value={form.category || ''} onChange={(event) => setForm({ ...form, category: event.target.value as ApplicationFeedbackCategory })} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:ring-2 focus:ring-brand-100 outline-none" required>
                  <option value="">Choose a category</option>
                  {categories.map((category) => <option key={category.value} value={category.value}>{category.label}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">Short title</label>
                <input value={form.title || ''} onChange={(event) => setForm({ ...form, title: event.target.value })} maxLength={160} placeholder="Briefly summarize your feedback" className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:ring-2 focus:ring-brand-100 outline-none" required />
              </div>
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1.5">{prompts[selectedType]}</label>
                <textarea value={form.description || ''} onChange={(event) => setForm({ ...form, description: event.target.value })} maxLength={4000} rows={5} placeholder="Please do not include tax documents, bank details, passwords, or other sensitive information." className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:ring-2 focus:ring-brand-100 outline-none resize-y" required />
                <p className="mt-1 text-right text-[11px] text-slate-400">{form.description?.length || 0}/4000</p>
              </div>
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-2">Rating <span className="font-normal text-slate-400">(optional)</span></label>
                <div className="flex gap-2">
                  {[1, 2, 3, 4, 5].map((rating) => <button key={rating} type="button" aria-label={`${rating} star${rating === 1 ? '' : 's'}`} onClick={() => setForm({ ...form, rating: form.rating === rating ? undefined : rating })} className="p-1"><Star className={`w-7 h-7 ${rating <= (form.rating || 0) ? 'fill-amber-400 text-amber-400' : 'text-slate-300 hover:text-amber-300'}`} /></button>)}
                </div>
              </div>
              <div className="flex justify-end pt-1"><Button type="submit" isLoading={submitting}>Submit feedback</Button></div>
            </form>
          </Card>
        )}
      </div>
    </div>
  );
};
