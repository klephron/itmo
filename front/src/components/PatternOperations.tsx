import { PatternDto } from "@api/generated/data-contracts";
import { Operation } from "./kind";
import styles from "./App.module.scss";
import { createSignal, Match, Switch } from "solid-js";
import { clientGet } from "@state/client";
import { hasString } from "./util";

export const PatternOperations = (props: {
  onUpdate: (op: Operation, pattern: PatternDto) => void;
  onCraft: (pattern: PatternDto) => void;
}) => {
  const client = clientGet();

  const [operationGet, operationSet] = createSignal(Operation.Add);
  const [idGet, idSet] = createSignal("");
  const [patternNameGet, patternNameSet] = createSignal("");
  const [inputItemsGet, inputItemsSet] = createSignal("");
  const [outputItemNameGet, outputItemNameSet] = createSignal("");
  const [outputQuantityGet, outputQuantitySet] = createSignal("");
  const [meInterfaceIdGet, meInterfaceIdSet] = createSignal("");

  const [craftTimesGet, craftTimesSet] = createSignal("");

  function addPattern() {
    const pattern: PatternDto = {
      id: hasString(idGet()) ? Number(idGet()) : undefined,
      patternName: patternNameGet(),
      inputItems: inputItemsGet(),
      outputItem: outputItemNameGet(),
      outputQuantity: Number(outputQuantityGet()),
      meInterfaceId: Number(meInterfaceIdGet()),
    };

    client.patterns
      .createPattern(pattern)
      .then(() => props.onUpdate(Operation.Add, pattern))
      .catch(async (e) => alert(await e.text()));
  }

  function deletePattern() {
    const id = Number(idGet());
    client.patterns
      .deletePattern(id)
      .then(() => props.onUpdate(Operation.Delete, { id } as PatternDto))
      .catch(async (e) => alert(await e.text()));
  }

  function updatePattern() {
    const pattern: PatternDto = {
      id: hasString(idGet()) ? Number(idGet()) : undefined,
      patternName: patternNameGet(),
      inputItems: inputItemsGet(),
      outputItem: outputItemNameGet(),
      outputQuantity: Number(outputQuantityGet()),
      meInterfaceId: Number(meInterfaceIdGet()),
    };

    client.patterns
      .updatePattern(pattern.id!, pattern)
      .then(() => props.onUpdate(Operation.Update, pattern))
      .catch(async (e) => alert(await e.text()));
  }

  function craftItems() {
    const id = Number(idGet());
    const times = hasString(craftTimesGet()) ? Number(craftTimesGet()) : 0;
    client.patterns
      .craftPattern(id, { times: times })
      .then(() => props.onCraft({ id: id } as PatternDto))
      .catch(async (e) => alert(await e.text()));
  }

  return (
    <div class={styles.operations}>
      <div class={styles.operation}>
        <select
          value={operationGet()}
          onChange={(e) => operationSet(parseInt(e.target.value))}
        >
          <option value={Operation.Add}>Add Pattern</option>
          <option value={Operation.Delete}>Delete Pattern</option>
          <option value={Operation.Update}>Update Pattern</option>
        </select>
        <Switch>
          <Match when={operationGet() === Operation.Add}>
            <div class={styles.form}>
              <input
                type="text"
                placeholder="Pattern Name"
                value={patternNameGet()}
                onChange={(e) => patternNameSet(e.target.value)}
              />
              <input
                type="text"
                placeholder="Input Items"
                value={inputItemsGet()}
                onChange={(e) => inputItemsSet(e.target.value)}
              />
              <input
                type="number"
                placeholder="Output Item Name"
                value={outputItemNameGet()}
                onChange={(e) => outputItemNameSet(e.target.value)}
              />
              <input
                type="number"
                placeholder="Output Quantity"
                min="0"
                value={outputQuantityGet()}
                onChange={(e) => outputQuantitySet(e.target.value)}
              />
              <input
                type="number"
                placeholder="ME Interface ID"
                value={meInterfaceIdGet()}
                onChange={(e) => meInterfaceIdSet(e.target.value)}
              />
              <button onClick={addPattern}>Add Pattern</button>
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
              <button onClick={deletePattern}>Delete Pattern</button>
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
                placeholder="New Pattern Name"
                value={patternNameGet()}
                onChange={(e) => patternNameSet(e.target.value)}
              />
              <input
                type="text"
                placeholder="New Input Items"
                value={inputItemsGet()}
                onChange={(e) => inputItemsSet(e.target.value)}
              />
              <input
                type="number"
                placeholder="New Output Item Name"
                value={outputItemNameGet()}
                onChange={(e) => outputItemNameSet(e.target.value)}
              />
              <input
                type="number"
                placeholder="New Output Quantity"
                min="0"
                value={outputQuantityGet()}
                onChange={(e) => outputQuantitySet(e.target.value)}
              />
              <input
                type="number"
                placeholder="New ME Interface ID"
                value={meInterfaceIdGet()}
                onChange={(e) => meInterfaceIdSet(e.target.value)}
              />
              <button onClick={updatePattern}>Update Pattern</button>
            </div>
          </Match>
        </Switch>
      </div>
      <div class={styles.operation}>
        <select value={"Craft Items"}>
          <option value={"Craft Items"}>Craft Items</option>
        </select>
        <div class={styles.form}>
          <input
            type="number"
            placeholder="Pattern ID"
            value={idGet()}
            onChange={(e) => idSet(e.target.value)}
          />
          <input
            type="text"
            placeholder="Craft Times"
            value={craftTimesGet()}
            onChange={(e) => craftTimesSet(e.target.value)}
          />
          <button onClick={craftItems}>Craft Items</button>
        </div>
      </div>
    </div>
  );
};
