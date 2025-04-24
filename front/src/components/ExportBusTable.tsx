import { ExportBusDto } from "@api/generated/data-contracts";

import { BusTable } from "./BusTable";

export const ExportBusTable = (props: { items: ExportBusDto[] }) => {
  return <BusTable items={props.items} />;
};
