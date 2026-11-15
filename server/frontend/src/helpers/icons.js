// Íconos SVG como string para los formatters de Tabulator, que devuelven HTML
// y no pueden usar componentes React. Mismos trazos que Eye, Pencil y Trash2
// de lucide-react, para que se vean igual que el resto de la interfaz.
const svg = (paths) =>
  `<svg aria-hidden="true" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${paths}</svg>`;

export const iconEye = svg('<path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>');
export const iconPencil = svg('<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/>');
export const iconTrash = svg('<path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14M10 11v5M14 11v5"/>');
