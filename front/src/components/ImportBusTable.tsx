import { ImportBusDto } from "@api/generated/data-contracts";

import { BusTable } from "./BusTable";

export const ImportBusTable = (props: { items: ImportBusDto[] }) => {
  return <BusTable items={props.items} />;
};
