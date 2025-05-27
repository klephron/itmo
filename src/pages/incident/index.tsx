import { Route } from "~react-router/src/pages/incident/+types";

export default function IncidentPage({ params }: Route.ComponentProps) {
  return <>Incident {params.id}</>;
}
