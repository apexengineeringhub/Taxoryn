import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { BrandingProvider } from './context/BrandingContext';
import { AppShell } from './components/layout/AppShell';

// Pages
import { LoginPage } from './pages/LoginPage';
import { RegisterOrgPage } from './pages/RegisterOrgPage';
import { DashboardPage } from './pages/DashboardPage';
import { ClientsPage } from './pages/ClientsPage';
import { ClientMigrationHubPage } from './pages/ClientMigrationHubPage';
import { TasksPage } from './pages/TasksPage';
import { BulkTasksGeneratorPage } from './pages/BulkTasksGeneratorPage';
import { GstCompliancePage } from './pages/GstCompliancePage';
import { GstDataMigrationHubPage } from './pages/GstDataMigrationHubPage';
import { ItrCompliancePage } from './pages/ItrCompliancePage';
import { ItrDataMigrationHubPage } from './pages/ItrDataMigrationHubPage';
import { TdsCompliancePage } from './pages/TdsCompliancePage';
import { TdsDataMigrationHubPage } from './pages/TdsDataMigrationHubPage';
import { ComplianceCalendarPage } from './pages/ComplianceCalendarPage';
import { DocumentsPage } from './pages/DocumentsPage';
import { BillingPage } from './pages/BillingPage';
import { ClientPortalManagementPage } from './pages/ClientPortalManagementPage';
import { TeamManagementPage } from './pages/TeamManagementPage';
import { BulkEmployeeOnboardingPage } from './pages/BulkEmployeeOnboardingPage';
import { AuditLogsPage } from './pages/AuditLogsPage';
import { PracticeBrandingPage } from './pages/PracticeBrandingPage';
import { SubscriptionsPage } from './pages/SubscriptionsPage';

// Protected Route Guard
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="h-screen w-screen flex items-center justify-center bg-slate-900 text-white text-xs">
        <div className="flex items-center gap-2">
          <svg className="animate-spin h-5 w-5 text-brand-500" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
          </svg>
          <span>Authenticating practice context...</span>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrandingProvider>
        <BrowserRouter>
          <Routes>
            {/* Public Auth Routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterOrgPage />} />

            {/* Protected Application Routes */}
            <Route
              element={
                <ProtectedRoute>
                  <AppShell />
                </ProtectedRoute>
              }
            >
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/clients" element={<ClientsPage />} />
              <Route path="/clients/migration" element={<ClientMigrationHubPage />} />
              <Route path="/tasks" element={<TasksPage />} />
              <Route path="/tasks/bulk" element={<BulkTasksGeneratorPage />} />
              <Route path="/gst" element={<GstCompliancePage />} />
              <Route path="/gst/migration" element={<GstDataMigrationHubPage />} />
              <Route path="/itr" element={<ItrCompliancePage />} />
              <Route path="/itr/migration" element={<ItrDataMigrationHubPage />} />
              <Route path="/tds" element={<TdsCompliancePage />} />
              <Route path="/tds/migration" element={<TdsDataMigrationHubPage />} />
              <Route path="/calendar" element={<ComplianceCalendarPage />} />
              <Route path="/documents" element={<DocumentsPage />} />
              <Route path="/billing" element={<BillingPage />} />
              <Route path="/portal" element={<ClientPortalManagementPage />} />
              <Route path="/team" element={<TeamManagementPage />} />
              <Route path="/team/bulk" element={<BulkEmployeeOnboardingPage />} />
              <Route path="/audit-logs" element={<AuditLogsPage />} />
              <Route path="/settings/branding" element={<PracticeBrandingPage />} />
              <Route path="/settings/subscription" element={<SubscriptionsPage />} />
            </Route>

            {/* Catch all redirect */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
      </BrandingProvider>
    </AuthProvider>
  );
};
