import { useAuth } from '@context/AuthContext';
import Form from '@components/Form';
import usePerfil from '@hooks/perfil/usePerfil.jsx';

const iniciales = (nombre = '') =>
  nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase();

const Perfil = () => {
  const { user } = useAuth();
  const { handleUpdate } = usePerfil();
  const handleSubmit = (formData) => handleUpdate(formData);

  return (
    <div className="page-enter">
      <div className="page-head">
        <h1 className="page-title">Mi perfil</h1>
      </div>

      <div className="perfil-grid">
        <aside className="panel panel--identity">
          <div className="panel__core perfil-id">
            <span className="avatar avatar--xl" aria-hidden="true">{iniciales(user?.nombre_completo)}</span>
            <p className="perfil-id__name">{user?.nombre_completo}</p>
            <p className="perfil-id__mail">{user?.email}</p>
            {user?.rol && <span className="badge badge-blue">{user.rol}</span>}
          </div>
        </aside>

        <section className="panel">
          <div className="panel__core perfil-form">
            <Form
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
        </section>
      </div>
    </div>
  );
};

export default Perfil;
