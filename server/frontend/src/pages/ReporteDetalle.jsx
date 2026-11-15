import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Play, Plus, X, Check, Sparkles, CarFront, AlertTriangle, IdCard, Download, MapPin, ExternalLink } from 'lucide-react';
import useReporte from '@hooks/reportes/useReporte.jsx';
import useValidarReporte from '@hooks/reportes/useValidarReporte.jsx';
import useRechazarReporte from '@hooks/reportes/useRechazarReporte.jsx';
import useAnalizarReporte from '@hooks/reportes/useAnalizarReporte.jsx';
import RechazarReportePopup from '@components/RechazarReportePopup';
import { getInfracciones } from '@services/infraction.service.js';
import { showErrorAlert } from '@helpers/sweetAlert.js';

const stateLabels = { enviado: 'Enviado', recibido: 'Recibido', analizando: 'Analizando', aprobado: 'Aprobado', rechazado: 'Rechazado' };
const badgeClass = { enviado: 'badge-neutral', recibido: 'badge-neutral', analizando: 'badge-blue badge-live', aprobado: 'badge-green', rechazado: 'badge-red' };

// Convención de claves que va a escribir backend-ia dentro de resultados_ia.
// Cualquier capa nueva que no esté en este mapa igual se muestra (fallback genérico más abajo).
const CAPAS_IA = {
  accidentes:   { label: 'Accidentes',   icon: CarFront },
  imprudencias: { label: 'Imprudencias', icon: AlertTriangle },
  patente:      { label: 'Patente',      icon: IdCard },
};

// Textos para el contrato v1 de resultados_ia (ver backend-ia/src/contrato.py).
const HALLAZGOS = {
  posible_luz_roja: 'Posible luz roja',
  senales_detectadas: 'Señales detectadas',
  sin_evidencia: 'Sin evidencia',
  detectada: 'Detectada',
  sin_lectura: 'Sin lectura',
  sin_deteccion: 'No detectada',
};
const ERRORES_CAPA = {
  video_ilegible: 'No se pudo leer el video',
  video_no_encontrado: 'Video no encontrado',
  error_interno: 'Error interno del análisis',
};

const backendOrigin = import.meta.env.VITE_BASE_URL.replace(/\/api\/?$/, '');

// Las URLs de archivos llegan firmadas desde el backend ('/storage/...?exp=..&sig=..')
const urlArchivo = (url) => (url ? `${backendOrigin}${url}` : null);

// true si la URL firmada ya venció o vence en menos de `margenS` segundos (lee su ?exp=)
const urlVencida = (url, margenS = 30) => {
  try {
    const exp = Number(new URL(url, backendOrigin).searchParams.get('exp'));
    return !exp || exp * 1000 <= Date.now() + margenS * 1000;
  } catch {
    return true;
  }
};

// Abre el punto del evento en OpenStreetMap (enlace externo, sin librerías de mapa).
const urlMapa = ({ lat, lon }) => `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lon}#map=17/${lat}/${lon}`;

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
  const [patente, setPatente] = useState('');
  const [showRechazar, setShowRechazar] = useState(false);
  const [analizando, setAnalizando] = useState(false);

  const pollRef = useRef(null);

  // ── Video del clip activo ──
  // El src se fija solo al cambiar de clip (video.id), no con cada refresco del reporte: así el
  // polling de 3 s del análisis no recarga el <video> aunque el backend entregue una URL nueva.
  const videoActual = reporte?.videos?.[clipActivo];
  const videoRef = useRef(null);
  const [srcVideo, setSrcVideo] = useState(null);
  const [errorVideo, setErrorVideo] = useState(false);
  const reproduciendoRef = useRef(false); // se lleva con onPlay/onPause: tras un error `paused` no es fiable
  const reintentandoRef = useRef(false);  // un solo reintento por error, para no entrar en bucle
  const reanudarRef = useRef(null);       // { tiempo, reproduciendo } a restaurar con la URL nueva

  useEffect(() => {
    setSrcVideo(urlArchivo(videoActual?.url));
    setErrorVideo(false);
    reintentandoRef.current = false;
    reanudarRef.current = null;
    reproduciendoRef.current = false;
    // Solo depende del id: un refresco del mismo clip no debe cambiar el src
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [videoActual?.id]);

  useEffect(() => {
    if (!reporte) return;
    if (reporte.severidad_validada) setSeveridad(reporte.severidad_validada);
    if (reporte.notas_admin) setNotas(reporte.notas_admin);
    // La IA solo precarga la patente si el admin no escribió una: no se pisa lo que ya hay en el campo.
    if (reporte.patente_confirmada) setPatente((actual) => actual || reporte.patente_confirmada);
    const delCatalogo = (reporte.infracciones || []).map((i) => ({ tipo: 'catalogo', id: i.id, texto: `${i.articulo} - ${i.descripcion}` }));
    const manuales = (reporte.infracciones_manuales || []).map((texto) => ({ tipo: 'manual', texto }));
    if (delCatalogo.length || manuales.length) setInfracciones([...delCatalogo, ...manuales]);
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
      severidad_validada: severidad, infraccion_ids, infracciones_manuales,
      patente_confirmada: patente.trim() || null, notas_admin: notas || null,
    });
    if (response?.status === 'Success') navigate('/reportes');
  };

  const onRechazar = async (motivo) => {
    const response = await handleRechazar(reporte.id, motivo);
    if (response?.status === 'Success') navigate('/reportes');
  };

  const onAnalizar = () => handleAnalizar(reporte.id);

  // La URL del video venció (403) o falló la carga: se pide una URL nueva y se retoma donde iba
  const onErrorVideo = async () => {
    const elemento = videoRef.current;
    if (!elemento || reintentandoRef.current) { setErrorVideo(true); return; }
    reintentandoRef.current = true;
    reanudarRef.current = { tiempo: elemento.currentTime, reproduciendo: reproduciendoRef.current };
    const idVideo = videoActual?.id;
    const nuevo = await fetchReporte({ silencioso: true });
    const src = urlArchivo(nuevo?.videos?.find((v) => v.id === idVideo)?.url);
    // Si no hay URL o es la misma que falló, no hay nada que reintentar
    if (!src || src === srcVideo) { setErrorVideo(true); return; }
    setSrcVideo(src);
  };

  // Cargó la URL (nueva o no): se restaura la posición y se rearma el reintento para un error futuro
  const onVideoCargado = () => {
    const elemento = videoRef.current;
    const reanudar = reanudarRef.current;
    reanudarRef.current = null;
    reintentandoRef.current = false;
    setErrorVideo(false);
    if (!elemento || !reanudar) return;
    elemento.currentTime = reanudar.tiempo;
    // play() puede rechazarse (p. ej. política de reproducción automática): se deja en pausa
    if (reanudar.reproduciendo) elemento.play().catch(() => {});
  };

  // PDF: si la URL firmada ya venció, se pide una nueva antes de abrirlo
  const onAbrirPdf = async (e) => {
    if (!urlVencida(reporte.pdf_url)) return; // vigente: el enlace abre normalmente
    e.preventDefault();
    // La pestaña se abre ya, dentro del clic: abrirla después del await la bloquearía el navegador
    const ventana = window.open('', '_blank');
    if (ventana) ventana.opener = null; // igual que rel="noreferrer": la pestaña no controla el panel
    const nuevo = await fetchReporte({ silencioso: true });
    const url = urlArchivo(nuevo?.pdf_url);
    if (ventana && url) {
      ventana.location.href = url;
    } else {
      ventana?.close();
      showErrorAlert('No se pudo abrir el PDF', 'Inténtalo de nuevo en unos segundos.');
    }
  };

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
                key={videoActual?.id}
                ref={videoRef}
                src={srcVideo || undefined}
                controls
                onError={onErrorVideo}
                onLoadedMetadata={onVideoCargado}
                onPlay={() => { reproduciendoRef.current = true; }}
                onPause={() => { reproduciendoRef.current = false; }}
              />
            </div>
            {errorVideo && (
              <p className="ia-error">No se pudo cargar el video. Recarga la página para intentarlo de nuevo.</p>
            )}
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
          <div className="panel">
            <div className="panel__core">
              <p className="panel__title lugar-title"><MapPin size={17} /> Lugar y hora de grabación</p>
              <p className="panel__text">
                {reporte.grabado_en
                  ? new Date(reporte.grabado_en).toLocaleString('es-CL', { timeZone: 'America/Santiago' })
                  : 'Hora de grabación no disponible'}
              </p>
              <p className="panel__text">
                {reporte.ubicacion ? (
                  <>
                    {reporte.ubicacion.lat.toFixed(5)}, {reporte.ubicacion.lon.toFixed(5)}{' · '}
                    <a className="lugar-link" href={urlMapa(reporte.ubicacion)} target="_blank" rel="noopener noreferrer">
                      Ver en el mapa <ExternalLink size={13} />
                    </a>
                  </>
                ) : 'Ubicación no disponible'}
              </p>
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
                    No se pudo completar el análisis. Puedes reintentar.
                  </p>
                )}

                {validacion?.resultados_ia && (
                  <div className="ia-list">
                    {/* Contrato v1: { version, capas: { <capa>: {...} } }. Los reportes analizados antes traen las capas en la raíz. */}
                    {Object.entries(validacion.resultados_ia.capas ?? validacion.resultados_ia).map(([clave, valor]) => {
                      const capa = CAPAS_IA[clave] || { label: clave, icon: Sparkles };
                      const Icono = capa.icon;
                      const confianza = valor?.confianza != null ? `${Math.round(valor.confianza * 100)}%` : null;
                      const hallazgo = valor?.hallazgo ?? valor?.resultado;
                      const detalle = valor?.detalle ?? {};
                      const enError = valor?.estado === 'error';
                      let texto;
                      if (enError) texto = ERRORES_CAPA[valor.codigo] || 'Error en el análisis';
                      else if (clave === 'patente' && (detalle.texto ?? valor?.texto)) {
                        texto = `${detalle.texto ?? valor.texto}${detalle.formato_valido === false ? ' (formato no válido)' : ''}`;
                      } else texto = HALLAZGOS[hallazgo] || hallazgo || 'Sin datos';
                      const esNeutro = hallazgo === 'sin_evidencia';
                      const clip = !enError && valor?.video ? ` · clip ${valor.video}` : '';
                      const claseBadge = enError ? 'badge-red' : esNeutro ? 'badge-green' : clave === 'patente' ? 'badge-neutral' : 'badge-amber';
                      return (
                        <div key={clave} className="ia-row">
                          <span className="ia-row__label">
                            <Icono size={18} /> {capa.label}
                          </span>
                          <span className={`badge ${claseBadge}`}>
                            {texto}{confianza ? ` · ${confianza}` : ''}{clip}
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
              {reporte.patente_confirmada && <p className="final__line">Patente: {reporte.patente_confirmada}</p>}
              {notas && <p className="final__line">Notas: {notas}</p>}
              {reporte.pdf_disponible && reporte.pdf_url && (
                <a className="btn" href={urlArchivo(reporte.pdf_url)} target="_blank" rel="noreferrer" onClick={onAbrirPdf}>
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
                  <label className="field-label" htmlFor="patente">Patente del vehículo</label>
                  <input
                    id="patente"
                    placeholder="Ej: BCDF12"
                    maxLength={10}
                    value={patente}
                    onChange={(e) => setPatente(e.target.value.toUpperCase())}
                  />
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