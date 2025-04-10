import { DriveEntityDto } from "@api/generated/data-contracts";
import { Operation } from "./kind";
import styles from "./App.module.scss";
import { createSignal, Match, Switch } from "solid-js";
import { clientGet } from "@state/client";
import { hasString } from "./util";

export const DriveOperations = (props: {
  onUpdate: (op: Operation, drive: DriveEntityDto) => void;
}) => {
  const client = clientGet();

  const [operationGet, operationSet] = createSignal(Operation.Add);
  const [idGet, idSet] = createSignal("");
  const [driveNameGet, driveNameSet] = createSignal("");
  const [networkIdGet, networkIdSet] = createSignal("");

  function addDrive() {
    const drive: DriveEntityDto = {
      id: hasString(idGet()) ? Number(idGet()) : undefined,
      driveName: driveNameGet(),
      channelsUsed: 0,
      networkId: Number(networkIdGet()),
    };

    client.drives
      .createDrive({ networkId: drive.networkId }, drive)
      .then(() => {
        props.onUpdate(Operation.Add, drive);
      });
  }

  function deleteDrive() {
    const id = Number(idGet());
    client.drives.deleteDrive(id).then(() => {
      props.onUpdate(Operation.Delete, { id } as DriveEntityDto);
    });
  }

  function updateDrive() {
    const drive: DriveEntityDto = {
      id: hasString(idGet()) ? Number(idGet()) : undefined,
      driveName: driveNameGet(),
      channelsUsed: 0,
      networkId: 0,
    };

    client.drives.updateDrive(drive.id!, drive).then(() => {
      props.onUpdate(Operation.Update, drive);
    });
  }

  return (
    <div class={styles.operations}>
      <div class={styles.operation}>
        <select
          value={operationGet()}
          onChange={(e) => operationSet(parseInt(e.target.value))}
        >
          <option value={Operation.Add}>Add Drive</option>
          <option value={Operation.Delete}>Delete Drive</option>
          <option value={Operation.Update}>Update Drive</option>
        </select>
        <Switch>
          <Match when={operationGet() === Operation.Add}>
            <div class={styles.form}>
              <input
                type="text"
                placeholder="Drive Name"
                value={driveNameGet()}
                onChange={(e) => driveNameSet(e.target.value)}
              />
              <input
                type="number"
                placeholder="Network ID"
                value={networkIdGet()}
                onChange={(e) => networkIdSet(e.target.value)}
              />
              <button onClick={addDrive}>Add Drive</button>
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
              <button onClick={deleteDrive}>Delete Drive</button>
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
                placeholder="New Drive Name"
                value={driveNameGet()}
                onChange={(e) => driveNameSet(e.target.value)}
              />
              <button onClick={updateDrive}>Update Drive</button>
            </div>
          </Match>
        </Switch>
      </div>
    </div>
  );
};
