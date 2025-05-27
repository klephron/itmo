import { Route } from "~react-router/src/pages/case/+types";

export default function CasePage({ params }: Route.ComponentProps) {
  return <>Case {params.id}</>;
}
