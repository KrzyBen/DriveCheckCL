import Form from './Form';
import '@styles/popup.css';

export default function RechazarReportePopup({ show, setShow, action }) {
  const handleSubmit = (formData) => {
    action(formData.notas_admin);
    setShow(false);
  };

  return (
    <div>
      {show && (
        <div className="bg">
          <div className="popup">
            <button className="close" onClick={() => setShow(false)}>✕</button>
            <Form
              title="Rechazar reporte"
              fields={[
                {
                  label: 'Motivo del rechazo',
                  name: 'notas_admin',
                  placeholder: 'Explica por qué se rechaza este reporte...',
                  fieldType: 'textarea',
                  required: true,
                  minLength: 10,
                  maxLength: 1000,
                },
              ]}
              onSubmit={handleSubmit}
              buttonText="Confirmar rechazo"
              backgroundColor="#fff"
            />
          </div>
        </div>
      )}
    </div>
  );
}