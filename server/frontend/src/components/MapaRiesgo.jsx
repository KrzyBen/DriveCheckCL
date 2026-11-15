import { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { LocateFixed, Crosshair } from 'lucide-react';
import useMapaRiesgo from '@hooks/mapa/useMapaRiesgo.jsx';
import { getInfracciones } from '@services/infraction.service.js';
import '@styles/mapa.css';

// Región del Biobío (suroeste y noreste). Ñuble ya no forma parte desde 2018.
const BIOBIO_LIMITES = [[-38.55, -73.95], [-36.4, -70.95]];
const DIAS = [
  { n: 1, letra: 'L', nombre: 'Lunes' }, { n: 2, letra: 'M', nombre: 'Martes' },
  { n: 3, letra: 'X', nombre: 'Miércoles' }, { n: 4, letra: 'J', nombre: 'Jueves' },
  { n: 5, letra: 'V', nombre: 'Viernes' }, { n: 6, letra: 'S', nombre: 'Sábado' },
  { n: 7, letra: 'D', nombre: 'Domingo' },
];
const SEVERIDADES = [
  { valor: 'leve', etiqueta: 'Leve', color: '#7FA4F0' },
  { valor: 'moderada', etiqueta: 'Moderada', color: '#E39A2B' },
  { valor: 'grave', etiqueta: 'Grave', color: '#D52B1E' },
];
const GRADIENTE = { 0.25: '#7FA4F0', 0.5: '#F0A630', 0.75: '#E8791C', 1: '#D52B1E' };
const ZONA = 'America/Santiago';

// Fondo del mapa. OpenStreetMap exige que el navegador envíe el Referer (referrerPolicy) y que se vea la
// atribución; sin el Referer bloquea los mapas ("Referer is required"). Para cambiar de proveedor,
// basta con cambiar este objeto.
const FONDO = {
  url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  opciones: {
    maxZoom: 19,
    referrerPolicy: 'origin',
    attribution: '&copy; Colaboradores de <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a>',
  },
};

const dosDigitos = (n) => String(n).padStart(2, '0');
const textoHora = (h) => `${dosDigitos(h)}:00`;
const fechaISO = (d) => `${d.getFullYear()}-${dosDigitos(d.getMonth() + 1)}-${dosDigitos(d.getDate())}`;
const fechaHoraLocal = (iso) => new Date(iso).toLocaleString('es-CL', { timeZone: ZONA });

// Misma regla que el servidor: desde inclusive, hasta exclusive, cruza la medianoche si desde > hasta.
const enFranja = (hora, desde, hasta) => {
  if (desde === hasta) return true;
  if (desde < hasta) return hora >= desde && hora < hasta;
  return hora >= desde || hora < hasta;
};

// El título lo escribe el usuario: se arma con textContent para no interpretar HTML.
const crearTooltip = (ev) => {
  const caja = document.createElement('div');
  const titulo = document.createElement('strong');
  titulo.textContent = ev.titulo || `Reporte ${ev.id}`;
  caja.appendChild(titulo);
  const linea = (texto) => {
    const p = document.createElement('div');
    p.textContent = texto;
    caja.appendChild(p);
  };
  linea(fechaHoraLocal(ev.fecha_hora));
  linea(`Severidad: ${ev.severidad || 'sin definir'}`);
  if (ev.infracciones?.length) linea(ev.infracciones.join(' · '));
  linea('Clic para abrir el reporte');
  return caja;
};

const MapaRiesgo = () => {
  const navigate = useNavigate();
  const { datos, loading, error, fetchMapa } = useMapaRiesgo();

  const hoy = new Date();
  const [desde, setDesde] = useState(fechaISO(new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() - 60)));
  const [hasta, setHasta] = useState(fechaISO(hoy));
  const [horaDesde, setHoraDesde] = useState(0);
  const [horaHasta, setHoraHasta] = useState(24);
  const [dias, setDias] = useState(new Set([1, 2, 3, 4, 5, 6, 7]));
  const [severidades, setSeveridades] = useState(new Set(['leve', 'moderada', 'grave']));
  const [infraccionId, setInfraccionId] = useState('');
  const [catalogo, setCatalogo] = useState([]);
  const [verCalor, setVerCalor] = useState(true);
  const [verEventos, setVerEventos] = useState(true);
  const [franja, setFranja] = useState({ desde: 0, hasta: 24 }); // la franja de la última consulta
  const [heatListo, setHeatListo] = useState(false);
  const [fondoFallo, setFondoFallo] = useState(false);

  const contenedorRef = useRef(null);
  const mapaRef = useRef(null);
  const calorRef = useRef(null);
  const marcadoresRef = useRef(null);

  const puedeConsultar = dias.size > 0 && severidades.size > 0 && desde && hasta && desde <= hasta;

  const consultar = useCallback(() => {
    const params = { desde, hasta, hora_desde: horaDesde, hora_hasta: horaHasta };
    if (dias.size < 7) params.dias = [...dias].sort().join(',');
    if (severidades.size < 3) params.severidad = [...severidades].join(',');
    if (infraccionId) params.infraccion_id = infraccionId;
    setFranja({ desde: Number(horaDesde), hasta: Number(horaHasta) });
    fetchMapa(params);
  }, [desde, hasta, horaDesde, horaHasta, dias, severidades, infraccionId, fetchMapa]);

  // Primera carga con los filtros por defecto y catálogo para el filtro por infracción.
  useEffect(() => {
    consultar();
    getInfracciones().then((response) => {
      if (response?.status === 'Success') setCatalogo(response.data);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Mapa: se crea una sola vez. leaflet.heat necesita L global, por eso se importa después.
  useEffect(() => {
    if (mapaRef.current || !contenedorRef.current) return undefined;
    const mapa = L.map(contenedorRef.current, {
      minZoom: 7,
      maxBounds: L.latLngBounds(BIOBIO_LIMITES).pad(0.5),
      maxBoundsViscosity: 0.7,
    });
    mapa.fitBounds(BIOBIO_LIMITES);
    const fondo = L.tileLayer(FONDO.url, FONDO.opciones).addTo(mapa);
    fondo.once('tileerror', () => setFondoFallo(true));
    mapa.createPane('eventos').style.zIndex = 450; // los marcadores quedan sobre el calor
    marcadoresRef.current = L.layerGroup().addTo(mapa);
    mapaRef.current = mapa;
    window.L = window.L || L;
    let activo = true;
    import('leaflet.heat').then(() => { if (activo) setHeatListo(true); });
    const t = setTimeout(() => mapa.invalidateSize(), 300); // por la animación de entrada de la página
    return () => {
      activo = false;
      clearTimeout(t);
      mapa.remove();
      mapaRef.current = null;
      calorRef.current = null;
      marcadoresRef.current = null;
    };
  }, []);

  // Capa de calor: una intensidad por celda, relativa a la celda con más eventos.
  useEffect(() => {
    const mapa = mapaRef.current;
    if (!mapa || !heatListo) return;
    if (calorRef.current) {
      mapa.removeLayer(calorRef.current);
      calorRef.current = null;
    }
    if (!verCalor || !datos?.celdas?.length) return;
    const maximo = Math.max(...datos.celdas.map((c) => c.total));
    calorRef.current = L.heatLayer(
      datos.celdas.map((c) => [c.lat, c.lon, c.total / maximo]),
      { radius: 26, blur: 20, maxZoom: 11, max: 1, minOpacity: 0.35, gradient: GRADIENTE },
    ).addTo(mapa);
  }, [datos, verCalor, heatListo]);

  // Marcadores: uno por reporte aprobado; el clic abre el reporte.
  useEffect(() => {
    const grupo = marcadoresRef.current;
    if (!grupo) return;
    grupo.clearLayers();
    if (!verEventos || !datos?.eventos) return;
    datos.eventos.forEach((ev) => {
      const sev = SEVERIDADES.find((s) => s.valor === ev.severidad);
      const marcador = L.circleMarker([ev.lat, ev.lon], {
        pane: 'eventos', radius: 7, weight: 2, color: '#0B1B3A',
        fillColor: sev?.color || '#5A6478', fillOpacity: 0.9,
      });
      marcador.bindTooltip(crearTooltip(ev), { direction: 'top' });
      marcador.on('click', () => navigate(`/reportes/${ev.id}`));
      marcador.addTo(grupo);
    });
  }, [datos, verEventos, navigate]);

  const alternar = (conjunto, setConjunto, valor) => {
    const nuevo = new Set(conjunto);
    if (nuevo.has(valor)) nuevo.delete(valor); else nuevo.add(valor);
    setConjunto(nuevo);
  };

  const porSeveridad = useMemo(() => {
    const cuenta = { leve: 0, moderada: 0, grave: 0, sin: 0 };
    (datos?.eventos || []).forEach((ev) => { cuenta[ev.severidad in cuenta ? ev.severidad : 'sin'] += 1; });
    return cuenta;
  }, [datos]);

  const maxPorHora = datos ? Math.max(...datos.por_hora) : 0;
  const horaPunta = useMemo(() => {
    if (!datos || maxPorHora === 0) return null;
    const h = datos.por_hora.indexOf(maxPorHora);
    return `${textoHora(h)} – ${textoHora((h + 1) % 24)}`;
  }, [datos, maxPorHora]);

  const irA = (lat, lon) => mapaRef.current?.flyTo([lat, lon], 14);
  const verRegion = () => mapaRef.current?.fitBounds(BIOBIO_LIMITES);

  return (
    <section className="mapa" aria-labelledby="mapa-titulo">
      <div>
        <h2 id="mapa-titulo" className="mapa__title">Zonas de riesgo vial</h2>
        <p className="mapa__sub">
          Reportes aprobados con ubicación en la región del Biobío. El calor agrupa los eventos en celdas de unos
          100 m; cada marcador abre su reporte.
        </p>
      </div>

      <div className="panel">
        <div className="panel__core mapa-filtros">
          <div className="mapa-campo">
            <label className="field-label" htmlFor="mapa-desde">Desde</label>
            <input id="mapa-desde" type="date" value={desde} max={hasta} onChange={(e) => setDesde(e.target.value)} />
          </div>
          <div className="mapa-campo">
            <label className="field-label" htmlFor="mapa-hasta">Hasta</label>
            <input id="mapa-hasta" type="date" value={hasta} min={desde} onChange={(e) => setHasta(e.target.value)} />
          </div>
          <div className="mapa-campo">
            <span className="field-label" id="mapa-franja">Franja horaria</span>
            <div className="mapa-franja">
              <select aria-labelledby="mapa-franja" value={horaDesde} onChange={(e) => setHoraDesde(Number(e.target.value))}>
                {Array.from({ length: 24 }, (_, h) => <option key={h} value={h}>{textoHora(h)}</option>)}
              </select>
              <span className="mapa-a">a</span>
              <select aria-label="Hasta la hora" value={horaHasta} onChange={(e) => setHoraHasta(Number(e.target.value))}>
                {Array.from({ length: 24 }, (_, i) => i + 1).map((h) => <option key={h} value={h}>{h === 24 ? '24:00' : textoHora(h)}</option>)}
              </select>
            </div>
          </div>
          <div className="mapa-campo">
            <span className="field-label" id="mapa-dias">Días de la semana</span>
            <div className="mapa-grupo" role="group" aria-labelledby="mapa-dias">
              {DIAS.map((d) => (
                <button key={d.n} type="button" aria-label={d.nombre} aria-pressed={dias.has(d.n)}
                  className={`btn btn-icon ${dias.has(d.n) ? 'is-active' : ''}`}
                  onClick={() => alternar(dias, setDias, d.n)}>{d.letra}</button>
              ))}
            </div>
          </div>
          <div className="mapa-campo">
            <span className="field-label" id="mapa-sev">Severidad</span>
            <div className="mapa-grupo" role="group" aria-labelledby="mapa-sev">
              {SEVERIDADES.map((s) => (
                <button key={s.valor} type="button" aria-pressed={severidades.has(s.valor)}
                  className={`btn ${severidades.has(s.valor) ? 'is-active' : ''}`}
                  onClick={() => alternar(severidades, setSeveridades, s.valor)}>
                  <span className="mapa-punto" style={{ background: s.color }} />{s.etiqueta}
                </button>
              ))}
            </div>
          </div>
          <div className="mapa-campo mapa-campo--ancho">
            <label className="field-label" htmlFor="mapa-infraccion">Infracción</label>
            <select id="mapa-infraccion" value={infraccionId} onChange={(e) => setInfraccionId(e.target.value)}>
              <option value="">Todas</option>
              {catalogo.map((i) => <option key={i.id} value={i.id}>{i.articulo} - {i.descripcion}</option>)}
            </select>
          </div>
          <button type="button" className="btn btn-primary mapa-aplicar" disabled={!puedeConsultar || loading} onClick={consultar}>
            {loading ? 'Cargando…' : 'Aplicar filtros'}
          </button>
          {!puedeConsultar && (
            <p className="mapa-error" role="alert">Elige al menos un día y una severidad, y una fecha inicial que no sea posterior a la final.</p>
          )}
        </div>
      </div>

      <div className="mapa-grid">
        <div className="mapa-col">
          <div className="panel">
            <div className="panel__core">
              <div className="mapa-barra">
                <button type="button" aria-pressed={verCalor} className={`btn ${verCalor ? 'is-active' : ''}`} onClick={() => setVerCalor(!verCalor)}>Calor</button>
                <button type="button" aria-pressed={verEventos} className={`btn ${verEventos ? 'is-active' : ''}`} onClick={() => setVerEventos(!verEventos)}>Eventos</button>
                <button type="button" className="btn mapa-espacio" onClick={verRegion}><LocateFixed size={16} />Ver toda la región</button>
              </div>
              <div className="mapa-lienzo-wrap">
                <div ref={contenedorRef} className="mapa-lienzo" role="region" aria-label="Mapa de riesgo vial de la región del Biobío" />
                {loading && <div className="mapa-estado"><span>Cargando datos…</span></div>}
                {!loading && !error && datos?.total === 0 && <div className="mapa-estado"><span>Sin eventos para estos filtros</span></div>}
              </div>
              {error && <p className="mapa-error" role="alert">{error}</p>}
              {fondoFallo && (
                <p className="mapa-nota" role="status">No se pudo cargar el fondo del mapa (el servicio lo bloqueó o no hay internet). El calor y los marcadores siguen funcionando.</p>
              )}
              <div className="mapa-leyenda">
                <span>Menos eventos</span><span className="mapa-gradiente" /><span>Más eventos</span>
                {SEVERIDADES.map((s) => (
                  <span key={s.valor}><span className="mapa-punto" style={{ background: s.color }} />{s.etiqueta}</span>
                ))}
              </div>
            </div>
          </div>

          <div className="panel">
            <div className="panel__core">
              <h3 className="mapa-sub mapa-sub--primero">Reportes por hora del día</h3>
              <p className="mapa-nota">
                {franja.desde === franja.hasta ? 'Todo el día' : `Franja filtrada ${textoHora(franja.desde)} – ${franja.hasta === 24 ? '24:00' : textoHora(franja.hasta)}, resaltada`}
                {horaPunta ? ` · Hora punta ${horaPunta}` : ''}
              </p>
              <div className="mapa-hist" role="img"
                aria-label={horaPunta ? `Reportes por hora del día. La hora con más reportes es ${horaPunta}.` : 'Reportes por hora del día. Sin datos.'}>
                {(datos?.por_hora || Array(24).fill(0)).map((v, h) => (
                  <i key={h} className={enFranja(h, franja.desde, franja.hasta) ? 'on' : ''}
                    style={{ height: `${maxPorHora ? Math.max(3, Math.round((v / maxPorHora) * 100)) : 2}%` }} title={`${textoHora(h)}: ${v}`} />
                ))}
              </div>
              <div className="mapa-ejes"><span>00</span><span>06</span><span>12</span><span>18</span><span>23</span></div>
            </div>
          </div>
        </div>

        <aside className="panel" aria-label="Resumen del mapa">
          <div className="panel__core">
            <div className="mapa-kpis">
              <div className="mapa-kpi"><strong>{datos?.total ?? '—'}</strong><span>reportes en el mapa</span></div>
              <div className="mapa-kpi"><strong>{horaPunta ? horaPunta.slice(0, 5) : '—'}</strong><span>hora punta</span></div>
            </div>
            {datos?.incluye_prueba > 0 && (
              <p className="mapa-aviso"><span className="badge badge-amber">Incluye {datos.incluye_prueba} {datos.incluye_prueba === 1 ? 'reporte' : 'reportes'} de prueba</span></p>
            )}
            {datos?.sin_ubicacion > 0 && (
              <p className="mapa-nota">{datos.sin_ubicacion} {datos.sin_ubicacion === 1 ? 'reporte aprobado no tiene' : 'reportes aprobados no tienen'} ubicación y no aparece{datos.sin_ubicacion === 1 ? '' : 'n'} en el mapa.</p>
            )}
            {datos?.eventos_recortados && (
              <p className="mapa-nota">Se muestran solo los primeros {datos.eventos.length} marcadores; el calor incluye todos los eventos.</p>
            )}

            <h3 className="mapa-sub">Severidad</h3>
            {SEVERIDADES.map((s) => {
              const n = porSeveridad[s.valor];
              const total = datos?.eventos?.length || 0;
              return (
                <div className="mapa-sev" key={s.valor}>
                  <span>{s.etiqueta}</span>
                  <span className="mapa-barra-sev"><i style={{ width: `${total ? Math.round((n / total) * 100) : 0}%`, background: s.color }} /></span>
                  <b>{n}</b>
                </div>
              );
            })}
            {porSeveridad.sin > 0 && <p className="mapa-nota">{porSeveridad.sin} sin severidad definida.</p>}

            <h3 className="mapa-sub">Zonas con más eventos</h3>
            {datos?.celdas?.length ? datos.celdas.slice(0, 5).map((c) => (
              <div className="mapa-fila" key={`${c.lat},${c.lon}`}>
                <span>{c.lat.toFixed(3)}, {c.lon.toFixed(3)} · <b>{c.total}</b> {c.total === 1 ? 'evento' : 'eventos'}</span>
                <button type="button" className="btn btn-icon" aria-label={`Centrar el mapa en ${c.lat.toFixed(3)}, ${c.lon.toFixed(3)}`} onClick={() => irA(c.lat, c.lon)}>
                  <Crosshair size={16} />
                </button>
              </div>
            )) : <p className="mapa-nota">Sin zonas para mostrar.</p>}
          </div>
        </aside>
      </div>
    </section>
  );
};

export default MapaRiesgo;