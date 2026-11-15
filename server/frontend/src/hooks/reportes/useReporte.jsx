import { useState, useEffect, useCallback } from 'react';
import { getReporte } from '@services/reporte.service.js';

const useReporte = (reporteId) => {
  const [reporte, setReporte] = useState(null);
  const [loading, setLoading] = useState(true);

  // { silencioso: true } refresca sin tocar `loading`: para renovar una URL firmada sin que
  // nada que dependa de `loading` desmonte el <video>. Sin opciones se comporta como siempre.
  const fetchReporte = useCallback(async (opciones) => {
    const silencioso = opciones?.silencioso === true;
    if (!silencioso) setLoading(true);
    const response = await getReporte(reporteId);
    // Devuelve el reporte nuevo (o null) para quien lo necesite enseguida, por ejemplo
    // para tomar una URL firmada recién emitida sin esperar al próximo render
    const nuevo = response?.status === 'Success' ? response.data : null;
    if (nuevo) setReporte(nuevo);
    if (!silencioso) setLoading(false);
    return nuevo;
  }, [reporteId]);

  useEffect(() => { fetchReporte(); }, [fetchReporte]);

  return { reporte, loading, fetchReporte };
};

export default useReporte;