import { createSignal } from 'solid-js';
import styles from './index.module.scss';
import { clientGet } from '@state/client';
import { PostofficesAddMessage } from '@websocket/message';

export default function ViewPostoffice() {
  const [getAddCountry, setAddCountry] = createSignal('');
  const [getAddCity, setAddCity] = createSignal('');
  const [getAddPostalCode, setAddPostalCode] = createSignal('');
  const [getAddStreet, setAddStreet] = createSignal('');
  const [getAddHouse, setAddHouse] = createSignal('');
  const [getAddCoordinatesX, setAddCoordinatesX] = createSignal('');
  const [getAddCoordinatesY, setAddCoordinatesY] = createSignal('');

  function addPostOffice() {
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
  return (
    <>
      <div class={styles.view_postoffices}>
        <div class={styles.info}>Something is here</div>
        <div class={styles.operations}>
          <h4>Operations</h4>
          <div class={styles.add}>
            <div>
              <input placeholder="Country" />
            </div>
            <div>
              <input placeholder="City" />
            </div>
            <div>
              <input placeholder="Postal code" />
            </div>
            <div>
              <input placeholder="Street" />
            </div>
            <div>
              <input placeholder="House" />
            </div>
            <div>
              <span>Coordinates</span>
              <div>
                <input placeholder="X" />
              </div>
              <div>
                <input placeholder="Y" />
              </div>
            </div>
            <button onClick={addPostOffice}>Add post office</button>
          </div>
        </div>
      </div>
    </>
  );
}
