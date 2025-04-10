import { NetworkDto } from "@api/generated/data-contracts";
import { Operation } from "./kind";
import { clientGet } from "@state/client";
import { createSignal, Match, Switch } from "solid-js";
import { hasString } from "./util";

import styles from "./App.module.scss";

export const NetworkOperations = (props: {
  onUpdate: (op: Operation, item: NetworkDto) => void;
}) => {
  const client = clientGet();

  const [operationGet, operationSet] = createSignal(Operation.Add);
  const [idGet, idSet] = createSignal("");
  const [nameGet, nameSet] = createSignal("");
  const [channelCapacityGet, channelCapacitySet] = createSignal("");
  const [usedChannelsGet, usedChannelsSet] = createSignal("");

  function addNetwork() {
    const network: NetworkDto = {
      name: nameGet(),
      channelCapacity: Number(channelCapacityGet()),
      usedChannels: Number(usedChannelsGet()),
    };

    client.networks.createNetwork(network).then(() => {
      props.onUpdate(Operation.Add, network);
    });
  }

  function deleteNetwork() {
    const id = Number(idGet());
    client.networks.deleteNetwork(id).then(() => {
      props.onUpdate(Operation.Delete, { id: id } as NetworkDto);
    });
  }

  function updateNetwork() {
    const network: NetworkDto = {
      id: hasString(idGet()) ? Number(idGet()) : undefined,
      name: nameGet(),
      channelCapacity: Number(channelCapacityGet()),
      usedChannels: Number(usedChannelsGet()),
    };

    client.networks.updateNetwork(network.id!, network).then(() => {
      props.onUpdate(Operation.Update, network);
    });
  }

  return (
    <div class={styles.operations}>
      <div class={styles.operation}>
        <select
          value={operationGet()}
          onChange={(e) => operationSet(parseInt(e.target.value))}
        >
          <option value={Operation.Add}>Add Network</option>
          <option value={Operation.Update}>Update Network</option>
          <option value={Operation.Delete}>Delete Network</option>
        </select>

        <Switch>
          <Match when={operationGet() === Operation.Add}>
            <div class={styles.form}>
              <input
                type="text"
                placeholder="Name"
                value={nameGet()}
                onChange={(e) => nameSet(e.target.value)}
              />
              <input
                type="number"
                placeholder="Channel Capacity"
                min="0"
                value={channelCapacityGet()}
                onChange={(e) => channelCapacitySet(e.target.value)}
              />
              <input
                type="number"
                placeholder="Used Channels"
                min="0"
                value={usedChannelsGet()}
                onChange={(e) => usedChannelsSet(e.target.value)}
              />
              <button onClick={addNetwork}>Add Network</button>
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
              <button onClick={deleteNetwork}>Delete Network</button>
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
                placeholder="New Name"
                value={nameGet()}
                onChange={(e) => nameSet(e.target.value)}
              />
              <input
                type="number"
                placeholder="New Channel Capacity"
                min="0"
                value={channelCapacityGet()}
                onChange={(e) => channelCapacitySet(e.target.value)}
              />
              <input
                type="number"
                placeholder="New Used Channels"
                min="0"
                value={usedChannelsGet()}
                onChange={(e) => usedChannelsSet(e.target.value)}
              />
              <button onClick={updateNetwork}>Update Network</button>
            </div>
          </Match>
        </Switch>
      </div>
    </div>
  );
};
