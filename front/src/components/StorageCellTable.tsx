import { StorageCellEntityDto } from "@api/generated/data-contracts";

import styles from "./App.module.scss";

export const StorageCellTable = (props: { items: StorageCellEntityDto[] }) => {
  return (
    <table class={styles.table}>
      <thead>
        <tr>
          <th>ID</th>
          <th>Capacity</th>
          <th>Drive ID</th>
        </tr>
      </thead>
      <tbody>
        {props.items.map((cell) => (
          <tr>
            <td>{cell.id}</td>
            <td>{cell.capacity}</td>
            <td>{cell.driveId}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};
