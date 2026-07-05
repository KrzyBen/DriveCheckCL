import { useAuth } from '@context/AuthContext';

const Home = () => {
  const { user } = useAuth();

  return (
    <div className="card" style={{ padding: '2rem' }}>
      <h3 style={{ margin: 0 }}>Hola, {user?.nombre_completo?.split(' ')[0] || 'administrador'}</h3>
      <p style={{ fontSize: 13, color: 'var(--text-muted)', margin: '4px 0 0' }}>
        Bienvenido al panel de administración de DriveCheckCL.
      </p>
    </div>
  );
};

export default Home;