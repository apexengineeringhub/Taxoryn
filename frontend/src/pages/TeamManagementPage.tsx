import React, { useState, useEffect } from 'react';
import { UserCheck, Shield, Plus, Mail, Phone } from 'lucide-react';
import { DataTable, Column } from '../components/common/DataTable';
import { StatusBadge } from '../components/common/StatusBadge';
import { Button } from '../components/common/Button';
import { teamApi } from '../api/endpoints';
import { Employee, Role } from '../types';

export const TeamManagementPage: React.FC = () => {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [activeTab, setActiveTab] = useState<'employees' | 'roles'>('employees');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setIsLoading(true);
      const [empRes, roleRes] = await Promise.all([
        teamApi.getEmployees(),
        teamApi.getRoles(),
      ]);
      setEmployees(empRes.content || []);
      setRoles(roleRes || []);
    } catch (err) {
      console.error('Failed to load team data', err);
    } finally {
      setIsLoading(false);
    }
  };

  const employeeColumns: Column<Employee>[] = [
    {
      header: 'Employee Name & Code',
      accessor: (row) => (
        <div>
          <span className="font-bold text-slate-900 block">{row.firstName} {row.lastName || ''}</span>
          <span className="font-mono text-[10px] text-slate-400 block">{row.employeeCode}</span>
        </div>
      ),
    },
    {
      header: 'Email Address',
      accessor: (row) => <span className="text-xs text-slate-700">{row.email}</span>,
    },
    {
      header: 'Department',
      accessor: (row) => <span className="text-xs font-medium text-slate-700">{row.department}</span>,
    },
    {
      header: 'Designation',
      accessor: (row) => <span className="text-xs text-slate-600">{row.designation}</span>,
    },
    {
      header: 'Status',
      accessor: (row) => <StatusBadge status={row.status} size="sm" />,
      align: 'center',
    },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">Team Directory & RBAC</h1>
          <p className="text-xs text-slate-500 mt-1">
            Manage practice partners, senior accountants, staff privileges, and role permissions.
          </p>
        </div>
        <Button leftIcon={<Plus className="w-4 h-4" />}>Add Team Member</Button>
      </div>

      {/* Tabs */}
      <div className="border-b border-slate-200 flex items-center gap-2">
        <button
          onClick={() => setActiveTab('employees')}
          className={`px-4 py-2.5 text-xs font-bold border-b-2 transition-all ${
            activeTab === 'employees'
              ? 'border-brand-600 text-brand-600 bg-brand-50/50 rounded-t-lg'
              : 'border-transparent text-slate-500 hover:text-slate-700'
          }`}
        >
          Employee Directory ({employees.length})
        </button>
        <button
          onClick={() => setActiveTab('roles')}
          className={`px-4 py-2.5 text-xs font-bold border-b-2 transition-all ${
            activeTab === 'roles'
              ? 'border-brand-600 text-brand-600 bg-brand-50/50 rounded-t-lg'
              : 'border-transparent text-slate-500 hover:text-slate-700'
          }`}
        >
          Roles & Permissions Matrix ({roles.length})
        </button>
      </div>

      {/* Content */}
      {activeTab === 'employees' ? (
        <DataTable
          columns={employeeColumns}
          data={employees}
          isLoading={isLoading}
          searchPlaceholder="Search employees by name, email, or code..."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {roles.map((role) => (
            <div key={role.id} className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-card space-y-3">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs font-bold bg-slate-100 text-slate-800 px-2 py-0.5 rounded border border-slate-200">
                  {role.code}
                </span>
                {role.isSystemRole && (
                  <span className="text-[10px] text-brand-600 font-semibold bg-brand-50 border border-brand-200 px-2 py-0.5 rounded-full">
                    System Role
                  </span>
                )}
              </div>
              <div>
                <h4 className="text-sm font-bold text-slate-900">{role.name}</h4>
                <p className="text-xs text-slate-500 mt-0.5">{role.description || 'Pre-configured access role'}</p>
              </div>
              <div className="pt-3 border-t border-slate-100 text-[10px] text-slate-500">
                <span className="font-semibold text-slate-700">{role.permissions?.length || 0}</span> Permissions Configured
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
