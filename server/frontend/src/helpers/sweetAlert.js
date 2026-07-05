import Swal from 'sweetalert2';

export function deleteDataAlert() {
  return Swal.fire({
    title: '¿Estás seguro?',
    text: 'Esta acción no se puede deshacer.',
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#0039A6',
    cancelButtonColor: '#D52B1E',
    confirmButtonText: 'Sí, eliminar',
    cancelButtonText: 'Cancelar',
  });
}

export function showSuccessAlert(title, text) {
  return Swal.fire({ icon: 'success', title, text, confirmButtonColor: '#0039A6' });
}

export function showErrorAlert(title, text) {
  return Swal.fire({ icon: 'error', title, text, confirmButtonColor: '#D52B1E' });
}