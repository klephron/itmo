import { MeInterfaceDto } from "@api/generated/data-contracts";
import { Operation } from "./kind";
import styles from "./App.module.scss";
import { createSignal, Match, Switch } from "solid-js";
import { clientGet } from "@state/client";
import { hasString } from "./util";

export const InterfaceOperations = (props: {
  onUpdate: (op: Operation, item: MeInterfaceDto) => void;
}) => {
  const client = clientGet();

  const [operation, operationSet] = createSignal(Operation.Add);
  const [idGet, idSet] = createSignal("");
  const [interfaceNameGet, interfaceNameSet] = createSignal("");
  const [networkIdGet, networkIdSet] = createSignal("");

  function addInterface() {
    const meInterface: MeInterfaceDto = {
      interfaceName: interfaceNameGet(),
      networkId: Number(networkIdGet()),
    };

    client.interfaces
      .createInterface({ networkId: meInterface.networkId }, meInterface)
      .then(() => props.onUpdate(Operation.Add, meInterface))
      .catch(async (e) => alert(await e.text()));
  }

  function deleteInterface() {
    const id = Number(idGet());
    client.interfaces
      .deleteInterface(id)
      .then(() => props.onUpdate(Operation.Delete, { id } as MeInterfaceDto))
      .catch(async (e) => alert(await e.text()));
  }

  function updateInterface() {
    const meInterface: MeInterfaceDto = {
      id: hasString(idGet()) ? Number(idGet()) : undefined,
      interfaceName: interfaceNameGet(),
      networkId: Number(networkIdGet()),
    };

    client.interfaces
      .updateInterface(meInterface.id!, meInterface)
      .then(() => props.onUpdate(Operation.Update, meInterface))
      .catch(async (e) => alert(await e.text()));
  }

  return (
    <div class={styles.operations}>
      <div class={styles.operation}>
        <select
          value={operation()}
          onChange={(e) => operationSet(parseInt(e.target.value))}
        >
          <option value={Operation.Add}>Add Interface</option>
          <option value={Operation.Delete}>Delete Interface</option>
          <option value={Operation.Update}>Update Interface</option>
        </select>
        <Switch>
          <Match when={operation() === Operation.Add}>
            <div class={styles.form}>
              <input
                type="text"
                placeholder="Interface Name"
                value={interfaceNameGet()}
                onChange={(e) => interfaceNameSet(e.target.value)}
              />
              <input
                type="number"
                placeholder="Network ID"
                value={networkIdGet()}
                onChange={(e) => networkIdSet(e.target.value)}
              />
              <button onClick={addInterface}>Add Interface</button>
            </div>
          </Match>
          <Match when={operation() === Operation.Delete}>
            <div class={styles.form}>
              <input
                type="number"
                placeholder="ID to Delete"
                value={idGet()}
                onChange={(e) => idSet(e.target.value)}
              />
              <button onClick={deleteInterface}>Delete Interface</button>
            </div>
          </Match>
          <Match when={operation() === Operation.Update}>
            <div class={styles.form}>
              <input
                type="number"
                placeholder="ID to Update"
                value={idGet()}
                onChange={(e) => idSet(e.target.value)}
              />
              <input
                type="text"
                placeholder="New Interface Name"
                value={interfaceNameGet()}
                onChange={(e) => interfaceNameSet(e.target.value)}
              />
              <button onClick={updateInterface}>Update Interface</button>
            </div>
          </Match>
        </Switch>
      </div>
    </div>
  );
};
