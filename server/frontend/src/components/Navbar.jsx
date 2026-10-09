import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { logout } from '@services/auth.service.js';
import { useState, useEffect } from 'react';
import { useAuth } from '@context/AuthContext';
import { Menu, X, Home, Users, FileText, User, LogOut } from 'lucide-react';
import '@styles/navbar.css';

const iniciales = (nombre = '') =>
  nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase();

const Navbar = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const userRole = user?.rol;
  const [menuOpen, setMenuOpen] = useState(false);

  const logoutSubmit = async () => {
    await logout();
    navigate('/login');
  };

  const toggleMenu = () => setMenuOpen((prev) => !prev);
  useEffect(() => { setMenuOpen(false); }, [location]);
  const closeMenu = () => setMenuOpen(false);

  // Escape cierra el menú
  useEffect(() => {
    if (!menuOpen) return undefined;
    const onKey = (e) => { if (e.key === 'Escape') setMenuOpen(false); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [menuOpen]);

  return (
    <>
      <div className="topbar">
        <div className="topbar__left">
          <button
            className="btn btn-icon"
            onClick={toggleMenu}
            aria-label={menuOpen ? 'Cerrar menú' : 'Abrir menú'}
            aria-expanded={menuOpen}
          >
            {menuOpen ? <X size={18} /> : <Menu size={18} />}
          </button>
          <div className="flag-chip">
            <span className="flag-bar flag-blue" />
            <span className="flag-bar flag-white" />
            <span className="flag-bar flag-red" />
            <span className="topbar__brand">DriveCheckCL</span>
          </div>
        </div>
        <div className="topbar__user">
          <span>{user?.nombre_completo}</span>
          <span className="avatar" aria-hidden="true">{iniciales(user?.nombre_completo)}</span>
        </div>
      </div>

      {menuOpen && (
        <>
          <button className="drawer-scrim" onClick={closeMenu} aria-label="Cerrar menú" tabIndex={-1} />
          <nav className="drawer" aria-label="Principal">
            <div className="drawer__core">
              <NavLink to="/home" className="drawer-item" onClick={closeMenu} end>
                <Home size={18} /> Inicio
              </NavLink>
              {userRole === 'administrador' && (
                <NavLink to="/usuarios" className="drawer-item" onClick={closeMenu}>
                  <Users size={18} /> Usuarios
                </NavLink>
              )}
              {(userRole === 'administrador' || userRole === 'validador') && (
                <NavLink to="/reportes" className="drawer-item" onClick={closeMenu}>
                  <FileText size={18} /> Reportes
                </NavLink>
              )}
              <div className="drawer-divider" role="separator" />
              <NavLink to="/perfil" className="drawer-item" onClick={closeMenu}>
                <User size={18} /> Perfil
              </NavLink>
              <button className="drawer-item drawer-item--danger" onClick={() => { logoutSubmit(); closeMenu(); }}>
                <LogOut size={18} /> Cerrar sesión
              </button>
            </div>
          </nav>
        </>
      )}
    </>
  );
};

export default Navbar;
