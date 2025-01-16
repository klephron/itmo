import { clientGet } from '@state/client';
import { subscriptionGet, subscriptionSet } from '@state/subscription';
import { setsEqual } from '@util/util';
import {
  TransportsAddMessage,
  TransportSetRouteMessage,
  TransportStartMessage,
  TransportStopMessage,
} from '@websocket/message';
import { RoutePack, TransportPack } from '@websocket/pack';
import { createEffect, createSignal, For } from 'solid-js';
import styles from './ViewTransport.module.scss';
import { world } from '@state/world';
import MailTable from './MailTable';
import PostalAddressTable from './PostalAddressTable';

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
      return styles.transports_table_body_row_extra + ' ' + styles.visible;
    } else {
      return styles.transports_table_body_row_extra;
    }
  }

  function TableEntrySetRoute({ item }: { item: TransportPack }) {
    const [getRoute, setRoute] = createSignal('');
    const [getIsCyclic, setIsCyclic] = createSignal(true);

    function setRouteSubmit() {
      const client = clientGet();
      if (client == null) {
        return;
      }
      const data: TransportSetRouteMessage = {
        args: {
          transport_id: item.id,
          route: {
            postoffice_ids: getRoute()
              .split(',')
              .map((e) => Number(e)),
            is_cyclic: getIsCyclic(),
          },
        },
      };
      client.transportSetRoute(data);
    }

    function setRouteClear() {
      setRoute('');
      setIsCyclic(true);
    }

    return (
      <div class={styles.set_route}>
        <h4>Set Route</h4>
        <div class={styles.set_route_body}>
          <div>
            <span>Postoffices (ID list)</span>
            <input
              value={getRoute()}
              onChange={(e) => setRoute(e.target.value)}
              placeholder="ex. 1,2,3"
            ></input>
          </div>
          <div>
            <span>Is Cyclic</span>
            <select
              value={getIsCyclic() ? 'true' : 'false'}
              onChange={(e) => {
                setIsCyclic(e.target.value === 'true');
              }}
            >
              <option value="true">True</option>
              <option value="false">False</option>
            </select>
          </div>
        </div>
        <div class={styles.set_route_buttons}>
          <button onClick={setRouteSubmit}>Submit</button>
          <button onClick={setRouteClear}>Clear</button>
        </div>
      </div>
    );
  }

  function TableEntryStart({ item }: { item: TransportPack }) {
    const [getIndex, setIndex] = createSignal('');

    function setRouteSubmit() {
      const client = clientGet();
      if (client == null) {
        return;
      }
      const data: TransportStartMessage = {
        args: {
          transport_id: item.id,
          index: Number(getIndex()),
        },
      };
      client.transportStart(data);
    }

    function setRouteClear() {
      setIndex('');
    }

    return (
      <div class={styles.start}>
        <h4>Start</h4>
        <div class={styles.start_body}>
          <div>
            <span>Start Index</span>
            <input
              value={getIndex()}
              onChange={(e) => setIndex(e.target.value)}
              placeholder="0"
            ></input>
          </div>
        </div>
        <div class={styles.start_buttons}>
          <button onClick={setRouteSubmit}>Submit</button>
          <button onClick={setRouteClear}>Clear</button>
        </div>
      </div>
    );
  }

  function TableEntryStop({ item }: { item: TransportPack }) {
    function setRouteSubmit() {
      const client = clientGet();
      if (client == null) {
        return;
      }
      const data: TransportStopMessage = {
        args: {
          transport_id: item.id,
        },
      };
      client.transportStop(data);
    }

    return (
      <div class={styles.stop}>
        <h4>Close</h4>
        <div class={styles.stop_buttons}>
          <button onClick={setRouteSubmit}>Submit</button>
        </div>
      </div>
    );
  }

  function TableEntry(item: TransportPack) {
    return (
      <>
        <div class={styles.transports_table_body_row} onClick={toggleEntrySelection(item)}>
          <div>{item.id}</div>
          <div>{item.coordinates.x}</div>
          <div>{item.coordinates.y}</div>
          <div>{item.at_postoffice_id ?? '-'}</div>
          <div>{item.mails_count}</div>
        </div>
        <div class={getEntryExtraClass(item)}>
          <div class={styles.operations}>
            <h4>Operations</h4>
            <div class={styles.operations_entries}>
              <TableEntrySetRoute item={item} />
              <TableEntryStart item={item} />
              <TableEntryStop item={item} />
            </div>
          </div>
          <div>
            <h4>Parameters</h4>
            <div class={styles.parameters}>
              <div>
                <span>Capacity</span>
                <input value={item.capacity}></input>
              </div>
              <div>
                <span>Speed</span>
                <input value={item.speed}></input>
              </div>
              <div>
                <span>Operations per second</span>
                <input value={item.speed}></input>
              </div>
            </div>
          </div>
          <div>
            <h4>Personal</h4>
            <div class={styles.personal}>
              <div>
                <span>License plate</span>
                <input value={item.license_plate}></input>
              </div>
            </div>
          </div>
          <div>
            <h4>Route</h4>
            <div class={styles.route}>
              <div>
                <span>Route Postoffice IDs</span>
                <input
                  value={
                    item.route == null || item.route.postoffice_ids == null
                      ? '-'
                      : item.route.postoffice_ids.join(',')
                  }
                ></input>
              </div>
              <div>
                <span>Is Cyclic</span>
                <input
                  value={item.route == null ? '-' : item.route.is_cyclic ? 'true' : 'false'}
                ></input>
              </div>
            </div>
          </div>
          <div>
            <h4>Loading Addresses</h4>
            {item.loading_addresses == null ? (
              <div>Loading...</div>
            ) : (
              <PostalAddressTable data={item.loading_addresses} />
            )}
          </div>
          <div>
            <h4>Delivery Addresses</h4>
            {item.delivery_addresses == null ? (
              <div>Loading...</div>
            ) : (
              <PostalAddressTable data={item.delivery_addresses} />
            )}
          </div>
          <div>
            <h4>Mails</h4>
            {item.mails == null ? <div>Loading...</div> : <MailTable data={item.mails} />}
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <div class={styles.transports}>
        <div class={styles.transports_table}>
          <div class={styles.transports_table_header}>
            <div>ID</div>
            <div>Coordinate X</div>
            <div>Coordinate Y</div>
            <div>At PostOffice Id</div>
            <div>Mails Count</div>
          </div>
          <div class={styles.transports_table_body}>
            <For each={world.transports} fallback={<div>Loading...</div>}>
              {(item, _) => TableEntry(item)}
            </For>
          </div>
        </div>
        <div class={styles.transports_operations}>
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
