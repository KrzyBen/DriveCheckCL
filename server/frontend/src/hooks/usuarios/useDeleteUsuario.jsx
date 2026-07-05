import { deleteUsuario } from '@services/usuario.service.js';
import { deleteDataAlert, showErrorAlert, showSuccessAlert } from '@helpers/sweetAlert.js';

const useDeleteUsuario = (fetchUsuarios) => {
  const handleDelete = async (usuarioId) => {
    try {
      const result = await deleteDataAlert();
      if (!result.isConfirmed) {
        return showErrorAlert('Cancelado', 'La operación ha sido cancelada.');
      }
      const response = await deleteUsuario(usuarioId);
      if (response.status === 'Client error') {
        return showErrorAlert('Error', response.details || response.message);
      }
      showSuccessAlert('¡Eliminado!', 'El usuario ha sido eliminado correctamente.');
      await fetchUsuarios();
    } catch (error) {
      console.error('Error al eliminar el usuario:', error);
      showErrorAlert('Error', 'Ocurrió un error al eliminar el usuario.');
    }
  };

  return { handleDelete };
};

export default useDeleteUsuario;