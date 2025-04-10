import { ItemDto } from "@api/generated/data-contracts";
import { Operation } from "./kind";
import styles from "./App.module.scss";
import { createSignal, Match, Switch } from "solid-js";
import { clientGet } from "@state/client";
import { hasString } from "./util";

export const ItemOperations = (props: {
  onUpdate: (op: Operation, item: ItemDto) => void;
}) => {
  const client = clientGet();

  const [operationGet, operationSet] = createSignal(Operation.Add);
  const [idGet, idSet] = createSignal("");
  const [itemNameGet, itemNameSet] = createSignal("");
  const [quantityGet, quantitySet] = createSignal("");

  function addItem() {
    const item: ItemDto = {
      itemName: itemNameGet(),
      quantity: Number(quantityGet()),
    };

    client.items.createItem(item).then(() => {
      props.onUpdate(Operation.Add, item);
    });
  }

  function deleteItem() {
    const id = Number(idGet());
    client.items.deleteItem(id).then(() => {
      props.onUpdate(Operation.Delete, { id } as ItemDto);
    });
  }

  function updateItem() {
    const item: ItemDto = {
      id: hasString(idGet()) ? Number(idGet()) : undefined,
      itemName: itemNameGet(),
      quantity: Number(quantityGet()),
    };

    client.items.updateItem(item.id!, item).then(() => {
      props.onUpdate(Operation.Update, item);
    });
  }

  return (
    <div class={styles.operations}>
      <div class={styles.operation}>
        <select
          value={operationGet()}
          onChange={(e) => operationSet(parseInt(e.target.value))}
        >
          <option value={Operation.Add}>Add Item</option>
          <option value={Operation.Delete}>Delete Item</option>
          <option value={Operation.Update}>Update Item</option>
        </select>
        <Switch>
          <Match when={operationGet() === Operation.Add}>
            <div class={styles.form}>
              <input
                type="text"
                placeholder="Item Name"
                value={itemNameGet()}
                onChange={(e) => itemNameSet(e.target.value)}
              />
              <input
                type="number"
                placeholder="Quantity"
                min="0"
                value={quantityGet()}
                onChange={(e) => quantitySet(e.target.value)}
              />
              <button onClick={addItem}>Add Item</button>
            </div>
          </Match>
          <Match when={operationGet() === Operation.Delete}>
            <div class={styles.form}>
              <input
                type="number"
                placeholder="ID to Delete"
                value={idGet()}
                onChange={(e) => idSet(e.target.value)}
              />
              <button onClick={deleteItem}>Delete Item</button>
            </div>
          </Match>
          <Match when={operationGet() === Operation.Update}>
            <div class={styles.form}>
              <input
                type="number"
                placeholder="ID to Update"
                value={idGet()}
                onChange={(e) => idSet(e.target.value)}
              />
              <input
                type="text"
                placeholder="New Item Name"
                value={itemNameGet()}
                onChange={(e) => itemNameSet(e.target.value)}
              />
              <input
                type="number"
                placeholder="New Quantity"
                min="0"
                value={quantityGet()}
                onChange={(e) => quantitySet(e.target.value)}
              />
              <button onClick={updateItem}>Update Item</button>
            </div>
          </Match>
        </Switch>
      </div>
    </div>
  );
};
