import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './context/auth-context';
import { Navbar } from './components/navbar';
import { Footer } from './components/footer';
import { BackButton } from './components/back-button';
import { ProtectedRoute } from './components/protected-route';
import { RoleRoute } from './components/role-route';
import { AuthHomePage } from './pages/auth-home-page';
import { AuthLoginPage } from './pages/auth-login-page';
import { AuthRegisterPage } from './pages/auth-register-page';
import { LotCreatePage } from './pages/lot-create-page';
import { LotEditPage } from './pages/lot-edit-page';
import { LotDashboardPage } from './pages/lot-dashboard-page';
import { LotSalesHistoryPage } from './pages/lot-sales-history-page';
import { LotBrowsePage } from './pages/lot-browse-page';
import { LotDetailPage } from './pages/lot-detail-page';
import { PurchaseCompletePage } from './pages/purchase-complete-page';
import { LogisticsPurchaseHistoryPage } from './pages/logistics-purchase-history-page';
import { BillingInvoicesPage } from './pages/billing-invoices-page';
import { AdminOversightPage } from './pages/admin-oversight-page';
import { AuthAdminUsersPage } from './pages/auth-admin-users-page';
import { AuthAdminAuditPage } from './pages/auth-admin-audit-page';
import { AuthAdminCreatePage } from './pages/auth-admin-create-page';
import './App.css';
import './components/shared.css';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <div className="App">
          <Navbar />
          <main>
            <BackButton />
            <Routes>
              <Route path="/" element={<AuthHomePage />} />
              <Route path="/login" element={<AuthLoginPage />} />
              <Route path="/register" element={<AuthRegisterPage />} />

              <Route element={<ProtectedRoute />}>
                <Route element={<RoleRoute allow={['FARMER']} />}>
                  <Route path="/farmer/dashboard" element={<LotDashboardPage />} />
                  <Route path="/farmer/create-lot" element={<LotCreatePage />} />
                  <Route path="/farmer/lots/:id/edit" element={<LotEditPage />} />
                  <Route path="/farmer/sales-history" element={<LotSalesHistoryPage />} />
                </Route>

                <Route element={<RoleRoute allow={['BUYER']} />}>
                  <Route path="/buyer/browse-lots" element={<LotBrowsePage />} />
                  <Route path="/buyer/lots/:id" element={<LotDetailPage />} />
                  <Route path="/buyer/lots/:lotId/purchase-complete" element={<PurchaseCompletePage />} />
                  <Route path="/buyer/purchase-history" element={<LogisticsPurchaseHistoryPage />} />
                  <Route path="/buyer/invoices" element={<BillingInvoicesPage />} />
                </Route>

                <Route element={<RoleRoute allow={['ADMIN']} />}>
                  <Route path="/admin/oversight" element={<AdminOversightPage />} />
                  <Route path="/admin/users" element={<AuthAdminUsersPage />} />
                  <Route path="/admin/audit-log" element={<AuthAdminAuditPage />} />
                  <Route path="/admin/create-admin" element={<AuthAdminCreatePage />} />
                </Route>
              </Route>
            </Routes>
          </main>
          <Footer />
        </div>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
