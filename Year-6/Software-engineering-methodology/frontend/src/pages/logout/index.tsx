import { useToken } from "@state/token";
import { useUser } from "@state/user";
import { useEffect } from "react";
import { useNavigate } from "react-router";

export default function LogoutPage() {
  const navigate = useNavigate();

  const userReset = useUser((state) => state.reset);
  const tokenReset = useToken((state) => state.reset);

  useEffect(() => {
    tokenReset();
    userReset();
    navigate("/login");
  }, []);

  return <></>;
}
