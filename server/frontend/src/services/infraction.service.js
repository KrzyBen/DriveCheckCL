import axios from './root.service.js';

export async function getInfracciones() {
  try {
    const response = await axios.get('/admin/reportes/infracciones');
    return response.data;
  } catch (error) {
    return error.response?.data;
  }
}