import { Link } from "react-router";
import clsx from "clsx";
import styles from "./index.module.scss";

export default function Logo({ className }: { className: string }) {
  return (
    <Link to="/" className={clsx(styles.logo, className)}>
      <h2>ITMO PAI</h2>
    </Link>
  );
}
