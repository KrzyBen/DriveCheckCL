import { Outlet } from 'react-router-dom';
import { AuthProvider } from '@context/AuthContext';
import Navbar from '@components/Navbar';
import '@styles/styles.css';

const Root = () => {
  return (
    <AuthProvider>
      <div className="app-shell">
        <div className="app-shell__inner">
          <Navbar />
          <main className="app-main">
            <Outlet />
          </main>
        </div>
      </div>
    </AuthProvider>
  );
};

export default Root;
