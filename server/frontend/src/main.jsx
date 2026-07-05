import ReactDOM from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import Login from '@pages/Login';
import Home from '@pages/Home';
import Usuarios from '@pages/Usuarios';
import Reportes from '@pages/Reportes';
import ReporteDetalle from '@pages/ReporteDetalle';
import Perfil from '@pages/Perfil';
import Error404 from '@pages/Error404';
import Root from '@pages/Root';
import ProtectedRoute from '@components/ProtectedRoute';
import '@styles/styles.css';

const router = createBrowserRouter([
  {
    path: '/',
    element: <Root />,
    errorElement: <Error404 />,
    children: [
      { index: true, element: <Home /> },
      { path: '/home', element: <Home /> },
      {
        path: '/usuarios',
        element: (
          <ProtectedRoute allowedRoles={['administrador']}>
            <Usuarios />
          </ProtectedRoute>
        ),
      },
      {
        path: '/reportes',
        element: (
          <ProtectedRoute allowedRoles={['administrador', 'validador']}>
            <Reportes />
          </ProtectedRoute>
        ),
      },
      {
        path: '/reportes/:id',
        element: (
          <ProtectedRoute allowedRoles={['administrador', 'validador']}>
            <ReporteDetalle />
          </ProtectedRoute>
        ),
      },
      { path: '/perfil', element: <Perfil /> },
    ],
  },
  { path: '/login', element: <Login /> },
]);

ReactDOM.createRoot(document.getElementById('root')).render(
  <RouterProvider router={router} />
);