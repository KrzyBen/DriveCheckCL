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
    <main className="login">
      <div className="panel login-card">
        <div className="panel__core">
          <div className="login-stripe" aria-hidden="true">
            <span style={{ background: 'var(--cl-blue)' }} />
            <span style={{ background: '#fff' }} />
            <span style={{ background: 'var(--cl-red)' }} />
          </div>
          <div className="login-brand">
            <div className="login-logo">
              <ShieldCheck size={26} color="#fff" />
            </div>
            <h1 className="login-title">DriveCheckCL</h1>
            <p className="login-sub">Panel de administración</p>
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