import { PatternDto } from "@api/generated/data-contracts";

import styles from "./App.module.scss";

export const PatternTable = (props: { items: PatternDto[] }) => {
  return (
    <table class={styles.table}>
      <thead>
        <tr>
          <th>ID</th>
          <th>Pattern Name</th>
          <th>Input Items</th>
          <th>Output Item Name</th>
          <th>Output Quantity</th>
          <th>ME Interface ID</th>
        </tr>
      </thead>
      <tbody>
        {props.items.map((pattern) => (
          <tr>
            <td>{pattern.id}</td>
            <td>{pattern.patternName}</td>
            <td>{pattern.inputItems}</td>
            <td>{pattern.outputItem}</td>
            <td>{pattern.outputQuantity}</td>
            <td>{pattern.meInterfaceId}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};
