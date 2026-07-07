import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { logout } from '@services/auth.service.js';
import { useState, useEffect } from 'react';
import { useAuth } from '@context/AuthContext';
import { Menu, Users, FileText, UserCircle, LogOut } from 'lucide-react';
import '@styles/navbar.css';

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

  return (
    <>
      <div className="topbar">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <button className="btn btn-icon" onClick={toggleMenu} aria-label="Abrir menú" aria-expanded={menuOpen}>
            <Menu size={18} />
          </button>
          <div className="flag-chip">
            <span className="flag-bar flag-blue" />
            <span className="flag-bar flag-white" />
            <span className="flag-bar flag-red" />
            <span style={{ fontSize: 13, fontWeight: 500, marginLeft: 4 }}>DriveCheckCL</span>
          </div>
        </div>
        <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{user?.nombre_completo}</span>
      </div>

      {menuOpen && (
        <div className="drawer">
          <NavLink to="/home" className="drawer-item" onClick={closeMenu} end>
            <UserCircle size={16} /> Inicio
          </NavLink>
          {userRole === 'administrador' && (
            <NavLink to="/usuarios" className="drawer-item" onClick={closeMenu}>
              <Users size={16} /> Usuarios
            </NavLink>
          )}
          {(userRole === 'administrador' || userRole === 'validador') && (
            <NavLink to="/reportes" className="drawer-item" onClick={closeMenu}>
              <FileText size={16} /> Reportes
            </NavLink>
          )}
          <NavLink to="/perfil" className="drawer-item drawer-separator" onClick={closeMenu}>
            <UserCircle size={16} /> Perfil
          </NavLink>
          <button className="drawer-item" onClick={() => { logoutSubmit(); closeMenu(); }}>
            <LogOut size={16} /> Cerrar sesión
          </button>
        </div>
      )}
    </>
  );
};

export default Navbar;