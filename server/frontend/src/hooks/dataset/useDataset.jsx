import { useState, useEffect, useCallback } from 'react';
import Swal from 'sweetalert2';
import {
  getResumenDataset, getClipsDataset, actualizarClipDataset, restablecerClipDataset,
  exportarDataset, descargarDataset,
} from '@services/dataset.service.js';
import { getInfracciones } from '@services/infraction.service.js';
import { showSuccessAlert, showErrorAlert } from '@helpers/sweetAlert.js';

const POR_PAGINA = 100; // máximo que acepta el servidor

const mensajeDe = (r, porDefecto) => r?.details || r?.message || porDefecto;

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
  const [generando, setGenerando] = useState(false);
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

  const generar = async () => {
    const { isConfirmed } = await confirmar(
      'Generar dataset',
      'Se vuelve a crear el dataset con los clips y las etiquetas actuales. Quien haya revocado su consentimiento sale del dataset.',
      'Generar',
    );
    if (!isConfirmed) return;
    setGenerando(true);
    const r = await exportarDataset();
    setGenerando(false);
    if (r?.status !== 'Success') {
      showErrorAlert('No se pudo generar el dataset', mensajeDe(r, 'Inténtalo de nuevo'));
      return;
    }
    const d = r.data;
    const partes = [`${d.clips} clips`, `${d.eventos} eventos`, `${d.conductores} conductores`];
    if (d.clips_excluidos) partes.push(`${d.clips_excluidos} excluidos a mano`);
    if (d.sin_anonimizar) partes.push(`${d.sin_anonimizar} sin video (falta anonimizar)`);
    showSuccessAlert('Dataset generado', partes.join(' · '));
    await cargar();
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
    resumen, clips, catalogo, loading, error, generando, descargando, guardando,
    cargar, generar, descargar, editar, restablecer,
  };
};

export default useDataset;
