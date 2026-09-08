import React, { useState, useEffect } from 'react';
import {
  X,
  Shield,
  User,
  AlertCircle,
  CheckCircle2,
  Lock,
  ArrowRight,
} from 'lucide-react';
import { Button } from '../common/Button';
import { teamApi } from '../../api/endpoints';
import { Employee, Role } from '../../types';

interface ChangeRoleModalProps {
  isOpen: boolean;
  employee: Employee | null;
  availableRoles: Role[];
  onClose: () => void;
  onSuccess: () => void;
}

export const ChangeRoleModal: React.FC<ChangeRoleModalProps> = ({
  isOpen,
  employee,
  availableRoles,
  onClose,
  onSuccess,
}) => {
  const [selectedRoleCode, setSelectedRoleCode] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    if (employee) {
      setSelectedRoleCode(employee.roleCode || 'PRACTITIONER');
      setErrorMessage(null);
      setSuccessMessage(null);
    }
  }, [employee, isOpen]);

  if (!isOpen || !employee) return null;

  const currentRoleCode = employee.roleCode || 'PRACTITIONER';
  const currentRoleName = employee.roleName || (
    availableRoles.find((r) => r.code === currentRoleCode)?.name || currentRoleCode
  );

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    if (!selectedRoleCode) {
      setErrorMessage('Please select a valid role.');
      return;
    }

    if (selectedRoleCode === currentRoleCode) {
      setErrorMessage('The employee already holds this role. Please select a different role.');
      return;
    }

    setIsSubmitting(true);
    try {
      // Use existing update employee endpoint with roleCode
      await teamApi.updateEmployee(employee.id, {
        firstName: employee.firstName,
        lastName: employee.lastName,
        email: employee.email,
        phone: employee.phone,
        department: employee.department,
        designation: employee.designation,
        status: employee.status,
        roleCode: selectedRoleCode,
      });

      setSuccessMessage(`Successfully updated role to ${availableRoles.find(r => r.code === selectedRoleCode)?.name || selectedRoleCode}!`);
      setTimeout(() => {
        onSuccess();
        onClose();
      }, 900);
    } catch (err: any) {
      console.error('Failed to change employee role:', err);
      const apiMsg = err.response?.data?.message || err.message || 'Failed to update employee role. Please check organization permissions.';
      setErrorMessage(apiMsg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="relative bg-white rounded-2xl max-w-md w-full shadow-2xl border border-slate-200 overflow-hidden transform transition-all">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-brand-50 text-brand-600 flex items-center justify-center border border-brand-100">
              <Shield className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">Change Employee Role</h3>
              <p className="text-xs text-slate-500">Update practice authorization and permissions</p>
            </div>
          </div>
          <button
            onClick={onClose}
            disabled={isSubmitting}
            className="text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-100 transition-colors disabled:opacity-50"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {errorMessage && (
            <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-xl flex items-start gap-2.5 text-rose-800 text-xs">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-600" />
              <div className="flex-1 font-medium">{errorMessage}</div>
            </div>
          )}

          {successMessage && (
            <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center gap-2.5 text-emerald-800 text-xs font-semibold">
              <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-600" />
              <div>{successMessage}</div>
            </div>
          )}

          {/* Employee summary */}
          <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200/80 space-y-1.5">
            <div className="flex items-center justify-between text-xs">
              <span className="text-slate-500 font-medium">Employee:</span>
              <span className="font-bold text-slate-900">{employee.fullName || `${employee.firstName} ${employee.lastName || ''}`}</span>
            </div>
            <div className="flex items-center justify-between text-xs">
              <span className="text-slate-500 font-medium">Code & Designation:</span>
              <span className="text-slate-700 font-mono text-[11px]">{employee.employeeCode} • {employee.designation}</span>
            </div>
            <div className="flex items-center justify-between text-xs">
              <span className="text-slate-500 font-medium">Email:</span>
              <span className="text-slate-700 text-[11px]">{employee.email}</span>
            </div>
            <div className="flex items-center justify-between text-xs pt-1 border-t border-slate-200/60">
              <span className="text-slate-500 font-medium">Current Role:</span>
              <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-bold bg-slate-200/80 text-slate-800">
                {currentRoleName}
              </span>
            </div>
          </div>

          {/* New Role Selector */}
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1.5">
              New Practice Role <span className="text-rose-500">*</span>
            </label>
            <div className="relative">
              <select
                value={selectedRoleCode}
                onChange={(e) => setSelectedRoleCode(e.target.value)}
                disabled={isSubmitting}
                className="w-full px-3.5 py-2.5 text-xs font-medium border border-slate-300 rounded-xl focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-hidden bg-white text-slate-800"
              >
                {availableRoles.map((role) => (
                  <option key={role.id || role.code} value={role.code}>
                    {role.name} ({role.code})
                  </option>
                ))}
              </select>
            </div>
            <p className="text-[11px] text-slate-400 mt-1.5">
              Only practice-scoped roles can be assigned. Taxoryn platform-internal roles cannot be selected.
            </p>
          </div>

          {/* Role difference visual indicator */}
          {selectedRoleCode && selectedRoleCode !== currentRoleCode && (
            <div className="p-3 bg-brand-50/60 border border-brand-200 rounded-xl text-xs text-brand-900 flex items-center justify-between">
              <span className="font-semibold text-[11px] text-slate-600">{currentRoleName}</span>
              <ArrowRight className="w-3.5 h-3.5 text-brand-600 shrink-0" />
              <span className="font-bold text-[11px] text-brand-700">
                {availableRoles.find(r => r.code === selectedRoleCode)?.name || selectedRoleCode}
              </span>
            </div>
          )}

          {/* Security footnote */}
          <div className="flex items-start gap-2 p-3 bg-slate-50 rounded-xl border border-slate-200 text-[11px] text-slate-600">
            <Lock className="w-3.5 h-3.5 text-brand-600 shrink-0 mt-0.5" />
            <span>
              Role transitions preserve current employment status (e.g. INVITED/ACTIVE) and are logged to the practice security audit log.
            </span>
          </div>

          {/* Footer Actions */}
          <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              isLoading={isSubmitting}
              disabled={isSubmitting || selectedRoleCode === currentRoleCode}
            >
              Update Role
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
