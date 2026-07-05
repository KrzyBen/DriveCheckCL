import { Outlet } from 'react-router-dom';
import { AuthProvider } from '@context/AuthContext';
import Navbar from '@components/Navbar';
import '@styles/styles.css';

const Root = () => {
  return (
    <AuthProvider>
      <Navbar />
      <main style={{ padding: '1.5rem' }}>
        <Outlet />
      </main>
    </AuthProvider>
  );
};

export default Root;