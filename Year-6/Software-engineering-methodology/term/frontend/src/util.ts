export function catchResponse(e: any) {
  const status = e?.status ?? e?.response?.status;

  if (status === 400 && e?.error?._embedded?.errors) {
    alert("Bad Request\n" + e.error._embedded.errors.map((ev: any) => ev.message).join("\n"));
    return;
  }
  if (status === 404) {
    alert("Not Found");
    return;
  }
  if (status === 401 || status === 403) {
    const msg =
      typeof e?.error === "string"
        ? e.error
        : e?.error?.message || e?.message || "Unauthorized";
    alert(msg);
    return;
  }

  if (typeof e === "string") {
    alert(e);
    return;
  }
  if (typeof e?.error === "string") {
    alert(e.error);
    return;
  }
  if (typeof e?.message === "string") {
    alert(e.message);
    return;
  }
  if (typeof e?.rawBody === "string") {
    alert(e.rawBody);
    return;
  }

  alert("Unexpected error");
}

export function exportToFile(content: string | Blob, type: string, filename: string) {
  const blob = content instanceof Blob ? content : new Blob([content], { type });
  const url = URL.createObjectURL(blob);

  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.style.display = "none";

  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export function formatUtc(value?: string | number | Date | null) {
  if (value === null || value === undefined) return "";
  const d = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(d.getTime())) return "";
  return d.toLocaleString(undefined, { timeZone: "UTC" });
}

export function formatUtcDate(value?: string | number | Date | null) {
  if (value === null || value === undefined) return "";
  const d = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(d.getTime())) return "";
  return d.toLocaleDateString(undefined, { timeZone: "UTC" });
}

/**
 * `datetime-local` input value -> ISO string in UTC.
 * Notes: browser returns a string without timezone, so we treat it as UTC by appending `Z`.
 */
export function datetimeLocalToUtcIso(value?: string | null) {
  if (!value) return undefined;
  const d = new Date(`${value}Z`);
  if (Number.isNaN(d.getTime())) return undefined;
  return d.toISOString();
}
