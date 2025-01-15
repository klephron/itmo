import { createEffect, createSignal, For, onMount } from 'solid-js';
import styles from './ViewPostoffice.module.scss';
import { clientGet } from '@state/client';
import { PostofficesAddMessage } from '@websocket/message';
import { world } from '@state/world';
import { PostofficePack } from '@websocket/pack';
import { subscriptionGet, subscriptionSet } from '@state/subscription';
import { setsEqual } from '@util/util';
import MailTable from './MailTable';

export default function ViewPostoffice() {
  const [getAddCountry, setAddCountry] = createSignal('');
  const [getAddCity, setAddCity] = createSignal('');
  const [getAddPostalCode, setAddPostalCode] = createSignal('');
  const [getAddStreet, setAddStreet] = createSignal('');
  const [getAddHouse, setAddHouse] = createSignal('');
  const [getAddCoordinatesX, setAddCoordinatesX] = createSignal('');
  const [getAddCoordinatesY, setAddCoordinatesY] = createSignal('');

  const [getSelectedIds, setSelectedIds] = createSignal(initSelected());

  createEffect(() => {
    updateSubscription();
  });

  function initSelected() {
    const subscription = subscriptionGet();
    if (
      subscription == null ||
      subscription.args == null ||
      subscription.args.postoffice_ids == null
    ) {
      return new Set<number>();
    }
    return new Set(subscription.args.postoffice_ids);
  }

  function updateSubscription() {
    const subscription = subscriptionGet();
    if (subscription == null) {
      return;
    }
    const selectedIds = getSelectedIds();
    if (!setsEqual(new Set(subscription.args.postoffice_ids), selectedIds)) {
      subscription.args.postoffice_ids = [...selectedIds];
      subscriptionSet({ ...subscription });
    }
  }

  function addSubmit() {
    const client = clientGet();
    if (client == null) {
      return;
    }

    const data: PostofficesAddMessage = {
      args: {
        address: {
          country: getAddCountry(),
          city: getAddCity(),
          postal_code: getAddPostalCode(),
          street: getAddStreet(),
          house: getAddHouse(),
        },
        coordinates: {
          x: Number(getAddCoordinatesX()),
          y: Number(getAddCoordinatesY()),
        },
      },
    };
    client.postofficesAdd(data);
  }

  function addClear() {
    setAddCountry('');
    setAddCity('');
    setAddPostalCode('');
    setAddStreet('');
    setAddHouse('');
    setAddCoordinatesX('');
    setAddCoordinatesY('');
  }

  function toggleEntrySelection(e: PostofficePack) {
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

  function getEntryExtraClass(e: PostofficePack) {
    const selected = getSelectedIds();
    if (selected.has(e.id)) {
      return styles.postoffices_table_body_row_extra + ' ' + styles.visible;
    } else {
      return styles.postoffices_table_body_row_extra;
    }
  }

  function TableEntry(item: PostofficePack) {
    return (
      <>
        <div class={styles.postoffices_table_body_row} onClick={toggleEntrySelection(item)}>
          <div>{item.id}</div>
          <div>{item.terminal_mails_count}</div>
          <div>{item.intermediate_mails_count}</div>
          <div>{item.transports_queued}</div>
          <div>{item.transports_processed}</div>
        </div>
        <div class={getEntryExtraClass(item)}>
          <div>
            <h4>Coordinates</h4>
            {item.coordinates == null ? (
              <div>Loading...</div>
            ) : (
              <div class={styles.coordinates}>
                <div>
                  <span>X</span>
                  <input value={item.coordinates.x}></input>
                </div>
                <div>
                  <span>Y</span>
                  <input value={item.coordinates.y}></input>
                </div>
              </div>
            )}
          </div>
          <div>
            <h4>Address</h4>
            {item.address == null ? (
              <div>Loading...</div>
            ) : (
              <div class={styles.address}>
                <div>
                  <span>Country</span>
                  <input value={item.address.country}></input>
                </div>
                <div>
                  <span>City</span>
                  <input value={item.address.city}></input>
                </div>
                <div>
                  <span>Postal Code</span>
                  <input value={item.address.postal_code}></input>
                </div>
                <div>
                  <span>Street</span>
                  <input value={item.address.street}></input>
                </div>
                <div>
                  <span>House</span>
                  <input value={item.address.house}></input>
                </div>
              </div>
            )}
          </div>
          <div>
            <h4>Itermediate Mails</h4>
            {item.intermediate_mails == null ? (
              <div>Loading...</div>
            ) : (
              <MailTable data={item.intermediate_mails} />
            )}
          </div>
          <div>
            <h4>Terminal Mails</h4>
            {item.terminal_mails == null ? (
              <div>Loading...</div>
            ) : (
              <MailTable data={item.terminal_mails} />
            )}
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <div class={styles.postoffices}>
        <div class={styles.postoffices_table}>
          <div class={styles.postoffices_table_header}>
            <div>ID</div>
            <div>Terminal Mails Count</div>
            <div>Intermediate Mails Count</div>
            <div>Transports Queued</div>
            <div>Transports Processed</div>
          </div>
          <div class={styles.postoffices_table_body}>
            <For each={world.postoffices} fallback={<div>Loading...</div>}>
              {(item, _) => TableEntry(item)}
            </For>
          </div>
        </div>
        <div class={styles.postoffices_operations}>
          <h4>Operations</h4>
          <div class={styles.add}>
            <h4>Add postoffice</h4>
            <div>
              <input
                value={getAddCountry()}
                onChange={(e) => setAddCountry(e.target.value)}
                placeholder="Country"
              />
            </div>
            <div>
              <input
                value={getAddCity()}
                onChange={(e) => setAddCity(e.target.value)}
                placeholder="City"
              />
            </div>
            <div>
              <input
                value={getAddPostalCode()}
                onChange={(e) => setAddPostalCode(e.target.value)}
                placeholder="Postal code"
              />
            </div>
            <div>
              <input
                value={getAddStreet()}
                onChange={(e) => setAddStreet(e.target.value)}
                placeholder="Street"
              />
            </div>
            <div>
              <input
                value={getAddHouse()}
                onChange={(e) => setAddHouse(e.target.value)}
                placeholder="House"
              />
            </div>
            <div>
              <span>Coordinates</span>
              <div>
                <input
                  value={getAddCoordinatesX()}
                  onChange={(e) => setAddCoordinatesX(e.target.value)}
                  placeholder="X"
                />
              </div>
              <div>
                <input
                  value={getAddCoordinatesY()}
                  onChange={(e) => setAddCoordinatesY(e.target.value)}
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
