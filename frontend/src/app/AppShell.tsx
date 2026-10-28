import { useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router';
import { Building2, LayoutDashboard, LogOut, Menu, Plus, Radar, X } from 'lucide-react';
import { useAuth } from './AuthContext';

export default function AppShell() {
  const { account, signOut } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);
  const [error, setError] = useState('');

  async function logout() {
    try {
      await signOut();
      navigate('/login', { replace: true });
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Sign out failed');
    }
  }

  return (
    <div className="s4-shell">
      <aside className={`s4-sidebar ${menuOpen ? 's4-sidebar-open' : ''}`}>
        <div className="s4-sidebar-brand"><Radar size={25} /> <span>S4 Neighborhood<br /><small>Intelligence</small></span></div>
        <nav aria-label="Primary navigation">
          <NavLink to="/dashboard" onClick={() => setMenuOpen(false)}><LayoutDashboard size={19} /> Dashboard</NavLink>
          <NavLink to="/businesses/new" onClick={() => setMenuOpen(false)}><Plus size={19} /> Add business</NavLink>
        </nav>
        <div className="s4-sidebar-footer"><Building2 size={17} /> Local Business Intelligence</div>
      </aside>
      {menuOpen && <button className="s4-backdrop" onClick={() => setMenuOpen(false)} aria-label="Close navigation" />}
      <div className="s4-main">
        <header className="s4-topbar">
          <button className="s4-menu-button" onClick={() => setMenuOpen(!menuOpen)} aria-label="Open navigation">{menuOpen ? <X /> : <Menu />}</button>
          <Link className="s4-topbar-title" to="/dashboard">S4 Workspace</Link>
          <div className="s4-profile"><span>{account?.displayName}</span><button onClick={() => void logout()} title="Sign out"><LogOut size={17} /> Sign out</button></div>
        </header>
        {error && <div className="s4-form-error" role="alert">{error}</div>}
        <Outlet />
      </div>
    </div>
  );
}
