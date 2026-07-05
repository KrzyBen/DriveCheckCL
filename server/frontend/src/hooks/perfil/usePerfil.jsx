import { updatePerfil } from '@services/perfil.service.js';
import { showErrorAlert, showSuccessAlert } from '@helpers/sweetAlert.js';

const usePerfil = () => {
  const handleUpdate = async (data) => {
    try {
      const response = await updatePerfil(data);
      if (response.status === 'Client error') {
        return showErrorAlert('Error', response.details || response.message);
      }
      showSuccessAlert('Perfil actualizado', 'Tus datos fueron guardados correctamente.');
      return response;
    } catch (error) {
      console.error('Error al actualizar perfil:', error);
      showErrorAlert('Error', 'Ocurrió un error al actualizar tu perfil.');
    }
  };

  return { handleUpdate };
};

export default usePerfil;