import styles from './index.module.scss';
import { clientUse } from '@state/client';

export default function View() {
  const clientGet = clientUse();

  return (
    <div class={styles.view}>
    TODO: VIEW (123)
    </div>
  );
}
