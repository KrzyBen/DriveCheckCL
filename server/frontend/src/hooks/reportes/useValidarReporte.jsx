import { validarReporte } from '@services/reporte.service.js';
import { showErrorAlert, showSuccessAlert } from '@helpers/sweetAlert.js';

const useValidarReporte = (fetchReportes) => {
  const handleValidar = async (reporteId, data) => {
    try {
      const response = await validarReporte(reporteId, data);
      if (response.status === 'Client error') {
        return showErrorAlert('Error', response.details || response.message);
      }
      showSuccessAlert('Reporte aprobado', 'El PDF se generará y quedará disponible para el usuario.');
      await fetchReportes();
      return response;
    } catch (error) {
      console.error('Error al validar el reporte:', error);
      showErrorAlert('Error', 'Ocurrió un error al validar el reporte.');
    }
  };

  return { handleValidar };
};

export default useValidarReporte;