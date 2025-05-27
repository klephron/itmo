import { usePermissions } from "@state/permissions";
import { useEffect } from "react";

export default function IncidentsPage() {
  const permissions = usePermissions((state) => state.permissions);

  useEffect(() => {
    if (permissions) {
      console.log("permissions loaded", permissions);
    } else {
      console.log("waiting permissions");
    }
  }, [permissions]);

  return <>Incidents</>;
}
