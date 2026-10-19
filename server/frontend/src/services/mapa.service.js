import axios from './root.service.js';

export async function getMapaRiesgo(filtros = {}) {
  try {
    const response = await axios.get('/admin/mapa/', { params: filtros });
    return response.data;
  } catch (error) {
    return error.response?.data;
  }
}
