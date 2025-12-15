export function humanSize(bytes) {
    if (bytes == null) return '—';
    const units = ['Б', 'КБ', 'МБ', 'ГБ', 'ТБ'];
    let i = 0;
    let val = bytes;
    while (val >= 1024 && i < units.length - 1) {
        val /= 1024;
        i++;
    }
    return val.toFixed(i === 0 ? 0 : 1) + ' ' + units[i];
}

export function formatDate(iso) {
    if (!iso) return '—';
    try {
        const d = new Date(iso);
        return d.toLocaleString();
    } catch {
        return String(iso);
    }
}

export function formatViews(count) {
    const n = Number.isFinite(count) ? count : 0;
    if (n === 0) return 'Нет просмотров';
    if (n === 1) return '1 просмотр';
    if (n >= 2 && n <= 4) return `${n} просмотра`;
    return `${n} просмотров`;
}
