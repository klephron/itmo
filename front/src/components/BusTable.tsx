import styles from "./App.module.scss";

export const BusTable = (props: {
  items: {
    id?: number;
    busName?: string;
    channelsUsed?: number;
    networkId?: number;
  }[];
}) => {
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
