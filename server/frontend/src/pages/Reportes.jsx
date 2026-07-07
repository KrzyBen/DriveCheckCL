import { useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import Table from '@components/Table';
import useReportes from '@hooks/reportes/useReportes.jsx';
import useDeleteReporte from '@hooks/reportes/useDeleteReporte.jsx';

const DIAS_MINIMOS = 60;

const stateLabels = { enviado: 'Enviado', recibido: 'Recibido', analizando: 'Analizando', aprobado: 'Aprobado', rechazado: 'Rechazado' };
const badgeClass = { enviado: 'badge-neutral', recibido: 'badge-neutral', analizando: 'badge-blue', aprobado: 'badge-green', rechazado: 'badge-red' };

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
    { title: 'ID', field: 'id', widthGrow: 1, formatter: (cell) => `#${cell.getValue()}` },
    { title: 'Título', field: 'titulo', widthGrow: 2 },
    { title: 'Usuario', field: 'usuario_nombre', widthGrow: 2 },
    {
      title: 'Estado', field: 'estado', widthGrow: 1,
      formatter: (cell) => `<span class="badge ${badgeClass[cell.getValue()]}">${stateLabels[cell.getValue()]}</span>`,
    },
    {
      title: 'Finalizado el', field: 'finalizado_at', widthGrow: 1,
      formatter: (cell) => (cell.getValue() ? new Date(cell.getValue()).toLocaleDateString('es-CL') : '—'),
    },
    {
      title: 'Acciones', widthGrow: 1, hozAlign: 'right', headerSort: false,
      formatter: (cell) => {
        const r = cell.getData();
        const terminado = r.estado === 'aprobado' || r.estado === 'rechazado';
        const puede = puedeEliminarse(r);
        const delBtn = terminado
          ? `<button class="table-action-btn" data-action="eliminar" title="${puede ? 'Eliminar' : `Disponible ${DIAS_MINIMOS} días después de aprobado`}" ${puede ? '' : 'disabled'}>🗑</button>`
          : '';
        return `<button class="table-action-btn" data-action="ver" title="Ver / validar">👁</button>${delBtn}`;
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
    <div>
      <div style={{ marginBottom: '1rem' }}>
        <h3 style={{ margin: 0 }}>Reportes</h3>
        <p style={{ fontSize: 13, color: 'var(--text-muted)', margin: '2px 0 0' }}>{reportes.length} reportes</p>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: '1rem' }}>
        <input placeholder="Buscar por usuario" style={{ flex: 1 }} disabled />
        <select
          style={{ width: 170 }}
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

      <Table data={reportes} columns={columns} initialSortName="id" />
    </div>
  );
};

export default Reportes;