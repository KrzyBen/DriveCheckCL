import { Link } from 'react-router-dom';
import { FileText, Users, ArrowRight } from 'lucide-react';
import { useAuth } from '@context/AuthContext';

const Home = () => {
  const { user } = useAuth();
  const rol = user?.rol;
  const puedeReportes = rol === 'administrador' || rol === 'validador';
  const puedeUsuarios = rol === 'administrador';

  return (
    <div className="home page-enter">
      <section className="home-hero">
        <svg className="home-rings" aria-hidden="true" width="420" height="420" viewBox="0 0 420 420" fill="none">
          <circle cx="210" cy="210" r="90" stroke="#0039A6" strokeOpacity=".14" strokeWidth="1.5" />
          <circle cx="210" cy="210" r="150" stroke="#0039A6" strokeOpacity=".1" strokeWidth="1.5" />
          <circle cx="210" cy="210" r="205" stroke="#0039A6" strokeOpacity=".07" strokeWidth="1.5" />
          <circle cx="210" cy="120" r="7" fill="#D52B1E" />
        </svg>
        <span className="eyebrow"><span className="eyebrow__dot" />Panel de administración</span>
        <h1 className="home-title">
          Hola, <em>{user?.nombre_completo?.split(' ')[0] || 'administrador'}</em>
        </h1>
        <p className="home-lead">Bienvenido al panel de administración de DriveCheckCL.</p>
      </section>

      <div className="home-cards">
        {puedeReportes && (
          <Link to="/reportes" className="shortcut">
            <div className="shortcut__core">
              <span className="shortcut__icon shortcut__icon--blue"><FileText size={22} /></span>
              <span className="shortcut__text">
                <span className="shortcut__title">Reportes</span>
                <span className="shortcut__desc">Revisa, valida o rechaza los reportes enviados.</span>
              </span>
              <span className="shortcut__go"><ArrowRight size={16} /></span>
            </div>
          </Link>
        )}
        {puedeUsuarios && (
          <Link to="/usuarios" className="shortcut">
            <div className="shortcut__core">
              <span className="shortcut__icon shortcut__icon--red"><Users size={22} /></span>
              <span className="shortcut__text">
                <span className="shortcut__title">Usuarios</span>
                <span className="shortcut__desc">Gestiona las cuentas y los roles del panel.</span>
              </span>
              <span className="shortcut__go"><ArrowRight size={16} /></span>
            </div>
          </Link>
        )}
      </div>
    </div>
  );
};

export default Home;
