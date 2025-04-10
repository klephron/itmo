import { ExportBusEntityDto } from "@api/generated/data-contracts";
import styles from "./App.module.scss";

export const ExportBusTable = (props: { items: ExportBusEntityDto[] }) => {
  return (
    <table class={styles.table}>
      <thead>
        <tr>
          <th>ID</th>
          <th>Bus Name</th>
          <th>Channels Used</th>
          <th>Network ID</th>
        </tr>
      </thead>
      <tbody>
        {props.items.map((bus) => (
          <tr>
            <td>{bus.id}</td>
            <td>{bus.busName}</td>
            <td>{bus.channelsUsed}</td>
            <td>{bus.networkId}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};
