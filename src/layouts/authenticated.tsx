import LoadingPage from "@pages/loading";
import { useToken } from "@state/token";
import { useEffect, useState } from "react";
import { Outlet, useNavigate } from "react-router";

export default function AuthenticatedLayout() {
  const navigate = useNavigate();
  const token = useToken((state) => state.token);
  const [isAuth, setIsAuth] = useState(false);

  useEffect(() => {
    if (token) {
      let tokenExpTime = token.issueAtMS + token.expiresInMS;
      let nowTime = Date.now();

      if (nowTime > tokenExpTime) {
        navigate("/refresh");
      } else {
        setIsAuth(true);
      }
    } else {
      navigate("/login");
    }
  }, [token]);

  return <>{isAuth ? <Outlet /> : <LoadingPage />}</>;
}
