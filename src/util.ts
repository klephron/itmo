export function catchResponse(e: any) {
  switch (e.status) {
    case 400:
      alert("Bad Request\n" + e.error._embedded.errors.map((ev: any) => ev.message).join("\n"));
      break;
    case 404:
      alert("Not Found");
      break;
    default:
      alert(JSON.stringify(e));
  }
}

export function exportToFile(content: string, type: string, filename: string) {
  const blob = new Blob([content], { type });
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
