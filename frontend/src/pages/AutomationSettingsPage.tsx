import React, { useState, useEffect, useCallback } from 'react';
import { Settings, Zap, Plus, Edit, Trash2, XCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import { automationRuleApi } from '../api/endpoints';

interface AutomationRule {
  id: string;
  organizationId?: string;
  name: string;
  description?: string;
  eventType: string;
  daysOffset: number;
  offsetDescription: string;
  actionType: string;
  targetType: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

const EVENT_LABELS: Record<string, string> = {
  TASK_CREATED: 'Task Created',
  TASK_ASSIGNED: 'Task Assigned',
  TASK_DUE: 'Task Due',
  TASK_OVERDUE: 'Task Overdue',
  TASK_COMPLETED: 'Task Completed',
  DOCUMENT_UPLOADED: 'Document Uploaded',
  WORK_INSTANCE_CREATED: 'Work Instance Created',
  WORK_INSTANCE_DUE: 'Work Instance Due',
};

const TARGET_LABELS: Record<string, string> = {
  TASK_ASSIGNEE: 'Task Assignee',
  ENGAGEMENT_OWNER: 'Engagement Owner',
  WORK_INSTANCE_ASSIGNEE: 'Work Instance Assignee',
  SPECIFIC_USER: 'Specific User',
};

export function AutomationSettingsPage() {
  const [rules, setRules] = useState<AutomationRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingRule, setEditingRule] = useState<AutomationRule | null>(null);

  const fetchRules = useCallback(async () => {
    setLoading(true);
    try {
      const data = await automationRuleApi.list();
      setRules(data || []);
    } catch {
      toast.error('Failed to load automation rules');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchRules(); }, [fetchRules]);

  const handleToggle = async (rule: AutomationRule) => {
    if (!rule.organizationId) {
      toast.error('System default rules cannot be modified');
      return;
    }
    try {
      if (rule.enabled) {
        await automationRuleApi.disable(rule.id);
        toast.success(`Rule "${rule.name}" disabled`);
      } else {
        await automationRuleApi.enable(rule.id);
        toast.success(`Rule "${rule.name}" enabled`);
      }
      fetchRules();
    } catch {
      toast.error('Failed to update rule');
    }
  };

  const handleDelete = async (rule: AutomationRule) => {
    if (!rule.organizationId) {
      toast.error('System default rules cannot be deleted');
      return;
    }
    if (!confirm(`Delete rule "${rule.name}"?`)) return;
    try {
      await automationRuleApi.delete(rule.id);
      toast.success('Rule deleted');
      fetchRules();
    } catch {
      toast.error('Failed to delete rule');
    }
  };

  const handleSave = async (data: any) => {
    try {
      if (editingRule) {
        await automationRuleApi.update(editingRule.id, data);
        toast.success('Rule updated');
      } else {
        await automationRuleApi.create(data);
        toast.success('Rule created');
      }
      setShowModal(false);
      setEditingRule(null);
      fetchRules();
    } catch {
      toast.error('Failed to save rule');
    }
  };

  const orgRules = rules.filter(r => r.organizationId);
  const systemRules = rules.filter(r => !r.organizationId);

  return (
    <div className="max-w-5xl mx-auto p-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <Zap className="w-7 h-7 text-indigo-600" />
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Automation Rules</h1>
            <p className="text-sm text-gray-500">Configure rules that automatically create reminders from business events</p>
          </div>
        </div>
        <button
          onClick={() => { setEditingRule(null); setShowModal(true); }}
          className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 transition"
        >
          <Plus className="w-4 h-4" />
          New Rule
        </button>
      </div>

      {loading ? (
        <div className="text-center py-16 text-gray-500">Loading rules...</div>
      ) : (
        <div className="space-y-8">
          {/* Org Rules */}
          <div>
            <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-3">Your Rules ({orgRules.length})</h2>
            {orgRules.length === 0 ? (
              <div className="bg-gray-50 rounded-xl border border-dashed border-gray-300 p-8 text-center">
                <Settings className="w-10 h-10 mx-auto text-gray-300 mb-2" />
                <p className="text-gray-500 text-sm">No custom rules yet. System defaults are active below.</p>
              </div>
            ) : (
              <div className="space-y-2">
                {orgRules.map(rule => (
                  <RuleCard
                    key={rule.id}
                    rule={rule}
                    isSystemDefault={false}
                    onToggle={() => handleToggle(rule)}
                    onEdit={() => { setEditingRule(rule); setShowModal(true); }}
                    onDelete={() => handleDelete(rule)}
                  />
                ))}
              </div>
            )}
          </div>

          {/* System Defaults */}
          {systemRules.length > 0 && (
            <div>
              <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-3">System Defaults ({systemRules.length})</h2>
              <div className="space-y-2">
                {systemRules.map(rule => (
                  <RuleCard key={rule.id} rule={rule} isSystemDefault />
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {showModal && (
        <RuleModal
          rule={editingRule}
          onClose={() => { setShowModal(false); setEditingRule(null); }}
          onSave={handleSave}
        />
      )}
    </div>
  );
}

function RuleCard({
  rule,
  isSystemDefault,
  onToggle,
  onEdit,
  onDelete,
}: {
  rule: AutomationRule;
  isSystemDefault: boolean;
  onToggle?: () => void;
  onEdit?: () => void;
  onDelete?: () => void;
}) {
  return (
    <div className={`bg-white rounded-xl border p-4 ${isSystemDefault ? 'border-gray-200 opacity-80' : 'border-gray-200'}`}>
      <div className="flex items-center justify-between gap-4">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <h3 className="font-semibold text-sm text-gray-900">{rule.name}</h3>
            {isSystemDefault && (
              <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-500">System Default</span>
            )}
            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${rule.enabled ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
              {rule.enabled ? 'Active' : 'Disabled'}
            </span>
          </div>
          <div className="flex items-center gap-3 text-xs text-gray-500">
            <span className="bg-purple-50 text-purple-700 px-2 py-0.5 rounded">{EVENT_LABELS[rule.eventType] || rule.eventType}</span>
            <span>→ {rule.offsetDescription}</span>
            <span>→ {TARGET_LABELS[rule.targetType] || rule.targetType}</span>
          </div>
          {rule.description && <p className="text-xs text-gray-400 mt-1">{rule.description}</p>}
        </div>
        <div className="flex items-center gap-1.5 flex-shrink-0">
          {!isSystemDefault && (
            <>
              <button
                onClick={onToggle}
                className={`relative inline-flex h-6 w-11 items-center rounded-full transition ${rule.enabled ? 'bg-indigo-600' : 'bg-gray-300'}`}
              >
                <span className={`inline-block h-4 w-4 rounded-full bg-white transition transform ${rule.enabled ? 'translate-x-6' : 'translate-x-1'}`} />
              </button>
              <button onClick={onEdit} className="p-1.5 rounded-lg text-gray-400 hover:bg-gray-100 hover:text-gray-600" title="Edit">
                <Edit className="w-4 h-4" />
              </button>
              <button onClick={onDelete} className="p-1.5 rounded-lg text-gray-400 hover:bg-red-50 hover:text-red-500" title="Delete">
                <Trash2 className="w-4 h-4" />
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function RuleModal({ rule, onClose, onSave }: { rule: AutomationRule | null; onClose: () => void; onSave: (data: any) => void }) {
  const [name, setName] = useState(rule?.name || '');
  const [description, setDescription] = useState(rule?.description || '');
  const [eventType, setEventType] = useState(rule?.eventType || 'TASK_DUE');
  const [daysOffset, setDaysOffset] = useState(rule?.daysOffset ?? -2);
  const [targetType, setTargetType] = useState(rule?.targetType || 'TASK_ASSIGNEE');
  const [enabled, setEnabled] = useState(rule?.enabled ?? true);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) { toast.error('Name is required'); return; }
    onSave({ name: name.trim(), description: description.trim() || undefined, eventType, daysOffset, targetType, enabled });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg mx-4">
        <div className="flex items-center justify-between p-5 border-b">
          <h2 className="text-lg font-semibold text-gray-900">{rule ? 'Edit Rule' : 'Create Rule'}</h2>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-gray-100"><XCircle className="w-5 h-5 text-gray-400" /></button>
        </div>
        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Rule Name *</label>
            <input value={name} onChange={(e) => setName(e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm" placeholder="e.g. Remind 2 days before due date" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
            <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={2} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Event Type *</label>
              <select value={eventType} onChange={(e) => setEventType(e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
                {Object.entries(EVENT_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Days Offset *</label>
              <input type="number" value={daysOffset} onChange={(e) => setDaysOffset(parseInt(e.target.value) || 0)} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm" />
              <p className="text-xs text-gray-400 mt-0.5">Negative = before event, 0 = same day, positive = after</p>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Target</label>
              <select value={targetType} onChange={(e) => setTargetType(e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm">
                {Object.entries(TARGET_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </div>
            <div className="flex items-center gap-2 pt-6">
              <button
                type="button"
                onClick={() => setEnabled(!enabled)}
                className={`relative inline-flex h-6 w-11 items-center rounded-full transition ${enabled ? 'bg-indigo-600' : 'bg-gray-300'}`}
              >
                <span className={`inline-block h-4 w-4 rounded-full bg-white transition transform ${enabled ? 'translate-x-6' : 'translate-x-1'}`} />
              </button>
              <span className="text-sm text-gray-700">{enabled ? 'Enabled' : 'Disabled'}</span>
            </div>
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200">Cancel</button>
            <button type="submit" className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-700">{rule ? 'Update Rule' : 'Create Rule'}</button>
          </div>
        </form>
      </div>
    </div>
  );
}
