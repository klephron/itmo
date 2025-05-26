import { useNavigate } from "react-router";

export default function IndexPage() {
  const navigate = useNavigate();

  // TODO: handle smarter
  navigate("/incidents");

  return <></>;
}
