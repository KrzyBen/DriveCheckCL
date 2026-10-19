import { useState, useCallback } from 'react';
import { getMapaRiesgo } from '@services/mapa.service.js';

const useMapaRiesgo = () => {
  const [datos, setDatos] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchMapa = useCallback(async (filtros) => {
    setLoading(true);
    setError('');
    const response = await getMapaRiesgo(filtros);
    if (response?.status === 'Success') {
      setDatos(response.data);
    } else {
      setError(response?.message || 'No se pudieron cargar los datos del mapa.');
    }
    setLoading(false);
  }, []);

  return { datos, loading, error, fetchMapa };
};

export default useMapaRiesgo;
