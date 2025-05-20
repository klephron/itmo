import { NetworkDto } from "@api/generated/data-contracts";

import styles from "./App.module.scss";

export const NetworkTable = (props: { items: NetworkDto[] }) => {
  return (
    <table class={styles.table}>
      <thead>
        <tr>
          <th>ID</th>
          <th>Name</th>
          <th>Channel Capacity</th>
          <th>Used Channels</th>
        </tr>
      </thead>
      <tbody>
        {props.items.map((network) => (
          <tr>
            <td>{network.id}</td>
            <td>{network.name}</td>
            <td>{network.channelCapacity}</td>
            <td>{network.usedChannels}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};
