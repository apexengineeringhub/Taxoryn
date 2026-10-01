import React, { useState } from 'react';
import { Calculator, GitCompareArrows, IndianRupee } from 'lucide-react';
import { itrApi, TaxCalculationInput, TaxCalculationResult, TaxRegimeComparison } from '../api/endpoints';
import { Button } from '../components/common/Button';

const initialInput: TaxCalculationInput = {
  assessmentYear: '2026-27', taxpayerType: 'INDIVIDUAL', age: 35,
  residentialStatus: 'RESIDENT', regime: 'NEW', totalIncome: 0, deductions: 0,
};
const money = (value: number) => new Intl.NumberFormat('en-IN', {
  style: 'currency', currency: 'INR', maximumFractionDigits: 2,
}).format(value || 0);

const Breakdown = ({ result }: { result: TaxCalculationResult }) => (
  <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
    <div className="mb-4 flex items-center justify-between">
      <h2 className="text-lg font-bold text-slate-900">{result.regime === 'OLD' ? 'Old regime' : 'New regime'}</h2>
      <span className="rounded-full bg-indigo-50 px-3 py-1 text-sm font-semibold text-indigo-700">{result.assessmentYear}</span>
    </div>
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {[
        ['Total income', result.totalIncome], ['Deductions', result.deductions], ['Taxable income', result.taxableIncome],
        ['Tax before rebate', result.slabTax], ['Rebate', result.rebate], ['Tax after rebate', result.taxAfterRebate],
        ['Surcharge', result.surcharge], ['Health & Education Cess', result.cess], ['Final tax', result.totalTax],
      ].map(([label, value]) => <div key={String(label)} className="rounded-lg bg-slate-50 p-3">
        <div className="text-xs text-slate-500">{label}</div><div className="mt-1 font-semibold text-slate-900">{money(Number(value))}</div>
      </div>)}
    </div>
    <h3 className="mb-2 mt-6 font-semibold text-slate-800">Slab-wise calculation</h3>
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm"><thead className="text-xs text-slate-500"><tr><th className="py-2">Income range</th><th>Rate</th><th className="text-right">Tax</th></tr></thead>
        <tbody>{result.slabBreakdown.map((line, index) => <tr key={index} className="border-t border-slate-100"><td className="py-2">{money(line.from)} – {money(line.to)}</td><td>{line.rate}%</td><td className="text-right">{money(line.tax)}</td></tr>)}</tbody>
      </table>
    </div>
  </section>
);

export const ItrTaxCalculatorPage: React.FC = () => {
  const [input, setInput] = useState(initialInput);
  const [result, setResult] = useState<TaxCalculationResult | null>(null);
  const [comparison, setComparison] = useState<TaxRegimeComparison | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const update = (key: keyof TaxCalculationInput, value: string) => setInput(current => ({
    ...current, [key]: key === 'assessmentYear' || key === 'regime' || key === 'residentialStatus'
      ? value : Number(value),
  }));
  const run = async (compare: boolean) => {
    setBusy(true); setError(''); setResult(null); setComparison(null);
    try {
      if (compare) setComparison(await itrApi.compareTaxRegimes({ ...input }));
      else setResult(await itrApi.calculateTax(input));
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Unable to calculate tax. Check the inputs and try again.');
    } finally { setBusy(false); }
  };
  const fieldClass = 'mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-100';
  return <main className="mx-auto max-w-6xl space-y-6 p-5 md:p-8">
    <header><div className="flex items-center gap-3"><span className="rounded-xl bg-indigo-100 p-3 text-indigo-700"><Calculator /></span><div><h1 className="text-2xl font-bold text-slate-900">ITR Tax Calculator</h1><p className="mt-1 text-sm text-slate-500">Estimated individual income tax for both regimes.</p></div></div></header>
    <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <label className="text-sm font-medium text-slate-700">Assessment year<select className={fieldClass} value={input.assessmentYear} onChange={e => update('assessmentYear', e.target.value)}><option>2026-27</option></select></label>
        <label className="text-sm font-medium text-slate-700">Taxpayer type<input className={fieldClass} value="Individual" disabled /></label>
        <label className="text-sm font-medium text-slate-700">Age<input className={fieldClass} type="number" min="0" value={input.age} onChange={e => update('age', e.target.value)} /></label>
        <label className="text-sm font-medium text-slate-700">Residential status<select className={fieldClass} value={input.residentialStatus} onChange={e => update('residentialStatus', e.target.value)}><option value="RESIDENT">Resident</option><option value="NON_RESIDENT">Non-resident</option></select></label>
        <label className="text-sm font-medium text-slate-700">Total income (₹)<input className={fieldClass} type="number" min="0" step="0.01" value={input.totalIncome} onChange={e => update('totalIncome', e.target.value)} /></label>
        <label className="text-sm font-medium text-slate-700">Deductions (₹)<input className={fieldClass} type="number" min="0" step="0.01" value={input.deductions} onChange={e => update('deductions', e.target.value)} /></label>
        <label className="text-sm font-medium text-slate-700">Tax regime<select className={fieldClass} value={input.regime} onChange={e => update('regime', e.target.value)}><option value="OLD">Old regime</option><option value="NEW">New regime</option></select></label>
      </div>
      <div className="mt-5 flex flex-wrap gap-3"><Button onClick={() => run(false)} disabled={busy} leftIcon={<IndianRupee className="h-4 w-4" />}>{busy ? 'Calculating…' : 'Calculate'}</Button><Button variant="outline" onClick={() => run(true)} disabled={busy} leftIcon={<GitCompareArrows className="h-4 w-4" />}>Compare Old vs New</Button></div>
      {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}
    </section>
    {result && <Breakdown result={result} />}
    {comparison && <div className="space-y-4"><div className="rounded-xl bg-indigo-50 p-4 text-sm text-indigo-900">Tax difference (old regime minus new regime): <strong>{money(comparison.taxDifference)}</strong></div><div className="grid gap-4 lg:grid-cols-2"><Breakdown result={comparison.oldRegime} /><Breakdown result={comparison.newRegime} /></div></div>}
    <p className="text-xs leading-5 text-slate-500">This is an estimation and calculation tool, not a complete government ITR filing engine. AY 2026-27 rules are supported in this version.</p>
  </main>;
};
