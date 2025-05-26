import { Link } from "react-router";

import styles from "./refresh.module.scss";

export default function RefreshPage() {
  return (
    <>
      <div className={styles.root}>
        <div>
          <p>
            You've been logged out. <Link to="/login">Login again</Link>
          </p>
        </div>
      </div>
    </>
  );
}
