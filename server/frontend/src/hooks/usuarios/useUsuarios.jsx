import { useState, useEffect, useCallback } from 'react';
import { getUsuarios } from '@services/usuario.service.js';

const useUsuarios = () => {
  const [usuarios, setUsuarios] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchUsuarios = useCallback(async () => {
    setLoading(true);
    const response = await getUsuarios();
    if (response?.status === 'Success') {
      setUsuarios(response.data);
    }
    setLoading(false);
  }, []);

  useEffect(() => { fetchUsuarios(); }, [fetchUsuarios]);

  return { usuarios, loading, fetchUsuarios, setUsuarios };
};

export default useUsuarios;