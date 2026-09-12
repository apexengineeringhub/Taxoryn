import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import {
  User as UserIcon,
  Mail,
  Phone,
  Building2,
  Shield,
  Briefcase,
  Camera,
  Trash2,
  CheckCircle2,
  AlertCircle,
  KeyRound,
  Sparkles,
  Save,
  RefreshCw,
  Info,
} from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { useAuth } from '../context/AuthContext';
import { userApi, employeeApi, portalApi } from '../api/endpoints';
import { User, Employee, ClientPortalProfile } from '../types';
import clsx from 'clsx';

export const UserProfilePage: React.FC = () => {
  const { user } = useAuth();

  const [loading, setLoading] = useState<boolean>(true);
  const [saving, setSaving] = useState<boolean>(false);
  const [uploadingAvatar, setUploadingAvatar] = useState<boolean>(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Form states
  const [firstName, setFirstName] = useState<string>('');
  const [lastName, setLastName] = useState<string>('');
  const [phone, setPhone] = useState<string>('');
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null);

  // Additional detail states
  const [employeeDetails, setEmployeeDetails] = useState<Employee | null>(null);
  const [clientDetails, setClientDetails] = useState<ClientPortalProfile | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const userRoleCodes = (user?.roles || []).map((r: any) => (typeof r === 'string' ? r : r.code || ''));
  const isClientUser = userRoleCodes.some((r: string) => ['CLIENT_USER', 'CLIENT_ADMIN', 'PRACTICE_CLIENT', 'MARKETPLACE_CUSTOMER'].includes(r));
  const isEmployee = !isClientUser;

  const loadUserProfile = async () => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const meUser = await userApi.getMe();
      setFirstName(meUser.firstName || '');
      setLastName(meUser.lastName || '');
      setPhone(meUser.phone || '');
      setAvatarUrl(meUser.avatarUrl || null);

      if (isEmployee) {
        try {
          const emp = await employeeApi.getMyProfile();
          setEmployeeDetails(emp);
          if (emp.avatarUrl) {
            setAvatarUrl(emp.avatarUrl);
          }
        } catch {
          // Fallback to user data if not registered as an employee entity
        }
      } else if (isClientUser) {
        try {
          const clientProf = await portalApi.getProfile();
          setClientDetails(clientProf);
          if (clientProf.avatarUrl) {
            setAvatarUrl(clientProf.avatarUrl);
          }
        } catch {
          // Fallback to user data
        }
      }
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || 'Failed to load user profile.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUserProfile();
  }, []);

  const handleProfileSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setSuccessMessage(null);
    setErrorMessage(null);

    try {
      if (isEmployee) {
        await employeeApi.updateMyProfile({
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          phone: phone.trim(),
        });
      } else {
        await userApi.updateMyProfile({
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          phone: phone.trim(),
        });
      }

      setSuccessMessage('Profile updated successfully.');
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || 'Failed to update profile.');
    } finally {
      setSaving(false);
    }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Client-side file validation
    const maxSizeBytes = 5 * 1024 * 1024; // 5MB
    const allowedMimeTypes = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp', 'image/gif'];

    if (!allowedMimeTypes.includes(file.type.toLowerCase())) {
      setErrorMessage('Invalid image format. Allowed formats are PNG, JPEG, WEBP, and GIF.');
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }

    if (file.size > maxSizeBytes) {
      setErrorMessage('Image size exceeds 5 MB limit.');
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }

    setUploadingAvatar(true);
    setSuccessMessage(null);
    setErrorMessage(null);

    try {
      let updatedAvatarUrl: string | undefined;
      if (isEmployee) {
        const emp = await employeeApi.uploadMyAvatar(file);
        updatedAvatarUrl = emp.avatarUrl;
      } else if (isClientUser) {
        const cl = await portalApi.uploadAvatar(file);
        updatedAvatarUrl = cl.avatarUrl;
      } else {
        const usr = await userApi.uploadMyAvatar(file);
        updatedAvatarUrl = usr.avatarUrl;
      }

      setAvatarUrl(updatedAvatarUrl || URL.createObjectURL(file));
      setSuccessMessage('Profile photo updated successfully.');
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || 'Failed to upload profile photo.');
    } finally {
      setUploadingAvatar(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleDeleteAvatar = async () => {
    if (!window.confirm('Are you sure you want to remove your profile photo?')) return;

    setUploadingAvatar(true);
    setSuccessMessage(null);
    setErrorMessage(null);

    try {
      if (isClientUser) {
        await portalApi.deleteAvatar();
      } else {
        await userApi.deleteMyAvatar();
      }
      setAvatarUrl(null);
      setSuccessMessage('Profile photo removed successfully.');
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || 'Failed to remove profile photo.');
    } finally {
      setUploadingAvatar(false);
    }
  };

  const getInitials = () => {
    const f = firstName ? firstName[0].toUpperCase() : '';
    const l = lastName ? lastName[0].toUpperCase() : '';
    return f + l || (user?.email ? user.email[0].toUpperCase() : 'U');
  };

  return (
    <div className="min-h-screen bg-slate-50/50 p-4 sm:p-6 lg:p-8 space-y-6 max-w-6xl mx-auto">
      {/* Page Title & Breadcrumb */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">My Profile</h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Manage your personal profile details, profile picture, and view account permissions.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Link to="/settings/security">
            <Button variant="outline" size="sm" className="gap-2 text-xs">
              <KeyRound className="w-4 h-4 text-slate-600" />
              Security & Password
            </Button>
          </Link>
        </div>
      </div>

      {/* Notifications */}
      {successMessage && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs sm:text-sm flex items-center gap-3">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          <p className="font-medium">{successMessage}</p>
        </div>
      )}

      {errorMessage && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs sm:text-sm flex items-center gap-3">
          <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
          <p className="font-medium">{errorMessage}</p>
        </div>
      )}

      {loading ? (
        <div className="p-12 flex flex-col items-center justify-center gap-3 bg-white rounded-2xl border border-slate-200 shadow-2xs">
          <RefreshCw className="w-8 h-8 text-brand-600 animate-spin" />
          <p className="text-sm font-medium text-slate-500">Loading your profile...</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Column: Avatar & Quick Summary */}
          <div className="space-y-6">
            <Card className="p-6 text-center space-y-5 bg-white border-slate-200 shadow-2xs">
              <div className="relative inline-block mx-auto">
                {avatarUrl ? (
                  <img
                    src={avatarUrl}
                    alt={`${firstName} ${lastName}`}
                    className="w-28 h-28 rounded-full object-cover border-4 border-white shadow-md ring-2 ring-slate-100"
                  />
                ) : (
                  <div className="w-28 h-28 rounded-full bg-gradient-to-tr from-brand-600 to-indigo-600 flex items-center justify-center text-white font-bold text-3xl shadow-md ring-2 ring-slate-100 mx-auto">
                    {getInitials()}
                  </div>
                )}

                {/* Upload Action Overlay / Button */}
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={uploadingAvatar}
                  title="Upload profile photo"
                  className="absolute bottom-0 right-0 p-2 bg-brand-600 hover:bg-brand-700 text-white rounded-full shadow-lg transition-transform hover:scale-105 disabled:opacity-50 cursor-pointer"
                >
                  <Camera className="w-4 h-4" />
                </button>
              </div>

              <input
                type="file"
                ref={fileInputRef}
                onChange={handleFileChange}
                accept="image/png,image/jpeg,image/jpg,image/webp,image/gif"
                className="hidden"
              />

              <div>
                <h2 className="text-lg font-bold text-slate-900 leading-snug">
                  {firstName} {lastName}
                </h2>
                <p className="text-xs text-slate-500 font-medium">{user?.email}</p>
              </div>

              <div className="flex flex-wrap items-center justify-center gap-1.5 pt-1">
                {userRoleCodes.map((role: string) => (
                  <span
                    key={role}
                    className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-brand-50 text-brand-700 border border-brand-200"
                  >
                    {role.replace(/_/g, ' ')}
                  </span>
                ))}
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={uploadingAvatar}
                  className="text-xs gap-1.5"
                >
                  <Camera className="w-3.5 h-3.5 text-slate-500" />
                  <span>{uploadingAvatar ? 'Uploading...' : 'Change Photo'}</span>
                </Button>

                {avatarUrl && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleDeleteAvatar}
                    disabled={uploadingAvatar}
                    className="text-xs gap-1.5 text-rose-600 border-rose-200 hover:bg-rose-50 hover:text-rose-700"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>Remove</span>
                  </Button>
                )}
              </div>

              <p className="text-[11px] text-slate-400">
                Allowed formats: PNG, JPG, WEBP, GIF. Max file size: 5 MB.
              </p>
            </Card>

            {/* Account & Tenant Security Overview */}
            <Card className="p-5 bg-white border-slate-200 shadow-2xs space-y-3">
              <div className="flex items-center gap-2 text-slate-900 font-bold text-xs uppercase tracking-wider">
                <Shield className="w-4 h-4 text-brand-600" />
                <span>Security & Isolation</span>
              </div>
              <p className="text-xs text-slate-600 leading-relaxed">
                Your profile details and documents are strictly scoped to your tenant workspace with fail-closed malware inspection on all media uploads.
              </p>
            </Card>
          </div>

          {/* Right Column: Edit Personal Details & System Details */}
          <div className="lg:col-span-2 space-y-6">
            {/* Form: Editable Personal Details */}
            <Card className="p-6 bg-white border-slate-200 shadow-2xs space-y-6">
              <div className="flex items-center justify-between border-b border-slate-100 pb-4">
                <div>
                  <h3 className="text-base font-bold text-slate-900">Personal Information</h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Update your personal contact details.
                  </p>
                </div>
                <Sparkles className="w-5 h-5 text-brand-500" />
              </div>

              <form onSubmit={handleProfileSave} className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      First Name <span className="text-rose-500">*</span>
                    </label>
                    <div className="relative">
                      <UserIcon className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                      <input
                        type="text"
                        required
                        value={firstName}
                        onChange={(e) => setFirstName(e.target.value)}
                        placeholder="John"
                        className="w-full pl-9 pr-3 py-2 text-xs sm:text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-colors"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Last Name
                    </label>
                    <div className="relative">
                      <UserIcon className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                      <input
                        type="text"
                        value={lastName}
                        onChange={(e) => setLastName(e.target.value)}
                        placeholder="Doe"
                        className="w-full pl-9 pr-3 py-2 text-xs sm:text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-colors"
                      />
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Phone Number
                    </label>
                    <div className="relative">
                      <Phone className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                      <input
                        type="tel"
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        placeholder="+91 98765 43210"
                        className="w-full pl-9 pr-3 py-2 text-xs sm:text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-colors"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Primary Email (Read-Only)
                    </label>
                    <div className="relative">
                      <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                      <input
                        type="email"
                        disabled
                        value={user?.email || ''}
                        className="w-full pl-9 pr-3 py-2 text-xs sm:text-sm bg-slate-50 border border-slate-200 rounded-lg text-slate-500 cursor-not-allowed select-none"
                      />
                    </div>
                  </div>
                </div>

                <div className="flex items-center justify-end pt-4 border-t border-slate-100">
                  <Button
                    type="submit"
                    disabled={saving}
                    className="gap-2 text-xs sm:text-sm"
                  >
                    <Save className="w-4 h-4" />
                    <span>{saving ? 'Saving...' : 'Save Profile Changes'}</span>
                  </Button>
                </div>
              </form>
            </Card>

            {/* Read-Only Account & Practice Details */}
            <Card className="p-6 bg-white border-slate-200 shadow-2xs space-y-4">
              <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                <div className="flex items-center gap-2">
                  <Building2 className="w-4 h-4 text-brand-600" />
                  <h3 className="text-sm font-bold text-slate-900">Organization & Practice Context</h3>
                </div>
                <span className="text-[11px] font-medium text-slate-400 flex items-center gap-1">
                  <Info className="w-3.5 h-3.5" />
                  Managed by Practice Admin
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                <div className="p-3 bg-slate-50/70 rounded-xl border border-slate-100">
                  <span className="text-[11px] font-semibold text-slate-500 block">Organization Name</span>
                  <span className="text-xs font-bold text-slate-800 mt-0.5 block">
                    {user?.organizationName || 'Default Organization'}
                  </span>
                </div>

                {employeeDetails && (
                  <>
                    <div className="p-3 bg-slate-50/70 rounded-xl border border-slate-100">
                      <span className="text-[11px] font-semibold text-slate-500 block">Employee Code</span>
                      <span className="text-xs font-bold text-slate-800 mt-0.5 block font-mono">
                        {employeeDetails.employeeCode || '—'}
                      </span>
                    </div>

                    <div className="p-3 bg-slate-50/70 rounded-xl border border-slate-100">
                      <span className="text-[11px] font-semibold text-slate-500 block">Department</span>
                      <span className="text-xs font-bold text-slate-800 mt-0.5 block">
                        {employeeDetails.department || 'Tax Practice'}
                      </span>
                    </div>

                    <div className="p-3 bg-slate-50/70 rounded-xl border border-slate-100">
                      <span className="text-[11px] font-semibold text-slate-500 block">Designation</span>
                      <span className="text-xs font-bold text-slate-800 mt-0.5 block">
                        {employeeDetails.designation || 'Staff'}
                      </span>
                    </div>
                  </>
                )}

                {clientDetails && (
                  <>
                    <div className="p-3 bg-slate-50/70 rounded-xl border border-slate-100">
                      <span className="text-[11px] font-semibold text-slate-500 block">PAN</span>
                      <span className="text-xs font-bold text-slate-800 mt-0.5 block font-mono">
                        {clientDetails.pan || '—'}
                      </span>
                    </div>

                    <div className="p-3 bg-slate-50/70 rounded-xl border border-slate-100">
                      <span className="text-[11px] font-semibold text-slate-500 block">GSTIN</span>
                      <span className="text-xs font-bold text-slate-800 mt-0.5 block font-mono">
                        {clientDetails.gstin || '—'}
                      </span>
                    </div>

                    {clientDetails.assignedPractitionerName && (
                      <div className="p-3 bg-slate-50/70 rounded-xl border border-slate-100 sm:col-span-2">
                        <span className="text-[11px] font-semibold text-slate-500 block">Assigned Tax Consultant</span>
                        <span className="text-xs font-bold text-slate-800 mt-0.5 block">
                          {clientDetails.assignedPractitionerName} ({clientDetails.assignedPractitionerEmail})
                        </span>
                      </div>
                    )}
                  </>
                )}
              </div>
            </Card>
          </div>
        </div>
      )}
    </div>
  );
};