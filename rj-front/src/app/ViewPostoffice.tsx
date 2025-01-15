import { createSignal, For } from 'solid-js';
import styles from './index.module.scss';
import { clientGet } from '@state/client';
import { PostofficesAddMessage } from '@websocket/message';

export default function ViewPostoffice() {
  const [getAddCountry, setAddCountry] = createSignal('');
  const [getAddCity, setAddCity] = createSignal('');
  const [getAddPostalCode, setAddPostalCode] = createSignal('');
  const [getAddStreet, setAddStreet] = createSignal('');
  const [getAddHouse, setAddHouse] = createSignal('');
  const [getAddCoordinatesX, setAddCoordinatesX] = createSignal('0');
  const [getAddCoordinatesY, setAddCoordinatesY] = createSignal('0');

  function addSubmit() {
    if (clientGet() == null) {
      return;
    }

    const client = clientGet()!;
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
    setAddCoordinatesX('0');
    setAddCoordinatesY('0');
  }

  return (
    <>
      <div class={styles.postoffices}>
        <div class={styles.table}>
          <div class={styles.table_header}>
            <div>ID</div>
            <div>Terminal Mails Count</div>
            <div>Intermediate Mails Count</div>
            <div>Transports Queued</div>
            <div>Transports Processed</div>
          </div>
          <div class={styles.table_body}>
            <div class={styles.table_body_row}>
              <div>ID</div>
              <div>Terminal Mails Count</div>
              <div>Intermediate Mails Count</div>
              <div>Transports Queued</div>
              <div>Transports Processed</div>
            </div>
            <div class={styles.table_body_row_extra}>Lalalala</div>
            <div class={styles.table_body_row}>
              <div>ID</div>
              <div>Terminal Mails Count</div>
              <div>Intermediate Mails Count</div>
              <div>Transports Queued</div>
              <div>Transports Processed</div>
            </div>
            <div class={styles.table_body_row_extra}>Lalalala</div>
          </div>
        </div>
        <div class={styles.operations}>
          <h4>Operations</h4>
          <div class={styles.add}>
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
