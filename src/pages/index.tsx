import { Link } from "react-router";
import LoginPage from "./login";

export default function IndexPage() {
  return (
    <>
      <p>
        You've been logged out. <Link to="/login">Login again</Link>
      </p>
    </>
  );
}
