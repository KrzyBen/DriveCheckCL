import Form from './Form';
import '@styles/popup.css';

export default function UsuarioPopup({ show, setShow, data, action, mode = 'editar' }) {
  const userData = data && data.length > 0 ? data[0] : {};
  const patternRut = /^(?:(?:[1-9]\d{0}|[1-2]\d{1})(\.\d{3}){2}|[1-9]\d{6}|[1-2]\d{7}|29\.999\.999|29999999)-[\dkK]$/;

  const handleSubmit = (formData) => action(formData);

  const fields = [
    {
      label: 'Nombre completo',
      name: 'nombre_completo',
      defaultValue: userData.nombre_completo || '',
      placeholder: 'Antonia Fuentes Rojas',
      fieldType: 'input',
      type: 'text',
      required: true,
      minLength: 10,
      maxLength: 50,
      pattern: /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/,
      patternMessage: 'Debe contener solo letras y espacios',
    },
    {
      label: 'Correo electrónico',
      name: 'email',
      defaultValue: userData.email || '',
      placeholder: 'nombre@drivecheckcl.cl',
      fieldType: 'input',
      type: 'email',
      required: true,
    },
    {
      label: 'RUT',
      name: 'rut',
      defaultValue: userData.rut || '',
      placeholder: '21.308.770-3',
      fieldType: 'input',
      type: 'text',
      required: true,
      pattern: patternRut,
      patternMessage: 'Debe ser xx.xxx.xxx-x o xxxxxxxx-x',
    },
    {
      label: 'Rol',
      name: 'rol',
      fieldType: 'select',
      defaultValue: userData.rol || 'conductor',
      required: true,
      options: [
        { value: 'administrador', label: 'Administrador' },
        { value: 'validador', label: 'Validador' },
        { value: 'conductor', label: 'Conductor' },
      ],
    },
    mode === 'crear'
      ? {
          label: 'Contraseña',
          name: 'password',
          placeholder: '**********',
          fieldType: 'input',
          type: 'password',
          required: true,
          minLength: 8,
          maxLength: 26,
          pattern: /^[a-zA-Z0-9]+$/,
          patternMessage: 'Debe contener solo letras y números',
        }
      : {
          label: 'Nueva contraseña (opcional)',
          name: 'new_password',
          placeholder: '**********',
          fieldType: 'input',
          type: 'password',
          required: false,
          minLength: 8,
          maxLength: 26,
          pattern: /^[a-zA-Z0-9]+$/,
          patternMessage: 'Debe contener solo letras y números',
        },
  ];

  return (
    <div>
      {show && (
        <div className="bg">
          <div className="popup">
            <button className="close" onClick={() => setShow(false)}>✕</button>
            <Form
              title={mode === 'crear' ? 'Nuevo usuario' : 'Editar usuario'}
              fields={fields}
              onSubmit={handleSubmit}
              buttonText={mode === 'crear' ? 'Crear usuario' : 'Guardar cambios'}
              backgroundColor="#fff"
            />
          </div>
        </div>
      )}
    </div>
  );
}