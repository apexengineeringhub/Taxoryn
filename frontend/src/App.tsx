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
const ActivateOrgPage = React.lazy(() => import('./pages/ActivateOrgPage').then(m => ({ default: m.ActivateOrgPage })));
const AccountSecurityPage = React.lazy(() => import('./pages/AccountSecurityPage').then(m => ({ default: m.AccountSecurityPage })));
const DashboardPage = React.lazy(() => import('./pages/DashboardPage').then(m => ({ default: m.DashboardPage })));
const ClientsPage = React.lazy(() => import('./pages/ClientsPage').then(m => ({ default: m.ClientsPage })));
const Client360Page = React.lazy(() => import('./pages/Client360Page').then(m => ({ default: m.Client360Page })));
const ClientMigrationHubPage = React.lazy(() => import('./pages/ClientMigrationHubPage').then(m => ({ default: m.ClientMigrationHubPage })));
const TasksPage = React.lazy(() => import('./pages/TasksPage').then(m => ({ default: m.TasksPage })));
const ComplianceWorkPage = React.lazy(() => import('./pages/ComplianceWorkPage').then(m => ({ default: m.ComplianceWorkPage })));
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
const TeamChatPage = React.lazy(() => import('./pages/TeamChatPage').then(m => ({ default: m.TeamChatPage })));
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
const UserProfilePage = React.lazy(() => import('./pages/UserProfilePage').then(m => ({ default: m.UserProfilePage })));
const CustomerTaxRequirementWizardPage = React.lazy(() => import('./pages/CustomerTaxRequirementWizardPage').then(m => ({ default: m.CustomerTaxRequirementWizardPage })));
const CustomerTaxRequirementsListPage = React.lazy(() => import('./pages/CustomerTaxRequirementsListPage').then(m => ({ default: m.CustomerTaxRequirementsListPage })));
const ApplicationFeedbackPage = React.lazy(() => import('./pages/ApplicationFeedbackPage').then(m => ({ default: m.ApplicationFeedbackPage })));
const AdminFeedbackPage = React.lazy(() => import('./pages/AdminFeedbackPage').then(m => ({ default: m.AdminFeedbackPage })));
const PlatformOverviewPage = React.lazy(() => import('./pages/PlatformOverviewPage').then(m => ({ default: m.PlatformOverviewPage })));
const PlatformPracticesPage = React.lazy(() => import('./pages/PlatformPracticesPage').then(m => ({ default: m.PlatformPracticesPage })));
const PlatformUsersPage = React.lazy(() => import('./pages/PlatformUsersPage').then(m => ({ default: m.PlatformUsersPage })));
const PlatformSubscriptionsPage = React.lazy(() => import('./pages/PlatformSubscriptionsPage').then(m => ({ default: m.PlatformSubscriptionsPage })));
const EarlyAccessPage = React.lazy(() => import('./pages/EarlyAccessPage').then(m => ({ default: m.EarlyAccessPage })));
const LearnLandingPage = React.lazy(() => import('./pages/learn/LearnLandingPage').then(m => ({ default: m.LearnLandingPage })));
const LearnContentBrowsePage = React.lazy(() => import('./pages/learn/LearnContentBrowsePage').then(m => ({ default: m.LearnContentBrowsePage })));
const LearnContentDetailPage = React.lazy(() => import('./pages/learn/LearnContentDetailPage').then(m => ({ default: m.LearnContentDetailPage })));
const PlatformContentManagementPage = React.lazy(() => import('./pages/PlatformContentManagementPage').then(m => ({ default: m.PlatformContentManagementPage })));
const WhatsAppMessagesPage = React.lazy(() => import('./pages/WhatsAppMessagesPage').then(m => ({ default: m.WhatsAppMessagesPage })));
const NotificationsPage = React.lazy(() => import('./pages/NotificationsPage').then(m => ({ default: m.NotificationsPage })));
const ReportsPage = React.lazy(() => import('./pages/ReportsPage').then(m => ({ default: m.ReportsPage })));
const NoticeCenterPage = React.lazy(() => import('./pages/NoticeCenterPage').then(m => ({ default: m.NoticeCenterPage })));
const NoticeDetailPage = React.lazy(() => import('./pages/NoticeDetailPage').then(m => ({ default: m.NoticeDetailPage })));
const ProductModulesPage = React.lazy(() => import('./pages/ProductModulesPage').then(m => ({ default: m.ProductModulesPage })));
const TaxNoticeSettingsPage = React.lazy(() => import('./pages/TaxNoticeSettingsPage').then(m => ({ default: m.TaxNoticeSettingsPage })));

import { RoleRouteGuard } from './components/common/RoleRouteGuard';
import { ModuleRouteGuard } from './components/common/ModuleRouteGuard';
import { ModuleEntitlementProvider } from './context/ModuleEntitlementContext';
import {
  NOTIFICATION_PERMISSIONS,
  NOTIFICATION_ADMIN_ROLES,
} from './utils/permissionUtils';

// Sleek Skeleton Page Fallback
const PageLoadingFallback: React.FC = () => (
  <div className="min-h-screen w-full flex items-center justify-center bg-slate-900 text-white">
    <div className="flex flex-col items-center gap-3">
      <div className="w-8 h-8 rounded-full border-2 border-[#00D1A3] border-t-transparent animate-spin" />
      <span className="text-xs font-semibold text-slate-300 tracking-wide">Loading module...</span>
    </div>
  </div>
);

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
        <ModuleEntitlementProvider>
          <BrowserRouter>
            <Suspense fallback={<PageLoadingFallback />}>
              <Routes>
            {/* Central SaaS Root Route */}
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            {/* Public Auth & Discovery Routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />
            <Route path="/register" element={<RegisterOrgPage />} />
            <Route path="/early-access" element={<EarlyAccessPage />} />
            <Route path="/request-access" element={<EarlyAccessPage />} />
            <Route path="/practice-access" element={<EarlyAccessPage />} />
            <Route path="/activate" element={<ActivateOrgPage />} />
            <Route path="/activate-organization" element={<ActivateOrgPage />} />
            <Route path="/marketplace/register" element={<RegisterCustomerPage />} />
            <Route path="/marketplace" element={<MarketplaceExplorePage />} />
            <Route path="/marketplace/explore" element={<MarketplaceExplorePage />} />
            <Route path="/marketplace/compare" element={<MarketplaceComparePage />} />
            <Route path="/marketplace/profile/:id" element={<PracticePublicProfilePage />} />
            <Route path="/marketplace/onboarding/:token" element={<CustomerOnboardingPortalPage />} />
            <Route path="/marketplace/proposal/:token" element={<CustomerOnboardingPortalPage />} />
            <Route path="/practice/:slug" element={<PracticePublicProfilePage />} />
            <Route path="/professional/:slug" element={<PracticePublicProfilePage />} />
            <Route path="/marketplace/:slug" element={<PracticePublicProfilePage />} />

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
              <Route
                path="/clients"
                element={
                  <ModuleRouteGuard moduleCode="CLIENTS">
                    <ClientsPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/clients/:clientId"
                element={
                  <ModuleRouteGuard moduleCode="CLIENTS">
                    <Client360Page />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/clients/migration"
                element={
                  <ModuleRouteGuard
                    moduleCode="CLIENTS"
                    allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'TAX_PROFESSIONAL', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'ACCOUNTANT']}
                  >
                    <ClientMigrationHubPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/tasks"
                element={
                  <ModuleRouteGuard moduleCode="TASKS">
                    <TasksPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/compliance-work"
                element={
                  <ModuleRouteGuard moduleCode="TASKS">
                    <ComplianceWorkPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/tasks/bulk"
                element={
                  <ModuleRouteGuard
                    moduleCode="TASKS"
                    allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'TAX_PROFESSIONAL', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'ACCOUNTANT']}
                  >
                    <BulkTasksGeneratorPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/gst"
                element={
                  <ModuleRouteGuard moduleCode="GST">
                    <GstCompliancePage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/gst/migration"
                element={
                  <ModuleRouteGuard
                    moduleCode="GST"
                    allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'TAX_PROFESSIONAL', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'ACCOUNTANT']}
                  >
                    <GstDataMigrationHubPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/itr"
                element={
                  <ModuleRouteGuard moduleCode="ITR">
                    <ItrCompliancePage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/itr/migration"
                element={
                  <ModuleRouteGuard
                    moduleCode="ITR"
                    allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'TAX_PROFESSIONAL', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'ACCOUNTANT']}
                  >
                    <ItrDataMigrationHubPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/tds"
                element={
                  <ModuleRouteGuard moduleCode="TDS">
                    <TdsCompliancePage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/tds/migration"
                element={
                  <ModuleRouteGuard
                    moduleCode="TDS"
                    allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'TAX_PROFESSIONAL', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'ACCOUNTANT']}
                  >
                    <TdsDataMigrationHubPage />
                  </ModuleRouteGuard>
                }
              />
              <Route path="/calendar" element={<ComplianceCalendarPage />} />
              <Route
                path="/notices"
                element={
                  <ModuleRouteGuard moduleCode="TAX_NOTICES">
                    <NoticeCenterPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/notices/:id"
                element={
                  <ModuleRouteGuard moduleCode="TAX_NOTICES">
                    <NoticeDetailPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/tax-notices"
                element={
                  <ModuleRouteGuard moduleCode="TAX_NOTICES">
                    <NoticeCenterPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/tax-notices/:id"
                element={
                  <ModuleRouteGuard moduleCode="TAX_NOTICES">
                    <NoticeDetailPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/documents"
                element={
                  <ModuleRouteGuard moduleCode="DOCUMENTS">
                    <DocumentsPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/billing"
                element={
                  <ModuleRouteGuard
                    moduleCode="BILLING"
                    allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'ACCOUNTANT']}
                    requiredPermissions={['BILLING_VIEW', 'BILLING_READ']}
                  >
                    <BillingPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/reports"
                element={
                  <ModuleRouteGuard
                    moduleCode="REPORTS"
                    allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'MANAGER']}
                    requiredPermissions={['REPORT_VIEW', 'REPORTS_VIEW']}
                  >
                    <ReportsPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/notifications"
                element={
                  <ModuleRouteGuard
                    moduleCode="NOTIFICATIONS"
                    allowedRoles={NOTIFICATION_ADMIN_ROLES}
                    requiredPermissions={NOTIFICATION_PERMISSIONS}
                  >
                    <NotificationsPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/marketplace/leads"
                element={
                  <ModuleRouteGuard
                    moduleCode="MARKETPLACE"
                    allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER']}
                  >
                    <MarketplaceLeadsPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/marketplace/onboarding"
                element={
                  <ModuleRouteGuard
                    moduleCode="MARKETPLACE"
                    allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER']}
                  >
                    <MarketplaceOnboardingHubPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/marketplace/practice-profile"
                element={
                  <ModuleRouteGuard
                    moduleCode="MARKETPLACE"
                    allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER']}
                  >
                    <PracticeMarketplaceProfilePage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/admin/overview"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_OPERATIONS_ADMIN', 'TAXORYN_SUPPORT_ADMIN', 'TAXORYN_FINANCE_ADMIN', 'TAXORYN_MARKETPLACE_ADMIN', 'TAXORYN_CONTENT_ADMIN', 'TAXORYN_SECURITY_ADMIN', 'TAXORYN_ENGINEERING_ADMIN']}>
                    <PlatformOverviewPage />
                  </RoleRouteGuard>
                }
              />
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
              <Route
                path="/admin/feedback"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_OPERATIONS_ADMIN', 'TAXORYN_SUPPORT_ADMIN', 'TAXORYN_ENGINEERING_ADMIN']} requiredPermissions={['FEEDBACK_VIEW', 'FEEDBACK_MANAGE']}>
                    <AdminFeedbackPage />
                  </RoleRouteGuard>
                }
              />
              <Route
                path="/admin/audit"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_SECURITY_ADMIN']} requiredPermissions={['AUDIT_VIEW']}>
                    <AuditLogsPage />
                  </RoleRouteGuard>
                }
              />
              <Route
                path="/portal"
                element={
                  <ModuleRouteGuard
                    moduleCode="CLIENT_PORTAL"
                    allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'TAX_PROFESSIONAL', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'ACCOUNTANT', 'CLIENT_ADMIN', 'CLIENT_USER', 'PRACTICE_CLIENT', 'MARKETPLACE_CUSTOMER']}
                  >
                    <ClientPortalManagementPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/chat"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'TAX_PROFESSIONAL', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'ACCOUNTANT']}>
                    <TeamChatPage />
                  </RoleRouteGuard>
                }
              />
              <Route
                path="/team/chat"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'PRACTITIONER', 'TAX_PROFESSIONAL', 'MANAGER', 'STAFF', 'ARTICLE_ASSISTANT', 'PRACTICE_EMPLOYEE', 'ACCOUNTANT']}>
                    <TeamChatPage />
                  </RoleRouteGuard>
                }
              />
              <Route
                path="/team"
                element={
                  <RoleRouteGuard
                    allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER']}
                    requiredPermissions={['USER_VIEW', 'ROLE_READ']}
                  >
                    <TeamManagementPage />
                  </RoleRouteGuard>
                }
              />
              <Route
                path="/team/bulk"
                element={
                  <RoleRouteGuard
                    allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER']}
                    requiredPermissions={['USER_CREATE', 'EMPLOYEE_CREATE']}
                  >
                    <BulkEmployeeOnboardingPage />
                  </RoleRouteGuard>
                }
              />
              <Route
                path="/audit-logs"
                element={
                  <ModuleRouteGuard
                    moduleCode="AUDIT"
                    allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_SECURITY_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'MANAGER', 'TAX_PROFESSIONAL', 'PRACTITIONER', 'ACCOUNTANT']}
                    requiredPermissions={['AUDIT_VIEW', 'AUDIT_READ']}
                  >
                    <AuditLogsPage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/settings/branding"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER']} requiredPermissions={['ORGANIZATION_UPDATE', 'ORG_WRITE']}>
                    <PracticeBrandingPage />
                  </RoleRouteGuard>
                }
              />
              <Route
                path="/settings/marketplace"
                element={
                  <ModuleRouteGuard
                    moduleCode="MARKETPLACE"
                    allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER']}
                    requiredPermissions={['ORGANIZATION_UPDATE', 'ORG_WRITE']}
                  >
                    <PracticeMarketplaceProfilePage />
                  </ModuleRouteGuard>
                }
              />
              <Route
                path="/settings/whatsapp"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER', 'TAXORYN_SUPPORT_ADMIN']} requiredPermissions={['COMMUNICATION_MANAGE']}>
                    <WhatsAppMessagesPage />
                  </RoleRouteGuard>
                }
              />
              <Route
                path="/admin/whatsapp"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'TAXORYN_OPERATIONS_ADMIN', 'TAXORYN_SUPPORT_ADMIN']}>
                    <WhatsAppMessagesPage />
                  </RoleRouteGuard>
                }
              />
              <Route
                path="/settings/subscription"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER']} requiredPermissions={['SUBSCRIPTION_VIEW', 'ORGANIZATION_UPDATE', 'ORG_WRITE']}>
                    <SubscriptionsPage />
                  </RoleRouteGuard>
                }
              />
              <Route
                path="/settings/modules"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER']} requiredPermissions={['ORGANIZATION_UPDATE', 'ORG_WRITE']}>
                    <ProductModulesPage />
                  </RoleRouteGuard>
                }
              />
              <Route
                path="/settings/tax-notices"
                element={
                  <RoleRouteGuard allowedRoles={['TAXORYN_SUPERADMIN', 'SUPER_ADMIN', 'PRACTICE_OWNER', 'PRACTICE_ADMIN', 'ORG_ADMIN', 'PARTNER']} requiredPermissions={['ORGANIZATION_UPDATE', 'ORG_WRITE']}>
                    <TaxNoticeSettingsPage />
                  </RoleRouteGuard>
                }
              />
              <Route path="/profile" element={<UserProfilePage />} />
              <Route path="/settings/security" element={<AccountSecurityPage />} />
              <Route path="/profile/security" element={<AccountSecurityPage />} />
              <Route path="/feedback" element={<ApplicationFeedbackPage />} />
            </Route>

            {/* Catch all redirect */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
        </ModuleEntitlementProvider>
      </BrandingProvider>
    </AuthProvider>
  );
};
