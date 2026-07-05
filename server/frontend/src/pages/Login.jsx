import { useNavigate } from 'react-router-dom';
import { login } from '@services/auth.service.js';
import Form from '@components/Form';
import useLogin from '@hooks/auth/useLogin.jsx';
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
    <main
      className="container login-bg"
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, var(--cl-blue-800), var(--cl-blue))',
      }}
    >
      <Form
        title="DriveCheckCL · Panel de administración"
        fields={[
          {
            label: 'Correo electrónico',
            name: 'email',
            placeholder: 'admin@drivecheckcl.cl',
            fieldType: 'input',
            type: 'email',
            required: true,
            errorMessageData: errorEmail,
            onChange: (e) => handleInputChange('email', e.target.value),
          },
          {
            label: 'Contraseña',
            name: 'password',
            placeholder: '**********',
            fieldType: 'input',
            type: 'password',
            required: true,
            minLength: 8,
            maxLength: 26,
            errorMessageData: errorPassword,
            onChange: (e) => handleInputChange('password', e.target.value),
          },
        ]}
        buttonText="Iniciar sesión"
        onSubmit={loginSubmit}
      />
    </main>
  );
};

export default Login;