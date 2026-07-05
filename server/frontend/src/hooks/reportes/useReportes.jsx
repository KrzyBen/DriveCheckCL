import { useState, useEffect, useCallback } from 'react';
import { getReportes } from '@services/reporte.service.js';

const useReportes = (filtrosIniciales = {}) => {
  const [reportes, setReportes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filtros, setFiltros] = useState(filtrosIniciales);

  const fetchReportes = useCallback(async () => {
    setLoading(true);
    const response = await getReportes(filtros);
    if (response?.status === 'Success') {
      setReportes(response.data);
    }
    setLoading(false);
  }, [filtros]);

  useEffect(() => { fetchReportes(); }, [fetchReportes]);

  return { reportes, loading, filtros, setFiltros, fetchReportes };
};

export default useReportes;