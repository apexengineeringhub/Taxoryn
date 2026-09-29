import React, { useState, useEffect } from 'react';
import {
  X,
  User,
  Building2,
  MapPin,
  Briefcase,
  Mail,
  Phone,
  Shield,
  AlertCircle,
  CheckCircle2,
  Save,
} from 'lucide-react';
import { Button } from '../common/Button';
import { teamApi, locationApi } from '../../api/endpoints';
import { Employee, Role, PracticeBranchLocation } from '../../types';

interface EditEmployeeModalProps {
  isOpen: boolean;
  employee: Employee | null;
  availableRoles: Role[];
  onClose: () => void;
  onSuccess: () => void;
}

const DEPARTMENTS = [
  'Taxation',
  'GST Compliance',
  'Direct Tax & ITR',
  'TDS & Withholding',
  'Audit & Assurance',
  'Accounting & Bookkeeping',
  'Corporate Secretarial',
  'Litigation & Notices',
  'Management',
];

const DESIGNATIONS = [
  'Partner / Proprietor',
  'Senior Tax Consultant',
  'Tax Consultant',
  'Senior Tax Associate',
  'Tax Associate',
  'Junior Tax Associate',
  'Audit Associate',
  'Accountant',
  'Article Assistant',
  'Trainee / Intern',
];

export const EditEmployeeModal: React.FC<EditEmployeeModalProps> = ({
  isOpen,
  employee,
  availableRoles,
  onClose,
  onSuccess,
}) => {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [department, setDepartment] = useState('Taxation');
  const [designation, setDesignation] = useState('Tax Associate');
  const [roleCode, setRoleCode] = useState('TAX_ASSOCIATE');
  const [locationId, setLocationId] = useState('');
  const [locations, setLocations] = useState<PracticeBranchLocation[]>([]);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen && employee) {
      setFirstName(employee.firstName || '');
      setLastName(employee.lastName || '');
      setEmail(employee.email || '');
      setPhone(employee.phone || '');
      setDepartment(employee.department || 'Taxation');
      setDesignation(employee.designation || 'Tax Associate');
      setRoleCode(employee.roleCode || 'TAX_ASSOCIATE');
      setLocationId(employee.locationId || '');
      setErrorMessage(null);
      setSuccessMessage(null);

      // Load organization locations
      locationApi
        .getLocations()
        .then((locs) => {
          if (locs && Array.isArray(locs)) {
            setLocations(locs);
          }
        })
        .catch((err) => {
          console.warn('Could not load locations for edit employee modal', err);
        });
    }
  }, [isOpen, employee]);

  if (!isOpen || !employee) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    if (!firstName.trim()) {
      setErrorMessage('First name is required');
      return;
    }

    if (!email.trim() || !email.includes('@')) {
      setErrorMessage('A valid official email is required');
      return;
    }

    setIsSubmitting(true);
    try {
      await teamApi.updateEmployee(employee.id, {
        firstName: firstName.trim(),
        lastName: lastName.trim() || undefined,
        email: email.trim().toLowerCase(),
        phone: phone.trim() || undefined,
        department: department.trim(),
        designation: designation.trim(),
        roleCode: roleCode || undefined,
        locationId: locationId || undefined,
        locationIds: locationId ? [locationId] : [],
      });

      setSuccessMessage(`Successfully updated ${firstName.trim()}'s profile!`);
      setTimeout(() => {
        onSuccess();
        onClose();
      }, 600);
    } catch (err: any) {
      console.error('Failed to update employee:', err);
      const apiMsg =
        err.response?.data?.message ||
        err.message ||
        'Failed to update employee details. Please verify inputs.';
      setErrorMessage(apiMsg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="relative bg-white rounded-2xl max-w-lg w-full shadow-2xl border border-slate-200 overflow-hidden transform transition-all">
        {/* Modal Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-brand-50 text-brand-600 flex items-center justify-center border border-brand-100">
              <User className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">Edit Employee Profile</h3>
              <p className="text-xs text-slate-500">
                {employee.employeeNumber || employee.employeeCode} • {employee.firstName} {employee.lastName || ''}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {errorMessage && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-700 flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{errorMessage}</span>
            </div>
          )}

          {successMessage && (
            <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-700 flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>{successMessage}</span>
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                First Name <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                required
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                placeholder="e.g. Rahul"
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-hidden"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Last Name</label>
              <input
                type="text"
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                placeholder="e.g. Sharma"
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-hidden"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                Official Email <span className="text-rose-500">*</span>
              </label>
              <div className="relative">
                <Mail className="w-3.5 h-3.5 absolute left-3 top-2.5 text-slate-400" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@practice.com"
                  className="w-full pl-8 pr-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-hidden"
                />
              </div>
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Phone Number</label>
              <div className="relative">
                <Phone className="w-3.5 h-3.5 absolute left-3 top-2.5 text-slate-400" />
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="+91 98765 43210"
                  className="w-full pl-8 pr-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-hidden"
                />
              </div>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Department</label>
              <select
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-hidden bg-white"
              >
                {DEPARTMENTS.map((dept) => (
                  <option key={dept} value={dept}>
                    {dept}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">Designation (Job Title)</label>
              <select
                value={designation}
                onChange={(e) => setDesignation(e.target.value)}
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-hidden bg-white"
              >
                {DESIGNATIONS.map((desig) => (
                  <option key={desig} value={desig}>
                    {desig}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1 flex items-center gap-1.5">
              <MapPin className="w-3.5 h-3.5 text-brand-600" />
              <span>Assigned Location (Branch)</span>
            </label>
            <select
              value={locationId}
              onChange={(e) => setLocationId(e.target.value)}
              className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-hidden bg-white font-medium"
            >
              <option value="">Not Assigned</option>
              {locations.map((loc) => (
                <option key={loc.id} value={loc.id}>
                  {loc.name} {loc.isHeadOffice ? '(Head Office)' : ''} {loc.city ? `- ${loc.city}` : ''}
                </option>
              ))}
            </select>
            <p className="text-[10px] text-slate-400 mt-1">
              Select an active practice branch or keep Not Assigned.
            </p>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1 flex items-center gap-1.5">
              <Shield className="w-3.5 h-3.5 text-brand-600" />
              <span>Practice Role (RBAC)</span>
            </label>
            <select
              value={roleCode}
              onChange={(e) => setRoleCode(e.target.value)}
              className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-hidden bg-white"
            >
              <option value="TAX_ASSOCIATE">Tax Associate (TAX_ASSOCIATE)</option>
              <option value="SENIOR_TAX_ASSOCIATE">Senior Tax Associate (SENIOR_TAX_ASSOCIATE)</option>
              <option value="TAX_PROFESSIONAL">Tax Professional (TAX_PROFESSIONAL)</option>
              <option value="ACCOUNTANT">Staff Accountant (ACCOUNTANT)</option>
              <option value="STAFF">Staff / Article Assistant (STAFF)</option>
              <option value="ARTICLE_ASSISTANT">Article Assistant (ARTICLE_ASSISTANT)</option>
              <option value="PRACTITIONER">Practitioner (PRACTITIONER)</option>
              <option value="MANAGER">Tax Manager (MANAGER)</option>
              <option value="PARTNER">Partner / Practice Admin (PARTNER)</option>
            </select>
          </div>

          {/* Modal Actions */}
          <div className="pt-4 border-t border-slate-100 flex items-center justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" isLoading={isSubmitting} className="gap-1.5">
              <Save className="w-3.5 h-3.5" />
              <span>Save Changes</span>
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
