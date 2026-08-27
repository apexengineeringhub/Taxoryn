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
import { MarketplaceExplorePage } from './pages/MarketplaceExplorePage';
import { MarketplaceProfileDetailPage } from './pages/MarketplaceProfileDetailPage';
import { PracticePublicProfilePage } from './pages/PracticePublicProfilePage';
import { MarketplaceComparePage } from './pages/MarketplaceComparePage';
import { PracticeMarketplaceProfilePage } from './pages/PracticeMarketplaceProfilePage';
import { MarketplaceLeadsPage } from './pages/MarketplaceLeadsPage';
import { PlatformAdminMarketplacePage } from './pages/PlatformAdminMarketplacePage';
import { MarketplaceOnboardingHubPage } from './pages/MarketplaceOnboardingHubPage';
import { CustomerOnboardingPortalPage } from './pages/CustomerOnboardingPortalPage';
import { RegisterCustomerPage } from './pages/RegisterCustomerPage';
import { MarketplaceCustomerDashboardPage } from './pages/MarketplaceCustomerDashboardPage';
import { CustomerProfilePage } from './pages/CustomerProfilePage';
import { CustomerTaxRequirementWizardPage } from './pages/CustomerTaxRequirementWizardPage';
import { CustomerTaxRequirementsListPage } from './pages/CustomerTaxRequirementsListPage';
import { ApplicationFeedbackPage } from './pages/ApplicationFeedbackPage';
import { AdminFeedbackPage } from './pages/AdminFeedbackPage';
import { PlatformOverviewPage } from './pages/PlatformOverviewPage';
import { PlatformPracticesPage } from './pages/PlatformPracticesPage';
import { PlatformUsersPage } from './pages/PlatformUsersPage';
import { PlatformSubscriptionsPage } from './pages/PlatformSubscriptionsPage';
import { LearnLandingPage } from './pages/learn/LearnLandingPage';
import { LearnContentBrowsePage } from './pages/learn/LearnContentBrowsePage';
import { LearnContentDetailPage } from './pages/learn/LearnContentDetailPage';
import { PlatformContentManagementPage } from './pages/PlatformContentManagementPage';

import { RoleRouteGuard } from './components/common/RoleRouteGuard';

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
          <span>Authenticating context...</span>
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
            {/* Public Auth & Discovery Routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterOrgPage />} />
            <Route path="/marketplace/register" element={<RegisterCustomerPage />} />
            <Route path="/marketplace" element={<MarketplaceExplorePage />} />
            <Route path="/practice/:slug" element={<PracticePublicProfilePage />} />
            <Route path="/professional/:slug" element={<PracticePublicProfilePage />} />
            <Route path="/marketplace/profile/:id" element={<PracticePublicProfilePage />} />
            <Route path="/marketplace/:slug" element={<PracticePublicProfilePage />} />
            <Route path="/marketplace/compare" element={<MarketplaceComparePage />} />
            <Route path="/marketplace/onboarding/:token" element={<CustomerOnboardingPortalPage />} />
            <Route path="/marketplace/proposal/:token" element={<CustomerOnboardingPortalPage />} />

            {/* Taxoryn Learn Public Knowledge Hub (Clean SEO Routes) */}
            <Route path="/learn" element={<LearnLandingPage />} />
            <Route path="/learn/content" element={<LearnContentBrowsePage />} />
            <Route path="/learn/articles" element={<LearnContentBrowsePage />} />
            <Route path="/learn/videos" element={<LearnContentBrowsePage />} />
            <Route path="/learn/guides" element={<LearnContentBrowsePage />} />
            <Route path="/learn/faqs" element={<LearnContentBrowsePage />} />
            <Route path="/learn/content/:slug" element={<LearnContentDetailPage />} />
            <Route path="/learn/articles/:slug" element={<LearnContentDetailPage />} />
            <Route path="/learn/:slug" element={<LearnContentDetailPage />} />

            {/* Customer Self-Service Routes */}
            <Route
              path="/marketplace/customer/dashboard"
              element={
                <ProtectedRoute>
                  <MarketplaceCustomerDashboardPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/marketplace/customer/profile"
              element={
                <ProtectedRoute>
                  <CustomerProfilePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/marketplace/customer/requirements"
              element={
                <ProtectedRoute>
                  <CustomerTaxRequirementsListPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/marketplace/customer/requirements/new"
              element={
                <ProtectedRoute>
                  <CustomerTaxRequirementWizardPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/marketplace/customer/feedback"
              element={
                <ProtectedRoute>
                  <ApplicationFeedbackPage />
                </ProtectedRoute>
              }
            />
            <Route path="/customer/dashboard" element={<Navigate to="/marketplace/customer/dashboard" replace />} />
            <Route path="/customer/profile" element={<Navigate to="/marketplace/customer/profile" replace />} />
            <Route path="/customer/requirements" element={<Navigate to="/marketplace/customer/requirements" replace />} />
            <Route path="/customer/requirements/new" element={<Navigate to="/marketplace/customer/requirements/new" replace />} />
            <Route path="/customer/feedback" element={<Navigate to="/marketplace/customer/feedback" replace />} />

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
              <Route path="/marketplace/leads" element={<MarketplaceLeadsPage />} />
              <Route path="/marketplace/onboarding" element={<MarketplaceOnboardingHubPage />} />
              <Route path="/marketplace/practice-profile" element={<PracticeMarketplaceProfilePage />} />
              <Route path="/admin/overview" element={<PlatformOverviewPage />} />
              <Route
                path="/admin/practices"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_OPERATIONS_ADMIN', 'TAXORYN_SUPPORT_ADMIN', 'TAXORYN_MARKETPLACE_ADMIN']} requiredPermissions={['PRACTICE_VIEW']}>
                    <PlatformPracticesPage />
                  </RoleRouteGuard>
                }
              />
              <Route
                path="/admin/users"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_OPERATIONS_ADMIN']} requiredPermissions={['USER_VIEW', 'PLATFORM_USER_VIEW']}>
                    <PlatformUsersPage />
                  </RoleRouteGuard>
                }
              />
              <Route
                path="/admin/subscriptions"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_FINANCE_ADMIN']} requiredPermissions={['SUBSCRIPTION_VIEW', 'MRR_VIEW']}>
                    <PlatformSubscriptionsPage />
                  </RoleRouteGuard>
                }
              />
              <Route
                path="/admin/marketplace"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_MARKETPLACE_ADMIN', 'TAXORYN_OPERATIONS_ADMIN']} requiredPermissions={['MARKETPLACE_VIEW']}>
                    <PlatformAdminMarketplacePage />
                  </RoleRouteGuard>
                }
              />
              <Route
                path="/admin/content"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_CONTENT_ADMIN', 'TAXORYN_OPERATIONS_ADMIN']} requiredPermissions={['CONTENT_VIEW']}>
                    <PlatformContentManagementPage />
                  </RoleRouteGuard>
                }
              />
              <Route
                path="/content-studio"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_CONTENT_ADMIN', 'TAXORYN_OPERATIONS_ADMIN']} requiredPermissions={['CONTENT_VIEW']}>
                    <PlatformContentManagementPage />
                  </RoleRouteGuard>
                }
              />
              <Route path="/admin/feedback" element={<AdminFeedbackPage />} />
              <Route
                path="/admin/audit"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_SECURITY_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER']} requiredPermissions={['AUDIT_VIEW']}>
                    <AuditLogsPage />
                  </RoleRouteGuard>
                }
              />
              <Route path="/portal" element={<ClientPortalManagementPage />} />
              <Route path="/team" element={<TeamManagementPage />} />
              <Route path="/team/bulk" element={<BulkEmployeeOnboardingPage />} />
              <Route
                path="/audit-logs"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_SECURITY_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER']} requiredPermissions={['AUDIT_VIEW']}>
                    <AuditLogsPage />
                  </RoleRouteGuard>
                }
              />
              <Route path="/settings/branding" element={<PracticeBrandingPage />} />
              <Route path="/settings/marketplace" element={<PracticeMarketplaceProfilePage />} />
              <Route path="/settings/subscription" element={<SubscriptionsPage />} />
              <Route path="/feedback" element={<ApplicationFeedbackPage />} />
            </Route>

            {/* Catch all redirect */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
      </BrandingProvider>
    </AuthProvider>
  );
};
