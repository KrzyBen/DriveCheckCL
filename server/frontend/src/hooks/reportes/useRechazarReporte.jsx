import { rechazarReporte } from '@services/reporte.service.js';
import { showErrorAlert, showSuccessAlert } from '@helpers/sweetAlert.js';

const useRechazarReporte = (fetchReportes) => {
  const handleRechazar = async (reporteId, notasAdmin) => {
    try {
      const response = await rechazarReporte(reporteId, { notas_admin: notasAdmin });
      if (response.status === 'Client error') {
        return showErrorAlert('Error', response.details || response.message);
      }
      showSuccessAlert('Reporte rechazado', 'El usuario verá el motivo del rechazo.');
      await fetchReportes();
      return response;
    } catch (error) {
      console.error('Error al rechazar el reporte:', error);
      showErrorAlert('Error', 'Ocurrió un error al rechazar el reporte.');
    }
  };

  return { handleRechazar };
};

export default useRechazarReporte;