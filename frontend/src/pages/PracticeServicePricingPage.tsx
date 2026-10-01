import React, { useEffect, useState } from 'react';
import { Edit2, RefreshCw } from 'lucide-react';
import { Button } from '../components/common/Button';
import { practiceServicePricingApi, PracticeServicePrice } from '../api/endpoints';

const formatMoney = (value: number, currency: string) => new Intl.NumberFormat('en-IN', {
  style: 'currency', currency: currency || 'INR', maximumFractionDigits: 2,
}).format(value || 0);

export const PracticeServicePricingPage: React.FC = () => {
  const [services, setServices] = useState<PracticeServicePrice[]>([]);
  const [editing, setEditing] = useState<PracticeServicePrice | null>(null);
  const [mode, setMode] = useState<'DEFAULT' | 'CUSTOM'>('DEFAULT');
  const [customPrice, setCustomPrice] = useState('');
  const [enabled, setEnabled] = useState(true);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = async () => {
    setLoading(true);
    try { setServices(await practiceServicePricingApi.getAll()); }
    catch (e: any) { setError(e?.response?.data?.message || 'Could not load service pricing.'); }
    finally { setLoading(false); }
  };
  useEffect(() => { void load(); }, []);

  const openEdit = (service: PracticeServicePrice) => {
    setEditing(service); setMode(service.pricingMode);
    setCustomPrice(service.pricingMode === 'CUSTOM' ? String(service.practicePrice) : '');
    setEnabled(service.enabled); setError(''); setMessage('');
  };
  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!editing) return;
    if (mode === 'CUSTOM' && (!customPrice.trim() || !Number.isFinite(Number(customPrice)) || Number(customPrice) < 0 || !/^\d+(\.\d{1,2})?$/.test(customPrice.trim()))) {
      setError('Enter a valid non-negative price with up to two decimal places.'); return;
    }
    setSaving(true); setError('');
    try {
      await practiceServicePricingApi.update(editing.serviceCode, {
        pricingMode: mode,
        ...(mode === 'CUSTOM' ? { customPrice: Number(customPrice) } : {}),
        enabled,
      });
      setEditing(null); setMessage('Practice service price saved.'); await load();
    } catch (e: any) { setError(e?.response?.data?.message || 'Could not save service pricing.'); }
    finally { setSaving(false); }
  };

  return <main className="mx-auto max-w-7xl space-y-6 p-5 md:p-8">
    <header className="flex flex-wrap items-start justify-between gap-4">
      <div><h1 className="text-2xl font-bold text-slate-900">Services & Pricing</h1><p className="mt-1 text-sm text-slate-500">Set practice fees for Taxoryn’s professional service catalog.</p></div>
      <Button variant="outline" leftIcon={<RefreshCw className="h-4 w-4" />} onClick={() => void load()} disabled={loading}>Refresh</Button>
    </header>
    {message && <p role="status" className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">{message}</p>}
    {error && !editing && <p role="alert" className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
    <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      {loading ? <p className="p-6 text-sm text-slate-500">Loading service prices…</p> : <div className="overflow-x-auto">
        <table className="w-full min-w-[850px] text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr>{['Service', 'Module', 'Taxoryn Suggested Price', 'My Price', 'Pricing Mode', 'Status', 'Action'].map(h => <th key={h} className="px-4 py-3">{h}</th>)}</tr></thead>
          <tbody className="divide-y divide-slate-100">{services.map(service => <tr key={service.serviceCode}>
            <td className="px-4 py-3"><div className="font-semibold text-slate-900">{service.serviceName}</div><div className="font-mono text-[11px] text-slate-400">{service.serviceCode}</div></td>
            <td className="px-4 py-3 text-slate-600">{service.moduleCode}</td>
            <td className="px-4 py-3">{formatMoney(service.suggestedPrice, service.currency)}</td>
            <td className="px-4 py-3 font-semibold">{formatMoney(service.practicePrice, service.currency)}</td>
            <td className="px-4 py-3">{service.pricingMode === 'DEFAULT' ? 'Default' : 'Custom'}</td>
            <td className="px-4 py-3"><span className={`rounded-full px-2 py-1 text-xs ${service.enabled ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'}`}>{service.enabled ? 'Active' : 'Disabled'}</span></td>
            <td className="px-4 py-3"><Button variant="outline" size="sm" leftIcon={<Edit2 className="h-3.5 w-3.5" />} onClick={() => openEdit(service)}>Edit</Button></td>
          </tr>)}</tbody>
        </table>
      </div>}
    </section>
    <p className="text-xs text-slate-500">Suggested prices are professional service references. Government fees, statutory charges, interest, penalties, and late fees are separate.</p>

    {editing && <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <form onSubmit={save} className="w-full max-w-lg space-y-5 rounded-2xl bg-white p-6 shadow-2xl">
        <div><h2 className="text-lg font-bold text-slate-900">Edit price</h2><p className="mt-1 text-sm text-slate-500">{editing.serviceName}</p></div>
        <div className="rounded-lg bg-slate-50 p-3 text-sm"><span className="text-slate-500">Taxoryn Suggested Price: </span><strong>{formatMoney(editing.suggestedPrice, editing.currency)}</strong></div>
        <fieldset className="space-y-3"><legend className="mb-2 text-sm font-semibold text-slate-800">Pricing Mode</legend>
          <label className="flex items-center gap-2 text-sm"><input type="radio" checked={mode === 'DEFAULT'} onChange={() => setMode('DEFAULT')} /> Use Taxoryn Suggested Price</label>
          <label className="flex items-center gap-2 text-sm"><input type="radio" checked={mode === 'CUSTOM'} onChange={() => setMode('CUSTOM')} /> Set My Own Price</label>
        </fieldset>
        {mode === 'CUSTOM' && <label className="block text-sm font-medium">Custom Price<input className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" type="number" min="0" step="0.01" value={customPrice} onChange={e => setCustomPrice(e.target.value)} /></label>}
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={enabled} onChange={e => setEnabled(e.target.checked)} /> Service is enabled for my practice</label>
        <p className="rounded-lg bg-indigo-50 p-3 text-xs leading-5 text-indigo-900">Taxoryn suggested price is a reference price. Your custom price will be used for your clients.</p>
        {error && <p role="alert" className="text-sm text-rose-700">{error}</p>}
        <div className="flex justify-end gap-2"><Button type="button" variant="outline" onClick={() => setEditing(null)}>Cancel</Button><Button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save'}</Button></div>
      </form>
    </div>}
  </main>;
};
