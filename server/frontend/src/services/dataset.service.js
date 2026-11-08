import axios from './root.service.js';

const SIN_CONEXION = { status: 'Client error', message: 'No se pudo conectar con el servidor' };

export async function getResumenDataset() {
  try {
    const response = await axios.get('/admin/dataset/');
    return response.data;
  } catch (error) {
    return error.response?.data ?? SIN_CONEXION;
  }
}

export async function getClipsDataset(pagina = 1, limite = 100) {
  try {
    const response = await axios.get('/admin/dataset/clips', { params: { pagina, limite } });
    return response.data;
  } catch (error) {
    return error.response?.data ?? SIN_CONEXION;
  }
}

// `cambios` solo lleva lo que cambió: { infracciones: [ids], severidad: 'leve'|null, excluido: bool }
export async function actualizarClipDataset(videoId, cambios) {
  try {
    const response = await axios.patch(`/admin/dataset/clips/${videoId}`, cambios);
    return response.data;
  } catch (error) {
    return error.response?.data ?? SIN_CONEXION;
  }
}

export async function restablecerClipDataset(videoId) {
  try {
    const response = await axios.delete(`/admin/dataset/clips/${videoId}/etiquetas`);
    return response.data;
  } catch (error) {
    return error.response?.data ?? SIN_CONEXION;
  }
}

export async function exportarDataset() {
  try {
    const response = await axios.post('/admin/dataset/exportar');
    return response.data;
  } catch (error) {
    return error.response?.data ?? SIN_CONEXION;
  }
}

// El zip se pide con la sesión del administrador (axios agrega el token) y se entrega como blob:
// no hay enlace directo, porque el dataset no se sirve por /storage.
export async function descargarDataset() {
  try {
    const response = await axios.get('/admin/dataset/descargar', { responseType: 'blob' });
    return { status: 'Success', blob: response.data };
  } catch (error) {
    const cuerpo = error.response?.data;
    if (cuerpo instanceof Blob) {
      try {
        return JSON.parse(await cuerpo.text());
      } catch {
        return SIN_CONEXION;
      }
    }
    return cuerpo ?? SIN_CONEXION;
  }
}
