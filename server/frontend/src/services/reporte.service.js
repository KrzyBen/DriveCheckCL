import axios from './root.service.js';

export async function getReportes(filtros = {}) {
  try {
    const response = await axios.get('/admin/reportes/', { params: filtros });
    return response.data;
  } catch (error) {
    return error.response?.data;
  }
}

export async function getReporte(reporteId) {
  try {
    const response = await axios.get(`/admin/reportes/${reporteId}`);
    return response.data;
  } catch (error) {
    return error.response?.data;
  }
}

export async function validarReporte(reporteId, data) {
  try {
    const response = await axios.patch(`/admin/reportes/${reporteId}/validar`, data);
    return response.data;
  } catch (error) {
    return error.response?.data;
  }
}

export async function rechazarReporte(reporteId, data) {
  try {
    const response = await axios.patch(`/admin/reportes/${reporteId}/rechazar`, data);
    return response.data;
  } catch (error) {
    return error.response?.data;
  }
}

export async function deleteReporte(reporteId) {
  try {
    const response = await axios.delete(`/admin/reportes/${reporteId}`);
    return response.data;
  } catch (error) {
    return error.response?.data;
  }
}