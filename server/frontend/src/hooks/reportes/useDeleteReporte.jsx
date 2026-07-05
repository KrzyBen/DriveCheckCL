import { deleteReporte } from '@services/reporte.service.js';
import { deleteDataAlert, showErrorAlert, showSuccessAlert } from '@helpers/sweetAlert.js';

const useDeleteReporte = (fetchReportes) => {
  const handleDelete = async (reporteId) => {
    try {
      const result = await deleteDataAlert();
      if (!result.isConfirmed) return;
      const response = await deleteReporte(reporteId);
      if (response.status === 'Client error') {
        return showErrorAlert('No se puede eliminar', response.details || response.message);
      }
      showSuccessAlert('Eliminado', 'El reporte fue eliminado correctamente.');
      await fetchReportes();
    } catch (error) {
      console.error('Error al eliminar el reporte:', error);
      showErrorAlert('Error', 'Ocurrió un error al eliminar el reporte.');
    }
  };

  return { handleDelete };
};

export default useDeleteReporte;