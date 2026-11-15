import { useState, useEffect, useCallback, useRef } from 'react';
import Swal from 'sweetalert2';
import {
  getResumenDataset, getClipsDataset, actualizarClipDataset, restablecerClipDataset,
  exportarDataset, getEstadoExportacion, descargarDataset,
} from '@services/dataset.service.js';
import { getInfracciones } from '@services/infraction.service.js';
import { showSuccessAlert, showErrorAlert } from '@helpers/sweetAlert.js';

const POR_PAGINA = 100; // máximo que acepta el servidor
const INTERVALO_ESTADO_MS = 2000; // cada cuánto se consulta el estado mientras se exporta

const mensajeDe = (r, porDefecto) => r?.details || r?.message || porDefecto;

// Motivos con que el servidor aborta una exportación (dataset_service / anonimizador_client).
// En todos los casos el dataset anterior queda sin cambios.
const MOTIVOS_ERROR = {
  en_curso: 'Ya había otra exportación en curso (iniciada desde la consola del servidor).',
  sin_conexion: 'No se pudo conectar con el servicio de anonimización (backend-ia).',
  clave_invalida: 'El servicio de anonimización rechazó la clave interna (INTERNAL_API_KEY no coincide).',
  sin_clave: 'Falta configurar INTERNAL_API_KEY en el servidor.',
  servicio_no_disponible: 'El servicio de anonimización no tiene sus modelos disponibles.',
  todos_fallaron: 'No se pudo anonimizar ninguno de los clips.',
  fallos_consecutivos: 'Fallaron 3 clips seguidos: el servicio de anonimización puede estar saturado.',
  error_interno: 'Ocurrió un error inesperado en el servidor.',
};

// Motivos por los que un clip quedó fuera del dataset (anonimizador_client).
const MOTIVOS_OMISION = {
  tiempo_agotado: 'la anonimización tardó más de lo permitido',
  error_anonimizacion: 'el servicio de anonimización falló al procesarlo',
  ruta_invalida: 'la ruta del video no es válida',
  video_no_encontrado: 'el servicio de anonimización no encontró el video',
  solicitud_invalida: 'solicitud inválida',
  respuesta_invalida: 'respuesta inesperada del servicio de anonimización',
  respuesta_incompleta: 'el video anonimizado llegó incompleto',
  respuesta_vacia: 'el servicio de anonimización devolvió un video vacío',
  error_comunicacion: 'se cortó la comunicación con el servicio de anonimización',
};

export const textoMotivoError = (error) => (
  `${MOTIVOS_ERROR[error?.motivo] || error?.mensaje || 'La exportación no se completó.'} `
  + 'El dataset anterior se conservó sin cambios.'
);

export const textoMotivoOmision = (motivo) => MOTIVOS_OMISION[motivo] || `error ${motivo}`;

const confirmar = (title, text, confirmButtonText) => Swal.fire({
  title, text, icon: 'question', showCancelButton: true,
  confirmButtonColor: '#0039A6', cancelButtonColor: '#D52B1E',
  confirmButtonText, cancelButtonText: 'Cancelar',
});

const useDataset = () => {
  const [resumen, setResumen] = useState(null);
  const [clips, setClips] = useState([]);
  const [catalogo, setCatalogo] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  // Estado de la exportación en el servidor (GET /exportar/estado); null hasta la primera consulta
  const [exportacion, setExportacion] = useState(null);
  const [iniciando, setIniciando] = useState(false); // entre el clic y la respuesta del POST
  // true si en esta visita se vio la exportación "en_curso": solo entonces se avisa al terminar
  // (al abrir la página no se vuelve a mostrar el resultado de una exportación vieja)
  const siguiendo = useRef(false);
  const [descargando, setDescargando] = useState(false);
  const [guardando, setGuardando] = useState(false);

  const cargar = useCallback(async () => {
    setLoading(true);
    setError(null);
    const r = await getResumenDataset();
    if (r?.status !== 'Success') {
      setError(mensajeDe(r, 'No se pudo cargar el dataset'));
      setLoading(false);
      return;
    }
    setResumen(r.data);

    // La tabla pagina en el navegador: se traen todas las páginas del servidor.
    let todos = [];
    for (let pagina = 1; ; pagina += 1) {
      const c = await getClipsDataset(pagina, POR_PAGINA);
      if (c?.status !== 'Success') {
        setError(mensajeDe(c, 'No se pudo cargar la tabla de clips'));
        break;
      }
      todos = todos.concat(c.data.clips);
      if (c.data.clips.length === 0 || todos.length >= c.data.total) break;
    }
    setClips(todos);
    setLoading(false);
  }, []);

  useEffect(() => { cargar(); }, [cargar]);

  useEffect(() => {
    (async () => {
      const r = await getInfracciones();
      if (r?.status === 'Success') {
        // Las "Manual" son texto libre del catálogo antiguo: no se usan en el dataset.
        setCatalogo(r.data.filter((i) => i.articulo !== 'Manual'));
      }
    })();
  }, []);

  const enCurso = exportacion?.estado === 'en_curso';
  const generando = iniciando || enCurso;

  // Al montar: si quedó una exportación corriendo (p. ej. se recargó la página), se retoma
  useEffect(() => {
    let activo = true;
    (async () => {
      const r = await getEstadoExportacion();
      if (!activo || r?.status !== 'Success') return;
      if (r.data.estado === 'en_curso') siguiendo.current = true;
      setExportacion(r.data);
    })();
    return () => { activo = false; };
  }, []);

  // Polling cada ~2 s solo mientras la exportación está "en_curso". Se detiene solo cuando el
  // estado pasa a "terminado" o "error" (enCurso cambia) o al salir de la página.
  // Una consulta fallida (p. ej. un corte de red) no detiene el polling: se reintenta.
  useEffect(() => {
    if (!enCurso) return undefined;
    let activo = true;
    let temporizador;
    const consultar = async () => {
      const r = await getEstadoExportacion();
      if (!activo) return;
      if (r?.status === 'Success') setExportacion(r.data);
      temporizador = setTimeout(consultar, INTERVALO_ESTADO_MS);
    };
    temporizador = setTimeout(consultar, INTERVALO_ESTADO_MS);
    return () => { activo = false; clearTimeout(temporizador); };
  }, [enCurso]);

  // Aviso al terminar una exportación que se siguió en esta visita
  useEffect(() => {
    if (!exportacion || exportacion.estado === 'en_curso' || !siguiendo.current) return;
    siguiendo.current = false;
    if (exportacion.estado === 'terminado') {
      const d = exportacion.resumen || {};
      const partes = [`${d.clips} clips`, `${d.eventos} eventos`, `${d.conductores} conductores`];
      if (d.omitidos?.total) partes.push(`${d.omitidos.total} omitidos (no se pudieron anonimizar)`);
      if (d.clips_excluidos) partes.push(`${d.clips_excluidos} excluidos a mano`);
      showSuccessAlert('Dataset generado', partes.join(' · '));
      cargar();
    } else if (exportacion.estado === 'error') {
      showErrorAlert('No se pudo generar el dataset', textoMotivoError(exportacion.error));
    } else if (exportacion.estado === 'inactivo') {
      // El servidor se reinició a mitad de camino: el estado vive en memoria y se perdió
      showErrorAlert(
        'La exportación se interrumpió',
        'El servidor se reinició antes de terminar. El dataset anterior se conservó sin cambios; vuelve a generarlo.',
      );
    }
  }, [exportacion, cargar]);

  const generar = async () => {
    if (generando) return;
    const { isConfirmed } = await confirmar(
      'Generar dataset',
      'Se vuelve a crear el dataset con los clips y las etiquetas actuales, y se anonimiza cada video. '
      + 'Puede tardar varios minutos; puedes seguir usando el panel mientras tanto. '
      + 'Quien haya revocado su consentimiento sale del dataset.',
      'Generar',
    );
    if (!isConfirmed) return;
    setIniciando(true);
    const r = await exportarDataset();
    setIniciando(false);
    if (r?.status === 'Success') {
      siguiendo.current = true;
      setExportacion(r.data);
      return;
    }
    if (r?.codigo === 409) {
      showErrorAlert('Ya hay una exportación en curso', mensajeDe(r, 'Espera a que termine e inténtalo de nuevo'));
      // Si corre en este servidor, se muestra su progreso (y se avisa cuando termine)
      const e = await getEstadoExportacion();
      if (e?.status === 'Success') {
        if (e.data.estado === 'en_curso') siguiendo.current = true;
        setExportacion(e.data);
      }
      return;
    }
    showErrorAlert('No se pudo generar el dataset', mensajeDe(r, 'Inténtalo de nuevo'));
  };

  const descargar = async () => {
    setDescargando(true);
    const r = await descargarDataset();
    setDescargando(false);
    if (r?.status !== 'Success') {
      showErrorAlert('No se pudo descargar', mensajeDe(r, 'Inténtalo de nuevo'));
      return;
    }
    const url = URL.createObjectURL(r.blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = `dataset_drivecheckcl_${new Date().toISOString().slice(0, 10)}.zip`;
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    URL.revokeObjectURL(url);
  };

  // Devuelve true si el servidor confirmó el cambio.
  const editar = async (clip, cambios) => {
    setGuardando(true);
    const r = await actualizarClipDataset(clip.video_id, cambios);
    setGuardando(false);
    if (r?.status !== 'Success') {
      showErrorAlert('No se pudo guardar', mensajeDe(r, 'Inténtalo de nuevo'));
      return false;
    }
    await cargar();
    return true;
  };

  const restablecer = async (clip) => {
    const { isConfirmed } = await confirmar(
      'Restablecer etiquetas',
      'El clip volverá a usar las infracciones y la severidad del reporte. Se pierden las etiquetas propias de este clip.',
      'Restablecer',
    );
    if (!isConfirmed) return false;
    setGuardando(true);
    const r = await restablecerClipDataset(clip.video_id);
    setGuardando(false);
    if (r?.status !== 'Success') {
      showErrorAlert('No se pudo restablecer', mensajeDe(r, 'Inténtalo de nuevo'));
      return false;
    }
    await cargar();
    return true;
  };

  return {
    resumen, clips, catalogo, loading, error, generando, exportacion, descargando, guardando,
    cargar, generar, descargar, editar, restablecer,
  };
};

export default useDataset;
