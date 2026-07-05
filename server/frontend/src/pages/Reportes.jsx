import { useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import Table from '@components/Table';
import useReportes from '@hooks/reportes/useReportes.jsx';
import useDeleteReporte from '@hooks/reportes/useDeleteReporte.jsx';

const DIAS_MINIMOS = 60;
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
  const { reportes, fetchReportes } = useReportes(usuarioId ? { usuario_id: usuarioId } : {});
  const { handleDelete } = useDeleteReporte(fetchReportes);
  const navigate = useNavigate();

  const columns = useMemo(() => [
    { title: 'ID', field: 'id', widthGrow: 1, formatter: (cell) => `#${cell.getValue()}` },
    { title: 'Título', field: 'titulo', widthGrow: 2 },
    {
      title: 'Estado', field: 'estado', widthGrow: 1,
      formatter: (cell) => `<span class="badge ${badgeClass[cell.getValue()]}">${cell.getValue()}</span>`,
    },
    {
      title: 'Finalizado el', field: 'finalizado_at', widthGrow: 1,
      formatter: (cell) => (cell.getValue() ? new Date(cell.getValue()).toLocaleDateString('es-CL') : '—'),
    },
    {
      title: 'Acciones', widthGrow: 1, hozAlign: 'center', headerSort: false,
      formatter: (cell) => {
        const r = cell.getData();
        const terminado = r.estado === 'aprobado' || r.estado === 'rechazado';
        const puede = puedeEliminarse(r);
        const delBtn = terminado
          ? `<button class="table-action-btn" data-action="eliminar" title="${puede ? 'Eliminar' : `Disponible ${DIAS_MINIMOS} días después de aprobado`}" ${puede ? '' : 'disabled style="opacity:.4;cursor:not-allowed;"'}>🗑</button>`
          : '';
        return `<button class="table-action-btn" data-action="ver" title="Ver / validar">👁</button>${delBtn}`;
      },
      cellClick: (e, cell) => {
        const btn = e.target.closest('button');
        if (!btn || btn.disabled) return;
        const action = btn.dataset.action;
        const r = cell.getData();
        if (action === 'ver') navigate(`/reportes/${r.id}`);
        if (action === 'eliminar') handleDelete(r.id);
      },
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
  ], []);

  return (
    <div className="card" style={{ padding: '1.25rem 1.5rem' }}>
      <div style={{ marginBottom: '1rem' }}>
        <h3 style={{ margin: 0 }}>Reportes</h3>
        <p style={{ fontSize: 13, color: 'var(--text-muted)', margin: '2px 0 0' }}>{reportes.length} reportes</p>
      </div>
      <Table data={reportes} columns={columns} initialSortName="id" />
    </div>
  );
};

export default Reportes;