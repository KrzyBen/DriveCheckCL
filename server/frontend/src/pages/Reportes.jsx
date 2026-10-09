import { useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import Table from '@components/Table';
import Search from '@components/Search';
import useReportes from '@hooks/reportes/useReportes.jsx';
import useDeleteReporte from '@hooks/reportes/useDeleteReporte.jsx';
import { iconEye, iconTrash } from '@helpers/icons.js';

const DIAS_MINIMOS = 60;

const stateLabels = { enviado: 'Enviado', recibido: 'Recibido', analizando: 'Analizando', aprobado: 'Aprobado', rechazado: 'Rechazado' };
const badgeClass = { enviado: 'badge-neutral', recibido: 'badge-neutral', analizando: 'badge-blue badge-live', aprobado: 'badge-green', rechazado: 'badge-red' };

function puedeEliminarse(r) {
  if (r.estado === 'rechazado') return true;
  if (r.estado !== 'aprobado' || !r.finalizado_at) return false;
  const dias = (Date.now() - new Date(r.finalizado_at)) / (1000 * 60 * 60 * 24);
  return dias >= DIAS_MINIMOS;
}

const Reportes = () => {
  const [searchParams] = useSearchParams();
  const usuarioId = searchParams.get('usuario_id');
  const { reportes, filtros, setFiltros, fetchReportes } = useReportes(usuarioId ? { usuario_id: usuarioId } : {});
  const { handleDelete } = useDeleteReporte(fetchReportes);
  const navigate = useNavigate();

  const columns = useMemo(() => [
    // `responsive: 0` = la columna nunca se oculta en el modo collapse (móvil): ID y Acciones
    // siempre visibles para poder abrir un reporte; el resto baja a la lista de detalle.
    // El estilo va por cssClass y no por formatter: 'titulo' lo escribe el conductor
    // y un formatter lo insertaría como HTML sin escapar.
    { title: 'ID', field: 'id', widthGrow: 1, minWidth: 80, responsive: 0, cssClass: 'cell-id', formatter: (cell) => `#${cell.getValue()}` },
    { title: 'Título', field: 'titulo', widthGrow: 3, minWidth: 220, responsive: 1, cssClass: 'cell-title' },
    { title: 'Usuario', field: 'usuario_nombre', widthGrow: 2, minWidth: 150, responsive: 4 },
    {
      title: 'Estado', field: 'estado', widthGrow: 1.2, minWidth: 150, responsive: 2,
      formatter: (cell) => `<span class="badge ${badgeClass[cell.getValue()]}">${stateLabels[cell.getValue()]}</span>`,
    },
    {
      title: 'Finalizado el', field: 'finalizado_at', widthGrow: 1.2, minWidth: 150, responsive: 3,
      formatter: (cell) => (cell.getValue() ? new Date(cell.getValue()).toLocaleDateString('es-CL') : '—'),
    },
    {
      title: 'Acciones', widthGrow: 1, minWidth: 140, responsive: 0, hozAlign: 'right', headerHozAlign: 'right', headerSort: false,
      formatter: (cell) => {
        const r = cell.getData();
        const terminado = r.estado === 'aprobado' || r.estado === 'rechazado';
        const puede = puedeEliminarse(r);
        const titulo = puede ? 'Eliminar' : `Disponible ${DIAS_MINIMOS} días después de aprobado`;
        const delBtn = terminado
          ? `<button class="table-action-btn table-action-btn--danger" data-action="eliminar" title="${titulo}" aria-label="${titulo}" ${puede ? '' : 'disabled'}>${iconTrash}</button>`
          : '';
        return `<button class="table-action-btn" data-action="ver" title="Ver / validar" aria-label="Ver / validar reporte">${iconEye}</button>${delBtn}`;
      },
      cellClick: (e, cell) => {
        const btn = e.target.closest('button');
        if (!btn || btn.disabled) return;
        const r = cell.getData();
        if (btn.dataset.action === 'ver') navigate(`/reportes/${r.id}`);
        if (btn.dataset.action === 'eliminar') handleDelete(r.id);
      },
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
  ], []);

  return (
    <div className="page-enter">
      <div className="page-head">
        <div>
          <h1 className="page-title">Reportes</h1>
          <p className="page-sub">{reportes.length} reportes</p>
        </div>
        <div className="page-actions">
          <div style={{ width: 260 }}>
            <Search value="" onChange={() => {}} placeholder="Buscar por usuario" disabled />
          </div>
          <select
            aria-label="Filtrar por estado"
            style={{ width: 190 }}
            value={filtros.estado || ''}
            onChange={(e) => setFiltros({ ...filtros, estado: e.target.value || undefined })}
          >
            <option value="">Todos los estados</option>
            <option value="enviado">Enviado</option>
            <option value="recibido">Recibido</option>
            <option value="analizando">Analizando</option>
            <option value="aprobado">Aprobado</option>
            <option value="rechazado">Rechazado</option>
          </select>
        </div>
      </div>

      <Table data={reportes} columns={columns} initialSortName="id" />
    </div>
  );
};

export default Reportes;
