import { updateUsuario } from '@services/usuario.service.js';
import { showErrorAlert, showSuccessAlert } from '@helpers/sweetAlert.js';

const useUpdateUsuario = (fetchUsuarios) => {
  const handleUpdate = async (id, data) => {
    try {
      const response = await updateUsuario(id, data);
      if (response.status === 'Client error') {
        return { error: response.details || response.message };
      }
      showSuccessAlert('Actualizado', 'El usuario fue modificado correctamente.');
      await fetchUsuarios();
      return { data: response.data };
    } catch (error) {
      console.error('Error al actualizar usuario:', error);
      showErrorAlert('Error', 'Ocurrió un error al modificar el usuario.');
      return { error };
    }
  };

  return { handleUpdate };
};

export default useUpdateUsuario;