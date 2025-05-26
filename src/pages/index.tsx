import { useUser } from "@state/user";

import styles from "./index.module.scss";

export default function IndexPage() {
  const user = useUser((state) => state.user);
  return (
    <>
      <div className={styles.root}>
        <span>
          Index page for user: {user.username} with permissions: {JSON.stringify(user.permissions)}
        </span>
      </div>
    </>
  );
}
