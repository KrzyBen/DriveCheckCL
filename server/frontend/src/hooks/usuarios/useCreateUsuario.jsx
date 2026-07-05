import { createUsuario } from '@services/usuario.service.js';
import { showErrorAlert, showSuccessAlert } from '@helpers/sweetAlert.js';

const useCreateUsuario = (fetchUsuarios) => {
  const handleCreate = async (data) => {
    try {
      const response = await createUsuario(data);
      if (response.status === 'Client error') {
        return { error: response.details || response.message };
      }
      showSuccessAlert('Usuario creado', 'El usuario fue registrado correctamente.');
      await fetchUsuarios();
      return { data: response.data };
    } catch (error) {
      console.error('Error al crear usuario:', error);
      showErrorAlert('Error', 'Ocurrió un error al crear el usuario.');
      return { error };
    }
  };

  return { handleCreate };
};

export default useCreateUsuario;