import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Play, Plus, X, Check, Sparkles, CarFront, AlertTriangle, IdCard, Download } from 'lucide-react';
import useReporte from '@hooks/reportes/useReporte.jsx';
import useValidarReporte from '@hooks/reportes/useValidarReporte.jsx';
import useRechazarReporte from '@hooks/reportes/useRechazarReporte.jsx';
import useAnalizarReporte from '@hooks/reportes/useAnalizarReporte.jsx';
import RechazarReportePopup from '@components/RechazarReportePopup';
import { getInfracciones } from '@services/infraction.service.js';

const stateLabels = { enviado: 'Enviado', recibido: 'Recibido', analizando: 'Analizando', aprobado: 'Aprobado', rechazado: 'Rechazado' };
const badgeClass = { enviado: 'badge-neutral', recibido: 'badge-neutral', analizando: 'badge-blue badge-live', aprobado: 'badge-green', rechazado: 'badge-red' };

// Convención de claves que va a escribir backend-ia dentro de resultados_ia.
// Cualquier capa nueva que no esté en este mapa igual se muestra (fallback genérico más abajo).
const CAPAS_IA = {
  accidentes:   { label: 'Accidentes',   icon: CarFront },
  imprudencias: { label: 'Imprudencias', icon: AlertTriangle },
  patente:      { label: 'Patente',      icon: IdCard },
};

const backendOrigin = import.meta.env.VITE_BASE_URL.replace(/\/api\/?$/, '');

const ReporteDetalle = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { reporte, loading, fetchReporte } = useReporte(id);
  const { handleValidar } = useValidarReporte(fetchReporte);
  const { handleRechazar } = useRechazarReporte(fetchReporte);
  const { handleAnalizar } = useAnalizarReporte(fetchReporte);

  const [catalogoInfracciones, setCatalogoInfracciones] = useState([]);

  useEffect(() => {
  getInfracciones().then((response) => {
    if (response?.status === 'Success' && Array.isArray(response.data)) {
      setCatalogoInfracciones(response.data);
    }
  });
}, []);

  const [clipActivo, setClipActivo] = useState(0);
  const [severidad, setSeveridad] = useState('moderada');
  const [infraccionSel, setInfraccionSel] = useState('');
  const [infracciones, setInfracciones] = useState([]);
  const [manualTexto, setManualTexto] = useState('');
  const [mostrarManual, setMostrarManual] = useState(false);
  const [notas, setNotas] = useState('');
  const [showRechazar, setShowRechazar] = useState(false);
  const [analizando, setAnalizando] = useState(false);

  const pollRef = useRef(null);

  useEffect(() => {
    if (!reporte) return;
    if (reporte.severidad_validada) setSeveridad(reporte.severidad_validada);
    else if (reporte.validacion?.severidad_propuesta) setSeveridad(reporte.validacion.severidad_propuesta);
    if (reporte.notas_admin) setNotas(reporte.notas_admin);
    if (reporte.infracciones?.length) {
      setInfracciones(reporte.infracciones.map((i) => ({ tipo: 'catalogo', id: i.id, texto: `${i.articulo} - ${i.descripcion}` })));
    }
  }, [reporte]);

  // Mientras el análisis está en curso, consulta el reporte cada 3s hasta que
  // backend-ia responda (completado o error). Sin esto habría que refrescar la
  // página a mano para ver el resultado.
  useEffect(() => {
    const enCurso = reporte?.validacion?.estado_analisis === 'analizando';
    setAnalizando(enCurso);

    if (enCurso && !pollRef.current) {
      pollRef.current = setInterval(() => { fetchReporte(); }, 3000);
    }
    if (!enCurso && pollRef.current) {
      clearInterval(pollRef.current);
      pollRef.current = null;
    }
    return () => {
      if (pollRef.current) { clearInterval(pollRef.current); pollRef.current = null; }
    };
  }, [reporte?.validacion?.estado_analisis, fetchReporte]);

  // Solo bloquea la pantalla en la carga inicial: durante el polling de 3s del
  // análisis con IA `loading` vuelve a true pero `reporte` ya existe, y desmontar
  // toda la página en cada refetch era lo que producía el pestañeo.
  if (loading && !reporte) return <p className="page-sub">Cargando...</p>;
  if (!reporte) return <p>Reporte no encontrado.</p>;

  const agregarInfraccion = () => {
    if (infraccionSel === '__otra__') { setMostrarManual(true); return; }
    const item = catalogoInfracciones.find((i) => String(i.id) === infraccionSel);
    if (item) {
      setInfracciones((prev) => [...prev, { tipo: 'catalogo', id: item.id, texto: `${item.articulo} - ${item.descripcion}` }]);
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

  const esFinal = reporte.estado === 'aprobado' || reporte.estado === 'rechazado';
  const validacion = reporte.validacion;
  const puedeAnalizar = reporte.estado === 'recibido' || (reporte.estado === 'analizando' && validacion?.estado_analisis === 'error');

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

  const onAnalizar = () => handleAnalizar(reporte.id);

  return (
    <div className="page-enter">
      <div className="page-head">
        <div>
          <div className="detalle-title">
            <h1 className="page-title">Reporte #{reporte.id}</h1>
            <span className={`badge ${badgeClass[reporte.estado]}`}>{stateLabels[reporte.estado]}</span>
          </div>
          <p className="page-sub">
            {reporte.videos.length} clip(s) · {new Date(reporte.created_at).toLocaleString('es-CL')}
          </p>
        </div>
        <button className="btn" onClick={() => navigate('/reportes')}>
          <ArrowLeft size={16} /> Volver
        </button>
      </div>

      <div className="detalle-grid">
        <div className="stack">
          <div className="panel">
            <div className="video-frame">
              <video
                key={reporte.videos[clipActivo]?.id}
                src={`${backendOrigin}/storage/${reporte.videos[clipActivo]?.path}`}
                controls
              />
            </div>
          </div>
          <div className="clip-tabs">
            {reporte.videos.map((v, idx) => (
              <button
                key={v.id}
                className={`btn${idx === clipActivo ? ' is-active' : ''}`}
                aria-pressed={idx === clipActivo}
                onClick={() => setClipActivo(idx)}
              >
                <Play size={14} /> Clip {idx + 1}
              </button>
            ))}
          </div>
          <div className="panel">
            <div className="panel__core">
              <p className="panel__title">Comentario del usuario</p>
              <p className="panel__text">{reporte.comentario || 'Sin comentario'}</p>
            </div>
          </div>
        </div>

        <div className="stack">

          {/* Análisis con IA: solo tiene sentido mientras el reporte no está finalizado */}
          {!esFinal && (
            <div className="panel panel--ai">
              <div className="panel__core">
                <div className="ia-head">
                  <p className="panel__title"><Sparkles size={17} /> Análisis con IA</p>
                  {puedeAnalizar && !analizando && (
                    <button className="btn btn-primary" onClick={onAnalizar}>
                      <Sparkles size={15} /> {validacion?.estado_analisis === 'error' ? 'Reintentar' : 'Analizar'}
                    </button>
                  )}
                  {analizando && (
                    <span className="badge badge-blue badge-live">Analizando…</span>
                  )}
                </div>

                {!validacion && (
                  <p className="ia-empty">Aún no se ha analizado este reporte.</p>
                )}

                {validacion?.estado_analisis === 'error' && (
                  <p className="ia-error">
                    No se pudo completar el análisis (backend-ia no respondió). Puedes reintentar.
                  </p>
                )}

                {validacion?.resultados_ia && (
                  <div className="ia-list">
                    {Object.entries(validacion.resultados_ia).map(([clave, valor]) => {
                      const capa = CAPAS_IA[clave] || { label: clave, icon: Sparkles };
                      const Icono = capa.icon;
                      const confianza = valor?.confianza != null ? `${Math.round(valor.confianza * 100)}%` : null;
                      const texto = valor?.texto || valor?.resultado || 'Sin datos';
                      const esNeutro = valor?.resultado === 'sin_evidencia';
                      return (
                        <div key={clave} className="ia-row">
                          <span className="ia-row__label">
                            <Icono size={18} /> {capa.label}
                          </span>
                          <span className={`badge ${esNeutro ? 'badge-green' : clave === 'patente' ? 'badge-neutral' : 'badge-amber'}`}>
                            {texto}{confianza ? ` · ${confianza}` : ''}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>
          )}

          {esFinal ? (
            /* Vista de solo lectura una vez finalizado: ya no se puede editar ni re-enviar. */
            <div className={`final ${reporte.estado === 'aprobado' ? 'final--ok' : 'final--bad'}`}>
              <div className="final__head">
                {reporte.estado === 'aprobado' ? <Check size={16} /> : <X size={16} />}
                <span>
                  {reporte.estado === 'aprobado' ? `Aprobado · Severidad ${severidad}` : 'Rechazado'}
                </span>
              </div>
              {infracciones.length > 0 && (
                <p className="final__line">
                  {infracciones.map((i) => i.texto).join(' · ')}
                </p>
              )}
              {notas && <p className="final__line">Notas: {notas}</p>}
              {reporte.pdf_disponible && (
                <a className="btn" href={`${backendOrigin}/storage/${reporte.pdf_path}`} target="_blank" rel="noreferrer">
                  <Download size={15} /> Ver PDF
                </a>
              )}
            </div>
          ) : (
            <>
              <div className="panel">
                <div className="panel__core">
                  <label className="field-label" htmlFor="severidad">
                    Severidad final
                    {validacion?.severidad_propuesta && (
                      <span className="field-label__hint"> · IA sugiere: {validacion.severidad_propuesta}</span>
                    )}
                  </label>
                  <select id="severidad" value={severidad} onChange={(e) => setSeveridad(e.target.value)}>
                    <option value="leve">Leve</option>
                    <option value="moderada">Moderada</option>
                    <option value="grave">Grave</option>
                  </select>
                </div>
              </div>

              <div className="panel">
                <div className="panel__core">
                  <p className="panel__title">Infracciones (Ley 18.290)</p>
                  <div className="infra-add">
                    <select aria-label="Catálogo de infracciones" value={infraccionSel} onChange={(e) => setInfraccionSel(e.target.value)}>
                      <option value="">Seleccionar del catálogo...</option>
                      {catalogoInfracciones.map((i) => <option key={i.id} value={i.id}>{i.articulo} - {i.descripcion}</option>)}
                      <option value="__otra__">Otra (escribir manualmente)</option>
                    </select>
                    <button className="btn" onClick={agregarInfraccion}><Plus size={15} /> Agregar</button>
                  </div>
                  {mostrarManual && (
                    <div className="infra-add">
                      <input aria-label="Infracción manual" placeholder="Ej: Art. 168 - No uso de cinturón de seguridad" value={manualTexto} onChange={(e) => setManualTexto(e.target.value)} />
                      <button className="btn" onClick={agregarManual}>Agregar</button>
                    </div>
                  )}
                  <div className="infra-list">
                    {infracciones.map((inf, idx) => (
                      <div key={idx} className="infra-item">
                        <span>{inf.texto}</span>
                        <button className="btn btn-icon" aria-label="Quitar infracción" onClick={() => quitarInfraccion(idx)}><X size={14} /></button>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              <div className="panel">
                <div className="panel__core">
                  <label className="field-label" htmlFor="notas">Notas del administrador</label>
                  <textarea id="notas" rows={2} value={notas} onChange={(e) => setNotas(e.target.value)} />
                </div>
              </div>

              <div className="actions-row">
                <button className="btn btn-danger-outline" onClick={() => setShowRechazar(true)}>
                  <X size={16} /> Rechazar
                </button>
                <button className="btn btn-primary" onClick={onValidar}>
                  <Check size={16} /> Validar y generar PDF
                </button>
              </div>
            </>
          )}
        </div>
      </div>

      <RechazarReportePopup show={showRechazar} setShow={setShowRechazar} action={onRechazar} />
    </div>
  );
};

export default ReporteDetalle;
