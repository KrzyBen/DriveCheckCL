import axios from './root.service.js';
import cookies from 'js-cookie';
import { jwtDecode } from 'jwt-decode';

export async function login(dataUser) {
  try {
    const response = await axios.post('/auth/login', {
      email: dataUser.email,
      password: dataUser.password,
    });
    const { status, data } = response;
    if (status === 200) {
      const { nombre_completo, email, rut, rol } = jwtDecode(data.data.token);
      const userData = { nombre_completo, email, rut, rol };
      sessionStorage.setItem('usuario', JSON.stringify(userData));
      cookies.set('jwt-auth', data.data.token, { path: '/' });
      return response.data;
    }
  } catch (error) {
    return error.response?.data;
  }
}

export async function logout() {
  try {
    await axios.post('/auth/logout');
  } catch (error) {
    console.error('Error al cerrar sesión:', error);
  } finally {
    sessionStorage.removeItem('usuario');
    cookies.remove('jwt-auth');
  }
}