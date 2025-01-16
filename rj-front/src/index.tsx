import { render } from 'solid-js/web';

import App from '@app/App';
import "./index.scss"

const root = document.getElementById('root');

if (root !== null) {
  render(() => <App />, root);
}
