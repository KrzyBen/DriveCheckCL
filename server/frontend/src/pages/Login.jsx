import { useNavigate } from 'react-router-dom';
import { login } from '@services/auth.service.js';
import Form from '@components/Form';
import useLogin from '@hooks/auth/useLogin.jsx';
import { ShieldCheck } from 'lucide-react';
import '@styles/form.css';

const Login = () => {
  const navigate = useNavigate();
  const { errorEmail, errorPassword, errorData, handleInputChange } = useLogin();

  const loginSubmit = async (data) => {
    try {
      const response = await login(data);
      if (response.status === 'Success') {
        navigate('/home');
      } else if (response.status === 'Client error') {
        errorData(response.details);
      }
    } catch (error) {
      console.log(error);
    }
  };

  return (
    <main style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--surface-1)' }}>
      <div className="card" style={{ width: '100%', maxWidth: 340, overflow: 'hidden' }}>
        <div style={{ height: 5, display: 'flex' }}>
          <div style={{ flex: 1, background: 'var(--cl-blue)' }} />
          <div style={{ flex: 1, background: '#fff' }} />
          <div style={{ flex: 1, background: 'var(--cl-red)' }} />
        </div>
        <div style={{ padding: '2rem 1.75rem' }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, marginBottom: '1.5rem' }}>
            <div style={{ width: 48, height: 48, borderRadius: 12, background: 'var(--cl-blue)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <ShieldCheck size={24} color="#fff" />
            </div>
            <h3 style={{ margin: '8px 0 0' }}>DriveCheckCL</h3>
            <p style={{ fontSize: 13, color: 'var(--text-muted)', margin: 0 }}>Panel de administración</p>
          </div>

          <Form
            fields={[
              {
                label: 'Correo', name: 'email', placeholder: 'usuario@drivecheckcl.cl',
                fieldType: 'input', type: 'email', required: true,
                errorMessageData: errorEmail,
                onChange: (e) => handleInputChange('email', e.target.value),
              },
              {
                label: 'Contraseña', name: 'password', placeholder: '••••••••',
                fieldType: 'input', type: 'password', required: true,
                minLength: 8, maxLength: 26,
                errorMessageData: errorPassword,
                onChange: (e) => handleInputChange('password', e.target.value),
              },
            ]}
            buttonText="Iniciar sesión"
            onSubmit={loginSubmit}
          />
        </div>
      </div>
    </main>
  );
};

export default Login;