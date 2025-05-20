import { ImportBusDto } from "@api/generated/data-contracts";
import { Operation } from "./kind";
import styles from "./App.module.scss";
import { createSignal, Match, Switch } from "solid-js";
import { clientGet } from "@state/client";
import { hasString } from "./util";

export const ImportBusOperations = (props: {
  onUpdate: (op: Operation, bus: ImportBusDto) => void;
  onImport: (bus: ImportBusDto) => void;
}) => {
  const client = clientGet();

  const [operationGet, operationSet] = createSignal(Operation.Add);
  const [idGet, idSet] = createSignal("");
  const [busNameGet, busNameSet] = createSignal("");
  const [networkIdGet, networkIdSet] = createSignal("");

  const [itemNameGet, itemNameSet] = createSignal("");
  const [itemQuantityGet, itemQuantitySet] = createSignal("");

  function addImportBus() {
    const bus: ImportBusDto = {
      id: hasString(idGet()) ? Number(idGet()) : undefined,
      busName: busNameGet(),
      channelsUsed: 0,
      networkId: Number(networkIdGet()),
    };

    client.importBuses
      .createImportBus({ networkId: bus.networkId }, bus)
      .then(() => props.onUpdate(Operation.Add, bus))
      .catch(async (e) => alert(await e.text()));
  }

  function deleteImportBus() {
    const id = Number(idGet());
    client.importBuses
      .deleteImportBus(id)
      .then(() => props.onUpdate(Operation.Delete, { id } as ImportBusDto))
      .catch(async (e) => alert(await e.text()));
  }

  function updateImportBus() {
    const bus: ImportBusDto = {
      id: hasString(idGet()) ? Number(idGet()) : undefined,
      busName: busNameGet(),
      channelsUsed: 0,
      networkId: Number(networkIdGet()),
    };

    client.importBuses
      .updateImportBus(bus.id!, bus)
      .then(() => props.onUpdate(Operation.Update, bus))
      .catch(async (e) => alert(await e.text()));
  }

  function importItems() {
    const id = Number(idGet());
    client.importBuses
      .importItems(id, {
        itemName: itemNameGet(),
        quantity: hasString(itemQuantityGet()) ? Number(itemQuantityGet()) : 0,
      })
      .then(() => props.onImport({ id: id } as ImportBusDto))
      .catch(async (e) => alert(await e.text()));
  }

  return (
    <div class={styles.operations}>
      <div class={styles.operation}>
        <select
          value={operationGet()}
          onChange={(e) => operationSet(parseInt(e.target.value))}
        >
          <option value={Operation.Add}>Add Import Bus</option>
          <option value={Operation.Delete}>Delete Import Bus</option>
          <option value={Operation.Update}>Update Import Bus</option>
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
              <button onClick={addImportBus}>Add Import Bus</button>
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
              <button onClick={deleteImportBus}>Delete Import Bus</button>
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
              <button onClick={updateImportBus}>Update Import Bus</button>
            </div>
          </Match>
        </Switch>
      </div>

      <div class={styles.operation}>
        <select value={"Import Items"}>
          <option value={"Import Items"}>Import Items</option>
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
          <button onClick={importItems}>Import Items</button>
        </div>
      </div>
    </div>
  );
};
