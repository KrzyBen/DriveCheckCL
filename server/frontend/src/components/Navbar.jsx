import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { logout } from '@services/auth.service.js';
import '@styles/navbar.css';
import { useState, useEffect } from 'react';
import { useAuth } from '@context/AuthContext';

const Navbar = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const userRole = user?.rol;
  const [menuOpen, setMenuOpen] = useState(false);

  const logoutSubmit = async () => {
    try {
      await logout();
      navigate('/login');
    } catch (error) {
      console.error('Error al cerrar sesión:', error);
    }
  };

  const toggleMenu = () => setMenuOpen((prev) => !prev);
  useEffect(() => { setMenuOpen(false); }, [location]);

  const linkClass = ({ isActive }) => (isActive ? 'active' : '');
  const closeMenu = () => setMenuOpen(false);

  return (
    <>
      <header className="topbar">
        <button className="hamburger" onClick={toggleMenu} aria-label="Abrir menú" aria-expanded={menuOpen}>
          <span className="bar" /><span className="bar" /><span className="bar" />
        </button>
        <div className="brand">
          <span className="flag-bar flag-blue" />
          <span className="flag-bar flag-white" />
          <span className="flag-bar flag-red" />
          <strong>DriveCheckCL</strong>
        </div>
        <span className="user-name">{user?.nombre_completo}</span>
      </header>

      <nav className={`drawer ${menuOpen ? 'open' : ''}`}>
        <ul>
          {userRole === 'administrador' && (
            <li>
              <NavLink to="/usuarios" className={linkClass} onClick={closeMenu}>Usuarios</NavLink>
            </li>
          )}
          {(userRole === 'administrador' || userRole === 'validador') && (
            <li>
              <NavLink to="/reportes" className={linkClass} onClick={closeMenu}>Reportes</NavLink>
            </li>
          )}
          <li className="drawer-separator">
            <NavLink to="/perfil" className={linkClass} onClick={closeMenu}>Perfil</NavLink>
          </li>
          <li>
            <button className="drawer-item logout-btn" onClick={() => { logoutSubmit(); closeMenu(); }}>
              Cerrar sesión
            </button>
          </li>
        </ul>
      </nav>

      {menuOpen && <div className="drawer-overlay" onClick={closeMenu} />}
    </>
  );
};

export default Navbar;