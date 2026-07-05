import { useState, useEffect, useCallback } from 'react';
import { getReporte } from '@services/reporte.service.js';

const useReporte = (reporteId) => {
  const [reporte, setReporte] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchReporte = useCallback(async () => {
    setLoading(true);
    const response = await getReporte(reporteId);
    if (response?.status === 'Success') {
      setReporte(response.data);
    }
    setLoading(false);
  }, [reporteId]);

  useEffect(() => { fetchReporte(); }, [fetchReporte]);

  return { reporte, loading, fetchReporte };
};

export default useReporte;