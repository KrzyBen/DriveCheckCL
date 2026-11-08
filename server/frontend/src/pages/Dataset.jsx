import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Download, RefreshCw } from 'lucide-react';
import Table from '@components/Table';
import ClipEtiquetasPopup from '@components/ClipEtiquetasPopup';
import useDataset from '@hooks/dataset/useDataset.jsx';
import { iconEye, iconPencil } from '@helpers/icons.js';
import '@styles/dataset.css';

// Los formatters de Tabulator devuelven HTML: todo texto que venga de la base se escapa.
const esc = (t) => String(t ?? '').replace(/[&<>"']/g, (c) => (
  { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
));

const severidadBadge = {
  leve: ['badge-green', 'Leve'],
  moderada: ['badge-amber', 'Moderada'],
  grave: ['badge-red', 'Grave'],
};

function estadoDe(c) {
  if (c.excluido) return ['badge-neutral', 'Excluido'];
  if (!c.archivo_ok) return ['badge-red', 'Sin video'];
  if (c.cambios_sin_exportar) return ['badge-amber', 'Cambios sin exportar'];
  if (c.en_dataset) return ['badge-green', 'En el dataset'];
  return ['badge-blue', 'Por exportar'];
}

const fechaHora = (iso) => (iso ? new Date(iso).toLocaleString('es-CL') : '—');

function Tarjeta({ titulo, valor, sub }) {
  return (
    <div className="panel">
      <div className="panel__core">
        <p className="ds-card__label">{titulo}</p>
        <p className="ds-card__value">{valor}</p>
        <p className="ds-card__sub">{sub}</p>
      </div>
    </div>
  );
}

const Dataset = () => {
  const navigate = useNavigate();
  const {
    resumen, clips, catalogo, loading, error, generando, descargando, guardando,
    cargar, generar, descargar, editar, restablecer,
  } = useDataset();
  const [clipEditando, setClipEditando] = useState(null);

  const columns = useMemo(() => [
    { title: 'Clip', field: 'clip_id', widthGrow: 2, minWidth: 150, responsive: 0, cssClass: 'cell-id' },
    {
      title: 'Estado', field: 'estado', widthGrow: 1.6, minWidth: 170, responsive: 1, headerSort: false,
      formatter: (cell) => { const [clase, texto] = estadoDe(cell.getData()); return `<span class="badge ${clase}">${texto}</span>`; },
    },
    {
      title: 'Severidad', field: 'severidad', widthGrow: 1, minWidth: 120, responsive: 2,
      formatter: (cell) => {
        const s = severidadBadge[cell.getValue()];
        return s ? `<span class="badge ${s[0]}">${s[1]}</span>` : '<span class="cell-muted">—</span>';
      },
    },
    {
      title: 'Infracciones', field: 'infracciones', widthGrow: 3, minWidth: 220, responsive: 3, headerSort: false,
      formatter: (cell) => {
        const lista = cell.getValue() || [];
        if (lista.length === 0) return '<span class="cell-muted">Sin infracciones</span>';
        const completo = lista.map((i) => `${i.articulo} - ${i.descripcion}`).join('\n');
        const resto = lista.length > 2 ? ` <span class="cell-muted">+${lista.length - 2}</span>` : '';
        return `<span title="${esc(completo)}">${lista.slice(0, 2).map((i) => esc(i.articulo)).join(' · ')}${resto}</span>`;
      },
    },
    {
      title: 'Etiquetas', field: 'etiquetas_origen', widthGrow: 1, minWidth: 120, responsive: 5,
      formatter: (cell) => (cell.getValue() === 'dataset'
        ? '<span class="badge badge-blue">Propias</span>'
        : '<span class="badge badge-neutral">Del reporte</span>'),
    },
    {
      title: 'Video', field: 'anonimizado', widthGrow: 1.2, minWidth: 140, responsive: 4,
      formatter: (cell) => (cell.getValue()
        ? '<span class="badge badge-green">Anonimizado</span>'
        : '<span class="badge badge-neutral">Sin anonimizar</span>'),
    },
    { title: 'Grabado', field: 'grabado_en', widthGrow: 1.5, minWidth: 160, responsive: 6, formatter: (cell) => fechaHora(cell.getValue()) },
    {
      title: 'Acciones', widthGrow: 1, minWidth: 120, responsive: 0, hozAlign: 'right', headerHozAlign: 'right', headerSort: false,
      formatter: () => (
        '<button class="table-action-btn" data-action="ver" title="Ver reporte" aria-label="Ver reporte">' + iconEye + '</button>'
        + '<button class="table-action-btn" data-action="editar" title="Editar etiquetas" aria-label="Editar etiquetas">' + iconPencil + '</button>'
      ),
      cellClick: (e, cell) => {
        const btn = e.target.closest('button');
        if (!btn || btn.disabled) return;
        const clip = cell.getData();
        if (btn.dataset.action === 'ver') navigate(`/reportes/${clip.evento_id}`);
        if (btn.dataset.action === 'editar') setClipEditando(clip);
      },
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
  ], []);

  const dataset = resumen?.dataset;
  const guardar = async (cambios) => {
    if (await editar(clipEditando, cambios)) setClipEditando(null);
  };
  const reset = async () => {
    if (await restablecer(clipEditando)) setClipEditando(null);
  };

  return (
    <div className="page-enter">
      <div className="page-head">
        <div>
          <h1 className="page-title">Dataset</h1>
          <p className="page-sub">
            {loading && !resumen ? 'Cargando...' : `${clips.length} clips de conductores que autorizaron su uso en el estudio`}
          </p>
        </div>
        <div className="page-actions">
          <button className="btn" onClick={descargar} disabled={!dataset?.generado || descargando}>
            <Download size={16} /> {descargando ? 'Descargando...' : 'Descargar'}
          </button>
          <button className="btn btn-primary" onClick={generar} disabled={generando || !resumen}>
            <RefreshCw size={16} /> {generando ? 'Generando...' : 'Generar dataset'}
          </button>
        </div>
      </div>

      {error && (
        <div className="ds-notice ds-notice--error" role="alert">
          <span>{error}</span>
          <button className="btn" onClick={cargar}>Reintentar</button>
        </div>
      )}
      {dataset?.desactualizado && (
        <div className="ds-notice ds-notice--warn">
          {dataset.generado
            ? 'Hay cambios sin exportar (clips nuevos, etiquetas editadas o consentimientos que cambiaron). Genera el dataset para aplicarlos.'
            : 'Aún no se ha generado el dataset.'}
        </div>
      )}
      {dataset?.clips > 0 && dataset.sin_anonimizar > 0 && (
        <div className="ds-notice ds-notice--info">
          El dataset incluye solo metadatos y etiquetas: los videos se agregan cuando exista el paso de anonimización.
        </div>
      )}

      {resumen && (
        <>
          <div className="ds-cards">
            <Tarjeta titulo="Clips listos" valor={resumen.clips.listos} sub={`de ${resumen.clips.elegibles} elegibles`} />
            <Tarjeta
              titulo="En el dataset" valor={dataset.clips}
              sub={dataset.generado ? `Generado el ${fechaHora(dataset.generado)}` : 'Aún sin generar'}
            />
            <Tarjeta titulo="Sin anonimizar" valor={dataset.sin_anonimizar} sub="No se exporta su video" />
            <Tarjeta titulo="Excluidos a mano" valor={resumen.clips.excluidos} sub="No entran al dataset" />
          </div>

          <div className="panel ds-section">
            <div className="panel__core">
              <p className="panel__title">Reportes y por qué entran o no</p>
              <div className="ds-list">
                <div className="ds-list__row"><span>Reportes en total</span><strong>{resumen.reportes.total}</strong></div>
                <div className="ds-list__row"><span>Aprobados con consentimiento vigente</span><strong>{resumen.reportes.elegibles}</strong></div>
                <div className="ds-list__row"><span>Aprobados sin consentimiento vigente</span><strong>{resumen.reportes.sin_consentimiento}</strong></div>
                <div className="ds-list__row"><span>Sin aprobar</span><strong>{resumen.reportes.no_aprobados}</strong></div>
                <div className="ds-list__row"><span>De prueba (siempre excluidos)</span><strong>{resumen.reportes.prueba}</strong></div>
              </div>
            </div>
          </div>
        </>
      )}

      <Table data={clips} columns={columns} initialSortName="clip_id" />

      {clipEditando && (
        <ClipEtiquetasPopup
          clip={clipEditando}
          catalogo={catalogo}
          guardando={guardando}
          onClose={() => setClipEditando(null)}
          onSave={guardar}
          onReset={reset}
        />
      )}
    </div>
  );
};

export default Dataset;
