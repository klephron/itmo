import { ExportBusEntityDto } from "@api/generated/data-contracts";
import { Operation } from "./kind";
import styles from "./App.module.scss";
import { createSignal, Match, Switch } from "solid-js";
import { clientGet } from "@state/client";
import { hasString } from "./util";

export const ExportBusOperations = (props: {
  onUpdate: (op: Operation, bus: ExportBusEntityDto) => void;
  onExport: (bus: ExportBusEntityDto) => void;
}) => {
  const client = clientGet();

  const [operationGet, operationSet] = createSignal(Operation.Add);
  const [idGet, idSet] = createSignal("");
  const [busNameGet, busNameSet] = createSignal("");
  const [networkIdGet, networkIdSet] = createSignal("");

  const [itemNameGet, itemNameSet] = createSignal("");
  const [itemQuantityGet, itemQuantitySet] = createSignal("");

  function addExportBus() {
    const bus: ExportBusEntityDto = {
      id: hasString(idGet()) ? Number(idGet()) : undefined,
      busName: busNameGet(),
      channelsUsed: 0,
      networkId: Number(networkIdGet()),
    };

    client.exportBuses
      .createExportBus({ networkId: bus.networkId }, bus)
      .then(() => {
        props.onUpdate(Operation.Add, bus);
      });
  }

  function deleteExportBus() {
    const id = Number(idGet());
    client.exportBuses.deleteExportBus(id).then(() => {
      props.onUpdate(Operation.Delete, { id } as ExportBusEntityDto);
    });
  }

  function updateExportBus() {
    const bus: ExportBusEntityDto = {
      id: hasString(idGet()) ? Number(idGet()) : undefined,
      busName: busNameGet(),
      channelsUsed: 0,
      networkId: Number(networkIdGet()),
    };

    client.exportBuses.updateExportBus(bus.id!, bus).then(() => {
      props.onUpdate(Operation.Update, bus);
    });
  }

  function exportItems() {
    const id = Number(idGet());
    client.exportBuses
      .exportItems(id, {
        itemName: itemNameGet(),
        quantity: hasString(itemQuantityGet()) ? Number(itemQuantityGet()) : 0,
      })
      .then(() => {
        props.onExport({ id: id } as ExportBusEntityDto);
      });
  }

  return (
    <div class={styles.operations}>
      <div class={styles.operation}>
        <select
          value={operationGet()}
          onChange={(e) => operationSet(parseInt(e.target.value))}
        >
          <option value={Operation.Add}>Add Export Bus</option>
          <option value={Operation.Delete}>Delete Export Bus</option>
          <option value={Operation.Update}>Update Export Bus</option>
        </select>
        <Switch>
          <Match when={operationGet() === Operation.Add}>
            <div class={styles.form}>
              <input
                type="text"
                placeholder="Bus Name"
                value={busNameGet()}
                onChange={(e) => busNameSet(e.target.value)}
              />
              <input
                type="number"
                placeholder="Network ID"
                value={networkIdGet()}
                onChange={(e) => networkIdSet(e.target.value)}
              />
              <button onClick={addExportBus}>Add Export Bus</button>
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
              <button onClick={deleteExportBus}>Delete Export Bus</button>
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
                placeholder="New Bus Name"
                value={busNameGet()}
                onChange={(e) => busNameSet(e.target.value)}
              />
              <button onClick={updateExportBus}>Update Export Bus</button>
            </div>
          </Match>
        </Switch>
      </div>
      <div class={styles.operation}>
        <select value={"Export Items"}>
          <option value={"Export Items"}>Export Items</option>
        </select>
        <div class={styles.form}>
          <input
            type="number"
            placeholder="Bus ID"
            value={idGet()}
            onChange={(e) => idSet(e.target.value)}
          />
          <input
            type="text"
            placeholder="Item Name"
            value={itemNameGet()}
            onChange={(e) => itemNameSet(e.target.value)}
          />
          <input
            type="number"
            placeholder="Quantity"
            value={itemQuantityGet()}
            onChange={(e) => itemQuantitySet(e.target.value)}
          />
          <button onClick={exportItems}>Export Items</button>
        </div>
      </div>
    </div>
  );
};
