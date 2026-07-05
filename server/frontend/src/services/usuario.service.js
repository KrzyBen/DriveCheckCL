import axios from './root.service.js';

export async function getUsuarios() {
  try {
    const response = await axios.get('/admin/');
    return response.data;
  } catch (error) {
    return error.response?.data;
  }
}

export async function getUsuario(id) {
  try {
    const response = await axios.get('/admin/detail', { params: { id } });
    return response.data;
  } catch (error) {
    return error.response?.data;
  }
}

export async function createUsuario(data) {
  try {
    const response = await axios.post('/admin/', data);
    return response.data;
  } catch (error) {
    return error.response?.data;
  }
}

export async function updateUsuario(id, data) {
  try {
    const response = await axios.patch('/admin/detail', data, { params: { id } });
    return response.data;
  } catch (error) {
    return error.response?.data;
  }
}

export async function deleteUsuario(id) {
  try {
    const response = await axios.delete('/admin/detail', { params: { id } });
    return response.data;
  } catch (error) {
    return error.response?.data;
  }
}