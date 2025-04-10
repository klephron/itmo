import { CellItemDto } from "@api/generated/data-contracts";

import styles from "./App.module.scss";

export const CellItemTable = (props: { items: CellItemDto[] }) => {
  return (
    <table class={styles.table}>
      <thead>
        <tr>
          <th>ID</th>
          <th>Item Name</th>
          <th>Quantity</th>
          <th>Storage Cell ID</th>
        </tr>
      </thead>
      <tbody>
        {props.items.map((item) => (
          <tr>
            <td>{item.id}</td>
            <td>{item.itemName}</td>
            <td>{item.quantity}</td>
            <td>{item.storageCellId}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};
