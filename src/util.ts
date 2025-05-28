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
