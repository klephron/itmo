import { Link, useNavigate } from "react-router";
import styles from "./index.module.scss";

export default function NotFound() {
  const navigate = useNavigate();

  const handleGoBack = () => {
    if (window.history.length > 1) {
      navigate(-1);
    } else {
      navigate("/");
    }
  };

  return (
    <>
      <div className={styles.root}>
        <p>
          Not Found 404.{" "}
          <Link onClick={handleGoBack} to={""}>
            Go Back
          </Link>
        </p>
      </div>
    </>
  );
}
