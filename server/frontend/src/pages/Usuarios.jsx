import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Edit, Trash2, Eye } from 'lucide-react';
import Table from '@components/Table';
import Search from '@components/Search';
import UsuarioPopup from '@components/UsuarioPopup';
import useUsuarios from '@hooks/usuarios/useUsuarios.jsx';
import useCreateUsuario from '@hooks/usuarios/useCreateUsuario.jsx';
import useUpdateUsuario from '@hooks/usuarios/useUpdateUsuario.jsx';
import useDeleteUsuario from '@hooks/usuarios/useDeleteUsuario.jsx';

const Usuarios = () => {
  const { usuarios, fetchUsuarios } = useUsuarios();
  const { handleCreate } = useCreateUsuario(fetchUsuarios);
  const { handleUpdate } = useUpdateUsuario(fetchUsuarios);
  const { handleDelete } = useDeleteUsuario(fetchUsuarios);
  const navigate = useNavigate();

  const [search, setSearch] = useState('');
  const [popup, setPopup] = useState({ show: false, mode: 'crear', data: [] });

  const openCreate = () => setPopup({ show: true, mode: 'crear', data: [] });
  const openEdit = (usuario) => setPopup({ show: true, mode: 'editar', data: [usuario] });

  const handlePopupSubmit = async (formData) => {
    if (popup.mode === 'crear') {
      const { error } = await handleCreate(formData);
      if (error) return;
    } else {
      const { error } = await handleUpdate(popup.data[0].id, formData);
      if (error) return;
    }
    setPopup({ show: false, mode: 'crear', data: [] });
  };

  const columns = useMemo(() => [
    { title: 'Nombre', field: 'nombre_completo', widthGrow: 2 },
    { title: 'Correo', field: 'email', widthGrow: 2 },
    { title: 'RUT', field: 'rut', widthGrow: 1 },
    {
      title: 'Rol', field: 'rol', widthGrow: 1,
      formatter: (cell) => {
        const rol = cell.getValue();
        const cls = rol === 'administrador' ? 'badge-blue' : rol === 'validador' ? 'badge-amber' : 'badge-neutral';
        return `<span class="badge ${cls}">${rol}</span>`;
      },
    },
    {
      title: 'Acciones', widthGrow: 1, hozAlign: 'right', headerSort: false,
      formatter: (cell) => {
        const rol = cell.getData().rol;
        const verBtn = rol === 'conductor' ? `<button class="table-action-btn" data-action="ver" title="Ver reportes">👁</button>` : '';
        return `${verBtn}<button class="table-action-btn" data-action="editar" title="Editar">✎</button><button class="table-action-btn" data-action="eliminar" title="Eliminar">🗑</button>`;
      },
      cellClick: (e, cell) => {
        const action = e.target.closest('button')?.dataset.action;
        const row = cell.getData();
        if (action === 'editar') openEdit(row);
        if (action === 'eliminar') handleDelete(row.id);
        if (action === 'ver') navigate(`/reportes?usuario_id=${row.id}`);
      },
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
  ], []);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <div>
          <h3 style={{ margin: 0 }}>Usuarios</h3>
          <p style={{ fontSize: 13, color: 'var(--text-muted)', margin: '2px 0 0' }}>
            {usuarios.length} usuarios registrados
          </p>
        </div>
        <button className="btn btn-primary" onClick={openCreate}>
          <Plus size={16} /> Nuevo usuario
        </button>
      </div>

      <div style={{ marginBottom: '1rem' }}>
        <Search value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Buscar por nombre o correo" />
      </div>

      <Table data={usuarios} columns={columns} filter={search} dataToFilter="nombre_completo" initialSortName="nombre_completo" />

      <UsuarioPopup
        show={popup.show}
        setShow={(v) => setPopup((p) => ({ ...p, show: v }))}
        data={popup.data}
        action={handlePopupSubmit}
        mode={popup.mode}
      />
    </div>
  );
};

export default Usuarios;