import { createContext, useContext, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

export const AuthContext = createContext();
export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }) {
  const navigate = useNavigate();
  const [user, setUser] = useState(() => {
    const stored = sessionStorage.getItem('usuario');
    return stored ? JSON.parse(stored) : null;
  });
  const isAuthenticated = !!user;

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
    }
  }, [isAuthenticated, navigate]);

  const updateUser = (nuevosDatos) => {
    setUser((prev) => {
      const actualizado = { ...prev, ...nuevosDatos };
      sessionStorage.setItem('usuario', JSON.stringify(actualizado));
      return actualizado;
    });
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated, user, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
}