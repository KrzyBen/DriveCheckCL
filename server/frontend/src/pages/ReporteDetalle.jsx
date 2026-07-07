import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Play, Plus, X, Check } from 'lucide-react';
import useReporte from '@hooks/reportes/useReporte.jsx';
import useValidarReporte from '@hooks/reportes/useValidarReporte.jsx';
import useRechazarReporte from '@hooks/reportes/useRechazarReporte.jsx';
import RechazarReportePopup from '@components/RechazarReportePopup';


const CATALOGO_INFRACCIONES = [
  { id: 1, label: 'Art. 141 - No respetar luz roja' },
  { id: 2, label: 'Art. 144 - Exceso de velocidad' },
  { id: 3, label: 'Art. 118 - No mantener distancia de seguimiento' },
  { id: 4, label: 'Art. 114 - Adelantamiento indebido' },
  { id: 5, label: 'Art. 197 - Conducción en estado de ebriedad' },
];

const stateLabels = { enviado: 'Enviado', recibido: 'Recibido', analizando: 'Analizando', aprobado: 'Aprobado', rechazado: 'Rechazado' };

const backendOrigin = import.meta.env.VITE_BASE_URL.replace(/\/api\/?$/, '');

const ReporteDetalle = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { reporte, loading, fetchReporte } = useReporte(id);
  const { handleValidar } = useValidarReporte(fetchReporte);
  const { handleRechazar } = useRechazarReporte(fetchReporte);

  const [clipActivo, setClipActivo] = useState(0);
  const [severidad, setSeveridad] = useState('moderada');
  const [infraccionSel, setInfraccionSel] = useState('');
  const [infracciones, setInfracciones] = useState([]);
  const [manualTexto, setManualTexto] = useState('');
  const [mostrarManual, setMostrarManual] = useState(false);
  const [notas, setNotas] = useState('');
  const [showRechazar, setShowRechazar] = useState(false);

  useEffect(() => {
    if (!reporte) return;
    if (reporte.severidad_validada) setSeveridad(reporte.severidad_validada);
    if (reporte.notas_admin) setNotas(reporte.notas_admin);
    if (reporte.infracciones?.length) {
      setInfracciones(reporte.infracciones.map((i) => ({ tipo: 'catalogo', id: i.id, texto: `${i.articulo} - ${i.descripcion}` })));
    }
  }, [reporte]);

  if (loading) return <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>Cargando...</p>;
  if (!reporte) return <p>Reporte no encontrado.</p>;

  const agregarInfraccion = () => {
    if (infraccionSel === '__otra__') { setMostrarManual(true); return; }
    const item = CATALOGO_INFRACCIONES.find((i) => String(i.id) === infraccionSel);
    if (item) {
      setInfracciones((prev) => [...prev, { tipo: 'catalogo', id: item.id, texto: item.label }]);
      setInfraccionSel('');
    }
  };

  const agregarManual = () => {
    if (manualTexto.trim()) {
      setInfracciones((prev) => [...prev, { tipo: 'manual', texto: manualTexto.trim() }]);
      setManualTexto('');
      setMostrarManual(false);
      setInfraccionSel('');
    }
  };

  const quitarInfraccion = (idx) => setInfracciones((prev) => prev.filter((_, i) => i !== idx));

  const badgeClass = { enviado: 'badge-neutral', recibido: 'badge-neutral', analizando: 'badge-blue', aprobado: 'badge-green', rechazado: 'badge-red' };

  const esFinal = reporte?.estado === 'aprobado' || reporte?.estado === 'rechazado';
  
  const onValidar = async () => {
    const infraccion_ids = infracciones.filter((i) => i.tipo === 'catalogo').map((i) => i.id);
    const infracciones_manuales = infracciones.filter((i) => i.tipo === 'manual').map((i) => i.texto);
    const response = await handleValidar(reporte.id, {
      severidad_validada: severidad, infraccion_ids, infracciones_manuales, notas_admin: notas || null,
    });
    if (response?.status === 'Success') navigate('/reportes');
  };

  const onRechazar = async (motivo) => {
    const response = await handleRechazar(reporte.id, motivo);
    if (response?.status === 'Success') navigate('/reportes');
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <h3 style={{ margin: 0 }}>Reporte #{reporte.id}</h3>
            <span className={`badge ${badgeClass[reporte.estado]}`}>{stateLabels[reporte.estado]}</span>
          </div>
          <p style={{ fontSize: 13, color: 'var(--text-muted)', margin: '4px 0 0' }}>
            {reporte.videos.length} clip(s) · {new Date(reporte.created_at).toLocaleString('es-CL')}
          </p>
        </div>
        <button className="btn" onClick={() => navigate('/reportes')}>
          <ArrowLeft size={14} /> Volver
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1.1fr 1fr', gap: 16 }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <video
            key={reporte.videos[clipActivo]?.id}
            src={`${backendOrigin}/storage/${reporte.videos[clipActivo]?.path}`}
            controls
            style={{ width: '100%', borderRadius: 12, background: '#111' }}
          />
          <div style={{ display: 'flex', gap: 8 }}>
            {reporte.videos.map((v, idx) => (
              <button
                key={v.id}
                className="btn"
                style={{ flex: 1, borderColor: idx === clipActivo ? 'var(--cl-blue)' : 'var(--border)' }}
                onClick={() => setClipActivo(idx)}
              >
                <Play size={13} /> Clip {idx + 1}
              </button>
            ))}
          </div>
          <div className="card" style={{ padding: '1rem 1.25rem', background: 'var(--surface-1)' }}>
            <p style={{ fontSize: 13, fontWeight: 500, margin: '0 0 6px' }}>Comentario del usuario</p>
            <p style={{ fontSize: 13, color: 'var(--text-secondary)', margin: 0 }}>{reporte.comentario || 'Sin comentario'}</p>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <div className="card" style={{ padding: '1rem 1.25rem', background: 'var(--surface-1)' }}>
            <p style={{ fontSize: 13, fontWeight: 500, margin: '0 0 10px' }}>Resultado del modelo</p>
            <div style={{ display: 'flex', gap: 8, marginBottom: 10 }}>
              <div style={{ flex: 1, background: '#fff', borderRadius: 'var(--radius)', padding: '10px 12px' }}>
                <p style={{ fontSize: 12, color: 'var(--text-muted)', margin: '0 0 4px' }}>Severidad IA</p>
                <p style={{ fontSize: 16, fontWeight: 500, margin: 0 }}>{reporte.severidad_ia || 'No procesado por IA'}</p>
              </div>
              <div style={{ flex: 1, background: '#fff', borderRadius: 'var(--radius)', padding: '10px 12px' }}>
                <p style={{ fontSize: 12, color: 'var(--text-muted)', margin: '0 0 4px' }}>Confianza</p>
                <p style={{ fontSize: 16, fontWeight: 500, margin: 0 }}>{reporte.confianza_ia ? `${Math.round(reporte.confianza_ia * 100)}%` : '—'}</p>
              </div>
            </div>
            <label style={{ fontSize: 12, color: 'var(--text-secondary)' }}>Severidad validada</label>
            <select value={severidad} onChange={(e) => setSeveridad(e.target.value)} style={{ width: '100%', marginTop: 4 }}>
              <option value="leve">Leve</option>
              <option value="moderada">Moderada</option>
              <option value="grave">Grave</option>
            </select>
          </div>

          <div className="card" style={{ padding: '1rem 1.25rem', background: 'var(--surface-1)' }}>
            <p style={{ fontSize: 13, fontWeight: 500, margin: '0 0 8px' }}>Infracciones (Ley 18.290)</p>
            <div style={{ display: 'flex', gap: 6, marginBottom: 8 }}>
              <select value={infraccionSel} onChange={(e) => setInfraccionSel(e.target.value)} style={{ flex: 1 }}>
                <option value="">Seleccionar del catálogo...</option>
                {CATALOGO_INFRACCIONES.map((i) => <option key={i.id} value={i.id}>{i.label}</option>)}
                <option value="__otra__">Otra (escribir manualmente)</option>
              </select>
              <button className="btn" onClick={agregarInfraccion}><Plus size={14} /> Agregar</button>
            </div>
            {mostrarManual && (
              <div style={{ display: 'flex', gap: 6, marginBottom: 8 }}>
                <input placeholder="Ej: Art. 168 - No uso de cinturón de seguridad" value={manualTexto} onChange={(e) => setManualTexto(e.target.value)} style={{ flex: 1 }} />
                <button className="btn" onClick={agregarManual}>Agregar</button>
              </div>
            )}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {infracciones.map((inf, idx) => (
                <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '6px 10px', background: '#fff', borderRadius: 8, fontSize: 13 }}>
                  <span>{inf.texto}</span>
                  <button className="btn btn-icon" onClick={() => quitarInfraccion(idx)}><X size={13} /></button>
                </div>
              ))}
            </div>
          </div>

          <div className="card" style={{ padding: '1rem 1.25rem', background: 'var(--surface-1)' }}>
            <label style={{ fontSize: 13, fontWeight: 500 }}>Notas del administrador</label>
            <textarea rows={2} value={notas} onChange={(e) => setNotas(e.target.value)} style={{ width: '100%', marginTop: 6 }} />
          </div>

          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-danger-outline" style={{ flex: 1, justifyContent: 'center' }} onClick={() => setShowRechazar(true)}>
              <X size={15} /> Rechazar
            </button>
            <button className="btn btn-primary" style={{ flex: 1, justifyContent: 'center' }} onClick={onValidar}>
              <Check size={15} /> Validar y generar PDF
            </button>
          </div>
        </div>
      </div>

      <RechazarReportePopup show={showRechazar} setShow={setShowRechazar} action={onRechazar} />
    </div>
  );
};

export default ReporteDetalle;