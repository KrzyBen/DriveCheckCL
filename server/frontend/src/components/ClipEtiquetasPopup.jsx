import { useEffect, useMemo, useState } from 'react';
import { Plus, X } from 'lucide-react';
import '@styles/popup.css';
import '@styles/dataset.css';

const SEVERIDADES = [['', 'Sin severidad'], ['leve', 'Leve'], ['moderada', 'Moderada'], ['grave', 'Grave']];

const ordenados = (ids) => [...ids].sort((a, b) => a - b);
const iguales = (a, b) => a.length === b.length && a.every((v, i) => v === b[i]);

// Edición de las etiquetas de UN clip. Solo catálogo y severidad: sin texto libre.
export default function ClipEtiquetasPopup({ clip, catalogo, guardando, onClose, onSave, onReset }) {
  const [severidad, setSeveridad] = useState(clip.severidad || '');
  const [seleccion, setSeleccion] = useState(() => clip.infracciones.map((i) => i.id));
  const [excluido, setExcluido] = useState(clip.excluido);
  const [porAgregar, setPorAgregar] = useState('');

  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  const porId = useMemo(
    () => new Map([...catalogo, ...clip.infracciones].map((i) => [i.id, i])),
    [catalogo, clip.infracciones],
  );
  const disponibles = catalogo.filter((i) => !seleccion.includes(i.id));

  // Solo se envía lo que cambió: así editar la exclusión no convierte en propias las etiquetas del reporte.
  const cambios = {};
  if (!iguales(ordenados(seleccion), ordenados(clip.infracciones.map((i) => i.id)))) cambios.infracciones = seleccion;
  if ((severidad || null) !== (clip.severidad || null)) cambios.severidad = severidad || null;
  if (excluido !== clip.excluido) cambios.excluido = excluido;
  const hayCambios = Object.keys(cambios).length > 0;

  const agregar = () => {
    if (!porAgregar) return;
    setSeleccion((prev) => [...prev, Number(porAgregar)]);
    setPorAgregar('');
  };
  const quitar = (id) => setSeleccion((prev) => prev.filter((x) => x !== id));
  const propias = clip.etiquetas_origen === 'dataset';

  return (
    <div className="bg">
      <div className="popup popup--wide" role="dialog" aria-modal="true" aria-labelledby="clip-popup-titulo">
        <button className="close" onClick={onClose} aria-label="Cerrar">✕</button>
        <h2 className="ds-popup__title" id="clip-popup-titulo">Etiquetas del clip</h2>
        <p className="ds-popup__sub">{clip.clip_id} · reporte #{clip.evento_id}</p>
        <p className="ds-popup__note">
          {propias
            ? 'Este clip tiene etiquetas propias: los cambios del reporte ya no lo afectan.'
            : 'Este clip usa las etiquetas del reporte. Si cambias infracciones o severidad, pasan a ser propias de este clip.'}
        </p>

        <div className="ds-field">
          <label htmlFor="clip-severidad">Severidad</label>
          <select id="clip-severidad" value={severidad} onChange={(e) => setSeveridad(e.target.value)}>
            {SEVERIDADES.map(([valor, texto]) => <option key={valor} value={valor}>{texto}</option>)}
          </select>
        </div>

        <div className="ds-field">
          <span className="ds-field__label">Infracciones (Ley 18.290)</span>
          <div className="infra-add">
            <select aria-label="Catálogo de infracciones" value={porAgregar} onChange={(e) => setPorAgregar(e.target.value)}>
              <option value="">Seleccionar del catálogo...</option>
              {disponibles.map((i) => <option key={i.id} value={i.id}>{i.articulo} - {i.descripcion}</option>)}
            </select>
            <button className="btn" onClick={agregar} disabled={!porAgregar}><Plus size={15} /> Agregar</button>
          </div>
          <div className="infra-list">
            {seleccion.length === 0 && <span className="cell-muted">Sin infracciones</span>}
            {seleccion.map((id) => {
              const i = porId.get(id);
              return (
                <div key={id} className="infra-item">
                  <span>{i ? `${i.articulo} - ${i.descripcion}` : `Infracción #${id}`}</span>
                  <button className="btn btn-icon" aria-label="Quitar infracción" onClick={() => quitar(id)}><X size={14} /></button>
                </div>
              );
            })}
          </div>
        </div>

        <label className="ds-check">
          <input type="checkbox" checked={excluido} onChange={(e) => setExcluido(e.target.checked)} />
          Excluir este clip del dataset
        </label>

        <div className="ds-popup__actions">
          {propias && (
            <button className="btn ds-reset" onClick={onReset} disabled={guardando}>Restablecer a las del reporte</button>
          )}
          <button className="btn" onClick={onClose} disabled={guardando}>Cancelar</button>
          <button className="btn btn-primary" onClick={() => onSave(cambios)} disabled={!hayCambios || guardando}>
            {guardando ? 'Guardando...' : 'Guardar'}
          </button>
        </div>
      </div>
    </div>
  );
}
