import { Outlet } from 'react-router-dom';
import { AuthProvider } from '@context/AuthContext';
import Navbar from '@components/Navbar';
import '@styles/styles.css';

const Root = () => {
  return (
    <AuthProvider>
      <div className="app-shell" style={{ position: 'relative' }}>
        <Navbar />
        <main style={{ padding: '1.25rem 1.5rem' }}>
          <Outlet />
        </main>
      </div>
    </AuthProvider>
  );
};

export default Root;