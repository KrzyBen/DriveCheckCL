import { useAuth } from '@context/AuthContext';
import Form from '@components/Form';
import usePerfil from '@hooks/perfil/usePerfil.jsx';

const Perfil = () => {
  const { user } = useAuth();
  const { handleUpdate } = usePerfil();
  const handleSubmit = (formData) => handleUpdate(formData);

  return (
    <div className="card" style={{ padding: '1.5rem', maxWidth: 420 }}>
      <Form
        title="Mi perfil"
        initialData={{ nombre_completo: user?.nombre_completo, email: user?.email, rut: user?.rut }}
        fields={[
          { label: 'Nombre completo', name: 'nombre_completo', fieldType: 'input', type: 'text', required: true },
          { label: 'Correo electrónico', name: 'email', fieldType: 'input', type: 'email', required: true },
          { label: 'RUT', name: 'rut', fieldType: 'input', type: 'text', disabled: true },
          { label: 'Contraseña actual', name: 'password', fieldType: 'input', type: 'password', placeholder: 'Requerida para cambiar datos' },
          { label: 'Nueva contraseña (opcional)', name: 'new_password', fieldType: 'input', type: 'password' },
        ]}
        onSubmit={handleSubmit}
        buttonText="Guardar cambios"
      />
    </div>
  );
};

export default Perfil;