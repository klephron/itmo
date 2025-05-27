import clsx from "clsx";
import styles from "./index.module.scss";

export default function Header({ className }: { className?: string }) {
  return (
    <>
      <div className={clsx(styles.header, className)}>
        <div className={styles.element}>
          <span className={styles.runningtext}>Здесь могла быть ваша реклама</span>
        </div>
      </div>
    </>
  );
}
