import React, { Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { BrandingProvider } from './context/BrandingContext';
import { AppShell } from './components/layout/AppShell';

// Lazy Loaded Pages for Instant Initial Load & Low Memory Footprint
const LoginPage = React.lazy(() => import('./pages/LoginPage').then(m => ({ default: m.LoginPage })));
const ForgotPasswordPage = React.lazy(() => import('./pages/ForgotPasswordPage').then(m => ({ default: m.ForgotPasswordPage })));
const ResetPasswordPage = React.lazy(() => import('./pages/ResetPasswordPage').then(m => ({ default: m.ResetPasswordPage })));
const RegisterOrgPage = React.lazy(() => import('./pages/RegisterOrgPage').then(m => ({ default: m.RegisterOrgPage })));
const AccountSecurityPage = React.lazy(() => import('./pages/AccountSecurityPage').then(m => ({ default: m.AccountSecurityPage })));
const DashboardPage = React.lazy(() => import('./pages/DashboardPage').then(m => ({ default: m.DashboardPage })));
const ClientsPage = React.lazy(() => import('./pages/ClientsPage').then(m => ({ default: m.ClientsPage })));
const ClientMigrationHubPage = React.lazy(() => import('./pages/ClientMigrationHubPage').then(m => ({ default: m.ClientMigrationHubPage })));
const TasksPage = React.lazy(() => import('./pages/TasksPage').then(m => ({ default: m.TasksPage })));
const BulkTasksGeneratorPage = React.lazy(() => import('./pages/BulkTasksGeneratorPage').then(m => ({ default: m.BulkTasksGeneratorPage })));
const GstCompliancePage = React.lazy(() => import('./pages/GstCompliancePage').then(m => ({ default: m.GstCompliancePage })));
const GstDataMigrationHubPage = React.lazy(() => import('./pages/GstDataMigrationHubPage').then(m => ({ default: m.GstDataMigrationHubPage })));
const ItrCompliancePage = React.lazy(() => import('./pages/ItrCompliancePage').then(m => ({ default: m.ItrCompliancePage })));
const ItrDataMigrationHubPage = React.lazy(() => import('./pages/ItrDataMigrationHubPage').then(m => ({ default: m.ItrDataMigrationHubPage })));
const TdsCompliancePage = React.lazy(() => import('./pages/TdsCompliancePage').then(m => ({ default: m.TdsCompliancePage })));
const TdsDataMigrationHubPage = React.lazy(() => import('./pages/TdsDataMigrationHubPage').then(m => ({ default: m.TdsDataMigrationHubPage })));
const ComplianceCalendarPage = React.lazy(() => import('./pages/ComplianceCalendarPage').then(m => ({ default: m.ComplianceCalendarPage })));
const DocumentsPage = React.lazy(() => import('./pages/DocumentsPage').then(m => ({ default: m.DocumentsPage })));
const BillingPage = React.lazy(() => import('./pages/BillingPage').then(m => ({ default: m.BillingPage })));
const ClientPortalManagementPage = React.lazy(() => import('./pages/ClientPortalManagementPage').then(m => ({ default: m.ClientPortalManagementPage })));
const TeamManagementPage = React.lazy(() => import('./pages/TeamManagementPage').then(m => ({ default: m.TeamManagementPage })));
const BulkEmployeeOnboardingPage = React.lazy(() => import('./pages/BulkEmployeeOnboardingPage').then(m => ({ default: m.BulkEmployeeOnboardingPage })));
const AuditLogsPage = React.lazy(() => import('./pages/AuditLogsPage').then(m => ({ default: m.AuditLogsPage })));
const PracticeBrandingPage = React.lazy(() => import('./pages/PracticeBrandingPage').then(m => ({ default: m.PracticeBrandingPage })));
const SubscriptionsPage = React.lazy(() => import('./pages/SubscriptionsPage').then(m => ({ default: m.SubscriptionsPage })));
const MarketplaceExplorePage = React.lazy(() => import('./pages/MarketplaceExplorePage').then(m => ({ default: m.MarketplaceExplorePage })));
const PracticePublicProfilePage = React.lazy(() => import('./pages/PracticePublicProfilePage').then(m => ({ default: m.PracticePublicProfilePage })));
const MarketplaceComparePage = React.lazy(() => import('./pages/MarketplaceComparePage').then(m => ({ default: m.MarketplaceComparePage })));
const PracticeMarketplaceProfilePage = React.lazy(() => import('./pages/PracticeMarketplaceProfilePage').then(m => ({ default: m.PracticeMarketplaceProfilePage })));
const MarketplaceLeadsPage = React.lazy(() => import('./pages/MarketplaceLeadsPage').then(m => ({ default: m.MarketplaceLeadsPage })));
const PlatformAdminMarketplacePage = React.lazy(() => import('./pages/PlatformAdminMarketplacePage').then(m => ({ default: m.PlatformAdminMarketplacePage })));
const MarketplaceOnboardingHubPage = React.lazy(() => import('./pages/MarketplaceOnboardingHubPage').then(m => ({ default: m.MarketplaceOnboardingHubPage })));
const CustomerOnboardingPortalPage = React.lazy(() => import('./pages/CustomerOnboardingPortalPage').then(m => ({ default: m.CustomerOnboardingPortalPage })));
const RegisterCustomerPage = React.lazy(() => import('./pages/RegisterCustomerPage').then(m => ({ default: m.RegisterCustomerPage })));
const MarketplaceCustomerDashboardPage = React.lazy(() => import('./pages/MarketplaceCustomerDashboardPage').then(m => ({ default: m.MarketplaceCustomerDashboardPage })));
const CustomerProfilePage = React.lazy(() => import('./pages/CustomerProfilePage').then(m => ({ default: m.CustomerProfilePage })));
const CustomerTaxRequirementWizardPage = React.lazy(() => import('./pages/CustomerTaxRequirementWizardPage').then(m => ({ default: m.CustomerTaxRequirementWizardPage })));
const CustomerTaxRequirementsListPage = React.lazy(() => import('./pages/CustomerTaxRequirementsListPage').then(m => ({ default: m.CustomerTaxRequirementsListPage })));
const ApplicationFeedbackPage = React.lazy(() => import('./pages/ApplicationFeedbackPage').then(m => ({ default: m.ApplicationFeedbackPage })));
const AdminFeedbackPage = React.lazy(() => import('./pages/AdminFeedbackPage').then(m => ({ default: m.AdminFeedbackPage })));
const PlatformOverviewPage = React.lazy(() => import('./pages/PlatformOverviewPage').then(m => ({ default: m.PlatformOverviewPage })));
const PlatformPracticesPage = React.lazy(() => import('./pages/PlatformPracticesPage').then(m => ({ default: m.PlatformPracticesPage })));
const PlatformUsersPage = React.lazy(() => import('./pages/PlatformUsersPage').then(m => ({ default: m.PlatformUsersPage })));
const PlatformSubscriptionsPage = React.lazy(() => import('./pages/PlatformSubscriptionsPage').then(m => ({ default: m.PlatformSubscriptionsPage })));
const LearnLandingPage = React.lazy(() => import('./pages/learn/LearnLandingPage').then(m => ({ default: m.LearnLandingPage })));
const LearnContentBrowsePage = React.lazy(() => import('./pages/learn/LearnContentBrowsePage').then(m => ({ default: m.LearnContentBrowsePage })));
const LearnContentDetailPage = React.lazy(() => import('./pages/learn/LearnContentDetailPage').then(m => ({ default: m.LearnContentDetailPage })));
const PlatformContentManagementPage = React.lazy(() => import('./pages/PlatformContentManagementPage').then(m => ({ default: m.PlatformContentManagementPage })));
const WhatsAppMessagesPage = React.lazy(() => import('./pages/WhatsAppMessagesPage').then(m => ({ default: m.WhatsAppMessagesPage })));
const NotificationsPage = React.lazy(() => import('./pages/NotificationsPage').then(m => ({ default: m.NotificationsPage })));
const ReportsPage = React.lazy(() => import('./pages/ReportsPage').then(m => ({ default: m.ReportsPage })));

import { RoleRouteGuard } from './components/common/RoleRouteGuard';
import { getTenantSubdomain } from './utils/tenantUrl';

// Sleek Skeleton Page Fallback
const PageLoadingFallback: React.FC = () => (
  <div className="min-h-screen w-full flex items-center justify-center bg-slate-900 text-white">
    <div className="flex flex-col items-center gap-3">
      <div className="w-8 h-8 rounded-full border-2 border-[#00D1A3] border-t-transparent animate-spin" />
      <span className="text-xs font-semibold text-slate-300 tracking-wide">Loading module...</span>
    </div>
  </div>
);

// Dynamic Multi-Tenant Subdomain Resolver
const TenantRootResolver: React.FC = () => {
  const tenantSubdomain = getTenantSubdomain();
  if (tenantSubdomain) {
    return <PracticePublicProfilePage overrideSlug={tenantSubdomain} />;
  }
  return <Navigate to="/dashboard" replace />;
};

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
          <Suspense fallback={<PageLoadingFallback />}>
            <Routes>
            {/* Root Route: Tenant Subdomain Resolution (e.g., https://apex.taxoryn.com) or Dashboard */}
            <Route path="/" element={<TenantRootResolver />} />
            {/* Public Auth & Discovery Routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />
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
            <Route path="/learn/tax-updates" element={<LearnContentBrowsePage />} />
            <Route path="/learn/content/:slug" element={<LearnContentDetailPage />} />
            <Route path="/learn/articles/:slug" element={<LearnContentDetailPage />} />
            <Route path="/learn/videos/:slug" element={<LearnContentDetailPage />} />
            <Route path="/learn/guides/:slug" element={<LearnContentDetailPage />} />
            <Route path="/learn/faqs/:slug" element={<LearnContentDetailPage />} />
            <Route path="/learn/tax-updates/:slug" element={<LearnContentDetailPage />} />
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
              <Route path="/reports" element={<ReportsPage />} />
              <Route path="/notifications" element={<NotificationsPage />} />
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
              <Route path="/settings/whatsapp" element={<WhatsAppMessagesPage />} />
              <Route path="/admin/whatsapp" element={<WhatsAppMessagesPage />} />
              <Route path="/settings/subscription" element={<SubscriptionsPage />} />
              <Route path="/settings/security" element={<AccountSecurityPage />} />
              <Route path="/profile/security" element={<AccountSecurityPage />} />
              <Route path="/feedback" element={<ApplicationFeedbackPage />} />
            </Route>

            {/* Catch all redirect */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </BrandingProvider>
  </AuthProvider>
  );
};
