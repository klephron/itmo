import { StorageCellEntityDto } from "@api/generated/data-contracts";
import { Operation } from "./kind";
import styles from "./App.module.scss";
import { createSignal, Match, Switch } from "solid-js";
import { clientGet } from "@state/client";
import { hasString } from "./util";

export const StorageCellOperations = (props: {
  onUpdate: (op: Operation, cell: StorageCellEntityDto) => void;
}) => {
  const client = clientGet();

  const [operationGet, operationSet] = createSignal(Operation.Add);
  const [idGet, idSet] = createSignal("");
  const [capacityGet, capacitySet] = createSignal("");
  const [driveIdGet, driveIdSet] = createSignal("");

  function addStorageCell() {
    const cell: StorageCellEntityDto = {
      id: hasString(idGet()) ? Number(idGet()) : undefined,
      capacity: Number(capacityGet()),
      driveId: Number(driveIdGet()),
    };

    client.storageCells
      .createStorageCell({ driveId: cell.driveId }, cell)
      .then(() => {
        props.onUpdate(Operation.Add, cell);
      });
  }

  function deleteStorageCell() {
    const id = Number(idGet());
    client.storageCells.deleteStorageCell(id).then(() => {
      props.onUpdate(Operation.Delete, { id } as StorageCellEntityDto);
    });
  }

  function updateStorageCell() {
    const cell: StorageCellEntityDto = {
      id: hasString(idGet()) ? Number(idGet()) : undefined,
      capacity: Number(capacityGet()),
      driveId: 0,
    };

    client.storageCells.updateStorageCell(cell.id!, cell).then(() => {
      props.onUpdate(Operation.Update, cell);
    });
  }

  return (
    <div class={styles.operations}>
      <div class={styles.operation}>
        <select
          value={operationGet()}
          onChange={(e) => operationSet(parseInt(e.target.value))}
        >
          <option value={Operation.Add}>Add Storage Cell</option>
          <option value={Operation.Delete}>Delete Storage Cell</option>
          <option value={Operation.Update}>Update Storage Cell</option>
        </select>
        <Switch>
          <Match when={operationGet() === Operation.Add}>
            <div class={styles.form}>
              <input
                type="number"
                placeholder="Capacity"
                min="0"
                value={capacityGet()}
                onChange={(e) => capacitySet(e.target.value)}
              />
              <input
                type="number"
                placeholder="Drive ID"
                value={driveIdGet()}
                onChange={(e) => driveIdSet(e.target.value)}
              />
              <button onClick={addStorageCell}>Add Storage Cell</button>
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
              <button onClick={deleteStorageCell}>Delete Storage Cell</button>
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
                type="number"
                placeholder="New Capacity"
                min="0"
                value={capacityGet()}
                onChange={(e) => capacitySet(e.target.value)}
              />
              <button onClick={updateStorageCell}>Update Storage Cell</button>
            </div>
          </Match>
        </Switch>
      </div>
    </div>
  );
};
