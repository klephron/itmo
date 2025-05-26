import { Link } from "react-router";

import styles from "./logo.module.scss";
import clsx from "clsx";

export default function Logo({ className }: { className: string }) {
  return (
    <Link to="/" className={clsx(styles.logo, className)}>
      <h2>ITMO PAI</h2>
    </Link>
  );
}
