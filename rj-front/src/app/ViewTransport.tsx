import { clientGet } from '@state/client';
import { subscriptionGet, subscriptionSet } from '@state/subscription';
import { setsEqual } from '@util/util';
import { TransportsAddMessage } from '@websocket/message';
import { TransportPack } from '@websocket/pack';
import { createEffect, createSignal, For } from 'solid-js';
import styles from './ViewTransport.module.scss';
import { world } from '@state/world';

export default function ViewTransport() {
  const [getAddCapacity, setAddCapacity] = createSignal('');
  const [getAddSpeed, setAddSpeed] = createSignal('');
  const [getAddOps, setAddOps] = createSignal('');
  const [getAddLicensePlate, setAddLicensePlate] = createSignal('');
  const [getAddCoordinateX, setAddCoordinateX] = createSignal('');
  const [getAddCoordinateY, setAddCoordinateY] = createSignal('');

  const [getSelectedIds, setSelectedIds] = createSignal(initSelected());

  createEffect(() => {
    updateSubscription();
  });

  function initSelected() {
    const subscription = subscriptionGet();
    if (
      subscription == null ||
      subscription.args == null ||
      subscription.args.transport_ids == null
    ) {
      return new Set<number>();
    }
    return new Set(subscription.args.transport_ids);
  }

  function updateSubscription() {
    const subscription = subscriptionGet();
    if (subscription == null) {
      return;
    }
    const selectedIds = getSelectedIds();
    if (!setsEqual(new Set(subscription.args.transport_ids), selectedIds)) {
      subscription.args.transport_ids = [...selectedIds];
      subscriptionSet({ ...subscription });
    }
  }

  function addSubmit() {
    const client = clientGet();
    if (client == null) {
      return;
    }

    const data: TransportsAddMessage = {
      args: {
        capacity: Number(getAddCapacity()),
        speed: Number(getAddSpeed()),
        ops: Number(getAddOps()),
        license_plate: getAddLicensePlate(),
        coordinates: {
          x: Number(getAddCoordinateX()),
          y: Number(getAddCoordinateY()),
        },
      },
    };

    client.transportsAdd(data);
  }

  function addClear() {
    setAddCapacity('');
    setAddSpeed('');
    setAddOps('');
    setAddLicensePlate('');
    setAddCoordinateX('');
    setAddCoordinateY('');
  }

  function toggleEntrySelection(e: TransportPack) {
    return () => {
      const selected = getSelectedIds();
      if (selected.has(e.id)) {
        selected.delete(e.id);
      } else {
        selected.add(e.id);
      }
      setSelectedIds(new Set(selected));
    };
  }

  function getEntryExtraClass(e: TransportPack) {
    const selected = getSelectedIds();
    if (selected.has(e.id)) {
      return styles.table_body_row_extra + ' ' + styles.visible;
    } else {
      return styles.table_body_row_extra;
    }
  }

  function tableEntry(item: TransportPack) {
    return (
      <>
        <div class={styles.table_body_row} onClick={toggleEntrySelection(item)}>
          <div>{item.id}</div>
          <div>{item.coordinates.x}</div>
          <div>{item.coordinates.y}</div>
          <div>{item.at_postoffice_id}</div>
          <div>{item.mails_count}</div>
        </div>
        <div class={getEntryExtraClass(item)}>
          <div>Extra</div>
        </div>
      </>
    );
  }

  return (
    <>
      <div class={styles.transports}>
        <div class={styles.table}>
          <div class={styles.table_header}>
            <div>ID</div>
            <div>Coordinate X</div>
            <div>Coordinate Y</div>
            <div>At PostOffice Id</div>
            <div>Mails Count</div>
          </div>
          <div class={styles.table_body}>
            <For each={world.transports} fallback={<div>Loading...</div>}>
              {(item, _) => tableEntry(item)}
            </For>
          </div>
        </div>
        <div class={styles.operations}>
          <h4>Operations</h4>
          <div class={styles.add}>
            <h4>Add transport</h4>
            <div>
              <input
                value={getAddCapacity()}
                onChange={(e) => setAddCapacity(e.target.value)}
                placeholder="Capacity"
              />
            </div>
            <div>
              <input
                value={getAddSpeed()}
                onChange={(e) => setAddSpeed(e.target.value)}
                placeholder="Speed"
              />
            </div>
            <div>
              <input
                value={getAddOps()}
                onChange={(e) => setAddOps(e.target.value)}
                placeholder="Operations per second"
              />
            </div>
            <div>
              <input
                value={getAddLicensePlate()}
                onChange={(e) => setAddLicensePlate(e.target.value)}
                placeholder="Country"
              />
            </div>
            <div>
              <span>Coordinates</span>
              <div>
                <input
                  value={getAddCoordinateX()}
                  onChange={(e) => setAddCoordinateX(e.target.value)}
                  placeholder="X"
                />
              </div>
              <div>
                <input
                  value={getAddCoordinateY()}
                  onChange={(e) => setAddCoordinateY(e.target.value)}
                  placeholder="Y"
                />
              </div>
            </div>
            <button onClick={addSubmit}>Submit</button>
            <button onClick={addClear}>Clear</button>
          </div>
        </div>
      </div>
    </>
  );
}
