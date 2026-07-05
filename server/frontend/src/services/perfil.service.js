import axios from './root.service.js';

export async function getPerfil() {
  try {
    const response = await axios.get('/admin/perfil');
    return response.data;
  } catch (error) {
    return error.response?.data;
  }
}

export async function updatePerfil(data) {
  try {
    const response = await axios.patch('/admin/perfil', data);
    return response.data;
  } catch (error) {
    return error.response?.data;
  }
}