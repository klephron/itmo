import { MeInterfaceDto } from "@api/generated/data-contracts";

import styles from "./App.module.scss";

export const InterfaceTable = (props: { items: MeInterfaceDto[] }) => {
  return (
    <table class={styles.table}>
      <thead>
        <tr>
          <th>ID</th>
          <th>Interface Name</th>
          <th>Channels Used</th>
          <th>Network Id</th>
        </tr>
      </thead>
      <tbody>
        {props.items.map((item) => (
          <tr>
            <td>{item.id}</td>
            <td>{item.interfaceName}</td>
            <td>{item.channelsUsed}</td>
            <td>{item.networkId}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};
