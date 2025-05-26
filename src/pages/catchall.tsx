import { Link } from "react-router";

import styles from "./catchall.module.scss";

export default function NotFound() {
  return (
    <>
      <div className={styles.root}>
        <p>
          Not Found 404. Return back to <Link to="/login">Index Page</Link>
        </p>
      </div>
    </>
  );
}
