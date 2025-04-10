import { DriveDto } from "@api/generated/data-contracts";

import styles from "./App.module.scss";

export const DriveTable = (props: { items: DriveDto[] }) => {
  return (
    <table class={styles.table}>
      <thead>
        <tr>
          <th>ID</th>
          <th>Drive Name</th>
          <th>Channels Used</th>
          <th>Network ID</th>
        </tr>
      </thead>
      <tbody>
        {props.items.map((drive) => (
          <tr>
            <td>{drive.id}</td>
            <td>{drive.driveName}</td>
            <td>{drive.channelsUsed}</td>
            <td>{drive.networkId}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};
