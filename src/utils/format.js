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

/**
 * Форматирует число в компактный вид:
 * 999 → 999
 * 1_000 → 1 K
 * 1_500 → 1.5 K
 * 999_000 → 999 K
 * 1_000_000 → 1 M
 * 999_000_000 → 999 M
 * 1_000_000_000 → 1 B
 */
export function formatCompactNumber(value) {
    if (value === null || value === undefined) return '0';

    const num = Number(value);
    if (!Number.isFinite(num)) return '0';

    const abs = Math.abs(num);

    if (abs < 1000) {
        return String(Math.trunc(num));
    }

    const format = (divisor, suffix) => {
        const truncated = Math.floor((num / divisor) * 10) / 10;
        return String(truncated).replace(/\.0$/, '') + ' ' + suffix;
    };

    if (abs < 1_000_000) {
        return format(1_000, 'K');
    }

    if (abs < 1_000_000_000) {
        return format(1_000_000, 'M');
    }

    return format(1_000_000_000, 'B');
}