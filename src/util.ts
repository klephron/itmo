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
