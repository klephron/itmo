import { Link } from "react-router";

export default function RefreshPage() {
  return (
    <>
      <p>
        You've been logged out. <Link to="/login">Login again</Link>
      </p>
    </>
  );
}
