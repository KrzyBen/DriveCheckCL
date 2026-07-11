import { analizarReporte } from '@services/reporte.service.js';
import { showErrorAlert } from '@helpers/sweetAlert.js';

const useAnalizarReporte = (fetchReporte) => {
  const handleAnalizar = async (reporteId) => {
    try {
      const response = await analizarReporte(reporteId);
      if (response.status === 'Client error') {
        return showErrorAlert('Error', response.details || response.message);
      }
      // No mostramos un alert de éxito acá: el resultado se ve directo en la
      // pantalla apenas termine el análisis (o el estado "error" si backend-ia
      // todavía no está levantado).
      await fetchReporte();
      return response;
    } catch (error) {
      console.error('Error al analizar el reporte:', error);
      showErrorAlert('Error', 'Ocurrió un error al iniciar el análisis.');
    }
  };

  return { handleAnalizar };
};

export default useAnalizarReporte;
