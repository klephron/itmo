import styles from './index.module.scss';

import Log from './Log';
import Map from './Map';
import View from './View';

const DEFAULT_SERVER_URL: string = 'ws://localhost:8088/ws';
const DEFAULT_UPS: number = 1;

export default function App() {
  return (
    <>
      <h1>Mail Simulation</h1>
      <div class={styles.outer}>
        <div class={styles.top}>
          <Map />
          <Log />
        </div>
        <View ups={DEFAULT_UPS} serverUrl={DEFAULT_SERVER_URL} />
      </div>
    </>
  );
}
