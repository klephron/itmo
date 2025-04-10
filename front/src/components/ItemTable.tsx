import { ItemDto } from "@api/generated/data-contracts";

import styles from "./App.module.scss";

export const ItemTable = (props: { items: ItemDto[] }) => {
  return (
    <table class={styles.table}>
      <thead>
        <tr>
          <th>ID</th>
          <th>Item Name</th>
        </tr>
      </thead>
      <tbody>
        {props.items.map((item) => (
          <tr>
            <td>{item.id}</td>
            <td>{item.itemName}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};
